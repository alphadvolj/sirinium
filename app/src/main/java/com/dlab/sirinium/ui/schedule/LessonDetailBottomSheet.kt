package com.dlab.sirinium.ui.schedule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.ui.theme.expressiveBounceClick
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.ui.components.HomeworkChecklistItem
import com.dlab.sirinium.ui.components.LessonTypeBadge

val NOTE_COLORS = listOf(
    0xFFFFF9C4, // Pastel Yellow (Evernote/ColorNote default)
    0xFFDCEDC8, // Pastel Green
    0xFFB3E5FC, // Pastel Blue
    0xFFFFCCBC, // Pastel Orange
    0xFFF8BBD0, // Pastel Pink
    0xFFE1BEE7  // Pastel Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailBottomSheet(
    lesson: Lesson,
    notes: List<LessonNote>,
    recommendedNotes: List<LessonNote> = emptyList(),
    tasks: List<HomeworkTask>,
    userMessage: String? = null,
    onDismissUserMessage: () -> Unit = {},
    onDismiss: () -> Unit,
    onSetAlarm: (minutesBefore: Int) -> Unit = {},
    onCancelAlarm: () -> Unit = {},
    onSaveNote: (title: String, content: String, colorHex: Long) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onAddTask: (text: String) -> Unit,
    onToggleTask: (taskId: Long, isDone: Boolean) -> Unit,
    onDeleteTask: (taskId: Long) -> Unit,
    allLessons: List<Lesson> = emptyList(),
    onSelectLesson: (Lesson) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetSnackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val maxContentHeight = remember(configuration.screenHeightDp, statusBarTop) {
        (configuration.screenHeightDp.dp - statusBarTop - 48.dp).coerceAtLeast(200.dp)
    }

    fun showSheetMessage(msg: String) {
        coroutineScope.launch {
            sheetSnackbarHostState.currentSnackbarData?.dismiss()
            sheetSnackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            sheetSnackbarHostState.currentSnackbarData?.dismiss()
            sheetSnackbarHostState.showSnackbar(msg)
            onDismissUserMessage()
        }
    }

    var newTaskText by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }
    var selectedNoteColor by remember { mutableLongStateOf(NOTE_COLORS[0]) }

    LaunchedEffect(lesson.id) {
        newTaskText = ""
        newNoteContent = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
        ) {
            AnimatedContent(
                targetState = lesson,
                transitionSpec = {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                    ) + fadeIn(animationSpec = tween(260)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                            ) + fadeOut(animationSpec = tween(180))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "LessonTransition"
            ) { currentLesson ->
                val lesson = currentLesson
                val nextLessons = remember(lesson, allLessons) {
                    NextLessonHelper.findNextLessonsForDiscipline(lesson, allLessons)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
            // Header Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LessonTypeBadge(
                        lessonType = lesson.lessonType,
                        text = lesson.rawLessonType.ifBlank { lesson.lessonType.title }
                    )

                    if (lesson.isTimeAnomaly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${lesson.startTime} – ${lesson.endTime}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Text(
                            text = "#${lesson.numberPair} пара  •  ${lesson.startTime} – ${lesson.endTime}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = lesson.discipline,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Details Rows based on sectionType:
                // Group: classroom + teacher
                // Teacher: classroom + group
                // Classroom: teacher + group
                when (lesson.sectionType) {
                    "teacher" -> {
                        if (lesson.classroom.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Аудитория",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Аудитория: ${lesson.classroom} ${if (lesson.address.isNotBlank()) "(${lesson.address})" else ""}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (lesson.group.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = "Группа",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Группа: ${lesson.group}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    "classroom" -> {
                        if (lesson.teacher.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Преподаватель",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Преподаватель: ${lesson.teacher}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (lesson.group.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = "Группа",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Группа: ${lesson.group}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    else -> { // "group" or fallback
                        if (lesson.classroom.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Аудитория",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Аудитория: ${lesson.classroom} ${if (lesson.address.isNotBlank()) "(${lesson.address})" else ""}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (lesson.teacher.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Преподаватель",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Преподаватель: ${lesson.teacher}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (!lesson.onlineUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lesson.onlineUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showSheetMessage("Невозможно открыть ссылку")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Перейти к онлайн-паре")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val lessonInfoText = remember(lesson) {
                    buildString {
                        appendLine(lesson.discipline)
                        val typeName = lesson.rawLessonType.ifBlank { lesson.lessonType.title }
                        if (typeName.isNotBlank()) {
                            appendLine("Тип: $typeName")
                        }
                        appendLine("Время: ${lesson.startTime} – ${lesson.endTime}")
                        if (lesson.date.isNotBlank()) {
                            appendLine("Дата: ${lesson.date}${if (lesson.dayOfWeek.isNotBlank()) " (${lesson.dayOfWeek})" else ""}")
                        }
                        if (lesson.classroom.isNotBlank()) {
                            appendLine("Аудитория: ${lesson.classroom}")
                        }
                        if (lesson.teacher.isNotBlank()) {
                            appendLine("Преподаватель: ${lesson.teacher}")
                        }
                        if (lesson.group.isNotBlank()) {
                            appendLine("Группа: ${lesson.group}")
                        }
                        if (lesson.address.isNotBlank()) {
                            appendLine("Адрес: ${lesson.address}")
                        }
                        if (!lesson.onlineUrl.isNullOrBlank()) {
                            appendLine("Ссылка: ${lesson.onlineUrl}")
                        }
                    }.trim()
                }

                val isPastLesson = remember(lesson.date, lesson.startTime, lesson.endTime) {
                    val date = DateTimeUtils.parseDate(lesson.date)
                    val timeStatus = DateTimeUtils.calculateLessonStatus(
                        dateStr = lesson.date,
                        startTimeStr = lesson.startTime,
                        endTimeStr = lesson.endTime
                    )
                    timeStatus is DateTimeUtils.LessonTimeStatus.Finished || (date != null && date.isBefore(LocalDate.now()))
                }

                // 3 small round action buttons in a row aligned to the right: Copy, Share, (and if not past) NotificationsOff
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Кнопка копировать основную информацию о паре
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Информация о паре", lessonInfoText)
                                clipboard?.setPrimaryClip(clip)
                                showSheetMessage("Информация о паре скопирована")
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Копировать информацию о паре",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // 2. Кнопка поделиться информацией о паре
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, lessonInfoText)
                                    }
                                    val chooser = Intent.createChooser(sendIntent, "Поделиться парой")
                                    context.startActivity(chooser)
                                } catch (_: Exception) {
                                    showSheetMessage("Не удалось открыть меню отправки")
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Поделиться информацией о паре",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // 3. Красная кнопка со значком без уведомлений (отключает уведомление, только для актуальных пар)
                    if (!isPastLesson) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onCancelAlarm()
                                    showSheetMessage("Уведомление для пары отключено")
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = "Отключить уведомление",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // Next lessons section (up to 3 weeks ahead)
                if (nextLessons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Следующие занятия",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        nextLessons.forEach { nextInfo ->
                            NextLessonItemCard(
                                nextLessonInfo = nextInfo,
                                onClick = { onSelectLesson(nextInfo.lesson) }
                            )
                        }
                    }
                }

                // Tasks & Notes are excluded for classroom schedules
                if (lesson.sectionType != "classroom") {
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Section: Homework Tasks
                    Text(
                        text = "Домашние задания",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (lesson.sectionType != "classroom") {
                // Tasks List
                items(tasks, key = { "task_${it.id}" }) { task ->
                    HomeworkChecklistItem(
                        task = task,
                        onToggle = { isDone -> onToggleTask(task.id, isDone) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }

            // Add Task input
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Новая задача к паре...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTaskText.isNotBlank()) {
                                    onAddTask(newTaskText.trim())
                                    newTaskText = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Добавить")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Section: Notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lesson.sectionType == "teacher") "Заметки для группы ${lesson.group}" else "Заметки по предмету",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (notes.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .expressiveBounceClick(scaleDown = 0.92f) {
                                        notes.forEach { onDeleteNote(it.id) }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Удалить все",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color palette row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Цвет карточки:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        NOTE_COLORS.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = if (selectedNoteColor == colorHex) 2.dp else 1.dp,
                                        color = if (selectedNoteColor == colorHex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedNoteColor = colorHex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newNoteContent,
                        onValueChange = { newNoteContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Текст заметки / шпоры / вопросы лектору...", fontSize = 13.sp) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (newNoteContent.isNotBlank()) {
                                onSaveNote("Заметка", newNoteContent.trim(), selectedNoteColor)
                                newNoteContent = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сохранить заметку")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Existing Notes list
            items(notes, key = { "note_${it.id}" }) { note ->
                ExpressiveNoteCard(
                    note = note,
                    onDelete = { onDeleteNote(note.id) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Note", note.content))
                        showSheetMessage("Заметка скопирована")
                    }
                )
            }

            // Recommended Notes list from other lessons of the same discipline
            if (recommendedNotes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Рекомендации с других пар",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Заметки к этой дисциплине с других дней",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(recommendedNotes, key = { "rec_note_${it.id}" }) { recNote ->
                    RecommendedNoteCard(
                        note = recNote,
                        onAttach = {
                            onSaveNote(recNote.title, recNote.content, recNote.colorHex)
                            showSheetMessage("Заметка прикреплена к этой паре")
                        },
                        onCopy = {
                            showSheetMessage("Заметка скопирована")
                        },
                        onDelete = {
                            onDeleteNote(recNote.id)
                        }
                    )
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

        SnackbarHost(
            hostState = sheetSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
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
    }
}
}

@Composable
private fun ExpressiveNoteCard(
    note: LessonNote,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val noteColor = Color(note.colorHex)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, noteColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color accent stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(noteColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Context badge: Day of month, Lesson name, Lesson type, Classroom
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(noteColor)
                        )
                        val metaParts = mutableListOf<String>()
                        if (note.lessonDate.isNotBlank()) {
                            metaParts.add(DateTimeUtils.formatShortDate(note.lessonDate))
                        }
                        if (note.lessonType.isNotBlank()) {
                            metaParts.add(note.lessonType)
                        }
                        if (note.classroom.isNotBlank()) {
                            metaParts.add("Ауд. ${note.classroom}")
                        }

                        val metaText = if (metaParts.isNotEmpty()) {
                            metaParts.joinToString(" • ")
                        } else {
                            note.title.ifBlank { "Заметка" }
                        }

                        Text(
                            text = metaText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Скопировать",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RecommendedNoteCard(
    note: LessonNote,
    onAttach: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val noteColor = Color(note.colorHex)
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header showing when and where it was made
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = noteColor.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, noteColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val dateInfo = if (note.lessonDate.isNotBlank()) DateTimeUtils.formatShortDate(note.lessonDate) else "Ранее"
                        val typeInfo = if (note.lessonType.isNotBlank()) " • ${note.lessonType}" else ""
                        val roomInfo = if (note.classroom.isNotBlank()) " • ${note.classroom}" else ""
                        Text(
                            text = "Сделана: $dateInfo$typeInfo$roomInfo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Note", note.content))
                            onCopy()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Скопировать",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Удалить из памяти",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action button to attach to current lesson
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(10.dp))
                    .expressiveBounceClick(scaleDown = 0.92f) {
                        onAttach()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Прикрепить к этой паре",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NextLessonItemCard(
    nextLessonInfo: NextLessonInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextLesson = nextLessonInfo.lesson
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .expressiveBounceClick(scaleDown = 0.96f, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Header badge or title (e.g. "Следующая пара по предмету" or "Следующая лекция по предмету")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = nextLessonInfo.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Перейти к паре",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details of the next lesson
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LessonTypeBadge(
                        lessonType = nextLesson.lessonType,
                        text = nextLesson.rawLessonType.ifBlank { nextLesson.lessonType.title }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = NextLessonHelper.formatLessonDateFriendly(nextLesson.date),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${nextLesson.startTime} – ${nextLesson.endTime}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (nextLesson.classroom.isNotBlank() || nextLesson.teacher.isNotBlank() || nextLesson.group.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (nextLesson.classroom.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nextLesson.classroom,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (nextLesson.teacher.isNotBlank() && nextLesson.sectionType != "teacher") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nextLesson.teacher,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (nextLesson.group.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nextLesson.group,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

