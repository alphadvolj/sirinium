package com.dlab.sirinium.ui.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

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
    private val repository: ScheduleRepository
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
        val current = DateTimeUtils.parseDate(_uiState.value.selectedDate) ?: DateTimeUtils.today()
        val newDate = current.plus(DatePeriod(days = offsetDays.toInt()))
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

            val cachedRawRooms = (repository.getClassrooms().dataOrNull ?: emptyList())
            val localRooms = cachedRawRooms.filter { !isInvalidClassroom(it) }.distinct().sorted()

            if (localRooms.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        allClassrooms = localRooms,
                        loadingProgress = 0.25f,
                        loadingStage = "Синхронизация с сервером..."
                    )
                }
            }

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
                        val pageSize = 1000
                        while (true) {
                            val chunk = try {
                                api.getSchedule(date = date, limit = pageSize, offset = offset)
                            } catch (_: Exception) {
                                emptyList()
                            }
                            if (chunk.isEmpty()) break
                            val mapped = chunk.map { it.toDomain(fallbackTarget = "") }
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
                            if (offset >= 5000) break
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

                    val (freeMap, occupiedMap) = computeFreeRooms(allRooms, networkLessons)

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
                        error = "Ошибка загрузки свободных аудиторий: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        fun isInvalidClassroom(name: String): Boolean {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return true
            val lower = trimmed.lowercase()

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

            if (lower.contains("дистанционн") || lower.contains("дистант") || lower.contains("онлайн") || lower.contains("online")) {
                return true
            }

            if (lower.contains("выездное мероприятие") || lower.contains("выездное") || lower.contains("выезд")) {
                return true
            }

            return false
        }

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
