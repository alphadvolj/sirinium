package com.dlab.sirinium.ui.onboarding

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.core.util.FeedbackHelper
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.sync.ScheduleSyncWorker
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
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
 * Persists selected values to SharedPreferences upon completion.
 */
class OnboardingViewModel(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: LessonAlarmScheduler
) : ViewModel() {

    private val settingsPrefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
    private val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    private val isFirstOnboarding = !settingsPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            selectedTarget = if (isFirstOnboarding) "" else {
                schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, "")
                    ?: ""
            },
            selectedSectionType = if (isFirstOnboarding) "group" else {
                schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, "group") ?: "group"
            },
            themeMode = settingsPrefs.getString("theme_mode", "system") ?: "system",
            dynamicColor = settingsPrefs.getBoolean("dynamic_color", true),
            notifyLessonsEnabled = settingsPrefs.getBoolean("notify_lessons_enabled", true),
            defaultAlarmMinutes = settingsPrefs.getInt("default_alarm_mins", 15),
            shakeToReportEnabled = settingsPrefs.getBoolean("shake_to_report_enabled", true),
            isOnboardingCompleted = settingsPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadEntities()
    }

    fun isOnboardingCompleted(): Boolean {
        return settingsPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun refreshEntities() {
        loadEntities()
    }

    private fun loadEntities() {
        viewModelScope.launch(Dispatchers.IO) {
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

    /**
     * Persists user preferences without marking onboarding as completed.
     */
    fun savePreferences() {
        val state = _uiState.value
        val target = state.effectiveTarget
        val sectionType = state.effectiveSectionType

        val currentFavs = if (isFirstOnboarding) {
            mutableSetOf()
        } else {
            schedulePrefs.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, null)?.toMutableSet() ?: mutableSetOf()
        }
        if (target.isNotBlank()) {
            currentFavs.add(FavoriteTarget(target = target, sectionType = sectionType).toSerializedString())
        }

        schedulePrefs.edit()
            .putString(ScheduleSyncWorker.KEY_CURRENT_TARGET, target)
            .putString(ScheduleSyncWorker.KEY_CURRENT_SECTION, sectionType)
            .putStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, currentFavs)
            .apply()

        settingsPrefs.edit()
            .putString("theme_mode", state.themeMode)
            .putBoolean("dynamic_color", state.dynamicColor)
            .putBoolean("notify_lessons_enabled", state.notifyLessonsEnabled)
            .putInt("default_alarm_mins", state.defaultAlarmMinutes)
            .putBoolean("shake_to_report_enabled", state.shakeToReportEnabled)
            .apply()
    }

    /**
     * Persists all selected settings, schedules lesson alarms, and marks onboarding as completed.
     */
    fun completeOnboarding() {
        val state = _uiState.value
        val target = state.effectiveTarget
        val sectionType = state.effectiveSectionType

        // 1. Save schedule target, section type & add to favorites
        val currentFavs = if (isFirstOnboarding) {
            mutableSetOf()
        } else {
            schedulePrefs.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, null)?.toMutableSet() ?: mutableSetOf()
        }
        if (target.isNotBlank()) {
            currentFavs.add(FavoriteTarget(target = target, sectionType = sectionType).toSerializedString())
        }

        schedulePrefs.edit()
            .putString(ScheduleSyncWorker.KEY_CURRENT_TARGET, target)
            .putString(ScheduleSyncWorker.KEY_CURRENT_SECTION, sectionType)
            .putStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, currentFavs)
            .commit()

        // 2. Save settings & mark onboarding completed
        settingsPrefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putString("theme_mode", state.themeMode)
            .putBoolean("dynamic_color", state.dynamicColor)
            .putBoolean("notify_lessons_enabled", state.notifyLessonsEnabled)
            .putInt("default_alarm_mins", state.defaultAlarmMinutes)
            .putBoolean("shake_to_report_enabled", state.shakeToReportEnabled)
            .apply()

        _uiState.update { it.copy(isOnboardingCompleted = true) }

        // 3. Sync and reschedule alarms in background
        viewModelScope.launch(Dispatchers.IO) {
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

                // Update Glance widgets immediately
                WidgetUpdateHelper.updateAllWidgets(context)
            } catch (e: Exception) {
                android.util.Log.e("OnboardingViewModel", "Error finishing onboarding background tasks", e)
            }
        }
    }

    /**
     * Resets onboarding state allowing the user to restart the setup wizard.
     */
    fun resetOnboarding() {
        settingsPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
        _uiState.update { it.copy(isOnboardingCompleted = false) }
    }
}
