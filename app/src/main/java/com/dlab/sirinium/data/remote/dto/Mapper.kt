package com.dlab.sirinium.data.remote.dto

import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.data.local.entity.HomeworkTaskEntity
import com.dlab.sirinium.data.local.entity.LessonNoteEntity
import com.dlab.sirinium.data.local.entity.ScheduleEntity
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote

import com.dlab.sirinium.core.util.DateTimeUtils

fun deducePairNumber(startTime: String?, fallback: Int = 1): Int {
    val totalMinutes = DateTimeUtils.parseTimeToMinutes(startTime) ?: return fallback

    return when {
        totalMinutes < 10 * 60 -> 1 // 08:45, 09:25
        totalMinutes < 11 * 60 + 40 -> 2 // 10:20
        totalMinutes < 13 * 60 + 15 -> 3 // 11:55
        totalMinutes < 15 * 60 -> 4 // 13:30
        totalMinutes < 16 * 60 + 25 -> 5 // 15:05
        totalMinutes < 18 * 60 + 0 -> 6 // 16:40
        totalMinutes < 19 * 60 + 35 -> 7 // 18:15
        else -> 8 // 19:50
    }
}

fun ScheduleItemDto.toEntity(
    fallbackTarget: String = "",
    forcedTarget: String? = null,
    forcedSectionType: String? = null
): ScheduleEntity {
    val t = forcedTarget?.ifBlank { null }
        ?: fallbackTarget.ifBlank { null }
        ?: target.orEmpty()
    val stype = forcedSectionType?.ifBlank { null }
        ?: sectionType
        ?: "group"
    val d = date.orEmpty()
    val st = startTime.orEmpty()
    val disc = discipline.orEmpty()
    val room = classroom.orEmpty()
    val np = numberPair ?: deducePairNumber(startTime)
    val teach = teacher.orEmpty().ifBlank { if (stype == "teacher") t else "" }
    val grp = group.orEmpty().ifBlank { if (stype == "group") t else "" }
    val generatedId = "${t}_${d}_${st}_${np}_${disc}_${room}_${teach}_${grp}".replace(" ", "_")

    return ScheduleEntity(
        id = generatedId,
        sectionType = stype,
        target = t,
        date = d,
        dayOfWeek = dayOfWeek.orEmpty(),
        startTime = st,
        endTime = endTime.orEmpty(),
        discipline = disc,
        lessonType = lessonType.orEmpty(),
        teacher = teach,
        classroom = room,
        address = address.orEmpty(),
        onlineUrl = onlineUrl,
        groupName = grp,
        numberPair = np
    )
}

fun ScheduleEntity.toDomain(
    notesCount: Int = 0,
    completedTasksCount: Int = 0,
    totalTasksCount: Int = 0
): Lesson {
    return Lesson(
        id = id,
        sectionType = sectionType,
        target = target,
        date = date,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        discipline = discipline,
        lessonType = LessonType.fromString(lessonType),
        rawLessonType = lessonType,
        teacher = teacher,
        classroom = classroom,
        address = address,
        onlineUrl = onlineUrl,
        group = groupName,
        numberPair = numberPair,
        notesCount = notesCount,
        completedTasksCount = completedTasksCount,
        totalTasksCount = totalTasksCount
    )
}

fun LessonNoteEntity.toDomain(tasks: List<HomeworkTask> = emptyList()): LessonNote {
    return LessonNote(
        id = id,
        lessonId = lessonId,
        scope = scope,
        groupName = groupName,
        target = target,
        discipline = discipline,
        lessonType = lessonType,
        lessonDate = lessonDate,
        classroom = classroom,
        title = title,
        content = content,
        colorHex = colorHex,
        deadlineMillis = deadlineMillis,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tasks = tasks
    )
}

fun LessonNote.toEntity(): LessonNoteEntity {
    return LessonNoteEntity(
        id = id,
        lessonId = lessonId,
        scope = scope,
        groupName = groupName,
        target = target,
        discipline = discipline,
        lessonType = lessonType,
        lessonDate = lessonDate,
        classroom = classroom,
        title = title,
        content = content,
        colorHex = colorHex,
        deadlineMillis = deadlineMillis,
        isPinned = isPinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun HomeworkTaskEntity.toDomain(): HomeworkTask {
    return HomeworkTask(
        id = id,
        lessonId = lessonId,
        noteId = noteId,
        text = text,
        isDone = isDone,
        deadlineMillis = deadlineMillis
    )
}

fun HomeworkTask.toEntity(): HomeworkTaskEntity {
    return HomeworkTaskEntity(
        id = id,
        lessonId = lessonId,
        noteId = noteId,
        text = text,
        isDone = isDone,
        deadlineMillis = deadlineMillis
    )
}
