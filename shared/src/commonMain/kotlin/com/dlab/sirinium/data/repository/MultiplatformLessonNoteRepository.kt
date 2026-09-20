package com.dlab.sirinium.data.repository

import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class MultiplatformLessonNoteRepository : LessonNoteRepository {

    private val notesFlow = MutableStateFlow<List<LessonNote>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<HomeworkTask>>(emptyList())

    override fun getNotesForLesson(lesson: Lesson): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter { it.lessonId == lesson.id }
        }
    }

    override fun getNotesForLesson(lessonId: String): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter { it.lessonId == lessonId }
        }
    }

    override fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter { it.discipline.equals(discipline, ignoreCase = true) && it.scope == "student" }
        }
    }

    override fun getTeacherNotesForDisciplineAndGroup(
        discipline: String,
        groupName: String
    ): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter {
                it.discipline.equals(discipline, ignoreCase = true) &&
                        it.groupName.equals(groupName, ignoreCase = true) &&
                        it.scope == "teacher"
            }
        }
    }

    override fun getRecommendedNotesForDiscipline(
        discipline: String,
        currentLessonId: String
    ): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter {
                it.discipline.equals(discipline, ignoreCase = true) && it.lessonId != currentLessonId
            }
        }
    }

    override fun getAllNotes(): Flow<List<LessonNote>> = notesFlow.asStateFlow()

    override fun getTasksForLesson(lessonId: String): Flow<List<HomeworkTask>> {
        return tasksFlow.map { tasks ->
            tasks.filter { it.lessonId == lessonId }
        }
    }

    override fun getAllTasks(): Flow<List<HomeworkTask>> = tasksFlow.asStateFlow()

    override suspend fun saveNote(note: LessonNote): Long {
        val currentNotes = notesFlow.value.toMutableList()
        val id = if (note.id == 0L) Clock.System.now().toEpochMilliseconds() else note.id
        val newNote = note.copy(id = id, updatedAt = Clock.System.now().toEpochMilliseconds())
        val index = currentNotes.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentNotes[index] = newNote
        } else {
            currentNotes.add(0, newNote)
        }
        notesFlow.value = currentNotes
        return id
    }

    override suspend fun deleteNote(noteId: Long) {
        notesFlow.update { it.filter { note -> note.id != noteId } }
        tasksFlow.update { it.filter { task -> task.noteId != noteId } }
    }

    override suspend fun deleteAllNotes() {
        notesFlow.value = emptyList()
        tasksFlow.value = emptyList()
    }

    override suspend fun saveTask(task: HomeworkTask): Long {
        val currentTasks = tasksFlow.value.toMutableList()
        val id = if (task.id == 0L) Clock.System.now().toEpochMilliseconds() else task.id
        val newTask = task.copy(id = id)
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentTasks[index] = newTask
        } else {
            currentTasks.add(newTask)
        }
        tasksFlow.value = currentTasks
        return id
    }

    override suspend fun toggleTaskCompletion(taskId: Long, isDone: Boolean) {
        tasksFlow.update { tasks ->
            tasks.map { if (it.id == taskId) it.copy(isDone = isDone) else it }
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        tasksFlow.update { it.filter { task -> task.id != taskId } }
    }
}
