package com.dlab.sirinium.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.dlab.sirinium.core.util.FeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.ui.theme.expressiveBounceClick
import com.dlab.sirinium.ui.theme.ExpressiveScheduleTheme

enum class TargetCategory(val title: String, val sectionType: String, val icon: ImageVector) {
    GROUP("Группы", "group", Icons.Rounded.School),
    TEACHER("Преподаватели", "teacher", Icons.Rounded.Person),
    CLASSROOM("Аудитории", "classroom", Icons.Rounded.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSelectorBottomSheet(
    currentTarget: String,
    currentSectionType: String,
    favoriteTargets: List<FavoriteTarget>,
    groups: List<String>,
    teachers: List<String>,
    classrooms: List<String>,
    onSelectTarget: (target: String, sectionType: String) -> Unit,
    onToggleFavorite: (target: String, sectionType: String) -> Unit,
    onRemoveFavorite: (target: String, sectionType: String) -> Unit,
    onDismiss: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var isAddingNew by remember { mutableStateOf(false) }

    val initialTabIndex = when (currentSectionType) {
        "teacher" -> 1
        "classroom" -> 2
        else -> 0
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(initialTabIndex) }
    var searchQuery by remember { mutableStateOf("") }

    val currentCategory = TargetCategory.entries[selectedCategoryIndex]

    val itemsList = when (currentCategory) {
        TargetCategory.GROUP -> groups
        TargetCategory.TEACHER -> teachers
        TargetCategory.CLASSROOM -> classrooms
    }

    val filteredList = remember(itemsList, searchQuery) {
        if (searchQuery.isBlank()) {
            itemsList
        } else {
            itemsList.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = isAddingNew,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "target_selector_mode"
            ) { addingNew ->
                if (!addingNew) {
                    // MODE 1: Favorites List (Shown immediately)
                    FavoritesListView(
                        currentTarget = currentTarget,
                        currentSectionType = currentSectionType,
                        favoriteTargets = favoriteTargets,
                        onAddNewClick = { isAddingNew = true },
                        onSelectFavorite = { fav ->
                            onSelectTarget(fav.target, fav.sectionType)
                            onDismiss()
                        },
                        onRemoveFavorite = { fav ->
                            onRemoveFavorite(fav.target, fav.sectionType)
                        }
                    )
                } else {
                    // MODE 2: Search & Add (After clicking "Добавить")
                    SearchAndAddView(
                        currentCategory = currentCategory,
                        selectedCategoryIndex = selectedCategoryIndex,
                        onCategorySelected = {
                            selectedCategoryIndex = it
                            searchQuery = ""
                        },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        itemsList = itemsList,
                        filteredList = filteredList,
                        favoriteTargets = favoriteTargets,
                        currentTarget = currentTarget,
                        currentSectionType = currentSectionType,
                        onBackClick = { isAddingNew = false },
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { item, sectionType ->
                            onToggleFavorite(item, sectionType)
                            onSelectTarget(item, sectionType)
                            onDismiss()
                        },
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * Screen 1: Favorites List
 */
@Composable
private fun FavoritesListView(
    currentTarget: String,
    currentSectionType: String,
    favoriteTargets: List<FavoriteTarget>,
    onAddNewClick: () -> Unit,
    onSelectFavorite: (FavoriteTarget) -> Unit,
    onRemoveFavorite: (FavoriteTarget) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Выбор расписания",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Сохранённые группы и преподаватели",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent "+ Добавить" Button
        Button(
            onClick = onAddNewClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Добавить группу или преподавателя",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ИЗБРАННЫЕ РАСПИСАНИЯ",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (favoriteTargets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⭐", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нет сохранённых расписаний",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Нажмите «Добавить», чтобы найти группу или преподавателя",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favoriteTargets, key = { it.toSerializedString() }) { fav ->
                    val isCurrent = fav.target.equals(currentTarget, ignoreCase = true) &&
                            fav.sectionType == currentSectionType

                    val icon = when (fav.sectionType) {
                        "teacher" -> Icons.Rounded.Person
                        "classroom" -> Icons.Rounded.LocationOn
                        else -> Icons.Rounded.School
                    }

                    val typeLabel = when (fav.sectionType) {
                        "teacher" -> "ПРЕПОДАВАТЕЛЬ"
                        "classroom" -> "АУДИТОРИЯ"
                        else -> "ГРУППА"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .expressiveBounceClick(scaleDown = 0.97f) { onSelectFavorite(fav) }
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = fav.target,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Выбрано",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onRemoveFavorite(fav) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Удалить из сохранённых",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 2: Search & Add New Target
 */
@Composable
private fun SearchAndAddView(
    currentCategory: TargetCategory,
    selectedCategoryIndex: Int,
    onCategorySelected: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    itemsList: List<String>,
    filteredList: List<String>,
    favoriteTargets: List<FavoriteTarget>,
    currentTarget: String,
    currentSectionType: String,
    onBackClick: () -> Unit,
    onToggleFavorite: (target: String, sectionType: String) -> Unit,
    onSelectItem: (item: String, sectionType: String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showCustomGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentCategory, itemsList.isEmpty()) {
        if (itemsList.isEmpty() && onRefresh != null && !isRefreshing) {
            onRefresh()
        }
    }

    if (showCustomGroupDialog) {
        CustomGroupInputDialog(
            initialGroupName = searchQuery.trim(),
            onDismiss = { showCustomGroupDialog = false },
            onConfirm = { customGroup, notifyDevs ->
                showCustomGroupDialog = false
                val appContext = context.applicationContext
                if (notifyDevs) {
                    CoroutineScope(Dispatchers.IO).launch {
                        FeedbackHelper.sendMissingGroupReport(appContext, customGroup)
                    }
                }
                onSelectItem(customGroup, TargetCategory.GROUP.sectionType)
                Toast.makeText(context, "Группа «$customGroup» выбрана", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Row: Back button & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад к сохраненным"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Добавить расписание",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Выберите категорию и найдите нужное расписание",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Notion-style Segmented Switcher for Categories
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
                        .clickable { onCategorySelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = category.title,
                        fontSize = 10.sp,
                        letterSpacing = (-0.35).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = when (currentCategory) {
                        TargetCategory.GROUP -> "Поиск группы (напр. К1609-241)..."
                        TargetCategory.TEACHER -> "Поиск преподавателя (ФИО)..."
                        TargetCategory.CLASSROOM -> "Поиск аудитории (напр. 3.2)..."
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
                    IconButton(onClick = { onSearchQueryChange("") }) {
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

        Spacer(modifier = Modifier.height(12.dp))

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
                        .clickable(enabled = !isRefreshing) { onRefresh() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRefreshing) {
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
                            text = if (isRefreshing) "Обновление..." else "Обновить список",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Items List
        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Загрузка списка...",
                            fontSize = 13.sp,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ничего не найдено по запросу «$searchQuery»",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (currentCategory == TargetCategory.GROUP) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { showCustomGroupDialog = true },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Ввести группу «${searchQuery.trim()}»" else "Ввести свою группу",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList, key = { "${currentCategory.sectionType}_$it" }) { item ->
                    val isFavorite = favoriteTargets.any {
                        it.target.equals(item, ignoreCase = true) && it.sectionType == currentCategory.sectionType
                    }
                    val isCurrentSelected = item.equals(currentTarget, ignoreCase = true) &&
                            currentCategory.sectionType == currentSectionType

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCurrentSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable {
                                onSelectItem(item, currentCategory.sectionType)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        // Favorite star button
                        IconButton(
                            onClick = { onToggleFavorite(item, currentCategory.sectionType) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "В избранное",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }

        // Pinned Bottom Action: "В списке нет группы" (always pinned and visible for Group category)
        if (currentCategory == TargetCategory.GROUP) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .expressiveBounceClick(scaleDown = 0.97f) {
                        showCustomGroupDialog = true
                    }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "В списке нет группы",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Нажмите, чтобы ввести свою группу вручную",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Favorites View Preview", showBackground = true)
@Composable
private fun TargetSelectorBottomSheetFavoritesPreview() {
    ExpressiveScheduleTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FavoritesListView(
                currentTarget = "К1609-241",
                currentSectionType = "group",
                favoriteTargets = listOf(
                    FavoriteTarget(target = "К1609-241", sectionType = "group"),
                    FavoriteTarget(target = "Прохоров А.С.", sectionType = "teacher"),
                    FavoriteTarget(target = "3.2 Ауд.", sectionType = "classroom")
                ),
                onAddNewClick = {},
                onSelectFavorite = {},
                onRemoveFavorite = {}
            )
        }
    }
}

@Composable
private fun CustomGroupInputDialog(
    initialGroupName: String,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, notifyDevelopers: Boolean) -> Unit
) {
    var groupName by rememberSaveable { mutableStateOf(initialGroupName) }
    var notifyDevelopers by rememberSaveable { mutableStateOf(true) }
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

