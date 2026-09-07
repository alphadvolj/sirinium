package com.dlab.sirinium.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.ui.components.TutorialTargetKey
import com.dlab.sirinium.ui.components.tutorialTarget
import com.dlab.sirinium.ui.theme.expressiveBounceClick

enum class NavigationTab(
    val title: String,
    val icon: ImageVector
) {
    SCHEDULE("Расписание", Icons.Rounded.CalendarToday),
    COMPARE("Окна", Icons.AutoMirrored.Rounded.CompareArrows),
    CLASSROOMS("Аудитории", Icons.Rounded.MeetingRoom),
    SETTINGS("Настройки", Icons.Rounded.Settings)
}

/**
 * Material 3 Expressive Floating Bottom Navigation Bar
 * Elegant floating pill with tactile bounce, smooth tab expansion,
 * an optional "Доступно обновление" droplet on the left,
 * and an optional separate "Сегодня" droplet bubble on the right.
 */
@Composable
fun FloatingBottomBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    isTodayVisible: Boolean = false,
    onTodayClick: () -> Unit = {},
    isUpdateAvailableVisible: Boolean = false,
    onUpdateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val isNarrow = maxWidth < 380.dp
        val tabPaddingH = if (isNarrow) 9.dp else 12.dp
        val tabPaddingV = if (isNarrow) 8.dp else 9.dp
        val tabFontSize = if (isNarrow) 11.sp else 12.sp
        val tabIconSize = if (isNarrow) 19.dp else 20.dp
        val tabSpacing = if (isNarrow) 1.dp else 2.dp
        val buttonSize = if (isNarrow) 44.dp else 48.dp

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Separate "Доступно обновление" Droplet on the left
            AnimatedVisibility(
                visible = isUpdateAvailableVisible,
                enter = fadeIn(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + scaleIn(
                    initialScale = 0.7f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + expandHorizontally(
                    expandFrom = Alignment.CenterHorizontally,
                    clip = false,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ),
                exit = fadeOut(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + scaleOut(
                    targetScale = 0.7f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + shrinkHorizontally(
                    shrinkTowards = Alignment.CenterHorizontally,
                    clip = false,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .expressiveBounceClick(scaleDown = 0.90f, bouncy = false, onClick = onUpdateClick)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = "Доступно обновление",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isNarrow) 20.dp else 22.dp)
                        )
                    }
                }
            }

            // Spacer between "Доступно обновление" Droplet and Main Navigation Pill
            val leftSpacerWidth by animateDpAsState(
                targetValue = if (isUpdateAvailableVisible) 6.dp else 0.dp,
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "update_droplet_spacer"
            )
            Spacer(modifier = Modifier.width(leftSpacerWidth))

            // Main Navigation Pill
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .tutorialTarget(TutorialTargetKey.BOTTOM_NAV_BAR)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isNarrow) 4.dp else 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(tabSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "tab_bg"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "tab_fg"
                        )

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(containerColor)
                                .expressiveBounceClick(scaleDown = 0.88f) { onTabSelected(tab) }
                                .padding(horizontal = tabPaddingH, vertical = tabPaddingV),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(tabIconSize)
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = tab.title,
                                        fontSize = tabFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Spacer between Main Navigation Pill and "Вернуться к сегодня" Droplet
            val spacerWidth by animateDpAsState(
                targetValue = if (isTodayVisible) 6.dp else 0.dp,
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "today_droplet_spacer"
            )
            Spacer(modifier = Modifier.width(spacerWidth))

            // Separate "Вернуться к сегодня" Droplet (Кнопка возврата назад)
            AnimatedVisibility(
                visible = isTodayVisible,
                enter = fadeIn(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + scaleIn(
                    initialScale = 0.7f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + expandHorizontally(
                    expandFrom = Alignment.CenterHorizontally,
                    clip = false,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ),
                exit = fadeOut(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + scaleOut(
                    targetScale = 0.7f,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + shrinkHorizontally(
                    shrinkTowards = Alignment.CenterHorizontally,
                    clip = false,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            ) {
                val buttonSize = if (isNarrow) 44.dp else 48.dp
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .expressiveBounceClick(scaleDown = 0.90f, bouncy = false, onClick = onTodayClick)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Вернуться к сегодняшнему дню",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(if (isNarrow) 20.dp else 22.dp)
                        )
                    }
                }
            }
        }
    }
}
