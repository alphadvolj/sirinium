package com.dlab.sirinium.platform

import androidx.compose.runtime.staticCompositionLocalOf

interface PlatformActions {
    fun openUrl(url: String)
    fun copyToClipboard(text: String)
    fun showToast(message: String)
    fun shareText(text: String) {
        copyToClipboard(text)
    }
    fun shareFeedback(subject: String, body: String, attachments: List<String>) {
        shareText("$subject\n\n$body")
    }
    fun pickImages(maxCount: Int, onResult: (List<String>) -> Unit) {
        onResult(emptyList())
    }
    fun getPlatformName(): String
}

interface PlatformSettings {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)
}

expect fun createPlatformSettings(): PlatformSettings
expect fun createPlatformActions(): PlatformActions

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> {
    createPlatformActions()
}

val LocalPlatformSettings = staticCompositionLocalOf<PlatformSettings> {
    createPlatformSettings()
}

@androidx.compose.runtime.Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
