package com.dlab.sirinium.data.repository

import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.platform.PlatformSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MultiplatformLessonNoteRepository(
    private val settings: PlatformSettings
) : LessonNoteRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val notesFlow = MutableStateFlow<List<LessonNote>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<HomeworkTask>>(emptyList())

    companion object {
        private const val KEY_NOTES = "saved_lesson_notes_json"
        private const val KEY_TASKS = "saved_homework_tasks_json"
    }

    init {
        try {
            val rawNotes = settings.getString(KEY_NOTES, "")
            if (rawNotes.isNotBlank()) {
                val loadedNotes = json.decodeFromString<List<LessonNote>>(rawNotes)
                notesFlow.value = loadedNotes
            }
        } catch (_: Exception) {}

        try {
            val rawTasks = settings.getString(KEY_TASKS, "")
            if (rawTasks.isNotBlank()) {
                val loadedTasks = json.decodeFromString<List<HomeworkTask>>(rawTasks)
                tasksFlow.value = loadedTasks
            }
        } catch (_: Exception) {}
    }

    private fun persistNotes(notes: List<LessonNote>) {
        try {
            val raw = json.encodeToString(notes)
            settings.putString(KEY_NOTES, raw)
        } catch (_: Exception) {}
    }

    private fun persistTasks(tasks: List<HomeworkTask>) {
        try {
            val raw = json.encodeToString(tasks)
            settings.putString(KEY_TASKS, raw)
        } catch (_: Exception) {}
    }

    override fun getNotesForLesson(lesson: Lesson): Flow<List<LessonNote>> {
        val trimmedDiscipline = lesson.discipline.trim()
        return notesFlow.map { notes ->
            when (lesson.sectionType) {
                "classroom" -> emptyList()
                "teacher" -> {
                    val trimmedGroup = lesson.group.trim()
                    notes.filter {
                        it.lessonId == lesson.id || (
                            trimmedDiscipline.isNotBlank() &&
                            it.discipline.trim().equals(trimmedDiscipline, ignoreCase = true) &&
                            it.groupName.trim().equals(trimmedGroup, ignoreCase = true) &&
                            it.scope == "teacher"
                        )
                    }
                }
                else -> {
                    notes.filter {
                        it.lessonId == lesson.id || (
                            trimmedDiscipline.isNotBlank() &&
                            it.discipline.trim().equals(trimmedDiscipline, ignoreCase = true) &&
                            it.scope == "student"
                        )
                    }
                }
            }
        }
    }

    override fun getNotesForLesson(lessonId: String): Flow<List<LessonNote>> {
        return notesFlow.map { notes ->
            notes.filter { it.lessonId == lessonId }
        }
    }

    override fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNote>> {
        val trimmed = discipline.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        return notesFlow.map { notes ->
            notes.filter { it.discipline.trim().equals(trimmed, ignoreCase = true) && it.scope == "student" }
        }
    }

    override fun getTeacherNotesForDisciplineAndGroup(
        discipline: String,
        groupName: String
    ): Flow<List<LessonNote>> {
        val trimmedDiscipline = discipline.trim()
        val trimmedGroup = groupName.trim()
        if (trimmedDiscipline.isBlank()) return flowOf(emptyList())
        return notesFlow.map { notes ->
            notes.filter {
                it.discipline.trim().equals(trimmedDiscipline, ignoreCase = true) &&
                        it.groupName.trim().equals(trimmedGroup, ignoreCase = true) &&
                        it.scope == "teacher"
            }
        }
    }

    override fun getRecommendedNotesForDiscipline(
        discipline: String,
        currentLessonId: String
    ): Flow<List<LessonNote>> {
        val trimmed = discipline.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        return notesFlow.map { notes ->
            notes.filter {
                it.discipline.trim().equals(trimmed, ignoreCase = true) && it.lessonId != currentLessonId
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
        persistNotes(currentNotes)
        return id
    }

    override suspend fun deleteNote(noteId: Long) {
        notesFlow.update { list ->
            val updated = list.filter { it.id != noteId }
            persistNotes(updated)
            updated
        }
        tasksFlow.update { list ->
            val updated = list.filter { it.noteId != noteId }
            persistTasks(updated)
            updated
        }
    }

    override suspend fun deleteAllNotes() {
        notesFlow.value = emptyList()
        tasksFlow.value = emptyList()
        persistNotes(emptyList())
        persistTasks(emptyList())
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
        persistTasks(currentTasks)
        return id
    }

    override suspend fun toggleTaskCompletion(taskId: Long, isDone: Boolean) {
        tasksFlow.update { list ->
            val updated = list.map { if (it.id == taskId) it.copy(isDone = isDone) else it }
            persistTasks(updated)
            updated
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        tasksFlow.update { list ->
            val updated = list.filter { it.id != taskId }
            persistTasks(updated)
            updated
        }
    }
}
