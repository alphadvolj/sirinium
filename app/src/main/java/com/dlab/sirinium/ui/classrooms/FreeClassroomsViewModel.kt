package com.dlab.sirinium.ui.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.local.dao.ScheduleDao
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.data.remote.dto.toEntity
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PairTimeInfo(
    val pairNumber: Int,
    val timeSlot: String
)

val STANDARD_PAIRS_INFO = DateTimeUtils.STANDARD_PAIRS.map { slot ->
    PairTimeInfo(
        pairNumber = slot.pairNumber,
        timeSlot = slot.timeSlot
    )
}

data class FreeClassroomsUiState(
    val selectedDate: String = DateTimeUtils.todayFormatted(),
    val selectedPair: Int = 1, // 1 to 8
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingProgress: Float = 0f,
    val loadingStage: String = "",
    val allClassrooms: List<String> = emptyList(),
    val freeClassroomsByPair: Map<Int, List<String>> = emptyMap(),
    val occupiedClassroomsByPair: Map<Int, Map<String, Lesson>> = emptyMap(),
    val error: String? = null
)

class FreeClassroomsViewModel(
    private val api: SiriusScheduleApi,
    private val repository: ScheduleRepository,
    private val scheduleDao: ScheduleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeClassroomsUiState())
    val uiState: StateFlow<FreeClassroomsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setSelectedPair(pair: Int) {
        _uiState.update { it.copy(selectedPair = pair) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadData()
    }

    fun changeDateOffset(offsetDays: Long) {
        val current = DateTimeUtils.parseDate(_uiState.value.selectedDate) ?: LocalDate.now()
        val newDate = current.plusDays(offsetDays)
        setDate(DateTimeUtils.formatDate(newDate))
    }

    fun refreshData() {
        loadData(isRefresh = true)
    }

    private fun computeFreeRooms(
        allRooms: List<String>,
        lessons: List<Lesson>
    ): Pair<Map<Int, List<String>>, Map<Int, MutableMap<String, Lesson>>> {
        val occupiedMap = mutableMapOf<Int, MutableMap<String, Lesson>>()
        val occupiedNormalizedSets = mutableMapOf<Int, MutableSet<String>>()
        DateTimeUtils.STANDARD_PAIRS.forEach { slot ->
            occupiedMap[slot.pairNumber] = mutableMapOf()
            occupiedNormalizedSets[slot.pairNumber] = mutableSetOf()
        }

        lessons.forEach { lesson ->
            val rawClassroom = lesson.classroom.trim()
            if (rawClassroom.isNotBlank() && !isInvalidClassroom(rawClassroom)) {
                // Support multi-room lessons (e.g. "K_19, 3.2")
                val rooms = rawClassroom.split(',', '/', ';')
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !isInvalidClassroom(it) }

                rooms.forEach { room ->
                    val normRoom = normalizeClassroom(room)
                    DateTimeUtils.STANDARD_PAIRS.forEach { slot ->
                        if (DateTimeUtils.doesLessonOverlapSlot(lesson.startTime, lesson.endTime, slot)) {
                            occupiedMap.getOrPut(slot.pairNumber) { mutableMapOf() }[room] = lesson
                            occupiedNormalizedSets.getOrPut(slot.pairNumber) { mutableSetOf() }.add(normRoom)
                        }
                    }
                }
            }
        }

        val freeMap = mutableMapOf<Int, List<String>>()
        DateTimeUtils.STANDARD_PAIRS.forEach { slot ->
            val occupiedNorm = occupiedNormalizedSets[slot.pairNumber] ?: emptySet()
            val freeInPair = allRooms.filter { room ->
                val norm = normalizeClassroom(room)
                norm !in occupiedNorm
            }
            freeMap[slot.pairNumber] = freeInPair
        }

        return Pair(freeMap, occupiedMap)
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _uiState.value.selectedDate
            val hasExistingData = _uiState.value.allClassrooms.isNotEmpty()

            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && !hasExistingData,
                    isRefreshing = isRefresh,
                    loadingProgress = 0.10f,
                    loadingStage = "Чтение локального кэша...",
                    error = null
                )
            }

            // 1. Instant cache display: check local DB and cached classrooms
            val localEntities = try {
                scheduleDao.getScheduleForDate(date)
            } catch (_: Exception) {
                emptyList()
            }
            val cachedRawRooms = (repository.getClassrooms().dataOrNull ?: emptyList())
            val localRooms = cachedRawRooms.filter { !isInvalidClassroom(it) }.distinct().sorted()

            if (localRooms.isNotEmpty()) {
                val (localFree, localOcc) = computeFreeRooms(localRooms, localEntities.map { it.toDomain() })
                _uiState.update {
                    it.copy(
                        allClassrooms = localRooms,
                        freeClassroomsByPair = localFree,
                        occupiedClassroomsByPair = localOcc,
                        loadingProgress = 0.25f,
                        loadingStage = "Синхронизация с сервером..."
                    )
                }
            }

            // 2. Fetch fresh classrooms and university schedule in parallel
            try {
                _uiState.update {
                    it.copy(
                        loadingProgress = 0.35f,
                        loadingStage = "Загрузка расписания университета..."
                    )
                }

                coroutineScope {
                    val classroomsDeferred = async {
                        (repository.getClassrooms().dataOrNull ?: emptyList())
                            .filter { !isInvalidClassroom(it) }
                            .distinct()
                            .sorted()
                    }

                    val scheduleDeferred = async {
                        val networkLessons = mutableListOf<Lesson>()
                        var offset = 0
                        val pageSize = 1000 // Fast bulk fetch
                        while (true) {
                            val chunk = try {
                                api.getSchedule(date = date, limit = pageSize, offset = offset)
                            } catch (_: Exception) {
                                emptyList()
                            }
                            if (chunk.isEmpty()) break
                            val mapped = chunk.map { it.toEntity(fallbackTarget = "").toDomain() }
                            networkLessons.addAll(mapped)

                            _uiState.update {
                                val newProgress = (it.loadingProgress + 0.25f).coerceAtMost(0.85f)
                                it.copy(
                                    loadingProgress = newProgress,
                                    loadingStage = "Получено ${networkLessons.size} пар..."
                                )
                            }

                            if (chunk.size < pageSize) break
                            offset += chunk.size
                            if (offset >= 5000) break // Safety guard
                        }
                        networkLessons
                    }

                    val allRooms = classroomsDeferred.await().ifEmpty { localRooms }
                    val networkLessons = scheduleDeferred.await()

                    _uiState.update {
                        it.copy(
                            loadingProgress = 0.92f,
                            loadingStage = "Анализ свободных аудиторий..."
                        )
                    }

                    // 3. Combine with local cached entries for maximum coverage
                    val localLessons = localEntities.map { it.toDomain() }
                    val combinedLessons = (networkLessons + localLessons).distinctBy {
                        "${it.date}_${it.startTime}_${it.endTime}_${it.classroom}_${it.discipline}_${it.groupName}"
                    }

                    // 4. Compute final occupancy
                    val (freeMap, occupiedMap) = computeFreeRooms(allRooms, combinedLessons)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loadingProgress = 1.0f,
                            loadingStage = "Готово!",
                            allClassrooms = allRooms,
                            freeClassroomsByPair = freeMap,
                            occupiedClassroomsByPair = occupiedMap
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Ошибка загрузки свободных аудиторий: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Checks whether the classroom name is invalid, virtual, a link, or a non-physical event.
         */
        fun isInvalidClassroom(name: String): Boolean {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return true
            val lower = trimmed.lowercase()

            // 1. Any URLs / Links
            if (lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.contains("://") ||
                lower.startsWith("www.") ||
                lower.contains(".ru/") || lower.endsWith(".ru") ||
                lower.contains(".com/") || lower.endsWith(".com") ||
                lower.contains(".org/") || lower.endsWith(".org") ||
                lower.contains(".work/") || lower.endsWith(".work") ||
                lower.contains(".me/") || lower.endsWith(".me") ||
                lower.contains(".online") || lower.contains(".рф") ||
                lower.contains("zoom.") || lower.contains("teams.") ||
                lower.contains("telemost.") || lower.contains("meet.google") ||
                lower.contains("jazz.")
            ) {
                return true
            }

            // 2. Just the word "аудитория" / "ауд" without any number/identifier
            if (lower == "аудитория" || lower == "ауд." || lower == "ауд") {
                return true
            }
            val withoutAud = lower
                .replace("аудитория", "")
                .replace("ауд.", "")
                .replace("ауд", "")
                .replace("кабинет", "")
                .replace("каб.", "")
                .replace("каб", "")
                .replace(".", "")
                .replace("-", "")
                .replace("_", "")
                .trim()
            if (withoutAud.isEmpty()) {
                return true
            }

            // 3. "Дистанционно" / "Онлайн"
            if (lower.contains("дистанционн") || lower.contains("дистант") || lower.contains("онлайн") || lower.contains("online")) {
                return true
            }

            // 4. "Выездное мероприятие"
            if (lower.contains("выездное мероприятие") || lower.contains("выездное") || lower.contains("выезд")) {
                return true
            }

            return false
        }

        /**
         * Normalizes classroom strings to allow robust fuzzy matching.
         * Maps Russian lookalikes to Latin homoglyphs, strips word prefixes, spaces, underscores, and hyphens.
         * E.g. "К_19", "K_19", "К19", "к 19" -> "k19".
         */
        fun normalizeClassroom(name: String): String {
            return name.trim().lowercase()
                .replace('а', 'a')
                .replace('в', 'b')
                .replace('е', 'e')
                .replace('к', 'k')
                .replace('м', 'm')
                .replace('н', 'h')
                .replace('о', 'o')
                .replace('р', 'p')
                .replace('с', 'c')
                .replace('т', 't')
                .replace('у', 'y')
                .replace('х', 'x')
                .replace("аудитория", "")
                .replace("кабинет", "")
                .replace("ауд.", "")
                .replace("каб.", "")
                .replace("ауд", "")
                .replace("каб", "")
                .replace("зал", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "")
        }

        /**
         * Checks if a classroom matches user search query, using both regular substring matching
         * and normalized matching (e.g. "К19" -> matches "К_19").
         */
        fun classroomMatchesQuery(room: String, query: String): Boolean {
            val q = query.trim()
            if (q.isBlank()) return true
            if (room.contains(q, ignoreCase = true)) return true
            val normRoom = normalizeClassroom(room)
            val normQuery = normalizeClassroom(q)
            return normQuery.isNotBlank() && (normRoom.contains(normQuery) || normRoom == normQuery)
        }
    }
}
