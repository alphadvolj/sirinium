package com.dlab.sirinium.domain.repository

import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote
import kotlinx.coroutines.flow.Flow

interface LessonNoteRepository {
    fun getNotesForLesson(lesson: Lesson): Flow<List<LessonNote>>
    fun getNotesForLesson(lessonId: String): Flow<List<LessonNote>>
    fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNote>>
    fun getTeacherNotesForDisciplineAndGroup(discipline: String, groupName: String): Flow<List<LessonNote>>
    fun getRecommendedNotesForDiscipline(discipline: String, currentLessonId: String): Flow<List<LessonNote>>
    fun getAllNotes(): Flow<List<LessonNote>>
    fun getTasksForLesson(lessonId: String): Flow<List<HomeworkTask>>
    fun getAllTasks(): Flow<List<HomeworkTask>>
    suspend fun saveNote(note: LessonNote): Long
    suspend fun deleteNote(noteId: Long)
    suspend fun deleteAllNotes()
    suspend fun saveTask(task: HomeworkTask): Long
    suspend fun toggleTaskCompletion(taskId: Long, isDone: Boolean)
    suspend fun deleteTask(taskId: Long)
}
