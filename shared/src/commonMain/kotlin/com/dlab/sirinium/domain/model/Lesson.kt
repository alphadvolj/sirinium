package com.dlab.sirinium.domain.model

import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils

data class Lesson(
    val id: String,
    val sectionType: String = "group",
    val target: String = "К1609-241",
    val date: String,
    val dayOfWeek: String = "",
    val startTime: String,
    val endTime: String,
    val discipline: String,
    val lessonType: LessonType = LessonType.OTHER,
    val rawLessonType: String = "",
    val teacher: String = "",
    val classroom: String = "",
    val address: String = "",
    val onlineUrl: String? = null,
    val group: String = "",
    val numberPair: Int = 1,
    val notesCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0
) {
    // Expressive aliases for UI layers
    val subject: String get() = discipline
    val link: String? get() = onlineUrl
    val isTimeAnomaly: Boolean get() = DateTimeUtils.isTimeAnomaly(startTime, endTime)
    val groupName: String get() = group
}
