package com.dlab.sirinium.ui.onboarding.steps

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.util.FeedbackHelper
import com.dlab.sirinium.ui.onboarding.OnboardingUiState
import com.dlab.sirinium.ui.onboarding.components.OnboardingStepLayout
import com.dlab.sirinium.ui.onboarding.components.rememberIsKeyboardVisible
import com.dlab.sirinium.ui.schedule.TargetCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Step 1: Настройка расписания (Выбор сущности: Группы / Преподаватели / Аудитории).
 * Styled exactly like the in-app schedule selector ("пункт добавить расписание").
 */
@Composable
fun TargetStep(
    state: OnboardingUiState,
    onSelectTarget: (target: String, sectionType: String) -> Unit,
    onSetCustomTarget: (Boolean) -> Unit,
    onSetCustomTargetInput: (String) -> Unit,
    onSetNotifyDevelopers: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isKeyboardOpen = rememberIsKeyboardVisible()

    val initialTabIndex = when (state.selectedSectionType) {
        "teacher" -> 1
        "classroom" -> 2
        else -> 0
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(initialTabIndex) }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomGroupDialog by remember { mutableStateOf(false) }
    var shakeSelection by remember { mutableStateOf(false) }

    // Shake animation for the "current selection" pill
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeSelection) 1f else 0f,
        animationSpec = if (shakeSelection) {
            spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            )
        } else {
            spring()
        },
        finishedListener = { shakeSelection = false },
        label = "shake"
    )
    // Convert the 0..1 animated float into a horizontal oscillation
    val shakeTranslation = if (shakeSelection) {
        sin(shakeOffset * 4 * Math.PI).toFloat() * 12f
    } else {
        0f
    }

    val currentCategory = TargetCategory.entries[selectedCategoryIndex]

    val itemsList = when (currentCategory) {
        TargetCategory.GROUP -> state.availableGroups
        TargetCategory.TEACHER -> state.availableTeachers
        TargetCategory.CLASSROOM -> state.availableClassrooms
    }

    val filteredList = remember(itemsList, searchQuery) {
        if (searchQuery.isBlank()) {
            itemsList
        } else {
            itemsList.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    if (showCustomGroupDialog) {
        OnboardingCustomGroupDialog(
            initialGroupName = if (state.isCustomTarget) state.customTargetInput else searchQuery.trim(),
            initialNotify = state.notifyDevelopersAboutCustom,
            onDismiss = { showCustomGroupDialog = false },
            onConfirm = { customGroup, notifyDevs ->
                showCustomGroupDialog = false
                onSetCustomTargetInput(customGroup)
                onSetNotifyDevelopers(notifyDevs)
                onSelectTarget(customGroup, "group")
                onSetCustomTarget(true)
                if (notifyDevs) {
                    val appContext = context.applicationContext
                    CoroutineScope(Dispatchers.IO).launch {
                        FeedbackHelper.sendMissingGroupReport(appContext, customGroup)
                    }
                }
                onNext()
            }
        )
    }

    OnboardingStepLayout(
        currentStep = 1,
        totalSteps = 4,
        icon = currentCategory.icon,
        title = "Выберите расписание",
        description = "Укажите учебную группу, преподавателя или аудиторию. Расписание загрузится автоматически и будет доступно офлайн.",
        onBack = onBack,
        onNext = {
            if (state.effectiveTarget.isNotBlank()) {
                onNext()
            } else {
                // Vibrate to indicate nothing is selected
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                shakeSelection = true
            }
        },
        onSkip = null,
        nextButtonText = "Продолжить",
        isNextEnabled = true,
        scrollable = false,
        hideHeaderWhenIme = true,
        modifier = modifier
    ) {
        val fontScale = LocalDensity.current.fontScale
        val tabFontSize = (10f / fontScale.coerceAtLeast(1f)).coerceIn(8.5f, 10f).sp

        // 1. Notion-style Segmented Switcher for Categories (guaranteed single line without wrapping)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TargetCategory.entries.forEachIndexed { index, category ->
                val isSelected = index == selectedCategoryIndex
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "category_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    label = "category_text"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(bgColor)
                        .clickable {
                            selectedCategoryIndex = index
                            searchQuery = ""
                        }
                        .padding(vertical = 6.dp, horizontal = 1.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = category.title,
                        fontSize = tabFontSize,
                        letterSpacing = (-0.35).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = when (currentCategory) {
                        TargetCategory.GROUP -> "Поиск группы (напр. К1609-241)..."
                        TargetCategory.TEACHER -> "Поиск преподавателя (ФИО)..."
                        TargetCategory.CLASSROOM -> "Поиск аудитории (напр. 312)..."
                    },
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Поиск",
                    tint = MaterialTheme.colorScheme.outline
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Очистить",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Current Selection Pill
        val isNothingSelected = state.effectiveTarget.isBlank()
        val pillBorderColor by animateColorAsState(
            targetValue = if (shakeSelection) MaterialTheme.colorScheme.error
            else if (isNothingSelected) MaterialTheme.colorScheme.outlineVariant
            else Color.Transparent,
            label = "pill_border"
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (shakeSelection) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeTranslation.roundToInt(), 0) }
                .border(
                    width = if (shakeSelection || isNothingSelected) 1.5.dp else 0.dp,
                    color = pillBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Текущий выбор:",
                    fontSize = 12.sp,
                    color = if (shakeSelection) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
                )
                Surface(
                    shape = CircleShape,
                    color = if (isNothingSelected) {
                        if (shakeSelection) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    } else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isNothingSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = state.effectiveTarget.ifBlank { "Не выбрано" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNothingSelected) {
                                if (shakeSelection) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline
                            } else MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Section header with "Обновить список" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (currentCategory) {
                    TargetCategory.GROUP -> "СПИСОК ГРУПП"
                    TargetCategory.TEACHER -> "СПИСОК ПРЕПОДАВАТЕЛЕЙ"
                    TargetCategory.CLASSROOM -> "СПИСОК АУДИТОРИЙ"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (onRefresh != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.isLoadingEntities) { onRefresh() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (state.isLoadingEntities) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Обновить список",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = if (state.isLoadingEntities) "Обновление..." else "Обновить список",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Items List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (itemsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isLoadingEntities) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Загрузка списка...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text(
                                text = "Список пуст или нет подключения к сети",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (onRefresh != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FilledTonalButton(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Обновить список", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ничего не найдено по запросу «$searchQuery»",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (currentCategory == TargetCategory.GROUP) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = { showCustomGroupDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Ввести группу «${searchQuery.trim()}»" else "Ввести свою группу",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList, key = { "${currentCategory.sectionType}_$it" }) { item ->
                        val isCurrentSelected = !state.isCustomTarget &&
                                item.equals(state.selectedTarget, ignoreCase = true) &&
                                currentCategory.sectionType == state.selectedSectionType

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isCurrentSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSelectTarget(item, currentCategory.sectionType)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrentSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentCategory.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isCurrentSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = item,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isCurrentSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Pinned Bottom Action: "В списке нет группы?" (hidden when keyboard is open to maximize list area)
        if (currentCategory == TargetCategory.GROUP && !isKeyboardOpen) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showCustomGroupDialog = true
                    }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "В списке нет группы?",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Нажмите, чтобы ввести название группы вручную",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCustomGroupDialog(
    initialGroupName: String,
    initialNotify: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, notifyDevelopers: Boolean) -> Unit
) {
    var groupName by rememberSaveable { mutableStateOf(initialGroupName) }
    var notifyDevelopers by rememberSaveable { mutableStateOf(initialNotify) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "В списке нет группы",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Введите номер или название вашей учебной группы вручную, чтобы открыть её расписание.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        if (errorMessage != null && it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Номер или название группы *") },
                    placeholder = { Text("Например: 24.1 или К1609-241") },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Checkbox: Сообщить разработчикам
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { notifyDevelopers = !notifyDevelopers }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = notifyDevelopers,
                        onCheckedChange = { notifyDevelopers = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Сообщить разработчикам",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Отправить запрос на добавление группы в общее расписание",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        errorMessage = "Пожалуйста, введите название группы"
                        return@Button
                    }
                    onConfirm(groupName.trim(), notifyDevelopers)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Выбрать группу", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
