package com.dlab.sirinium.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.platform.PlatformAlarmScheduler
import com.dlab.sirinium.platform.PlatformIconManager
import com.dlab.sirinium.platform.PlatformSettings
import com.dlab.sirinium.platform.PlatformSyncScheduler
import com.dlab.sirinium.platform.PlatformWidgetUpdater
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system", // "system", "light", "dark"
    val dynamicColor: Boolean = true,
    val notifyLessonsEnabled: Boolean = true,
    val defaultAlarmMinutes: Int = 15,
    val autoSyncEnabled: Boolean = true,
    val autoSyncIntervalMinutes: Long = 120L,
    val shakeToReportEnabled: Boolean = true,
    val serverStatus: String? = null,
    val userMessage: String? = null,
    val isEasterEggUnlocked: Boolean = false,
    val isCustomIconEnabled: Boolean = false
)

class SettingsViewModel(
    private val api: SiriusScheduleApi,
    private val alarmScheduler: PlatformAlarmScheduler,
    private val repository: ScheduleRepository,
    private val lessonNoteRepository: LessonNoteRepository,
    private val widgetUpdater: PlatformWidgetUpdater,
    private val iconManager: PlatformIconManager,
    private val syncScheduler: PlatformSyncScheduler,
    private val settings: PlatformSettings
) : ViewModel() {

    private var easterEggTapCount = 0

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settings.getString("theme_mode", "system").ifBlank { "system" },
            dynamicColor = settings.getBoolean("dynamic_color", true),
            notifyLessonsEnabled = settings.getBoolean("notify_lessons_enabled", true),
            defaultAlarmMinutes = settings.getInt("default_alarm_mins", 15),
            autoSyncEnabled = settings.getBoolean("auto_sync_enabled", true),
            autoSyncIntervalMinutes = settings.getLong("auto_sync_interval_mins", 120L),
            shakeToReportEnabled = settings.getBoolean("shake_to_report_enabled", true),
            isEasterEggUnlocked = settings.getBoolean("easter_egg_unlocked", false),
            isCustomIconEnabled = settings.getBoolean("custom_icon_enabled", false)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkServerStatus()
    }

    fun setShakeToReportEnabled(enabled: Boolean) {
        settings.putBoolean("shake_to_report_enabled", enabled)
        _uiState.update { it.copy(shakeToReportEnabled = enabled) }
    }

    fun setThemeMode(mode: String) {
        settings.putString("theme_mode", mode)
        _uiState.update { it.copy(themeMode = mode) }
        widgetUpdater.updateWidgets()
    }

    fun setDynamicColor(enabled: Boolean) {
        settings.putBoolean("dynamic_color", enabled)
        _uiState.update { it.copy(dynamicColor = enabled) }
        widgetUpdater.updateWidgets()
    }

    fun reloadFromPreferences() {
        _uiState.update {
            it.copy(
                themeMode = settings.getString("theme_mode", "system").ifBlank { "system" },
                dynamicColor = settings.getBoolean("dynamic_color", true),
                notifyLessonsEnabled = settings.getBoolean("notify_lessons_enabled", true),
                defaultAlarmMinutes = settings.getInt("default_alarm_mins", 15),
                autoSyncEnabled = settings.getBoolean("auto_sync_enabled", true),
                autoSyncIntervalMinutes = settings.getLong("auto_sync_interval_mins", 120L),
                shakeToReportEnabled = settings.getBoolean("shake_to_report_enabled", true)
            )
        }
    }

    fun setNotifyLessonsEnabled(enabled: Boolean) {
        settings.putBoolean("notify_lessons_enabled", enabled)
        _uiState.update { it.copy(notifyLessonsEnabled = enabled) }

        viewModelScope.launch {
            val currentTarget = settings.getString(ScheduleViewModel.KEY_CURRENT_TARGET, "К1609-241").ifBlank { "К1609-241" }
            val currentSection = settings.getString(ScheduleViewModel.KEY_CURRENT_SECTION, "group").ifBlank { "group" }
            val cachedLessons = repository.getSchedule(
                ScheduleFilter(target = currentTarget, sectionType = currentSection)
            ).first().dataOrNull ?: emptyList()

            if (enabled) {
                alarmScheduler.rescheduleAlarmsForLessons(cachedLessons, _uiState.value.defaultAlarmMinutes)
            } else {
                alarmScheduler.cancelAllAlarms()
            }
        }
    }

    fun setDefaultAlarmMinutes(minutes: Int) {
        settings.putInt("default_alarm_mins", minutes)
        _uiState.update { it.copy(defaultAlarmMinutes = minutes) }

        if (_uiState.value.notifyLessonsEnabled) {
            viewModelScope.launch {
                val currentTarget = settings.getString(ScheduleViewModel.KEY_CURRENT_TARGET, "К1609-241").ifBlank { "К1609-241" }
                val currentSection = settings.getString(ScheduleViewModel.KEY_CURRENT_SECTION, "group").ifBlank { "group" }
                val cachedLessons = repository.getSchedule(
                    ScheduleFilter(target = currentTarget, sectionType = currentSection)
                ).first().dataOrNull ?: emptyList()
                alarmScheduler.rescheduleAlarmsForLessons(cachedLessons, minutes)
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        settings.putBoolean("auto_sync_enabled", enabled)
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
        updateSyncSchedule()
    }

    fun setAutoSyncInterval(intervalMinutes: Long) {
        settings.putLong("auto_sync_interval_mins", intervalMinutes)
        _uiState.update { it.copy(autoSyncIntervalMinutes = intervalMinutes) }
        if (_uiState.value.autoSyncEnabled) {
            updateSyncSchedule()
        }
    }

    private fun updateSyncSchedule() {
        val enabled = _uiState.value.autoSyncEnabled
        val interval = _uiState.value.autoSyncIntervalMinutes
        if (enabled) {
            syncScheduler.schedulePeriodicSync(interval)
        } else {
            syncScheduler.cancelPeriodicSync()
        }
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            try {
                val status = api.getStatus()
                _uiState.update {
                    it.copy(serverStatus = "Сервер онлайн • Синхронизация: ${status.lastRunAt ?: "Недавно"}")
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(serverStatus = "Сервер доступен в автономном режиме")
                }
            }
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            try {
                lessonNoteRepository.deleteAllNotes()
                _uiState.update { it.copy(userMessage = "Все сохранённые заметки удалены") }
            } catch (_: Exception) {
                _uiState.update { it.copy(userMessage = "Ошибка при удалении заметок") }
            }
        }
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun hasSavedTeacher(): Boolean {
        val rawSet = settings.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS)
        val favorites = rawSet.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        val currentSection = settings.getString(ScheduleViewModel.KEY_CURRENT_SECTION, "")

        val hasTeacherInFavorites = favorites.any { it.sectionType.equals("teacher", ignoreCase = true) } ||
                rawSet.any { it.startsWith("teacher:", ignoreCase = true) }
        val hasTeacherAsCurrent = currentSection.equals("teacher", ignoreCase = true)

        return hasTeacherInFavorites || hasTeacherAsCurrent
    }

    fun onEasterEggLogoClick() {
        if (_uiState.value.isEasterEggUnlocked) return

        easterEggTapCount++
        if (easterEggTapCount >= 10) {
            easterEggTapCount = 0

            if (hasSavedTeacher()) {
                _uiState.update {
                    it.copy(userMessage = "Упс, почти, но что-то мешает")
                }
            } else {
                iconManager.setCustomIconEnabled(true)
                settings.putBoolean("easter_egg_unlocked", true)
                settings.putBoolean("custom_icon_enabled", true)

                _uiState.update {
                    it.copy(
                        isEasterEggUnlocked = true,
                        isCustomIconEnabled = true,
                        userMessage = "Поздравляю, вы нашли пасхалку"
                    )
                }
            }
        }
    }

    fun setCustomIconEnabled(enabled: Boolean) {
        if (!enabled) {
            iconManager.setCustomIconEnabled(false)
            settings.putBoolean("easter_egg_unlocked", false)
            settings.putBoolean("custom_icon_enabled", false)
            easterEggTapCount = 0
            _uiState.update {
                it.copy(
                    isEasterEggUnlocked = false,
                    isCustomIconEnabled = false
                )
            }
        } else {
            if (hasSavedTeacher()) {
                _uiState.update {
                    it.copy(userMessage = "Упс, почти, но что-то мешает")
                }
                return
            }
            iconManager.setCustomIconEnabled(true)
            settings.putBoolean("custom_icon_enabled", true)
            _uiState.update { it.copy(isCustomIconEnabled = true) }
        }
    }
}
