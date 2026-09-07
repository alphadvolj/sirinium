package com.dlab.sirinium.ui.settings

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.sync.ScheduleSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.dlab.sirinium.core.util.AppIconManager
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.ui.schedule.ScheduleViewModel

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
    private val context: Context,
    private val api: SiriusScheduleApi,
    private val alarmScheduler: LessonAlarmScheduler,
    private val repository: ScheduleRepository,
    private val lessonNoteRepository: LessonNoteRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
    private var easterEggTapCount = 0

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = prefs.getString("theme_mode", "system") ?: "system",
            dynamicColor = prefs.getBoolean("dynamic_color", true),
            notifyLessonsEnabled = prefs.getBoolean("notify_lessons_enabled", true),
            defaultAlarmMinutes = prefs.getInt("default_alarm_mins", 15),
            autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true),
            autoSyncIntervalMinutes = prefs.getLong("auto_sync_interval_mins", 120L),
            shakeToReportEnabled = prefs.getBoolean("shake_to_report_enabled", true),
            isEasterEggUnlocked = prefs.getBoolean("easter_egg_unlocked", false),
            isCustomIconEnabled = prefs.getBoolean("custom_icon_enabled", false)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkServerStatus()
    }

    fun setShakeToReportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shake_to_report_enabled", enabled).apply()
        _uiState.update { it.copy(shakeToReportEnabled = enabled) }
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).commit()
        _uiState.update { it.copy(themeMode = mode) }
        updateWidgets()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).commit()
        _uiState.update { it.copy(dynamicColor = enabled) }
        updateWidgets()
    }

    private fun updateWidgets() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                com.dlab.sirinium.widget.ScheduleGlanceWidget().updateAll(context)
                com.dlab.sirinium.widget.ScheduleListGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error updating widgets", e)
            }
        }
    }

    fun reloadFromPreferences() {
        _uiState.update {
            it.copy(
                themeMode = prefs.getString("theme_mode", "system") ?: "system",
                dynamicColor = prefs.getBoolean("dynamic_color", true),
                notifyLessonsEnabled = prefs.getBoolean("notify_lessons_enabled", true),
                defaultAlarmMinutes = prefs.getInt("default_alarm_mins", 15),
                autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true),
                autoSyncIntervalMinutes = prefs.getLong("auto_sync_interval_mins", 120L),
                shakeToReportEnabled = prefs.getBoolean("shake_to_report_enabled", true)
            )
        }
    }

    fun setNotifyLessonsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notify_lessons_enabled", enabled).apply()
        _uiState.update { it.copy(notifyLessonsEnabled = enabled) }

        viewModelScope.launch(Dispatchers.IO) {
            val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
            val currentTarget = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, ScheduleSyncWorker.DEFAULT_TARGET)
                ?: ScheduleSyncWorker.DEFAULT_TARGET
            val currentSection = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, ScheduleSyncWorker.DEFAULT_SECTION)
                ?: ScheduleSyncWorker.DEFAULT_SECTION
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
        prefs.edit().putInt("default_alarm_mins", minutes).apply()
        _uiState.update { it.copy(defaultAlarmMinutes = minutes) }

        if (_uiState.value.notifyLessonsEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                val currentTarget = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, ScheduleSyncWorker.DEFAULT_TARGET)
                    ?: ScheduleSyncWorker.DEFAULT_TARGET
                val currentSection = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, ScheduleSyncWorker.DEFAULT_SECTION)
                    ?: ScheduleSyncWorker.DEFAULT_SECTION
                val cachedLessons = repository.getSchedule(
                    ScheduleFilter(target = currentTarget, sectionType = currentSection)
                ).first().dataOrNull ?: emptyList()
                alarmScheduler.rescheduleAlarmsForLessons(cachedLessons, minutes)
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).commit()
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
        updateSyncSchedule()
    }

    fun setAutoSyncInterval(intervalMinutes: Long) {
        prefs.edit().putLong("auto_sync_interval_mins", intervalMinutes).commit()
        _uiState.update { it.copy(autoSyncIntervalMinutes = intervalMinutes) }
        if (_uiState.value.autoSyncEnabled) {
            updateSyncSchedule()
        }
    }

    private fun updateSyncSchedule() {
        val enabled = _uiState.value.autoSyncEnabled
        val interval = _uiState.value.autoSyncIntervalMinutes
        if (enabled) {
            com.dlab.sirinium.sync.SyncScheduler.schedulePeriodicSync(context, interval)
        } else {
            com.dlab.sirinium.sync.SyncScheduler.cancelPeriodicSync(context)
        }
    }

    fun checkServerStatus() {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
        val rawSet = schedulePrefs.getStringSet(ScheduleViewModel.KEY_FAVORITE_TARGETS, null)
            ?: schedulePrefs.getStringSet("favorite_targets", null)
            ?: emptySet()
        val favorites = rawSet.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        val currentSection = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, null)

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
                AppIconManager.setCustomIconEnabled(context, true)
                prefs.edit()
                    .putBoolean("easter_egg_unlocked", true)
                    .putBoolean("custom_icon_enabled", true)
                    .apply()

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
            AppIconManager.setCustomIconEnabled(context, false)
            prefs.edit()
                .putBoolean("easter_egg_unlocked", false)
                .putBoolean("custom_icon_enabled", false)
                .apply()
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
            AppIconManager.setCustomIconEnabled(context, true)
            prefs.edit().putBoolean("custom_icon_enabled", true).apply()
            _uiState.update { it.copy(isCustomIconEnabled = true) }
        }
    }
}
