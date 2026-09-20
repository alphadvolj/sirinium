package com.dlab.sirinium.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.AppInfoDto
import com.dlab.sirinium.data.remote.dto.AppUpdateDto
import com.dlab.sirinium.platform.PlatformSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val isUpdateAvailable: Boolean = false,
    val showUpdateScreen: Boolean = false,
    val updateInfo: AppUpdateDto? = null,
    val showInfoScreen: Boolean = false,
    val appInfo: AppInfoDto? = null
)

class AppUpdateViewModel(
    private val api: SiriusScheduleApi,
    private val settings: PlatformSettings
) : ViewModel() {

    companion object {
        const val PREFS_NAME = "sirinium_app_update_prefs"
        const val KEY_LAST_DISMISSED_VERSION = "last_dismissed_version_code"
        const val KEY_LAST_DISMISSED_TIME = "last_dismissed_time_millis"
        const val KEY_LAST_SEEN_INFO_DATE = "last_seen_info_date"
        const val SNOOZE_DURATION_MILLIS = 6 * 60 * 60 * 1000L // 6 hours
    }

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdatesAndInfo()
    }

    fun checkForUpdatesAndInfo() {
        viewModelScope.launch {
            checkAppUpdatesAndInfoInternal()
        }
    }

    private suspend fun checkAppUpdatesAndInfoInternal() {
        var updateDto: AppUpdateDto? = null
        try {
            updateDto = api.getAppUpdate()
        } catch (_: Exception) {}

        var infoDto: AppInfoDto? = null
        try {
            infoDto = api.getAppInfo()
        } catch (_: Exception) {}

        val currentVersion = 301 // Sirinium version 3.0.1
        val lastDismissedVersion = settings.getLong(KEY_LAST_DISMISSED_VERSION, 0L)
        val lastDismissedTime = settings.getLong(KEY_LAST_DISMISSED_TIME, 0L)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val isSnoozed = (now - lastDismissedTime) < SNOOZE_DURATION_MILLIS

        val hasNewUpdate = updateDto != null &&
                updateDto.versionCode > currentVersion &&
                (updateDto.versionCode.toLong() != lastDismissedVersion || !isSnoozed)

        val lastSeenInfoDate = settings.getString(KEY_LAST_SEEN_INFO_DATE, "")
        val hasNewInfo = infoDto != null &&
                infoDto.text.isNotBlank() &&
                infoDto.date.isNotBlank() &&
                infoDto.date != lastSeenInfoDate

        _uiState.update { current ->
            current.copy(
                isUpdateAvailable = updateDto != null && updateDto.versionCode > currentVersion,
                showUpdateScreen = hasNewUpdate,
                updateInfo = updateDto,
                showInfoScreen = hasNewInfo && !hasNewUpdate,
                appInfo = infoDto
            )
        }
    }

    fun snoozeUpdate() {
        val update = _uiState.value.updateInfo
        if (update != null) {
            settings.putLong(KEY_LAST_DISMISSED_VERSION, update.versionCode.toLong())
            settings.putLong(KEY_LAST_DISMISSED_TIME, kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        }
        _uiState.update { it.copy(showUpdateScreen = false) }
    }

    fun dismissInfo() {
        val info = _uiState.value.appInfo
        if (info != null && info.date.isNotBlank()) {
            settings.putString(KEY_LAST_SEEN_INFO_DATE, info.date)
        }
        _uiState.update { it.copy(showInfoScreen = false) }
    }

    fun openUpdateScreenManually() {
        if (_uiState.value.updateInfo != null) {
            _uiState.update { it.copy(showUpdateScreen = true, showInfoScreen = false) }
        }
    }

    fun openInfoScreenManually() {
        if (_uiState.value.appInfo != null) {
            _uiState.update { it.copy(showInfoScreen = true, showUpdateScreen = false) }
        }
    }
}
