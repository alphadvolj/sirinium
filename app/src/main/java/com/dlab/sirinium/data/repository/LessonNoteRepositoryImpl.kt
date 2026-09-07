package com.dlab.sirinium.data.repository

import com.dlab.sirinium.data.local.dao.LessonNoteDao
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.data.remote.dto.toEntity
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LessonNoteRepositoryImpl(
    private val lessonNoteDao: LessonNoteDao
) : LessonNoteRepository {

    override fun getNotesForLesson(lesson: Lesson): Flow<List<LessonNote>> {
        return when (lesson.sectionType) {
            "classroom" -> flowOf(emptyList())
            "teacher" -> getTeacherNotesForDisciplineAndGroup(lesson.discipline, lesson.group)
            else -> getStudentNotesForDiscipline(lesson.discipline)
        }
    }

    override fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNote>> {
        val trimmed = discipline.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        return lessonNoteDao.getStudentNotesForDiscipline(trimmed).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTeacherNotesForDisciplineAndGroup(discipline: String, groupName: String): Flow<List<LessonNote>> {
        val trimmedDiscipline = discipline.trim()
        val trimmedGroup = groupName.trim()
        if (trimmedDiscipline.isBlank()) return flowOf(emptyList())
        return lessonNoteDao.getTeacherNotesForDisciplineAndGroup(trimmedDiscipline, trimmedGroup).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesForLesson(lessonId: String): Flow<List<LessonNote>> {
        return lessonNoteDao.getNotesForLesson(lessonId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecommendedNotesForDiscipline(discipline: String, currentLessonId: String): Flow<List<LessonNote>> {
        return lessonNoteDao.getNotesForDisciplineExceptLesson(discipline, currentLessonId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllNotes(): Flow<List<LessonNote>> {
        return lessonNoteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksForLesson(lessonId: String): Flow<List<HomeworkTask>> {
        return lessonNoteDao.getTasksForLesson(lessonId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllTasks(): Flow<List<HomeworkTask>> {
        return lessonNoteDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveNote(note: LessonNote): Long {
        return lessonNoteDao.insertNote(note.toEntity())
    }

    override suspend fun deleteNote(noteId: Long) {
        lessonNoteDao.deleteNoteById(noteId)
    }

    override suspend fun deleteAllNotes() {
        lessonNoteDao.deleteAllNotes()
    }

    override suspend fun saveTask(task: HomeworkTask): Long {
        return lessonNoteDao.insertTask(task.toEntity())
    }

    override suspend fun toggleTaskCompletion(taskId: Long, isDone: Boolean) {
        lessonNoteDao.updateTaskStatus(taskId, isDone)
    }

    override suspend fun deleteTask(taskId: Long) {
        lessonNoteDao.deleteTaskById(taskId)
    }
}
