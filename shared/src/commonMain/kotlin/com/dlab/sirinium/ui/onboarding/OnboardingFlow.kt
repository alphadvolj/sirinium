package com.dlab.sirinium.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dlab.sirinium.ui.onboarding.steps.AlarmStep
import com.dlab.sirinium.ui.onboarding.steps.CompletionStep
import com.dlab.sirinium.ui.onboarding.steps.ShakeStep
import com.dlab.sirinium.ui.onboarding.steps.TargetStep
import com.dlab.sirinium.ui.onboarding.steps.ThemeStep
import com.dlab.sirinium.ui.onboarding.steps.TutorialStep
import com.dlab.sirinium.ui.onboarding.steps.WelcomeStep

enum class OnboardingStep(val stepIndex: Int) {
    WELCOME(0),
    TARGET(1),
    THEME(2),
    ALARM(3),
    SHAKE(4),
    COMPLETE(5),
    TUTORIAL(6)
}

/**
 * Cross-platform onboarding wizard flow for Android, iOS, and Desktop.
 * Manages animated navigation across all onboarding setup steps and feature tour.
 */
@Composable
fun OnboardingFlow(
    onboardingViewModel: OnboardingViewModel,
    onRequestNotificationPermission: () -> Unit = {},
    onFinish: (target: String, sectionType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by onboardingViewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.stepIndex > initialState.stepIndex) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "onboarding_step_animation",
        modifier = modifier.fillMaxSize()
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> {
                WelcomeStep(
                    onNext = { currentStep = OnboardingStep.TARGET }
                )
            }
            OnboardingStep.TARGET -> {
                TargetStep(
                    state = state,
                    onSelectTarget = onboardingViewModel::selectTarget,
                    onSetCustomTarget = onboardingViewModel::setCustomTarget,
                    onSetCustomTargetInput = onboardingViewModel::setCustomTargetInput,
                    onSetNotifyDevelopers = onboardingViewModel::setNotifyDevelopersAboutCustom,
                    onRefresh = { onboardingViewModel.refreshEntities() },
                    onBack = { currentStep = OnboardingStep.WELCOME },
                    onNext = {
                        onboardingViewModel.savePreferences()
                        currentStep = OnboardingStep.THEME
                    }
                )
            }
            OnboardingStep.THEME -> {
                ThemeStep(
                    state = state,
                    onSetThemeMode = onboardingViewModel::setThemeMode,
                    onSetDynamicColor = onboardingViewModel::setDynamicColor,
                    onBack = { currentStep = OnboardingStep.TARGET },
                    onNext = {
                        onboardingViewModel.savePreferences()
                        currentStep = OnboardingStep.ALARM
                    }
                )
            }
            OnboardingStep.ALARM -> {
                AlarmStep(
                    state = state,
                    onSetNotifyLessons = onboardingViewModel::setNotifyLessons,
                    onSetDefaultAlarmMinutes = onboardingViewModel::setDefaultAlarmMinutes,
                    onBack = { currentStep = OnboardingStep.THEME },
                    onNext = {
                        onboardingViewModel.savePreferences()
                        if (state.notifyLessonsEnabled) {
                            onRequestNotificationPermission()
                        }
                        currentStep = OnboardingStep.SHAKE
                    }
                )
            }
            OnboardingStep.SHAKE -> {
                ShakeStep(
                    state = state,
                    onSetShakeToReport = onboardingViewModel::setShakeToReport,
                    onBack = { currentStep = OnboardingStep.ALARM },
                    onNext = {
                        onboardingViewModel.savePreferences()
                        currentStep = OnboardingStep.COMPLETE
                    }
                )
            }
            OnboardingStep.COMPLETE -> {
                CompletionStep(
                    state = state,
                    onBack = { currentStep = OnboardingStep.SHAKE },
                    onComplete = {
                        onboardingViewModel.completeOnboarding()
                        currentStep = OnboardingStep.TUTORIAL
                    }
                )
            }
            OnboardingStep.TUTORIAL -> {
                TutorialStep(
                    shakeToReportEnabled = state.shakeToReportEnabled,
                    onBack = { currentStep = OnboardingStep.COMPLETE },
                    onSkip = {
                        onboardingViewModel.completeOnboarding()
                        onFinish(state.effectiveTarget, state.effectiveSectionType)
                    },
                    onNext = {
                        onboardingViewModel.completeOnboarding()
                        onFinish(state.effectiveTarget, state.effectiveSectionType)
                    }
                )
            }
        }
    }
}
