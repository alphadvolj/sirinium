package com.dlab.sirinium.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AndroidPlatformContextHolder {
    var appContext: Context? = null
}

class AndroidPlatformSettings(private val context: Context) : PlatformSettings {
    private val prefs = context.getSharedPreferences("sirinium_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> = prefs.getStringSet(key, defaultValue) ?: defaultValue
    override fun putStringSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, value).apply() }
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun shareText(text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, "Поделиться").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyToClipboard(text)
        }
    }

    override fun getPlatformName(): String = "Android"
}

actual fun createPlatformSettings(): PlatformSettings {
    val ctx = AndroidPlatformContextHolder.appContext
        ?: error("AndroidPlatformContextHolder.appContext must be initialized in Android Application")
    return AndroidPlatformSettings(ctx)
}

actual fun createPlatformActions(): PlatformActions {
    val ctx = AndroidPlatformContextHolder.appContext
        ?: error("AndroidPlatformContextHolder.appContext must be initialized in Android Application")
    return AndroidPlatformActions(ctx)
}

@androidx.compose.runtime.Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = NoOpAlarmScheduler()

actual fun createPlatformWidgetUpdater(
    settings: PlatformSettings,
    repository: com.dlab.sirinium.domain.repository.ScheduleRepository
): PlatformWidgetUpdater = NoOpWidgetUpdater()
