package com.dlab.sirinium.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.dlab.sirinium.ui.theme.ExpressiveDarkColorScheme
import com.dlab.sirinium.ui.theme.ExpressiveLightColorScheme

object GlanceThemeHelper {

    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
        return prefs.getString("theme_mode", "system") ?: "system"
    }

    fun isDynamicColorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("dynamic_color", true)
    }

    fun getColorProviders(context: Context): ColorProviders {
        val themeMode = getThemeMode(context)
        val dynamicColor = isDynamicColorEnabled(context)
        val canUseDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val lightScheme = if (canUseDynamic) dynamicLightColorScheme(context) else ExpressiveLightColorScheme
        val darkScheme = if (canUseDynamic) dynamicDarkColorScheme(context) else ExpressiveDarkColorScheme

        return when (themeMode) {
            "light" -> ColorProviders(scheme = lightScheme)
            "dark" -> ColorProviders(scheme = darkScheme)
            else -> ColorProviders(light = lightScheme, dark = darkScheme)
        }
    }

    fun adaptiveColor(
        context: Context,
        dayColor: Color,
        nightColor: Color
    ): GlanceColorProvider {
        val themeMode = getThemeMode(context)
        return when (themeMode) {
            "light" -> androidx.glance.unit.ColorProvider(color = dayColor)
            "dark" -> androidx.glance.unit.ColorProvider(color = nightColor)
            else -> ColorProvider(day = dayColor, night = nightColor)
        }
    }
}
