package com.dlab.sirinium.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dlab.sirinium.data.local.entity.HomeworkTaskEntity
import com.dlab.sirinium.data.local.entity.LessonNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonNoteDao {

    @Query("SELECT * FROM lesson_notes WHERE scope = 'student' AND LOWER(TRIM(discipline)) = LOWER(TRIM(:discipline)) ORDER BY isPinned DESC, createdAt DESC")
    fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNoteEntity>>

    @Query("SELECT * FROM lesson_notes WHERE scope = 'teacher' AND LOWER(TRIM(discipline)) = LOWER(TRIM(:discipline)) AND LOWER(TRIM(groupName)) = LOWER(TRIM(:groupName)) ORDER BY isPinned DESC, createdAt DESC")
    fun getTeacherNotesForDisciplineAndGroup(discipline: String, groupName: String): Flow<List<LessonNoteEntity>>

    @Query("SELECT * FROM lesson_notes WHERE lessonId = :lessonId ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesForLesson(lessonId: String): Flow<List<LessonNoteEntity>>

    @Query("SELECT * FROM lesson_notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<LessonNoteEntity>>

    @Query("SELECT * FROM lesson_notes WHERE discipline = :discipline AND lessonId != :currentLessonId ORDER BY createdAt DESC")
    fun getNotesForDisciplineExceptLesson(discipline: String, currentLessonId: String): Flow<List<LessonNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: LessonNoteEntity): Long

    @Update
    suspend fun updateNote(note: LessonNoteEntity)

    @Query("DELETE FROM lesson_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long): Int

    @Query("DELETE FROM lesson_notes")
    suspend fun deleteAllNotes(): Int

    @Query("SELECT * FROM homework_tasks ORDER BY isDone ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<HomeworkTaskEntity>>

    @Query("SELECT * FROM homework_tasks WHERE lessonId = :lessonId ORDER BY isDone ASC, createdAt ASC")
    fun getTasksForLesson(lessonId: String): Flow<List<HomeworkTaskEntity>>

    @Query("SELECT * FROM homework_tasks WHERE lessonId = :lessonId")
    suspend fun getTasksForLessonSync(lessonId: String): List<HomeworkTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: HomeworkTaskEntity): Long

    @Query("UPDATE homework_tasks SET isDone = :isDone WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, isDone: Boolean): Int

    @Query("DELETE FROM homework_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long): Int

    @Query("SELECT COUNT(*) FROM lesson_notes WHERE lessonId = :lessonId")
    fun getNotesCountForLesson(lessonId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM homework_tasks WHERE lessonId = :lessonId")
    fun getTotalTasksCountForLesson(lessonId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM homework_tasks WHERE lessonId = :lessonId AND isDone = 1")
    fun getCompletedTasksCountForLesson(lessonId: String): Flow<Int>
}
