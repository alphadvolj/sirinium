package com.dlab.sirinium.domain.model

data class LessonNote(
    val id: Long = 0,
    val lessonId: String = "",
    val scope: String = "student", // "student" or "teacher"
    val groupName: String = "",
    val target: String = "",
    val discipline: String = "",
    val lessonType: String = "",
    val lessonDate: String = "",
    val classroom: String = "",
    val title: String,
    val content: String,
    val colorHex: Long = 0xFFFFF9C4, // Default soft yellow post-it note
    val deadlineMillis: Long? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tasks: List<HomeworkTask> = emptyList()
)

data class HomeworkTask(
    val id: Long = 0,
    val lessonId: String,
    val noteId: Long? = null,
    val text: String,
    val isDone: Boolean = false,
    val deadlineMillis: Long? = null
)

data class LessonMarkerInfo(
    val notesCount: Int = 0,
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0
) {
    val hasNotes: Boolean get() = notesCount > 0
    val hasTasks: Boolean get() = totalTasksCount > 0
    val isAllTasksDone: Boolean get() = hasTasks && completedTasksCount == totalTasksCount
}
