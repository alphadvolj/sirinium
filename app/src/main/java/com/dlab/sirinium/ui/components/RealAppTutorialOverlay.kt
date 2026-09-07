package com.dlab.sirinium.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.navigation.NavigationTab
import com.dlab.sirinium.ui.schedule.ScheduleViewModel

enum class TutorialTargetArea {
    TOP_DATES,        // 1. Полоса дат и расписание (увеличенное окно для листания расписания)
    LESSONS_LIST,     // 2. Информация о паре (точный размер карточки пары)
    COMPARE_ENTITIES, // 3. Страница «Окна» (точный размер блока сравнения)
    CLASSROOM_PAIRS,  // 4. Страница «Поиск аудиторий» (точный размер полосы пар)
    SHAKE_FEEDBACK,   // 5. Потряси и сообщи (с анимацией тряски телефона)
    BOTTOM_BAR,       // 6. Нижняя панель навигации (точный размер плавающей капсулы)
    WIDGET_TIP        // 7. Совет по добавлению виджета на рабочий стол
}

private data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetTab: NavigationTab,
    val targetArea: TutorialTargetArea,
    val pointerText: String,
    val cardAlignment: Alignment
)

/**
 * Interactive Real App Tutorial:
 * - Optimized spotlight cutouts hugging exact UI tile dimensions
 * - Step 1 expanded to encompass both date strip and swipeable schedule body
 * - Real touch pass-through in spotlights allowing native swipes and clicks
 * - Step 2 focused on Lesson Info (details, homework, notes)
 * - Step 5 with dedicated phone shaking animation
 * - Step 6 focused on bottom navigation menu switching (exact floating pill tile size)
 * - Step 7 dedicated widget advice instructions
 */
@Composable
fun RealAppTutorialOverlay(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    scheduleViewModel: ScheduleViewModel,
    compareViewModel: CompareViewModel,
    freeClassroomsViewModel: FreeClassroomsViewModel,
    onOpenFeedback: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tutorialBoundsMap: Map<TutorialTargetKey, Rect> = emptyMap(),
    shakeToReportEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var currentStepIndex by remember { mutableIntStateOf(0) }

    val steps = remember(shakeToReportEnabled) {
        buildList {
            // 1. Навигация и дни недели (увеличенное окно: полоса + расписание)
            add(
                TutorialStep(
                    title = "Навигация и дни недели",
                    description = "Листайте само расписание свайпом влево и вправо для перехода между днями, или переключайте даты в верхней полосе. Потяните расписание вниз для обновления данных.",
                    icon = Icons.Rounded.Gesture,
                    targetTab = NavigationTab.SCHEDULE,
                    targetArea = TutorialTargetArea.TOP_DATES,
                    pointerText = "Листайте расписание или дни свайпом",
                    cardAlignment = Alignment.BottomCenter
                )
            )
            // 2. Информация о паре (точно под размер плитки пары)
            add(
                TutorialStep(
                    title = "Информация о паре",
                    description = "Нажмите на пару в расписании, чтобы открыть подробную информацию о ней, узнать аудиторию и преподавателя, а также записать домашнее задание или заметку.",
                    icon = Icons.Rounded.EditNote,
                    targetTab = NavigationTab.SCHEDULE,
                    targetArea = TutorialTargetArea.LESSONS_LIST,
                    pointerText = "Карточка занятия в расписании",
                    cardAlignment = Alignment.BottomCenter
                )
            )
            // 3. Страница «Окна» (точно под размер блока сравнения)
            add(
                TutorialStep(
                    title = "Страница «Окна»",
                    description = "Сравнивайте расписание двух любых групп, преподавателей или аудиторий одновременно. Приложение автоматически найдет совместные свободные окна и пересечения.",
                    icon = Icons.AutoMirrored.Rounded.CompareArrows,
                    targetTab = NavigationTab.COMPARE,
                    targetArea = TutorialTargetArea.COMPARE_ENTITIES,
                    pointerText = "Объекты сравнения",
                    cardAlignment = Alignment.BottomCenter
                )
            )
            // 4. Поиск аудиторий (точно под размер полосы пар)
            add(
                TutorialStep(
                    title = "Страница «Поиск аудиторий»",
                    description = "Узнайте, какие аудитории свободны прямо сейчас или на любой паре. Нажимайте на номера пар вверху, чтобы мгновенно увидеть список свободных кабинетов.",
                    icon = Icons.Rounded.MeetingRoom,
                    targetTab = NavigationTab.CLASSROOMS,
                    targetArea = TutorialTargetArea.CLASSROOM_PAIRS,
                    pointerText = "Выбор пары для поиска аудиторий",
                    cardAlignment = Alignment.BottomCenter
                )
            )
            // 5. Потряси и сообщи (с анимацией тряски телефона, если включено)
            if (shakeToReportEnabled) {
                add(
                    TutorialStep(
                        title = "Потряси и сообщи",
                        description = "Заметили ошибку в расписании или хотите предложить идею? Просто потрясите смартфон на любом экране — откроется окно быстрой связи с нами.",
                        icon = Icons.Rounded.Vibration,
                        targetTab = NavigationTab.SCHEDULE,
                        targetArea = TutorialTargetArea.SHAKE_FEEDBACK,
                        pointerText = "Жест встряхивания для связи",
                        cardAlignment = Alignment.Center
                    )
                )
            }
            // 6. Нижняя панель навигации (точно под размер плавающей капсулы)
            add(
                TutorialStep(
                    title = "Нижняя панель навигации",
                    description = "Плавающая нижняя панель позволяет мгновенно переключаться между разделами меню: расписанием («Пары»), поиском окон («Окна»), поиском аудиторий и настройками.",
                    icon = Icons.Rounded.TouchApp,
                    targetTab = NavigationTab.SCHEDULE,
                    targetArea = TutorialTargetArea.BOTTOM_BAR,
                    pointerText = "Переключение разделов меню",
                    cardAlignment = Alignment.TopCenter
                )
            )
            // 7. Совет: виджет на рабочий стол
            add(
                TutorialStep(
                    title = "Совет: виджет на рабочий стол",
                    description = "Добавьте интерактивный виджет Sirinium на главный экран смартфона, чтобы расписание ближайших пар всегда было перед глазами без запуска приложения!",
                    icon = Icons.Rounded.Widgets,
                    targetTab = NavigationTab.SCHEDULE,
                    targetArea = TutorialTargetArea.WIDGET_TIP,
                    pointerText = "Виджет рабочего стола",
                    cardAlignment = Alignment.Center
                )
            )
        }
    }

    val currentStep = steps[currentStepIndex.coerceIn(0, steps.lastIndex)]
    val isLastStep = currentStepIndex >= steps.lastIndex

    // Synchronize current step with actual app screen tab
    LaunchedEffect(currentStepIndex) {
        val stepTab = steps[currentStepIndex].targetTab
        if (selectedTab != stepTab) {
            onTabSelected(stepTab)
        }
    }

    // Pulsing animation for spotlight frame and pointer
    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pointer_offset"
    )

    fun triggerHapticFeedback() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(70)
            }
        } catch (_: Exception) {}
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        var tutorialCardTop by remember { mutableFloatStateOf(0f) }

        // Calculate Spotlight Cutout Rectangle dynamically according to target bounds
        val (targetRect, cornerRadius) = remember(
            currentStep.targetArea,
            tutorialBoundsMap[TutorialTargetKey.SCHEDULE_DATE_ROW],
            tutorialBoundsMap[TutorialTargetKey.FIRST_LESSON_CARD],
            tutorialBoundsMap[TutorialTargetKey.COMPARE_HEADER],
            tutorialBoundsMap[TutorialTargetKey.CLASSROOMS_PAIRS],
            tutorialBoundsMap[TutorialTargetKey.BOTTOM_NAV_BAR],
            tutorialCardTop,
            screenWidth,
            screenHeight,
            statusBarTop,
            navBarBottom
        ) {
            when (currentStep.targetArea) {
                // Step 1: Expanded window including date strip AND main schedule area so user can swipe schedule
                TutorialTargetArea.TOP_DATES -> {
                    val dateBounds = tutorialBoundsMap[TutorialTargetKey.SCHEDULE_DATE_ROW]
                    val top = if (dateBounds != null && dateBounds.top > 0f) {
                        dateBounds.top - with(density) { 3.dp.toPx() }
                    } else {
                        with(density) { (statusBarTop + 54.dp).toPx() }
                    }
                    val left = with(density) { 8.dp.toPx() }
                    val right = with(density) { (screenWidth - 8.dp).toPx() }
                    val bottom = if (tutorialCardTop > 0f) {
                        (tutorialCardTop - with(density) { 12.dp.toPx() }).coerceAtLeast(top + with(density) { 180.dp.toPx() })
                    } else {
                        with(density) { (screenHeight - navBarBottom - 260.dp).toPx() }
                    }
                    Rect(left, top, right, bottom) to 24.dp
                }

                // Step 2: Dynamically measured exact size of the lesson card tile
                TutorialTargetArea.LESSONS_LIST -> {
                    val bounds = tutorialBoundsMap[TutorialTargetKey.FIRST_LESSON_CARD]
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        val padX = with(density) { 3.dp.toPx() }
                        val padY = with(density) { 3.dp.toPx() }
                        Rect(
                            left = bounds.left - padX,
                            top = bounds.top - padY,
                            right = bounds.right + padX,
                            bottom = bounds.bottom + padY
                        ) to 22.dp
                    } else {
                        val left = with(density) { 14.dp.toPx() }
                        val right = with(density) { (screenWidth - 14.dp).toPx() }
                        val top = with(density) { (statusBarTop + 148.dp).toPx() }
                        val bottom = with(density) { (statusBarTop + 270.dp).toPx() }
                        Rect(left, top, right, bottom) to 22.dp
                    }
                }

                // Step 3: Dynamically measured exact size of EntityComparisonHeader tile on «Окна» page
                TutorialTargetArea.COMPARE_ENTITIES -> {
                    val bounds = tutorialBoundsMap[TutorialTargetKey.COMPARE_HEADER]
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        val padX = with(density) { 3.dp.toPx() }
                        val padY = with(density) { 3.dp.toPx() }
                        Rect(
                            left = bounds.left - padX,
                            top = bounds.top - padY,
                            right = bounds.right + padX,
                            bottom = bounds.bottom + padY
                        ) to 22.dp
                    } else {
                        val left = with(density) { 12.dp.toPx() }
                        val right = with(density) { (screenWidth - 12.dp).toPx() }
                        val top = with(density) { (statusBarTop + 120.dp).toPx() }
                        val bottom = with(density) { (statusBarTop + 180.dp).toPx() }
                        Rect(left, top, right, bottom) to 22.dp
                    }
                }

                // Step 4: Dynamically measured exact size of classrooms pair selector strip
                TutorialTargetArea.CLASSROOM_PAIRS -> {
                    val bounds = tutorialBoundsMap[TutorialTargetKey.CLASSROOMS_PAIRS]
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        val padY = with(density) { 3.dp.toPx() }
                        Rect(
                            left = (bounds.left - with(density) { 2.dp.toPx() }).coerceAtLeast(0f),
                            top = bounds.top - padY,
                            right = (bounds.right + with(density) { 2.dp.toPx() }).coerceAtMost(with(density) { screenWidth.toPx() }),
                            bottom = bounds.bottom + padY
                        ) to 20.dp
                    } else {
                        val left = with(density) { 8.dp.toPx() }
                        val right = with(density) { (screenWidth - 8.dp).toPx() }
                        val top = with(density) { (statusBarTop + 120.dp).toPx() }
                        val bottom = with(density) { (statusBarTop + 180.dp).toPx() }
                        Rect(left, top, right, bottom) to 20.dp
                    }
                }

                // Step 6: Dynamically measured exact size of floating bottom navigation bar capsule
                TutorialTargetArea.BOTTOM_BAR -> {
                    val bounds = tutorialBoundsMap[TutorialTargetKey.BOTTOM_NAV_BAR]
                    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                        val padX = with(density) { 3.dp.toPx() }
                        val padY = with(density) { 3.dp.toPx() }
                        Rect(
                            left = bounds.left - padX,
                            top = bounds.top - padY,
                            right = bounds.right + padX,
                            bottom = bounds.bottom + padY
                        ) to 32.dp
                    } else {
                        val pillWidth = 280.dp.coerceAtMost(screenWidth - 32.dp)
                        val left = with(density) { ((screenWidth - pillWidth) / 2).toPx() }
                        val right = with(density) { ((screenWidth + pillWidth) / 2).toPx() }
                        val top = with(density) { (screenHeight - navBarBottom - 68.dp).toPx() }
                        val bottom = with(density) { (screenHeight - navBarBottom - 10.dp).toPx() }
                        Rect(left, top, right, bottom) to 32.dp
                    }
                }

                TutorialTargetArea.SHAKE_FEEDBACK,
                TutorialTargetArea.WIDGET_TIP -> {
                    null to 0.dp
                }
            }
        }

        // Animated target rectangle transitions
        val animLeft by animateFloatAsState(targetValue = targetRect?.left ?: 0f, label = "spotlight_left")
        val animTop by animateFloatAsState(targetValue = targetRect?.top ?: 0f, label = "spotlight_top")
        val animRight by animateFloatAsState(targetValue = targetRect?.right ?: 0f, label = "spotlight_right")
        val animBottom by animateFloatAsState(targetValue = targetRect?.bottom ?: 0f, label = "spotlight_bottom")

        val currentCutout = if (targetRect != null) {
            Rect(animLeft, animTop, animRight, animBottom)
        } else null

        val primaryColor = MaterialTheme.colorScheme.primary

        // 1. Dark scrim canvas with clear cutout hole over the highlighted live app element
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Draw dark backdrop
            drawRect(Color.Black.copy(alpha = 0.72f))

            // Cut out spotlight
            if (currentCutout != null && currentCutout.width > 0 && currentCutout.height > 0) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = currentCutout.topLeft,
                    size = currentCutout.size,
                    cornerRadius = CornerRadius(with(density) { cornerRadius.toPx() }),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // 2. Touch Blockers around the spotlight cutout:
        // Inside currentCutout, there are NO blockers so user can touch, swipe, and tap the real app elements!
        if (currentCutout != null && currentCutout.width > 0 && currentCutout.height > 0) {
            val screenWidthPx = with(density) { screenWidth.toPx() }
            val screenHeightPx = with(density) { screenHeight.toPx() }

            // Top blocker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { currentCutout.top.toDp() })
                    .align(Alignment.TopStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb clicks above cutout */ }
            )

            // Bottom blocker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (screenHeightPx - currentCutout.bottom).toDp().coerceAtLeast(0.dp) })
                    .align(Alignment.BottomStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb clicks below cutout */ }
            )

            // Left blocker
            Box(
                modifier = Modifier
                    .width(with(density) { currentCutout.left.toDp() })
                    .height(with(density) { (currentCutout.bottom - currentCutout.top).toDp().coerceAtLeast(0.dp) })
                    .offset(y = with(density) { currentCutout.top.toDp() })
                    .align(Alignment.TopStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb clicks left of cutout */ }
            )

            // Right blocker
            Box(
                modifier = Modifier
                    .width(with(density) { (screenWidthPx - currentCutout.right).toDp().coerceAtLeast(0.dp) })
                    .height(with(density) { (currentCutout.bottom - currentCutout.top).toDp().coerceAtLeast(0.dp) })
                    .offset(y = with(density) { currentCutout.top.toDp() })
                    .align(Alignment.TopEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb clicks right of cutout */ }
            )
        } else {
            // Full-screen blocker for full overlay steps (Step 5 and Step 7)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb stray clicks */ }
            )
        }

        // 3. Glowing animated outline around the active feature
        if (currentCutout != null && currentCutout.width > 0 && currentCutout.height > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cr = with(density) { cornerRadius.toPx() }
                drawRoundRect(
                    color = primaryColor.copy(alpha = pulseGlowAlpha),
                    topLeft = currentCutout.topLeft,
                    size = currentCutout.size,
                    cornerRadius = CornerRadius(cr),
                    style = Stroke(width = with(density) { 2.5.dp.toPx() })
                )
            }

            // Pointer Badge directly pointing to highlighted feature
            val badgeAbove = currentCutout.top > with(density) { (screenHeight / 2).toPx() }
            val badgeY = with(density) {
                when {
                    currentStep.targetArea == TutorialTargetArea.TOP_DATES -> {
                        val dateBounds = tutorialBoundsMap[TutorialTargetKey.SCHEDULE_DATE_ROW]
                        if (dateBounds != null && dateBounds.bottom > 0f) {
                            (dateBounds.bottom + 8.dp.toPx() + pulseOffset.dp.toPx()).toInt()
                        } else {
                            (currentCutout.top + 72.dp.toPx() + pulseOffset.dp.toPx()).toInt()
                        }
                    }
                    badgeAbove -> {
                        (currentCutout.top - 38.dp.toPx() + pulseOffset.dp.toPx()).toInt()
                    }
                    else -> {
                        (currentCutout.bottom + 10.dp.toPx() + pulseOffset.dp.toPx()).toInt()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, badgeY) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentStep.pointerText,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Tutorial Card
        AnimatedContent(
            targetState = currentStepIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "tutorial_card_content",
            modifier = Modifier
                .align(currentStep.cardAlignment)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 18.dp,
                    vertical = if (currentStep.targetArea == TutorialTargetArea.BOTTOM_BAR) 24.dp else 16.dp
                )
                .widthIn(max = 480.dp)
        ) { stepIdx ->
            val step = steps[stepIdx]

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            val top = coords.boundsInRoot().top
                            if (top > 0f && tutorialCardTop != top) {
                                tutorialCardTop = top
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Step Badge & Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Шаг ${stepIdx + 1} из ${steps.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Пропустить",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title & Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    // Step 5: Animated Shaking Phone Component
                    if (step.targetArea == TutorialTargetArea.SHAKE_FEEDBACK) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ShakingPhoneComponent(
                            onShakeTest = {
                                triggerHapticFeedback()
                                onOpenFeedback()
                            }
                        )
                    }

                    // Step 7: Advice on adding widget to the home screen (ONLY displayed on step 7)
                    if (step.targetArea == TutorialTargetArea.WIDGET_TIP) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "КАК ДОБАВИТЬ ВИДЖЕТ:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                listOf(
                                    "1. Зажмите свободное место на рабочем столе смартфона",
                                    "2. Откройте «Виджеты» и найдите Sirinium",
                                    "3. Перетащите удобный виджет на экран"
                                ).forEach { instruction ->
                                    Text(
                                        text = instruction,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stepIdx > 0) {
                            OutlinedButton(
                                onClick = {
                                    currentStepIndex--
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Назад",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onDismiss()
                                } else {
                                    currentStepIndex++
                                }
                            },
                            modifier = Modifier
                                .weight(if (stepIdx > 0) 1.5f else 1f)
                                .heightIn(min = 46.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (isLastStep) "Начать пользоваться" else "Далее",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLastStep) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated smartphone illustration for "Потряси и сообщи"
 * Performs a lively physical shake animation and allows testing via tap
 */
@Composable
private fun ShakingPhoneComponent(
    onShakeTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "phone_shake_animation")

    // Lively rotation wiggle (-14deg to +14deg)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(110, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_rot"
    )

    // Lateral vibration (-8px to +8px)
    val translationX by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(85, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_trans"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onShakeTest() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = rotation
                        this.translationX = translationX
                    }
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Потрясите смартфон или нажмите сюда",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Сработает датчик движения и откроется форма связи",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
