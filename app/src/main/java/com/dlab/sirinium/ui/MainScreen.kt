package com.dlab.sirinium.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.core.util.ShakeDetector
import com.dlab.sirinium.ui.classrooms.FreeClassroomsScreen
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareScheduleScreen
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.components.FeedbackBottomSheet
import com.dlab.sirinium.ui.components.LocalTutorialBoundsRecorder
import com.dlab.sirinium.ui.components.RealAppTutorialOverlay
import com.dlab.sirinium.ui.components.TutorialTargetKey
import com.dlab.sirinium.ui.navigation.FloatingBottomBar
import com.dlab.sirinium.ui.navigation.NavigationTab
import com.dlab.sirinium.ui.schedule.ScheduleScreen
import com.dlab.sirinium.ui.schedule.ScheduleUiIntent
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsScreen
import com.dlab.sirinium.ui.settings.SettingsViewModel
import com.dlab.sirinium.ui.update.AppInfoScreen
import com.dlab.sirinium.ui.update.AppUpdateScreen
import com.dlab.sirinium.ui.update.AppUpdateViewModel
import androidx.compose.animation.core.tween
import org.koin.androidx.compose.koinViewModel

/**
 * Root Main Screen with Material 3 Expressive Floating Bottom Bar.
 * Hosts Schedule, Mutual Free Windows Comparison, Free Classrooms Finder, and Settings.
 * Also monitors device shake to open the bug report / feedback sheet.
 */
@Composable
fun MainScreen(
    scheduleViewModel: ScheduleViewModel,
    compareViewModel: CompareViewModel,
    freeClassroomsViewModel: FreeClassroomsViewModel,
    settingsViewModel: SettingsViewModel,
    appUpdateViewModel: AppUpdateViewModel = koinViewModel(),
    onRestartOnboarding: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    showTutorial: Boolean = false,
    onDismissTutorial: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.SCHEDULE) }
    var showFeedbackSheet by rememberSaveable { mutableStateOf(false) }
    var showOfflineDialog by rememberSaveable { mutableStateOf(false) }
    var isTutorialVisible by rememberSaveable(showTutorial) { mutableStateOf(showTutorial) }

    LaunchedEffect(showTutorial) {
        if (showTutorial) {
            isTutorialVisible = true
            selectedTab = NavigationTab.SCHEDULE
        }
    }

    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val updateState by appUpdateViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Shake to Report listener with lifecycle management
    DisposableEffect(lifecycleOwner, settingsState.shakeToReportEnabled) {
        if (!settingsState.shakeToReportEnabled) {
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        val shakeDetector = ShakeDetector(context) {
            showFeedbackSheet = true
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(shakeDetector)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(shakeDetector)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val tutorialBoundsMap = remember { mutableStateMapOf<TutorialTargetKey, Rect>() }

    CompositionLocalProvider(
        LocalTutorialBoundsRecorder provides { key, bounds ->
            if (tutorialBoundsMap[key] != bounds) {
                tutorialBoundsMap[key] = bounds
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Screen Content Area
            Crossfade(
                targetState = selectedTab,
                label = "main_screen_tabs",
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) { tab ->
                when (tab) {
                    NavigationTab.SCHEDULE -> {
                        ScheduleScreen(
                            state = scheduleState,
                            onIntent = scheduleViewModel::onIntent
                        )
                    }
                    NavigationTab.COMPARE -> {
                        CompareScheduleScreen(
                            viewModel = compareViewModel
                        )
                    }
                    NavigationTab.CLASSROOMS -> {
                        FreeClassroomsScreen(
                            viewModel = freeClassroomsViewModel
                        )
                    }
                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onOpenFeedback = { showFeedbackSheet = true },
                            onRestartOnboarding = onRestartOnboarding,
                            onOpenTutorial = {
                                selectedTab = NavigationTab.SCHEDULE
                                isTutorialVisible = true
                                onOpenTutorial()
                            },
                            onRequestNotificationPermission = onRequestNotificationPermission
                        )
                    }
                }
            }

            val todayStr = remember { DateTimeUtils.todayFormatted() }
            val isTodayVisible = selectedTab == NavigationTab.SCHEDULE && scheduleState.filter.selectedDate != todayStr

            // Offline Status Red Square Indicator (Global across all screens)
            AnimatedVisibility(
                visible = scheduleState.isOffline,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 14.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showOfflineDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = "Оффлайн-режим",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Offline Details Dialog popup
            if (showOfflineDialog) {
                val updatedText = if (scheduleState.lastUpdateTime > 0L) {
                    DateTimeUtils.formatLastUpdated(scheduleState.lastUpdateTime)
                } else {
                    "Данные сохранены в кэше"
                }

                AlertDialog(
                    onDismissRequest = { showOfflineDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Оффлайн-режим",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Обновлено: $updatedText",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Приложение использует локально сохраненные данные и работает без подключения к сети.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showOfflineDialog = false }) {
                            Text("Понятно", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // Floating Bottom Navigation Bar
            FloatingBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                isTodayVisible = isTodayVisible,
                onTodayClick = { scheduleViewModel.onIntent(ScheduleUiIntent.ChangeDate(todayStr)) },
                isUpdateAvailableVisible = updateState.isUpdateAvailable,
                onUpdateClick = { appUpdateViewModel.openUpdateScreen() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Shake & Settings Feedback Bottom Sheet
            if (showFeedbackSheet) {
                FeedbackBottomSheet(
                    onDismiss = { showFeedbackSheet = false }
                )
            }

            // Interactive Real App Tutorial Overlay
            if (isTutorialVisible) {
                RealAppTutorialOverlay(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    scheduleViewModel = scheduleViewModel,
                    compareViewModel = compareViewModel,
                    freeClassroomsViewModel = freeClassroomsViewModel,
                    onOpenFeedback = { showFeedbackSheet = true },
                    onDismiss = {
                        selectedTab = NavigationTab.SCHEDULE
                        isTutorialVisible = false
                        onDismissTutorial()
                    },
                    tutorialBoundsMap = tutorialBoundsMap,
                    shakeToReportEnabled = settingsState.shakeToReportEnabled
                )
            }

            // Important Info Fullscreen Overlay (/api/appinfo)
            AnimatedVisibility(
                visible = updateState.showInfoScreen && updateState.appInfo != null,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                updateState.appInfo?.let { info ->
                    AppInfoScreen(
                        info = info,
                        onDismiss = { appUpdateViewModel.dismissInfoScreen() }
                    )
                }
            }

            // App Update Fullscreen Overlay (/api/appupdate)
            AnimatedVisibility(
                visible = updateState.showUpdateScreen && updateState.updateInfo != null,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                updateState.updateInfo?.let { update ->
                    AppUpdateScreen(
                        update = update,
                        onSnooze = { appUpdateViewModel.snoozeUpdate() }
                    )
                }
            }
        }
    }
}
