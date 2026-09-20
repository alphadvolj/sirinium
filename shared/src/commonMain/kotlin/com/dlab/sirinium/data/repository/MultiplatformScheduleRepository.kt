package com.dlab.sirinium.data.repository

import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.platform.PlatformSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class MultiplatformScheduleRepository(
    private val api: SiriusScheduleApi,
    private val settings: PlatformSettings,
    private val noteRepository: LessonNoteRepository? = null
) : ScheduleRepository {

    private val cachedLessonsMap = mutableMapOf<String, MutableList<Lesson>>()
    private val loadedWeeks = mutableMapOf<String, MutableSet<Int>>()
    private val lastUpdateFlows = mutableMapOf<String, MutableStateFlow<Long>>()

    private var cachedGroups: List<String> = emptyList()
    private var cachedTeachers: List<String> = emptyList()
    private var cachedClassrooms: List<String> = emptyList()

    init {
        try {
            val groupsStr = settings.getString("cached_groups", "")
            if (groupsStr.isNotBlank()) cachedGroups = groupsStr.split("\n").filter { it.isNotBlank() }
            val teachersStr = settings.getString("cached_teachers", "")
            if (teachersStr.isNotBlank()) cachedTeachers = teachersStr.split("\n").filter { it.isNotBlank() }
            val roomsStr = settings.getString("cached_classrooms", "")
            if (roomsStr.isNotBlank()) cachedClassrooms = roomsStr.split("\n").filter { it.isNotBlank() }
        } catch (_: Exception) {}
    }

    override fun getLastUpdateTime(target: String): Long {
        if (target.isBlank()) return 0L
        return settings.getLong("last_update_$target", 0L)
    }

    override fun observeLastUpdateTime(target: String): Flow<Long> {
        val flow = lastUpdateFlows.getOrPut(target) {
            MutableStateFlow(getLastUpdateTime(target))
        }
        return flow.asStateFlow()
    }

    private fun setLastUpdateTime(target: String, time: Long) {
        settings.putLong("last_update_$target", time)
        lastUpdateFlows.getOrPut(target) { MutableStateFlow(time) }.value = time
    }

    override fun isWeekLoaded(target: String, weekOffset: Int): Boolean {
        return loadedWeeks[target]?.contains(weekOffset) == true
    }

    override fun markWeekLoaded(target: String, weekOffset: Int) {
        loadedWeeks.getOrPut(target) { mutableSetOf() }.add(weekOffset)
    }

    override fun getSchedule(filter: ScheduleFilter): Flow<Resource<List<Lesson>>> = flow {
        emit(Resource.Loading)

        val target = filter.target.trim()
        val sectionType = filter.sectionType
        val selectedDate = filter.selectedDate

        // 1. Emit cached data first
        val cached = cachedLessonsMap[target]?.filter { lesson ->
            val matchTarget = lesson.target.equals(target, ignoreCase = true)
            val matchDate = selectedDate.isBlank() || lesson.date == selectedDate
            val matchQuery = filter.searchQuery.isBlank() ||
                    lesson.discipline.contains(filter.searchQuery, ignoreCase = true) ||
                    lesson.teacher.contains(filter.searchQuery, ignoreCase = true) ||
                    lesson.classroom.contains(filter.searchQuery, ignoreCase = true)
            matchTarget && matchDate && matchQuery
        } ?: emptyList()

        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.sortedBy { it.startTime }))
        }

        // 2. Fetch fresh data from network
        try {
            val dt = DateTimeUtils.parseDate(selectedDate)
            val weekOffset = if (dt != null) DateTimeUtils.calculateWeekOffset(dt) else 0

            val items = when (sectionType) {
                "teacher" -> api.getTeacherSchedule(teacher = target, weekOffset = weekOffset)
                "classroom" -> api.getClassroomSchedule(classroomName = target, weekOffset = weekOffset)
                else -> api.getGroupSchedule(groupName = target, weekOffset = weekOffset)
            }

            val domainLessons = items.map { it.toDomain(fallbackTarget = target, forcedSectionType = sectionType) }

            // Update in-memory cache
            val targetLessons = cachedLessonsMap.getOrPut(target) { mutableListOf() }
            targetLessons.removeAll { it.target.equals(target, ignoreCase = true) && domainLessons.any { fresh -> fresh.id == it.id } }
            targetLessons.addAll(domainLessons)

            markWeekLoaded(target, weekOffset)
            setLastUpdateTime(target, Clock.System.now().toEpochMilliseconds())

            val result = targetLessons.filter { lesson ->
                val matchTarget = lesson.target.equals(target, ignoreCase = true)
                val matchDate = selectedDate.isBlank() || lesson.date == selectedDate
                val matchQuery = filter.searchQuery.isBlank() ||
                        lesson.discipline.contains(filter.searchQuery, ignoreCase = true) ||
                        lesson.teacher.contains(filter.searchQuery, ignoreCase = true) ||
                        lesson.classroom.contains(filter.searchQuery, ignoreCase = true)
                matchTarget && matchDate && matchQuery
            }.sortedBy { it.startTime }

            emit(Resource.Success(result))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                emit(Resource.Error(e.message ?: "Не удалось загрузить расписание", e))
            }
        }
    }

    override suspend fun preloadSchedule(target: String, sectionType: String): Resource<Unit> {
        return coroutineScope {
            try {
                (-3..3).map { offset ->
                    async { loadWeekSchedule(target, sectionType, offset) }
                }.awaitAll()
                setLastUpdateTime(target, Clock.System.now().toEpochMilliseconds())
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Ошибка предзагрузки расписания", e)
            }
        }
    }

    override suspend fun loadWeekSchedule(target: String, sectionType: String, weekOffset: Int): Resource<Unit> {
        return try {
            val items = when (sectionType) {
                "teacher" -> api.getTeacherSchedule(teacher = target, weekOffset = weekOffset)
                "classroom" -> api.getClassroomSchedule(classroomName = target, weekOffset = weekOffset)
                else -> api.getGroupSchedule(groupName = target, weekOffset = weekOffset)
            }
            val domainLessons = items.map { it.toDomain(fallbackTarget = target, forcedSectionType = sectionType) }
            val targetLessons = cachedLessonsMap.getOrPut(target) { mutableListOf() }
            targetLessons.removeAll { it.target.equals(target, ignoreCase = true) && domainLessons.any { fresh -> fresh.id == it.id } }
            targetLessons.addAll(domainLessons)
            markWeekLoaded(target, weekOffset)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ошибка загрузки недели $weekOffset", e)
        }
    }

    override suspend fun refreshSchedule(target: String, sectionType: String, date: String?): Resource<Unit> {
        val dt = DateTimeUtils.parseDate(date ?: DateTimeUtils.todayFormatted())
        val weekOffset = if (dt != null) DateTimeUtils.calculateWeekOffset(dt) else 0
        return loadWeekSchedule(target, sectionType, weekOffset)
    }

    override suspend fun getGroups(query: String?): Resource<List<String>> {
        return try {
            val groups = api.getGroups(query)
            if (groups.isNotEmpty() && query.isNullOrBlank()) {
                cachedGroups = groups
                settings.putString("cached_groups", groups.joinToString("\n"))
            }
            Resource.Success(groups)
        } catch (e: Exception) {
            if (cachedGroups.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedGroups else cachedGroups.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered)
            } else {
                Resource.Error(e.message ?: "Не удалось получить список групп", e)
            }
        }
    }

    override suspend fun getTeachers(query: String?): Resource<List<String>> {
        return try {
            val teachers = api.getTeachers(query).map { it.displayName }
            if (teachers.isNotEmpty() && query.isNullOrBlank()) {
                cachedTeachers = teachers
                settings.putString("cached_teachers", teachers.joinToString("\n"))
            }
            Resource.Success(teachers)
        } catch (e: Exception) {
            if (cachedTeachers.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedTeachers else cachedTeachers.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered)
            } else {
                Resource.Error(e.message ?: "Не удалось получить список преподавателей", e)
            }
        }
    }

    override suspend fun getClassrooms(query: String?): Resource<List<String>> {
        return try {
            val rooms = api.getClassrooms(query)
            if (rooms.isNotEmpty() && query.isNullOrBlank()) {
                cachedClassrooms = rooms
                settings.putString("cached_classrooms", rooms.joinToString("\n"))
            }
            Resource.Success(rooms)
        } catch (e: Exception) {
            if (cachedClassrooms.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedClassrooms else cachedClassrooms.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered)
            } else {
                Resource.Error(e.message ?: "Не удалось получить список аудиторий", e)
            }
        }
    }

    override suspend fun getUpcomingLessons(target: String, limit: Int): List<Lesson> {
        val all = cachedLessonsMap[target] ?: emptyList()
        val today = DateTimeUtils.todayFormatted()
        return all.filter { it.date >= today }.sortedBy { "${it.date} ${it.startTime}" }.take(limit)
    }

    override suspend fun getLessonsForGroup(groupName: String): List<Lesson> {
        return cachedLessonsMap[groupName] ?: emptyList()
    }
}
