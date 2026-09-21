package com.dlab.sirinium.data.repository

import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.ScheduleItemDto
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Lesson.toDto(): ScheduleItemDto = ScheduleItemDto(
    sectionType = sectionType,
    target = target,
    date = date,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    discipline = discipline,
    lessonType = rawLessonType.ifBlank { lessonType.title },
    teacher = teacher,
    classroom = classroom,
    address = address,
    onlineUrl = onlineUrl,
    group = group,
    numberPair = numberPair
)

class MultiplatformScheduleRepository(
    private val api: SiriusScheduleApi,
    private val settings: PlatformSettings,
    private val noteRepository: LessonNoteRepository? = null
) : ScheduleRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val cacheMutex = Mutex()
    private val cachedLessonsMap = mutableMapOf<String, MutableList<Lesson>>()
    private val loadedWeeks = mutableMapOf<String, MutableSet<Int>>()
    private val lastUpdateFlows = mutableMapOf<String, MutableStateFlow<Long>>()
    private val scheduleFlows = mutableMapOf<String, MutableStateFlow<List<Lesson>>>()

    private var cachedGroups: List<String> = emptyList()
    private var cachedTeachers: List<String> = emptyList()
    private var cachedClassrooms: List<String> = emptyList()

    init {
        try {
            val groupsStr = settings.getString("cached_groups", "")
            if (groupsStr.isNotBlank()) cachedGroups = groupsStr.split("\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val teachersStr = settings.getString("cached_teachers", "")
            if (teachersStr.isNotBlank()) cachedTeachers = teachersStr.split("\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val roomsStr = settings.getString("cached_classrooms", "")
            if (roomsStr.isNotBlank()) cachedClassrooms = roomsStr.split("\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        } catch (_: Exception) {}
    }

    private fun getTargetScheduleFlow(target: String): MutableStateFlow<List<Lesson>> {
        return scheduleFlows.getOrPut(target) {
            val loaded = loadCachedLessonsFromSettings(target)
            cachedLessonsMap[target] = loaded.toMutableList()
            MutableStateFlow(loaded)
        }
    }

    private fun loadCachedLessonsFromSettings(target: String): List<Lesson> {
        if (target.isBlank()) return emptyList()
        val inMemory = cachedLessonsMap[target]
        if (!inMemory.isNullOrEmpty()) return inMemory

        return try {
            val raw = settings.getString("cached_schedule_$target", "")
            if (raw.isNotBlank()) {
                val dtos = json.decodeFromString<List<ScheduleItemDto>>(raw)
                dtos.map { it.toDomain(fallbackTarget = target) }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistLessons(target: String, lessons: List<Lesson>) {
        if (target.isBlank()) return
        try {
            val dtos = lessons.map { it.toDto() }
            val raw = json.encodeToString(dtos)
            settings.putString("cached_schedule_$target", raw)
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
        if (target.isBlank()) {
            emit(Resource.Success(emptyList()))
            return@flow
        }

        val targetFlow = getTargetScheduleFlow(target)
        val initialCached = targetFlow.value
        if (initialCached.isNotEmpty()) {
            emit(Resource.Success(initialCached.sortedWith(compareBy({ it.date }, { it.startTime }))))
        }

        coroutineScope {
            // Trigger preloading in background
            launch {
                try {
                    preloadSchedule(target, sectionType)
                } catch (_: Exception) {}
            }

            // Stream continuous updates whenever new lessons are added
            targetFlow.collect { lessons ->
                emit(Resource.Success(lessons.sortedWith(compareBy({ it.date }, { it.startTime }))))
            }
        }
    }

    override suspend fun preloadSchedule(target: String, sectionType: String): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        return coroutineScope {
            // Load current week first
            val week0Result = loadWeekSchedule(target, sectionType, 0)

            // Preload adjacent weeks in parallel
            val otherOffsets = listOf(1, -1, 2, -2, 3, -3)
            val deferreds = otherOffsets.map { offset ->
                async {
                    try {
                        loadWeekSchedule(target, sectionType, offset)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            deferreds.awaitAll()
            setLastUpdateTime(target, Clock.System.now().toEpochMilliseconds())
            week0Result
        }
    }

    override suspend fun loadWeekSchedule(target: String, sectionType: String, weekOffset: Int): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        return try {
            val items = when (sectionType) {
                "teacher" -> api.getTeacherSchedule(teacher = target, weekOffset = weekOffset)
                "classroom" -> api.getClassroomSchedule(classroomName = target, weekOffset = weekOffset)
                else -> api.getGroupSchedule(groupName = target, weekOffset = weekOffset)
            }
            val domainLessons = items.map { it.toDomain(fallbackTarget = target, forcedSectionType = sectionType) }
            val deduplicated = cacheMutex.withLock {
                val targetLessons = cachedLessonsMap.getOrPut(target) {
                    loadCachedLessonsFromSettings(target).toMutableList()
                }

                // Remove existing lessons for dates present in the fetched week
                val fetchedDates = domainLessons.map { it.date }.filter { it.isNotBlank() }.toSet()
                if (fetchedDates.isNotEmpty()) {
                    targetLessons.removeAll { it.date in fetchedDates }
                } else {
                    val weekDates = DateTimeUtils.getWeekDatesForOffset(weekOffset).toSet()
                    targetLessons.removeAll { it.date in weekDates }
                }
                targetLessons.addAll(domainLessons)

                val distinct = targetLessons
                    .distinctBy { lesson ->
                        "${lesson.date}_${lesson.startTime}_${lesson.numberPair}_${lesson.discipline.trim().lowercase()}_${lesson.classroom.trim().lowercase()}_${lesson.teacher.trim().lowercase()}"
                    }
                    .sortedWith(compareBy({ it.date }, { it.startTime }))

                targetLessons.clear()
                targetLessons.addAll(distinct)
                distinct
            }

            markWeekLoaded(target, weekOffset)
            setLastUpdateTime(target, Clock.System.now().toEpochMilliseconds())

            // Update reactive flow and persistent settings
            getTargetScheduleFlow(target).value = deduplicated
            persistLessons(target, deduplicated)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ошибка загрузки недели $weekOffset", e)
        }
    }

    override suspend fun refreshSchedule(target: String, sectionType: String, date: String?): Resource<Unit> {
        if (target.isBlank()) return Resource.Success(Unit)
        val dt = DateTimeUtils.parseDate(date ?: DateTimeUtils.todayFormatted())
        val weekOffset = if (dt != null) DateTimeUtils.calculateWeekOffset(dt) else 0
        val res = loadWeekSchedule(target, sectionType, weekOffset)
        preloadSchedule(target, sectionType)
        return res
    }

    override suspend fun getGroups(query: String?): Resource<List<String>> {
        return try {
            val groups = api.getGroups(query).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (groups.isNotEmpty() && query.isNullOrBlank()) {
                cachedGroups = groups
                settings.putString("cached_groups", groups.joinToString("\n"))
            }
            Resource.Success(groups)
        } catch (e: Exception) {
            if (cachedGroups.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedGroups else cachedGroups.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered.distinct())
            } else {
                Resource.Error(e.message ?: "Не удалось получить список групп", e)
            }
        }
    }

    override suspend fun getTeachers(query: String?): Resource<List<String>> {
        return try {
            val teachers = api.getTeachers(query).map { it.displayName.trim() }.filter { it.isNotBlank() }.distinct()
            if (teachers.isNotEmpty() && query.isNullOrBlank()) {
                cachedTeachers = teachers
                settings.putString("cached_teachers", teachers.joinToString("\n"))
            }
            Resource.Success(teachers)
        } catch (e: Exception) {
            if (cachedTeachers.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedTeachers else cachedTeachers.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered.distinct())
            } else {
                Resource.Error(e.message ?: "Не удалось получить список преподавателей", e)
            }
        }
    }

    override suspend fun getClassrooms(query: String?): Resource<List<String>> {
        return try {
            val rooms = api.getClassrooms(query).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (rooms.isNotEmpty() && query.isNullOrBlank()) {
                cachedClassrooms = rooms
                settings.putString("cached_classrooms", rooms.joinToString("\n"))
            }
            Resource.Success(rooms)
        } catch (e: Exception) {
            if (cachedClassrooms.isNotEmpty()) {
                val filtered = if (query.isNullOrBlank()) cachedClassrooms else cachedClassrooms.filter { it.contains(query, ignoreCase = true) }
                Resource.Success(filtered.distinct())
            } else {
                Resource.Error(e.message ?: "Не удалось получить список аудиторий", e)
            }
        }
    }

    override suspend fun getUpcomingLessons(target: String, limit: Int): List<Lesson> {
        var all = getTargetScheduleFlow(target).value
        val today = DateTimeUtils.today()
        val todayEpoch = today.toEpochDays()
        var upcoming = all.mapNotNull { lesson ->
            val parsed = DateTimeUtils.parseDate(lesson.date) ?: return@mapNotNull null
            if (parsed.toEpochDays() >= todayEpoch) {
                Triple(parsed, lesson.startTime, lesson)
            } else null
        }.sortedWith(compareBy({ it.first.toEpochDays() }, { it.second })).map { it.third }

        if (upcoming.size < 6 && target.isNotBlank()) {
            val currentOffset = DateTimeUtils.calculateWeekOffset(today)
            val section = settings.getString("pref_current_section", "group").ifBlank { "group" }
            try {
                if (!isWeekLoaded(target, currentOffset)) {
                    loadWeekSchedule(target, section, currentOffset)
                }
                if (!isWeekLoaded(target, currentOffset + 1)) {
                    loadWeekSchedule(target, section, currentOffset + 1)
                }
                all = getTargetScheduleFlow(target).value
                upcoming = all.mapNotNull { lesson ->
                    val parsed = DateTimeUtils.parseDate(lesson.date) ?: return@mapNotNull null
                    if (parsed.toEpochDays() >= todayEpoch) {
                        Triple(parsed, lesson.startTime, lesson)
                    } else null
                }.sortedWith(compareBy({ it.first.toEpochDays() }, { it.second })).map { it.third }
            } catch (_: Exception) {}
        }
        return upcoming.take(limit)
    }

    override suspend fun getLessonsForGroup(groupName: String): List<Lesson> {
        return getTargetScheduleFlow(groupName).value
    }
}
