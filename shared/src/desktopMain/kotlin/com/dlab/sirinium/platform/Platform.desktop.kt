package com.dlab.sirinium.platform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.util.prefs.Preferences

class DesktopPlatformSettings : PlatformSettings {
    private val prefs = Preferences.userNodeForPackage(DesktopPlatformSettings::class.java)

    override fun getString(key: String, defaultValue: String): String = prefs.get(key, defaultValue)
    override fun putString(key: String, value: String) { prefs.put(key, value) }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) { prefs.putBoolean(key, value) }

    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) { prefs.putLong(key, value) }

    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) { prefs.putInt(key, value) }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        val raw = prefs.get(key, null) ?: return defaultValue
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        prefs.put(key, value.joinToString("\n"))
    }
}

class DesktopPlatformActions : PlatformActions {
    override fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (_: Exception) {}
    }

    override fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (_: Exception) {}
    }

    override fun showToast(message: String) {
        println("[Notification] $message")
    }

    override fun getPlatformName(): String = "Desktop (${System.getProperty("os.name")})"
}

actual fun createPlatformSettings(): PlatformSettings = DesktopPlatformSettings()
actual fun createPlatformActions(): PlatformActions = DesktopPlatformActions()

@androidx.compose.runtime.Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = NoOpAlarmScheduler()

actual fun createPlatformWidgetUpdater(
    settings: PlatformSettings,
    repository: com.dlab.sirinium.domain.repository.ScheduleRepository
): PlatformWidgetUpdater = NoOpWidgetUpdater()
