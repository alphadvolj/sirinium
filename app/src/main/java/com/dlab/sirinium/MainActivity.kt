package com.dlab.sirinium

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.notification.ScheduleNotificationManager
import com.dlab.sirinium.ui.MainScreen
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.navigation.NavRoutes
import com.dlab.sirinium.ui.onboarding.OnboardingViewModel
import com.dlab.sirinium.ui.onboarding.steps.AlarmStep
import com.dlab.sirinium.ui.onboarding.steps.CompletionStep
import com.dlab.sirinium.ui.onboarding.steps.ShakeStep
import com.dlab.sirinium.ui.onboarding.steps.TargetStep
import com.dlab.sirinium.ui.onboarding.steps.ThemeStep
import com.dlab.sirinium.ui.onboarding.steps.TutorialStep
import com.dlab.sirinium.ui.onboarding.steps.WelcomeStep
import com.dlab.sirinium.ui.schedule.ScheduleUiIntent
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsViewModel
import com.dlab.sirinium.ui.theme.SiriniumTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val alarmScheduler: LessonAlarmScheduler by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scheduleViewModel: ScheduleViewModel = koinViewModel()
            val compareViewModel: CompareViewModel = koinViewModel()
            val freeClassroomsViewModel: FreeClassroomsViewModel = koinViewModel()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val onboardingViewModel: OnboardingViewModel = koinViewModel()

            val scheduleUiState by scheduleViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()
            val onboardingState by onboardingViewModel.uiState.collectAsState()

            val isOnboardingCompleted = onboardingState.isOnboardingCompleted

            // Dynamic theme preview: adapts in real-time if user changes theme during onboarding
            val activeThemeMode = if (!isOnboardingCompleted) onboardingState.themeMode else settingsState.themeMode
            val activeDynamicColor = if (!isOnboardingCompleted) onboardingState.dynamicColor else settingsState.dynamicColor

            val isDark = when (activeThemeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SiriniumTheme(darkTheme = isDark, dynamicColor = activeDynamicColor) {
                val colorScheme = MaterialTheme.colorScheme
                val view = LocalView.current

                // Dynamically synchronize the OS Window background with Compose theme
                // to eliminate white flashes during transitions and animations throughout the app
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as? Activity)?.window
                        if (window != null) {
                            val bgArgb = colorScheme.background.toArgb()
                            window.setBackgroundDrawable(ColorDrawable(bgArgb))
                            window.decorView.setBackgroundColor(bgArgb)
                        }
                    }
                }

                // Synchronize edge-to-edge system bars style
                DisposableEffect(isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        },
                        navigationBarStyle = if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        }
                    )
                    onDispose {}
                }

                // Dynamically synchronize app label and task description in OS Recent Apps with easter egg state
                val isEasterEggActive = settingsState.isEasterEggUnlocked && settingsState.isCustomIconEnabled
                DisposableEffect(isEasterEggActive) {
                    val appDisplayName = if (isEasterEggActive) "Zernovium" else "Sirinium"
                    val activity = view.context as? Activity
                    if (activity != null) {
                        activity.title = appDisplayName
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                activity.setTaskDescription(
                                    android.app.ActivityManager.TaskDescription.Builder()
                                        .setLabel(appDisplayName)
                                        .build()
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                activity.setTaskDescription(
                                    android.app.ActivityManager.TaskDescription(appDisplayName)
                                )
                            }
                        } catch (_: Exception) {}
                    }
                    onDispose {}
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (isOnboardingCompleted) NavRoutes.MAIN else NavRoutes.ONBOARDING_WELCOME
                    var showTutorialOnMain by remember { mutableStateOf(false) }

                    // Helper to save all user choices, update ViewModels and navigate into app
                    val finishOnboarding = { startTutorial: Boolean ->
                        onboardingViewModel.completeOnboarding()
                        val target = onboardingState.effectiveTarget
                        val sectionType = onboardingState.effectiveSectionType
                        scheduleViewModel.onIntent(ScheduleUiIntent.ChangeTarget(target, sectionType))
                        scheduleViewModel.onIntent(ScheduleUiIntent.ReloadFavorites)
                        compareViewModel.refreshFavorites()
                        settingsViewModel.reloadFromPreferences()
                        showTutorialOnMain = startTutorial
                        navController.navigate(NavRoutes.MAIN) {
                            popUpTo(NavRoutes.ONBOARDING_WELCOME) { inclusive = true }
                        }
                    }

                    // Handle deep-link from notifications or widgets
                    LaunchedEffect(intent) {
                        val lessonId = intent?.getStringExtra(ScheduleNotificationManager.EXTRA_LESSON_ID)
                        if (!lessonId.isNullOrBlank()) {
                            val lesson = scheduleUiState.lessons.find { it.id == lessonId }
                            if (lesson != null) {
                                scheduleViewModel.onIntent(ScheduleUiIntent.SelectLessonForDetails(lesson))
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                    // 1. Приветствие (Welcome / Intro screen)
                    composable(
                        route = NavRoutes.ONBOARDING_WELCOME,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        WelcomeStep(
                            onNext = { navController.navigate(NavRoutes.ONBOARDING_TARGET) }
                        )
                    }

                    // 2. Выбор расписания (Target selection: group, teacher, classroom)
                    composable(
                        route = NavRoutes.ONBOARDING_TARGET,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        TargetStep(
                            state = onboardingState,
                            onSelectTarget = onboardingViewModel::selectTarget,
                            onSetCustomTarget = onboardingViewModel::setCustomTarget,
                            onSetCustomTargetInput = onboardingViewModel::setCustomTargetInput,
                            onSetNotifyDevelopers = onboardingViewModel::setNotifyDevelopersAboutCustom,
                            onBack = { navController.popBackStack() },
                            onRefresh = { onboardingViewModel.refreshEntities() },
                            onNext = {
                                onboardingViewModel.savePreferences()
                                settingsViewModel.reloadFromPreferences()
                                navController.navigate(NavRoutes.ONBOARDING_THEME)
                            }
                        )
                    }

                    // 3. Внешний вид (Theme mode & Dynamic color)
                    composable(
                        route = NavRoutes.ONBOARDING_THEME,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        ThemeStep(
                            state = onboardingState,
                            onSetThemeMode = onboardingViewModel::setThemeMode,
                            onSetDynamicColor = onboardingViewModel::setDynamicColor,
                            onBack = { navController.popBackStack() },
                            onNext = {
                                onboardingViewModel.savePreferences()
                                settingsViewModel.reloadFromPreferences()
                                navController.navigate(NavRoutes.ONBOARDING_ALARM)
                            }
                        )
                    }

                    // 4. Уведомления о парах (Alarm minutes)
                    composable(
                        route = NavRoutes.ONBOARDING_ALARM,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        AlarmStep(
                            state = onboardingState,
                            onSetNotifyLessons = onboardingViewModel::setNotifyLessons,
                            onSetDefaultAlarmMinutes = onboardingViewModel::setDefaultAlarmMinutes,
                            onBack = { navController.popBackStack() },
                            onNext = {
                                onboardingViewModel.savePreferences()
                                settingsViewModel.reloadFromPreferences()
                                if (onboardingState.notifyLessonsEnabled) {
                                    requestNotificationPermission()
                                }
                                navController.navigate(NavRoutes.ONBOARDING_SHAKE)
                            }
                        )
                    }

                    // 5. Потряси и сообщи (Shake to report)
                    composable(
                        route = NavRoutes.ONBOARDING_SHAKE,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        ShakeStep(
                            state = onboardingState,
                            onSetShakeToReport = onboardingViewModel::setShakeToReport,
                            onBack = { navController.popBackStack() },
                            onNext = {
                                onboardingViewModel.savePreferences()
                                settingsViewModel.reloadFromPreferences()
                                navController.navigate(NavRoutes.ONBOARDING_COMPLETE)
                            }
                        )
                    }

                    // 6. Всё готово! (Completion screen before real app tutorial)
                    composable(
                        route = NavRoutes.ONBOARDING_COMPLETE,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        }
                    ) {
                        CompletionStep(
                            state = onboardingState,
                            onBack = { navController.popBackStack() },
                            onComplete = {
                                finishOnboarding(true)
                            }
                        )
                    }

                    // Fallback route for tutorial redirecting directly into real app
                    composable(route = NavRoutes.ONBOARDING_TUTORIAL) {
                        LaunchedEffect(Unit) {
                            showTutorialOnMain = true
                            navController.navigate(NavRoutes.MAIN) {
                                popUpTo(NavRoutes.ONBOARDING_WELCOME) { inclusive = true }
                            }
                        }
                    }

                    // 7. Само приложение (Main Dashboard with interactive tutorial overlay)
                    composable(
                        route = NavRoutes.MAIN,
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        MainScreen(
                            scheduleViewModel = scheduleViewModel,
                            compareViewModel = compareViewModel,
                            freeClassroomsViewModel = freeClassroomsViewModel,
                            settingsViewModel = settingsViewModel,
                            showTutorial = showTutorialOnMain,
                            onDismissTutorial = { showTutorialOnMain = false },
                            onRestartOnboarding = {
                                onboardingViewModel.resetOnboarding()
                                navController.navigate(NavRoutes.ONBOARDING_WELCOME) {
                                    popUpTo(NavRoutes.MAIN) { inclusive = true }
                                }
                            },
                            onOpenTutorial = {
                                showTutorialOnMain = true
                            },
                            onRequestNotificationPermission = {
                                requestNotificationPermission()
                            }
                        )
                    }
                }
            }
        }
    }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    fun requestNotificationPermission() {
        // Android 13+ (API 33) Runtime Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}