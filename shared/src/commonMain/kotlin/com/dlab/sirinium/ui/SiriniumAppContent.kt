package com.dlab.sirinium.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.onboarding.OnboardingViewModel
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsViewModel
import com.dlab.sirinium.ui.theme.SiriniumTheme
import com.dlab.sirinium.ui.update.AppUpdateViewModel
import org.koin.compose.koinInject

@Composable
fun SiriniumAppContent(
    scheduleViewModel: ScheduleViewModel = koinInject(),
    compareViewModel: CompareViewModel = koinInject(),
    freeClassroomsViewModel: FreeClassroomsViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    appUpdateViewModel: AppUpdateViewModel = koinInject(),
    onboardingViewModel: OnboardingViewModel = koinInject(),
    onRestartOnboarding: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    val isOnboardingCompleted = onboardingState.isOnboardingCompleted
    val activeThemeMode = if (!isOnboardingCompleted) onboardingState.themeMode else settingsState.themeMode
    val activeDynamicColor = if (!isOnboardingCompleted) onboardingState.dynamicColor else settingsState.dynamicColor

    val isDark = when (activeThemeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    SiriniumTheme(darkTheme = isDark, dynamicColor = activeDynamicColor) {
        MainScreen(
            scheduleViewModel = scheduleViewModel,
            compareViewModel = compareViewModel,
            freeClassroomsViewModel = freeClassroomsViewModel,
            settingsViewModel = settingsViewModel,
            appUpdateViewModel = appUpdateViewModel,
            onRestartOnboarding = onRestartOnboarding,
            onOpenFeedback = onOpenFeedback,
            onRequestNotificationPermission = onRequestNotificationPermission,
            modifier = modifier
        )
    }
}
