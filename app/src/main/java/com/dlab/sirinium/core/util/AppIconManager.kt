package com.dlab.sirinium.core.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    const val ALIAS_DEFAULT = "com.dlab.sirinium.MainActivityDefault"
    const val ALIAS_CUSTOM = "com.dlab.sirinium.MainActivityCustom"

    fun setCustomIconEnabled(context: Context, enabled: Boolean) {
        val pm = context.packageManager
        val defaultComponent = ComponentName(context, ALIAS_DEFAULT)
        val customComponent = ComponentName(context, ALIAS_CUSTOM)

        try {
            if (enabled) {
                pm.setComponentEnabledSetting(
                    customComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    defaultComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    defaultComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    customComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            Log.d("AppIconManager", "App icon custom enabled: $enabled")
        } catch (e: Exception) {
            Log.e("AppIconManager", "Failed to switch app icon", e)
        }
    }

    fun isCustomIconEnabled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val customComponent = ComponentName(context, ALIAS_CUSTOM)
            val state = pm.getComponentEnabledSetting(customComponent)
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (_: Exception) {
            false
        }
    }
}
