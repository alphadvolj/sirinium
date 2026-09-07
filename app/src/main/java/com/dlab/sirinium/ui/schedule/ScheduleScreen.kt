package com.dlab.sirinium.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.dlab.sirinium.ui.components.TutorialTargetKey
import com.dlab.sirinium.ui.components.tutorialTarget
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.ui.components.CurrentLessonHeroCard
import com.dlab.sirinium.ui.components.DateSelectorRow
import com.dlab.sirinium.ui.components.ScheduleTimelineItem
import com.dlab.sirinium.ui.theme.ExpressiveScheduleTheme
import com.dlab.sirinium.ui.theme.expressiveBounceClick
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val INITIAL_PAGE = 5000
private const val TOTAL_PAGES = 10000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onIntent: (ScheduleUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showTargetSelectorSheet by remember { mutableStateOf(false) }

    val anchorDate = remember { LocalDate.now() }
    val todayStr = remember { anchorDate.format(DateTimeUtils.DATE_FORMATTER) }

    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { TOTAL_PAGES }
    )

    fun dateToPage(dateStr: String): Int {
        val date = DateTimeUtils.parseDate(dateStr) ?: return INITIAL_PAGE
        val days = ChronoUnit.DAYS.between(anchorDate, date)
        return (INITIAL_PAGE + days).toInt().coerceIn(0, TOTAL_PAGES - 1)
    }

    fun pageToDate(page: Int): String {
        val offset = (page - INITIAL_PAGE).toLong()
        return anchorDate.plusDays(offset).format(DateTimeUtils.DATE_FORMATTER)
    }

    val currentSelectedDate by androidx.compose.runtime.rememberUpdatedState(state.filter.selectedDate)
    val onIntentAction by androidx.compose.runtime.rememberUpdatedState(onIntent)

    // Synchronize Pager swipe -> State filter date immediately as target page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.targetPage }.collect { page ->
            val newDate = pageToDate(page)
            if (newDate != currentSelectedDate) {
                onIntentAction(ScheduleUiIntent.ChangeDate(newDate))
            }
        }
    }

    // Synchronize State filter date -> Pager scroll (from DateSelectorRow or Today droplet)
    LaunchedEffect(state.filter.selectedDate) {
        val targetPage = dateToPage(state.filter.selectedDate)
        if (pagerState.targetPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val isSheetOpen = state.selectedLessonForDetails != null
    LaunchedEffect(state.userMessage, isSheetOpen) {
        if (!isSheetOpen) {
            state.userMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                onIntent(ScheduleUiIntent.DismissUserMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 68.dp)
                    .padding(horizontal = 16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    val targetIcon = when (state.filter.sectionType) {
                        "teacher" -> Icons.Rounded.Person
                        "classroom" -> Icons.Rounded.LocationOn
                        else -> Icons.Rounded.School
                    }

                    // Seamless Target Selector Button without box/border
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .expressiveBounceClick(scaleDown = 0.94f) { showTargetSelectorSheet = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = targetIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.filter.target,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "Изменить",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 1. M3 Expressive Date Selector Strip (with static calendar button)
            DateSelectorRow(
                selectedDateStr = state.filter.selectedDate,
                onDateSelected = { onIntent(ScheduleUiIntent.ChangeDate(it)) },
                modifier = Modifier.tutorialTarget(TutorialTargetKey.SCHEDULE_DATE_ROW)
            )

            val pullRefreshState = rememberPullToRefreshState()
            val density = LocalDensity.current
            val pullFraction = pullRefreshState.distanceFraction

            // Elastic downward stretch when pulling down
            val targetDisplacement = remember(pullFraction, state.isRefreshing) {
                if (state.isRefreshing) {
                    with(density) { 54.dp.toPx() }
                } else if (pullFraction > 0f) {
                    val base = pullFraction.coerceIn(0f, 2.5f)
                    with(density) { (base * 56.dp.toPx()).coerceAtMost(110.dp.toPx()) }
                } else {
                    0f
                }
            }

            val animatedDisplacement by animateFloatAsState(
                targetValue = targetDisplacement,
                animationSpec = if (state.isRefreshing || pullFraction == 0f) {
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                } else {
                    spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                },
                label = "pull_to_refresh_displacement"
            )

            // 2. Main Schedule Body: Pull-to-refresh container + HorizontalPager
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(ScheduleUiIntent.Refresh) },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .tutorialTarget(TutorialTargetKey.SCHEDULE_SWIPE_AREA)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = animatedDisplacement
                        },
                    key = { page -> pageToDate(page) }
                ) { page ->
                    val pageDateStr = pageToDate(page)
                    val pageLessons = remember(state.allLessons, pageDateStr, state.filter.searchQuery) {
                        state.lessonsForDate(pageDateStr)
                    }

                    val activeHero = remember(pageLessons) {
                        pageLessons.firstNotNullOfOrNull { lesson ->
                            val status = DateTimeUtils.calculateLessonStatus(
                                dateStr = lesson.date,
                                startTimeStr = lesson.startTime,
                                endTimeStr = lesson.endTime
                            )
                            if (status is DateTimeUtils.LessonTimeStatus.Ongoing) Pair(lesson, status) else null
                        } ?: pageLessons.firstNotNullOfOrNull { lesson ->
                            val status = DateTimeUtils.calculateLessonStatus(
                                dateStr = lesson.date,
                                startTimeStr = lesson.startTime,
                                endTimeStr = lesson.endTime
                            )
                            if (status is DateTimeUtils.LessonTimeStatus.Upcoming) Pair(lesson, status) else null
                        }
                    }

                    val isPageWeekLoading = remember(pageDateStr, state.loadingWeekOffsets) {
                        val date = DateTimeUtils.parseDate(pageDateStr)
                        val offset = date?.let { DateTimeUtils.calculateWeekOffset(it) } ?: 0
                        offset in state.loadingWeekOffsets
                    }

                    if ((state.isLoading && state.allLessons.isEmpty()) || (isPageWeekLoading && pageLessons.isEmpty())) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Загрузка расписания...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (pageLessons.isEmpty()) {
                        EmptyScheduleState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Hero Card (if ongoing or upcoming lesson present on this day)
                            activeHero?.let { (heroLesson, status) ->
                                item(key = "hero_${heroLesson.id}") {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        CurrentLessonHeroCard(
                                            lesson = heroLesson,
                                            timeStatus = status,
                                            onClick = { onIntent(ScheduleUiIntent.SelectLessonForDetails(heroLesson)) },
                                            onAddNoteClick = { onIntent(ScheduleUiIntent.SelectLessonForDetails(heroLesson)) },
                                            onSetAlarmClick = { onIntent(ScheduleUiIntent.ScheduleAlarmForLesson(heroLesson, 15)) },
                                            markerInfo = state.lessonMarkers[heroLesson.id],
                                            modifier = Modifier.tutorialTarget(TutorialTargetKey.FIRST_LESSON_CARD)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "РАСПИСАНИЕ НА ДЕНЬ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            // Daily Timeline Items
                            itemsIndexed(pageLessons, key = { _, item -> item.id }) { index, lesson ->
                                ScheduleTimelineItem(
                                    lesson = lesson,
                                    isFirst = index == 0,
                                    isLast = index == pageLessons.size - 1,
                                    onClick = { onIntent(ScheduleUiIntent.SelectLessonForDetails(lesson)) },
                                    onSetAlarmClick = { onIntent(ScheduleUiIntent.ScheduleAlarmForLesson(lesson, 15)) },
                                    cardModifier = if (activeHero == null && index == 0) Modifier.tutorialTarget(TutorialTargetKey.FIRST_LESSON_CARD) else Modifier,
                                    markerInfo = state.lessonMarkers[lesson.id]
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    state.selectedLessonForDetails?.let { selectedLesson ->
        LessonDetailBottomSheet(
            lesson = selectedLesson,
            allLessons = remember(state.allLessons, state.extraGroupLessons) {
                (state.allLessons + state.extraGroupLessons).distinctBy { it.id }
            },
            onSelectLesson = { nextLesson ->
                onIntent(ScheduleUiIntent.ChangeDate(nextLesson.date))
                onIntent(ScheduleUiIntent.SelectLessonForDetails(nextLesson))
            },
            notes = state.selectedLessonNotes,
            recommendedNotes = state.recommendedLessonNotes,
            tasks = state.selectedLessonTasks,
            userMessage = state.userMessage,
            onDismissUserMessage = { onIntent(ScheduleUiIntent.DismissUserMessage) },
            onDismiss = { onIntent(ScheduleUiIntent.SelectLessonForDetails(null)) },
            onSetAlarm = { mins -> onIntent(ScheduleUiIntent.ScheduleAlarmForLesson(selectedLesson, mins)) },
            onCancelAlarm = { onIntent(ScheduleUiIntent.CancelAlarmForLesson(selectedLesson)) },
            onSaveNote = { title, content, color -> onIntent(ScheduleUiIntent.SaveNote(title, content, color)) },
            onDeleteNote = { noteId -> onIntent(ScheduleUiIntent.DeleteNote(noteId)) },
            onAddTask = { text -> onIntent(ScheduleUiIntent.AddHomeworkTask(text)) },
            onToggleTask = { taskId, isDone -> onIntent(ScheduleUiIntent.ToggleHomeworkTask(taskId, isDone)) },
            onDeleteTask = { taskId -> onIntent(ScheduleUiIntent.DeleteHomeworkTask(taskId)) }
        )
    }

    // Target (Group / Teacher / Classroom) Selector Bottom Sheet
    if (showTargetSelectorSheet) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            onIntent(ScheduleUiIntent.ReloadFavorites)
            if (state.availableGroups.isEmpty() || state.availableTeachers.isEmpty() || state.availableClassrooms.isEmpty()) {
                onIntent(ScheduleUiIntent.RefreshSelectors)
            }
        }
        TargetSelectorBottomSheet(
            currentTarget = state.filter.target,
            currentSectionType = state.filter.sectionType,
            favoriteTargets = state.favoriteTargets,
            groups = state.availableGroups,
            teachers = state.availableTeachers,
            classrooms = state.availableClassrooms,
            onSelectTarget = { target, sectionType ->
                onIntent(ScheduleUiIntent.ChangeTarget(target, sectionType))
            },
            onToggleFavorite = { target, sectionType ->
                onIntent(ScheduleUiIntent.ToggleFavoriteTarget(target, sectionType))
            },
            onRemoveFavorite = { target, sectionType ->
                onIntent(ScheduleUiIntent.RemoveFavoriteTarget(target, sectionType))
            },
            isRefreshing = state.isRefreshingSelectors,
            onRefresh = { onIntent(ScheduleUiIntent.RefreshSelectors) },
            onDismiss = { showTargetSelectorSheet = false }
        )
    }
}

@Composable
private fun EmptyScheduleState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .tutorialTarget(TutorialTargetKey.FIRST_LESSON_CARD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "На этот день занятий нет",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Отличное время для отдыха или подготовки к проектам!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(name = "Schedule Screen Light Preview", showBackground = true)
@Composable
private fun ScheduleScreenLightPreview() {
    ExpressiveScheduleTheme(darkTheme = false) {
        ScheduleScreen(
            state = ScheduleUiState(
                filter = ScheduleFilter(target = "К1609-241"),
                allLessons = listOf(
                    Lesson(
                        id = "1",
                        discipline = "Инженерия машинного обучения и нейросетей",
                        teacher = "Прохоров А.С.",
                        classroom = "3.2 Ауд.",
                        startTime = "08:45",
                        endTime = "10:05",
                        date = DateTimeUtils.todayFormatted(),
                        numberPair = 1,
                        lessonType = LessonType.LECTURE
                    ),
                    Lesson(
                        id = "2",
                        discipline = "Архитектура распределенных систем",
                        teacher = "Смирнов К.А.",
                        classroom = "1.3К_5",
                        startTime = "10:20",
                        endTime = "11:40",
                        date = DateTimeUtils.todayFormatted(),
                        numberPair = 2,
                        lessonType = LessonType.LAB
                    )
                ),
                favoriteTargets = listOf(
                    FavoriteTarget(target = "К1609-241", sectionType = "group")
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Schedule Screen Dark Preview", showBackground = true)
@Composable
private fun ScheduleScreenDarkPreview() {
    ExpressiveScheduleTheme(darkTheme = true) {
        ScheduleScreen(
            state = ScheduleUiState(
                filter = ScheduleFilter(target = "Иванов И.И.", sectionType = "teacher"),
                allLessons = listOf(
                    Lesson(
                        id = "1",
                        discipline = "Базы данных и распределенные хранилища",
                        teacher = "Иванов И.И.",
                        classroom = "2.4 Ауд.",
                        startTime = "11:55",
                        endTime = "13:15",
                        date = DateTimeUtils.todayFormatted(),
                        numberPair = 3,
                        lessonType = LessonType.PRACTICE
                    )
                ),
                favoriteTargets = listOf(
                    FavoriteTarget(target = "Иванов И.И.", sectionType = "teacher")
                )
            ),
            onIntent = {}
        )
    }
}
