package com.dlab.sirinium.ui.update

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.AppInfoDto
import com.dlab.sirinium.data.remote.dto.AppUpdateDto
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
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch /api/appupdate", e)
        }

        var infoDto: AppInfoDto? = null
        try {
            infoDto = api.getAppInfo()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch /api/appinfo", e)
        }

        val currentVersion = getCurrentVersionCode()
        val isUpdateAvailable = updateDto != null && updateDto.versionCode > currentVersion
        val isSnoozed = isUpdateSnoozed()
        val shouldShowUpdate = isUpdateAvailable && !isSnoozed

        val hasUnreadInfo = if (infoDto != null && infoDto.text.isNotBlank()) {
            val infoKey = "${infoDto.date}_${infoDto.time}_${infoDto.text}"
            val lastReadInfoKey = prefs.getString(KEY_LAST_READ_INFO_KEY, null)
            lastReadInfoKey != infoKey
        } else {
            false
        }

        Log.d(
            TAG,
            "Check finished: isUpdateAvailable=$isUpdateAvailable (shouldShow=$shouldShowUpdate), hasUnreadInfo=$hasUnreadInfo"
        )

        _uiState.update {
            it.copy(
                isUpdateAvailable = isUpdateAvailable,
                updateInfo = updateDto,
                showUpdateScreen = shouldShowUpdate,
                appInfo = infoDto,
                // Priority: Show update first. Only show info if update is not being shown.
                showInfoScreen = !shouldShowUpdate && hasUnreadInfo
            )
        }
    }

    fun openUpdateScreen() {
        if (_uiState.value.isUpdateAvailable) {
            _uiState.update {
                it.copy(
                    showUpdateScreen = true,
                    showInfoScreen = false
                )
            }
        }
    }

    fun snoozeUpdate() {
        prefs.edit().putLong(KEY_UPDATE_SNOOZE_TIMESTAMP, System.currentTimeMillis()).apply()
        onUpdateScreenClosed()
    }

    fun dismissUpdateScreen() {
        onUpdateScreenClosed()
    }

    private fun onUpdateScreenClosed() {
        val hasUnreadInfo = isUnreadInfoAvailable()
        _uiState.update {
            it.copy(
                showUpdateScreen = false,
                // Sequentially show info screen right after update screen is closed/snoozed!
                showInfoScreen = hasUnreadInfo
            )
        }
    }

    private fun isUnreadInfoAvailable(): Boolean {
        val currentInfo = _uiState.value.appInfo ?: return false
        if (currentInfo.text.isBlank()) return false
        val infoKey = "${currentInfo.date}_${currentInfo.time}_${currentInfo.text}"
        val lastReadInfoKey = prefs.getString(KEY_LAST_READ_INFO_KEY, null)
        return lastReadInfoKey != infoKey
    }

    fun dismissInfoScreen() {
        val currentInfo = _uiState.value.appInfo
        if (currentInfo != null) {
            val infoKey = "${currentInfo.date}_${currentInfo.time}_${currentInfo.text}"
            prefs.edit().putString(KEY_LAST_READ_INFO_KEY, infoKey).apply()
        }

        _uiState.update {
            it.copy(
                showInfoScreen = false
            )
        }
    }

    private fun isUpdateSnoozed(): Boolean {
        val lastSnooze = prefs.getLong(KEY_UPDATE_SNOOZE_TIMESTAMP, 0L)
        val elapsed = System.currentTimeMillis() - lastSnooze
        return elapsed in 0 until SNOOZE_DURATION_MILLIS
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            301L
        }
    }

    companion object {
        private const val TAG = "AppUpdateViewModel"
        private const val PREFS_NAME = "sirinium_app_updates"
        private const val KEY_UPDATE_SNOOZE_TIMESTAMP = "update_snooze_timestamp"
        private const val KEY_LAST_READ_INFO_KEY = "last_read_app_info_key"
        private const val SNOOZE_DURATION_MILLIS = 6 * 60 * 60 * 1000L // 6 hours
    }
}
