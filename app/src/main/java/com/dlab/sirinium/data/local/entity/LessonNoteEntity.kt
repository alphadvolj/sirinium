package com.dlab.sirinium.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_notes",
    indices = [
        Index(value = ["lessonId"]),
        Index(value = ["discipline"]),
        Index(value = ["scope", "discipline"]),
        Index(value = ["scope", "discipline", "groupName"])
    ]
)
data class LessonNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: String = "",
    val scope: String = "student", // "student" or "teacher"
    val groupName: String = "",    // bound group for teacher scope notes
    val target: String = "",       // original viewing target
    val discipline: String = "",
    val lessonType: String = "",
    val lessonDate: String = "",
    val classroom: String = "",
    val title: String,
    val content: String,
    val colorHex: Long,
    val deadlineMillis: Long? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "homework_tasks",
    indices = [
        Index(value = ["lessonId"]),
        Index(value = ["noteId"])
    ]
)
data class HomeworkTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: String,
    val noteId: Long? = null,
    val text: String,
    val isDone: Boolean = false,
    val deadlineMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
