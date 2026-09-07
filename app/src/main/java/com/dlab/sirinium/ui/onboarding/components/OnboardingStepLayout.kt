package com.dlab.sirinium.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Foolproof keyboard visibility detector for Android, specifically handling custom OEM insets
 * (ColorOS, OxygenOS, HyperOS, One UI) where WindowInsets.isImeVisible alone may fail.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberIsKeyboardVisible(): Boolean {
    val view = LocalView.current
    val density = LocalDensity.current
    val composeIme = WindowInsets.isImeVisible || WindowInsets.ime.getBottom(density) > 0

    var isViewKeyboardVisible by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            val insetsIme = rootInsets?.isVisible(WindowInsetsCompat.Type.ime()) == true
            val imeHeight = rootInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0

            val r = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(r)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - r.bottom
            val isHeightDiff = keypadHeight > screenHeight * 0.15

            isViewKeyboardVisible = insetsIme || imeHeight > 0 || isHeightDiff
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return composeIme || isViewKeyboardVisible
}

/**
 * Standard Google Pixel setup wizard step layout:
 * - Top header with Back, animated progress bar, and Skip.
 * - Hero icon in an Expressive rounded container.
 * - Setting Title (подпись настройки).
 * - Setting Description (описание настройки).
 * - Dedicated setting controls.
 * - Bottom action buttons (Назад / Далее) on a single uniform background without colored boxes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingStepLayout(
    currentStep: Int,
    totalSteps: Int = 6,
    icon: ImageVector,
    title: String,
    description: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: (() -> Unit)? = null,
    nextButtonText: String = "Продолжить",
    isNextEnabled: Boolean = true,
    scrollable: Boolean = true,
    hideHeaderWhenIme: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isKeyboardOpen = rememberIsKeyboardVisible()
    val isHeaderVisible = !hideHeaderWhenIme || !isKeyboardOpen

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        // Pixel Setup Wizard Header
        OnboardingHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            onBackClick = onBack,
            onSkipClick = onSkip
        )

        // Middle Content Area
        val scrollState = rememberScrollState()
        val scrollModifier = if (scrollable) Modifier.verticalScroll(scrollState) else Modifier

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(scrollModifier)
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // 1. Вверху иконка
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Под ней подпись настройки
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Под подписью настройки описание
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 4. Элементы настройки для конкретного экрана
            content()
        }

        // Bottom Navigation on uniform background (NO colored box!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                modifier = Modifier
                    .weight(0.38f)
                    .heightIn(min = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Назад",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onNext,
                enabled = isNextEnabled,
                shape = CircleShape,
                modifier = Modifier
                    .weight(0.62f)
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = nextButtonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
