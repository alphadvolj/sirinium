package com.dlab.sirinium.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.platform.PlatformAlarmScheduler
import com.dlab.sirinium.platform.PlatformSettings
import com.dlab.sirinium.platform.PlatformWidgetUpdater
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel retaining user preferences and configuration during the onboarding wizard flow.
 * Supports selecting any entity (group, teacher, classroom) or custom target.
 * Persists selected values to PlatformSettings upon completion.
 */
class OnboardingViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: PlatformAlarmScheduler,
    private val widgetUpdater: PlatformWidgetUpdater,
    private val settings: PlatformSettings
) : ViewModel() {

    companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    private val isFirstOnboarding = !settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            selectedTarget = if (isFirstOnboarding) "" else settings.getString(ScheduleViewModel.KEY_CURRENT_TARGET, ""),
            selectedSectionType = if (isFirstOnboarding) "group" else settings.getString(ScheduleViewModel.KEY_CURRENT_SECTION, "group").ifBlank { "group" },
            themeMode = settings.getString("theme_mode", "system").ifBlank { "system" },
            dynamicColor = settings.getBoolean("dynamic_color", true),
            notifyLessonsEnabled = settings.getBoolean("notify_lessons_enabled", true),
            defaultAlarmMinutes = settings.getInt("default_alarm_mins", 15),
            shakeToReportEnabled = settings.getBoolean("shake_to_report_enabled", true),
            isOnboardingCompleted = settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadEntities()
    }

    fun isOnboardingCompleted(): Boolean {
        return settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun refreshEntities() {
        loadEntities()
    }

    private fun loadEntities() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEntities = true) }
            try {
                coroutineScope {
                    launch {
                        try {
                            val groupsResult = scheduleRepository.getGroups()
                            val groups = groupsResult.dataOrNull
                            if (!groups.isNullOrEmpty()) {
                                _uiState.update { current ->
                                    current.copy(availableGroups = groups)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    launch {
                        try {
                            val teachersResult = scheduleRepository.getTeachers()
                            val teachers = teachersResult.dataOrNull
                            if (!teachers.isNullOrEmpty()) {
                                _uiState.update { current ->
                                    current.copy(availableTeachers = teachers)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    launch {
                        try {
                            val classroomsResult = scheduleRepository.getClassrooms()
                            val classrooms = classroomsResult.dataOrNull
                            if (!classrooms.isNullOrEmpty()) {
                                _uiState.update { current ->
                                    current.copy(availableClassrooms = classrooms)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoadingEntities = false) }
            }
        }
    }

    fun selectTarget(target: String, sectionType: String) {
        _uiState.update {
            it.copy(
                selectedTarget = target,
                selectedSectionType = sectionType,
                isCustomTarget = false
            )
        }
    }

    fun setSectionType(sectionType: String) {
        _uiState.update { it.copy(selectedSectionType = sectionType) }
    }

    fun setCustomTarget(isCustom: Boolean) {
        _uiState.update { it.copy(isCustomTarget = isCustom) }
    }

    fun setCustomTargetInput(input: String) {
        _uiState.update { it.copy(customTargetInput = input, isCustomTarget = true) }
    }

    fun setNotifyDevelopersAboutCustom(notify: Boolean) {
        _uiState.update { it.copy(notifyDevelopersAboutCustom = notify) }
    }

    fun setThemeMode(mode: String) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColor = enabled) }
    }

    fun setNotifyLessons(enabled: Boolean) {
        _uiState.update { it.copy(notifyLessonsEnabled = enabled) }
    }

    fun setDefaultAlarmMinutes(minutes: Int) {
        _uiState.update { it.copy(defaultAlarmMinutes = minutes) }
    }

    fun setShakeToReport(enabled: Boolean) {
        _uiState.update { it.copy(shakeToReportEnabled = enabled) }
    }

    fun savePreferences() {
        val state = _uiState.value
        val target = state.effectiveTarget
        val sectionType = state.effectiveSectionType

        val currentFavs = if (isFirstOnboarding) {
            mutableSetOf()
        } else {
            settings.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS).toMutableSet()
        }
        if (target.isNotBlank()) {
            currentFavs.add(FavoriteTarget(target = target, sectionType = sectionType).toSerializedString())
        }

        settings.putString(ScheduleViewModel.KEY_CURRENT_TARGET, target)
        settings.putString(ScheduleViewModel.KEY_CURRENT_SECTION, sectionType)
        settings.putStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, currentFavs)

        settings.putString("theme_mode", state.themeMode)
        settings.putBoolean("dynamic_color", state.dynamicColor)
        settings.putBoolean("notify_lessons_enabled", state.notifyLessonsEnabled)
        settings.putInt("default_alarm_mins", state.defaultAlarmMinutes)
        settings.putBoolean("shake_to_report_enabled", state.shakeToReportEnabled)
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val target = state.effectiveTarget
        val sectionType = state.effectiveSectionType

        val currentFavs = if (isFirstOnboarding) {
            mutableSetOf()
        } else {
            settings.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS).toMutableSet()
        }
        if (target.isNotBlank()) {
            currentFavs.add(FavoriteTarget(target = target, sectionType = sectionType).toSerializedString())
        }

        settings.putString(ScheduleViewModel.KEY_CURRENT_TARGET, target)
        settings.putString(ScheduleViewModel.KEY_CURRENT_SECTION, sectionType)
        settings.putStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, currentFavs)

        settings.putBoolean(KEY_ONBOARDING_COMPLETED, true)
        settings.putString("theme_mode", state.themeMode)
        settings.putBoolean("dynamic_color", state.dynamicColor)
        settings.putBoolean("notify_lessons_enabled", state.notifyLessonsEnabled)
        settings.putInt("default_alarm_mins", state.defaultAlarmMinutes)
        settings.putBoolean("shake_to_report_enabled", state.shakeToReportEnabled)

        _uiState.update { it.copy(isOnboardingCompleted = true) }

        viewModelScope.launch {
            try {
                if (state.notifyLessonsEnabled && target.isNotBlank()) {
                    val lessons = scheduleRepository.getSchedule(
                        ScheduleFilter(target = target, sectionType = sectionType)
                    ).first().dataOrNull ?: emptyList()
                    if (lessons.isNotEmpty()) {
                        alarmScheduler.rescheduleAlarmsForLessons(lessons, state.defaultAlarmMinutes)
                    }
                } else {
                    alarmScheduler.cancelAllAlarms()
                }

                widgetUpdater.updateWidgets()
            } catch (_: Exception) {}
        }
    }

    fun resetOnboarding() {
        settings.putBoolean(KEY_ONBOARDING_COMPLETED, false)
        _uiState.update { it.copy(isOnboardingCompleted = false) }
    }
}
