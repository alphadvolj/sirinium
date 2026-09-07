package com.dlab.sirinium.data.repository

import android.content.Context
import android.util.Log
import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.local.dao.LessonNoteDao
import com.dlab.sirinium.data.local.dao.ScheduleDao
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.ScheduleItemDto
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.data.remote.dto.toEntity
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.ScheduleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

class ScheduleRepositoryImpl(
    private val context: Context,
    private val api: SiriusScheduleApi,
    private val scheduleDao: ScheduleDao,
    private val lessonNoteDao: LessonNoteDao
) : ScheduleRepository {

    private val entityPrefs by lazy {
        context.getSharedPreferences("sirinium_entities_cache", Context.MODE_PRIVATE)
    }

    private var cachedGroups: List<String> = emptyList()
    private var cachedTeachers: List<String> = emptyList()
    private var cachedClassrooms: List<String> = emptyList()

    init {
        try {
            cachedGroups = entityPrefs.getString("cached_groups", null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
            cachedTeachers = entityPrefs.getString("cached_teachers", null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
            cachedClassrooms = entityPrefs.getString("cached_classrooms", null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        } catch (_: Exception) {}
    }

    // SharedPreferences for persisting last successful update timestamps
    private val syncPrefs by lazy {
        context.getSharedPreferences("sirinium_sync_prefs", Context.MODE_PRIVATE)
    }

    private val lastUpdateFlows = ConcurrentHashMap<String, MutableStateFlow<Long>>()

    override fun getLastUpdateTime(target: String): Long {
        if (target.isBlank()) return 0L
        return syncPrefs.getLong("last_update_$target", 0L)
    }

    override fun observeLastUpdateTime(target: String): Flow<Long> {
        return lastUpdateFlows.computeIfAbsent(target) {
            MutableStateFlow(getLastUpdateTime(target))
        }.asStateFlow()
    }

    private fun recordLastUpdateTime(target: String) {
        if (target.isBlank()) return
        val now = System.currentTimeMillis()
        syncPrefs.edit().putLong("last_update_$target", now).apply()
        lastUpdateFlows.computeIfAbsent(target) { MutableStateFlow(now) }.value = now
    }

    // Thread-safe tracking of loaded week offsets for each target
    private val loadedWeeksByTarget = ConcurrentHashMap<String, MutableSet<Int>>()

    override fun isWeekLoaded(target: String, weekOffset: Int): Boolean {
        if (target.isBlank()) return false
        return loadedWeeksByTarget[target]?.contains(weekOffset) == true
    }

    override fun markWeekLoaded(target: String, weekOffset: Int) {
        if (target.isBlank()) return
        loadedWeeksByTarget.computeIfAbsent(target) { ConcurrentHashMap.newKeySet() }.add(weekOffset)
    }

    private suspend fun fetchWeekScheduleFromNetwork(
        target: String,
        sectionType: String,
        weekOffset: Int
    ): List<ScheduleItemDto> {
        return try {
            when (sectionType) {
                "group" -> api.getGroupSchedule(groupName = target, weekOffset = weekOffset)
                "teacher" -> api.getTeacherSchedule(teacher = target, weekOffset = weekOffset)
                "classroom" -> api.getClassroomSchedule(classroomName = target, weekOffset = weekOffset)
                else -> api.getSchedule(
                    sectionType = sectionType,
                    weekOffset = weekOffset,
                    group = if (sectionType == "group") target else null,
                    teacher = if (sectionType == "teacher") target else null,
                    classroom = if (sectionType == "classroom") target else null
                )
            }
        } catch (e: Exception) {
            // Fallback to query-parameter endpoints or general schedule query if path request fails
            try {
                when (sectionType) {
                    "group" -> api.getGroupScheduleByQuery(group = target, weekOffset = weekOffset)
                    "teacher" -> api.getTeacherScheduleByQuery(teacher = target, weekOffset = weekOffset)
                    "classroom" -> api.getClassroomScheduleByQuery(classroom = target, weekOffset = weekOffset)
                    else -> api.getSchedule(
                        sectionType = sectionType,
                        weekOffset = weekOffset,
                        group = if (sectionType == "group") target else null,
                        teacher = if (sectionType == "teacher") target else null,
                        classroom = if (sectionType == "classroom") target else null
                    )
                }
            } catch (_: Exception) {
                throw e
            }
        }
    }

    override suspend fun loadWeekSchedule(
        target: String,
        sectionType: String,
        weekOffset: Int
    ): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        return try {
            val networkItems = fetchWeekScheduleFromNetwork(target, sectionType, weekOffset)
            val entities = networkItems
                .map {
                    it.toEntity(
                        fallbackTarget = target,
                        forcedTarget = target,
                        forcedSectionType = sectionType
                    )
                }
                .distinctBy { it.id }

            val weekDates = DateTimeUtils.getWeekDatesForOffset(weekOffset)
            scheduleDao.replaceScheduleForDates(target, weekDates, entities)
            markWeekLoaded(target, weekOffset)
            recordLastUpdateTime(target)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Failed to load schedule for target=$target, weekOffset=$weekOffset", e)
            Resource.Error(e.localizedMessage ?: "Ошибка загрузки недели $weekOffset", e)
        }
    }

    override suspend fun preloadSchedule(target: String, sectionType: String): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        return coroutineScope {
            // 1. Load current week first so user sees data immediately
            val week0Result = loadWeekSchedule(target, sectionType, 0)

            // 2. Preload adjacent weeks (1, -1, 2, -2, 3, -3) in parallel and batch-save to Room
            val otherOffsets = listOf(1, -1, 2, -2, 3, -3)
            val deferreds = otherOffsets.map { offset ->
                async {
                    try {
                        val networkItems = fetchWeekScheduleFromNetwork(target, sectionType, offset)
                        val entities = networkItems
                            .map {
                                it.toEntity(
                                    fallbackTarget = target,
                                    forcedTarget = target,
                                    forcedSectionType = sectionType
                                )
                            }
                            .distinctBy { it.id }
                        val weekDates = DateTimeUtils.getWeekDatesForOffset(offset)
                        Triple(offset, weekDates, entities)
                    } catch (e: Exception) {
                        Log.w("ScheduleRepo", "Failed to load adjacent week $offset for $target", e)
                        null
                    }
                }
            }
            val results = deferreds.awaitAll().filterNotNull()
            if (results.isNotEmpty()) {
                val allDates = results.flatMap { it.second }.distinct()
                val allEntities = results.flatMap { it.third }.distinctBy { it.id }
                scheduleDao.replaceScheduleForDates(target, allDates, allEntities)
                results.forEach { markWeekLoaded(target, it.first) }
                recordLastUpdateTime(target)
            }

            if (week0Result is Resource.Error && results.isEmpty()) {
                week0Result
            } else {
                recordLastUpdateTime(target)
                Resource.Success(Unit)
            }
        }
    }

    override fun getSchedule(filter: ScheduleFilter): Flow<Resource<List<Lesson>>> = flow {
        emit(Resource.Loading)

        if (filter.target.isNotBlank()) {
            coroutineScope {
                // Preload current week, 3 following weeks, and 3 previous weeks in background
                launch {
                    try {
                        preloadSchedule(filter.target, filter.sectionType)
                    } catch (e: Exception) {
                        Log.e("ScheduleRepo", "Preload failed, falling back to cache", e)
                    }
                }

                // Room is the single source of truth - emit cached data immediately and on any db update
                scheduleDao.getAllScheduleForTarget(filter.target)
                    .map { entities ->
                        entities.map { it.toDomain() }
                    }
                    .collect { domainLessons ->
                        emit(Resource.Success(domainLessons))
                    }
            }
        } else {
            emit(Resource.Success(emptyList()))
        }
    }.catch { e ->
        emit(Resource.Error("Ошибка загрузки расписания: ${e.localizedMessage ?: "Неизвестная ошибка"}", e))
    }

    override suspend fun refreshSchedule(
        target: String,
        sectionType: String,
        date: String?
    ): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        // Clear in-memory loaded weeks tracking for this target
        loadedWeeksByTarget.remove(target)
        return try {
            // Re-preload the window (-3..3)
            val preloadResult = preloadSchedule(target, sectionType)

            // If a specific date outside -3..3 was requested, also refresh that week
            if (!date.isNullOrBlank()) {
                val parsed = DateTimeUtils.parseDate(date)
                if (parsed != null) {
                    val offset = DateTimeUtils.calculateWeekOffset(parsed)
                    if (offset < -3 || offset > 3) {
                        loadWeekSchedule(target, sectionType, offset)
                    }
                }
            }
            preloadResult
        } catch (e: Exception) {
            Log.e("ScheduleRepo", "Failed to refresh schedule", e)
            Resource.Error(e.localizedMessage ?: "Ошибка сети при обновлении", e)
        }
    }

    override suspend fun getGroups(query: String?): Resource<List<String>> {
        if (!query.isNullOrBlank()) {
            return try {
                val groups = api.getGroups(query).filter { it.isNotBlank() }.distinct().sorted()
                Resource.Success(groups)
            } catch (e: Exception) {
                val local = cachedGroups.filter { it.contains(query, ignoreCase = true) }
                if (local.isNotEmpty()) Resource.Success(local)
                else Resource.Error("Не удалось найти группы", e)
            }
        }

        return try {
            val groups = api.getGroups(null).filter { it.isNotBlank() }.distinct().sorted()
            if (groups.isNotEmpty()) {
                cachedGroups = groups
                entityPrefs.edit().putString("cached_groups", groups.joinToString("\n")).apply()
            }
            Resource.Success(groups)
        } catch (e: Exception) {
            if (cachedGroups.isNotEmpty()) {
                Resource.Success(cachedGroups)
            } else {
                val cached = scheduleDao.getDistinctGroups()
                if (cached.isNotEmpty()) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Не удалось получить список групп", e)
                }
            }
        }
    }

    override suspend fun getTeachers(query: String?): Resource<List<String>> {
        if (!query.isNullOrBlank()) {
            return try {
                val teachers = api.getTeachers(query)
                    .map { it.displayName.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                Resource.Success(teachers)
            } catch (e: Exception) {
                val local = cachedTeachers.filter { it.contains(query, ignoreCase = true) }
                if (local.isNotEmpty()) Resource.Success(local)
                else Resource.Error("Не удалось найти преподавателей", e)
            }
        }

        return try {
            val teachers = api.getTeachers(null)
                .map { it.displayName.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            if (teachers.isNotEmpty()) {
                cachedTeachers = teachers
                entityPrefs.edit().putString("cached_teachers", teachers.joinToString("\n")).apply()
            }
            Resource.Success(teachers)
        } catch (e: Exception) {
            if (cachedTeachers.isNotEmpty()) {
                Resource.Success(cachedTeachers)
            } else {
                val cached = scheduleDao.getDistinctTeachers()
                if (cached.isNotEmpty()) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Не удалось получить список преподавателей", e)
                }
            }
        }
    }

    override suspend fun getClassrooms(query: String?): Resource<List<String>> {
        if (!query.isNullOrBlank()) {
            return try {
                val classrooms = api.getClassrooms(query).filter { it.isNotBlank() }.distinct().sorted()
                Resource.Success(classrooms)
            } catch (e: Exception) {
                val local = cachedClassrooms.filter { it.contains(query, ignoreCase = true) }
                if (local.isNotEmpty()) Resource.Success(local)
                else Resource.Error("Не удалось найти аудитории", e)
            }
        }

        return try {
            val classrooms = api.getClassrooms(null).filter { it.isNotBlank() }.distinct().sorted()
            if (classrooms.isNotEmpty()) {
                cachedClassrooms = classrooms
                entityPrefs.edit().putString("cached_classrooms", classrooms.joinToString("\n")).apply()
            }
            Resource.Success(classrooms)
        } catch (e: Exception) {
            if (cachedClassrooms.isNotEmpty()) {
                Resource.Success(cachedClassrooms)
            } else {
                val cached = scheduleDao.getDistinctClassrooms()
                if (cached.isNotEmpty()) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Не удалось получить список аудиторий", e)
                }
            }
        }
    }

    override suspend fun getUpcomingLessons(target: String, limit: Int): List<Lesson> {
        val now = LocalDate.now().format(DateTimeUtils.DATE_FORMATTER)
        val timeNow = LocalTime.now().format(DateTimeUtils.TIME_FORMATTER)
        val entities = scheduleDao.getUpcomingLessons(target, now, timeNow, limit)
        return entities.map { it.toDomain() }
    }

    override suspend fun getLessonsForGroup(groupName: String): List<Lesson> {
        if (groupName.isBlank()) return emptyList()
        return scheduleDao.getLessonsForGroup(groupName).map { it.toDomain() }
    }
}
