package com.dlab.sirinium.ui.compare

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.data.remote.dto.toEntity
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.sync.ScheduleSyncWorker
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EntityTarget(
    val name: String,
    val type: String // "group", "teacher", "classroom"
)

data class PairComparison(
    val pairNumber: Int,
    val timeSlot: String,
    val lesson1: Lesson?,
    val lesson2: Lesson?,
    val isCommonWindow: Boolean
)

data class CompareUiState(
    val entity1: EntityTarget = EntityTarget("", "group"),
    val entity2: EntityTarget = EntityTarget("", "group"),
    val selectedDate: String = DateTimeUtils.todayFormatted(),
    val isLoading: Boolean = false,
    val isRefreshingSelectors: Boolean = false,
    val comparisons: List<PairComparison> = emptyList(),
    val commonWindowsCount: Int = 0,
    val availableGroups: List<String> = emptyList(),
    val availableTeachers: List<String> = emptyList(),
    val availableClassrooms: List<String> = emptyList(),
    val favoriteTargets: List<FavoriteTarget> = emptyList(),
    val error: String? = null
)

class CompareViewModel(
    private val context: Context,
    private val api: SiriusScheduleApi,
    private val repository: ScheduleRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CompareUiState(favoriteTargets = loadFavorites()))
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        refreshAvailableTargets()
    }

    fun refreshAvailableTargets() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshingSelectors = true) }
            try {
                coroutineScope {
                    launch {
                        val groups = (repository.getGroups().dataOrNull ?: emptyList()).sorted()
                        _uiState.update { it.copy(availableGroups = groups) }
                    }
                    launch {
                        val teachers = (repository.getTeachers().dataOrNull ?: emptyList()).sorted()
                        _uiState.update { it.copy(availableTeachers = teachers) }
                    }
                    launch {
                        val classrooms = (repository.getClassrooms().dataOrNull ?: emptyList()).sorted()
                        _uiState.update { it.copy(availableClassrooms = classrooms) }
                    }
                }
                compare()
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isRefreshingSelectors = false) }
            }
        }
    }

    fun setEntity1(target: EntityTarget) {
        _uiState.update { it.copy(entity1 = target) }
        compare()
    }

    fun setEntity2(target: EntityTarget) {
        _uiState.update { it.copy(entity2 = target) }
        compare()
    }

    fun swapEntities() {
        _uiState.update {
            it.copy(
                entity1 = it.entity2,
                entity2 = it.entity1
            )
        }
        compare()
    }

    fun setDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        compare()
    }

    fun changeDateOffset(offsetDays: Long) {
        val current = DateTimeUtils.parseDate(_uiState.value.selectedDate) ?: LocalDate.now()
        val newDate = current.plusDays(offsetDays)
        setDate(DateTimeUtils.formatDate(newDate))
    }

    fun compare() {
        val e1 = _uiState.value.entity1
        val e2 = _uiState.value.entity2

        // Don't compare if entities are not selected
        if (e1.name.isBlank() || e2.name.isBlank()) {
            _uiState.update { it.copy(isLoading = false, comparisons = emptyList(), commonWindowsCount = 0) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val date = _uiState.value.selectedDate

            try {
                // Fetch lessons for Entity 1 and Entity 2 concurrently
                val (lessons1, lessons2) = coroutineScope {
                    val d1 = async { fetchLessonsForEntity(e1, date) }
                    val d2 = async { fetchLessonsForEntity(e2, date) }
                    Pair(d1.await(), d2.await())
                }

                val comparisons = DateTimeUtils.STANDARD_PAIRS.map { slot ->
                    val l1 = lessons1.find { DateTimeUtils.doesLessonOverlapSlot(it.startTime, it.endTime, slot) }
                    val l2 = lessons2.find { DateTimeUtils.doesLessonOverlapSlot(it.startTime, it.endTime, slot) }
                    val isFree = (l1 == null && l2 == null)

                    PairComparison(
                        pairNumber = slot.pairNumber,
                        timeSlot = slot.timeSlot,
                        lesson1 = l1,
                        lesson2 = l2,
                        isCommonWindow = isFree
                    )
                }

                val windowsCount = comparisons.count { it.isCommonWindow }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        comparisons = comparisons,
                        commonWindowsCount = windowsCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Не удалось загрузить расписание для сравнения"
                    )
                }
            }
        }
    }

    private suspend fun fetchLessonsForEntity(entity: EntityTarget, date: String): List<Lesson> {
        return try {
            val dtoList = when (entity.type) {
                "group" -> api.getGroupSchedule(entity.name, date)
                "teacher" -> api.getTeacherSchedule(entity.name, date)
                "classroom" -> api.getClassroomSchedule(entity.name, date)
                else -> api.getSchedule(
                    sectionType = entity.type,
                    date = date,
                    group = if (entity.type == "group") entity.name else null,
                    teacher = if (entity.type == "teacher") entity.name else null,
                    classroom = if (entity.type == "classroom") entity.name else null
                )
            }
            dtoList
                .map {
                    it.toEntity(
                        fallbackTarget = entity.name,
                        forcedTarget = entity.name,
                        forcedSectionType = entity.type
                    ).toDomain()
                }
                .distinctBy { it.id }
                .filter { it.date == date }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun refreshFavorites() {
        _uiState.update { it.copy(favoriteTargets = loadFavorites()) }
    }

    private fun loadFavorites(): List<FavoriteTarget> {
        val rawSet = prefs.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, null)
        return if (rawSet.isNullOrEmpty()) {
            emptyList()
        } else {
            rawSet.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        }
    }

    private fun saveFavorites(list: List<FavoriteTarget>) {
        val serializedSet = list.map { it.toSerializedString() }.toSet()
        prefs.edit().putStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, serializedSet).apply()
    }

    fun toggleFavorite(target: String, sectionType: String) {
        val current = loadFavorites().toMutableList()
        val index = current.indexOfFirst { it.target.equals(target, ignoreCase = true) && it.sectionType == sectionType }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(FavoriteTarget(target = target, sectionType = sectionType))
        }
        saveFavorites(current)
        _uiState.update { it.copy(favoriteTargets = current) }
    }

    fun removeFavorite(target: String, sectionType: String) {
        val current = loadFavorites().toMutableList()
        current.removeAll { it.target.equals(target, ignoreCase = true) && it.sectionType == sectionType }
        saveFavorites(current)
        _uiState.update { it.copy(favoriteTargets = current) }
    }
}
