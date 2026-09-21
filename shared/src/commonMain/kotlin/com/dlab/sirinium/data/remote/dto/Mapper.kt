package com.dlab.sirinium.data.remote.dto

import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote

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

fun ScheduleItemDto.toDomain(
    fallbackTarget: String = "",
    forcedTarget: String? = null,
    forcedSectionType: String? = null,
    notesCount: Int = 0,
    completedTasksCount: Int = 0,
    totalTasksCount: Int = 0
): Lesson {
    val t = forcedTarget?.ifBlank { null }
        ?: fallbackTarget.ifBlank { null }
        ?: target.orEmpty()
    val stype = forcedSectionType?.ifBlank { null }
        ?: sectionType
        ?: "group"
    val d = date.orEmpty()
    val st = startTime.orEmpty()
    val disc = discipline.orEmpty()
    val room = classroom.orEmpty().ifBlank { if (stype == "classroom") t else "" }
    val np = numberPair ?: deducePairNumber(startTime)
    val teach = teacher.orEmpty().ifBlank { if (stype == "teacher") t else "" }
    val grp = group.orEmpty().ifBlank { if (stype == "group") t else "" }
    val generatedId = "${t}_${d}_${st}_${np}_${disc}_${room}_${teach}_${grp}".replace(" ", "_")

    return Lesson(
        id = generatedId,
        sectionType = stype,
        target = t,
        date = d,
        dayOfWeek = dayOfWeek.orEmpty(),
        startTime = st,
        endTime = endTime.orEmpty(),
        discipline = disc,
        lessonType = LessonType.fromString(lessonType),
        rawLessonType = lessonType.orEmpty(),
        teacher = teach,
        classroom = room,
        address = address.orEmpty(),
        onlineUrl = onlineUrl,
        group = grp,
        numberPair = np,
        notesCount = notesCount,
        completedTasksCount = completedTasksCount,
        totalTasksCount = totalTasksCount
    )
}
