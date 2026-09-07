package com.dlab.sirinium

import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.data.local.dao.LessonNoteDao
import com.dlab.sirinium.data.local.entity.HomeworkTaskEntity
import com.dlab.sirinium.data.local.entity.LessonNoteEntity
import com.dlab.sirinium.data.repository.LessonNoteRepositoryImpl
import com.dlab.sirinium.domain.model.Lesson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonNoteRepositoryTest {

    private class InMemoryLessonNoteDao : LessonNoteDao {
        val notes = mutableListOf<LessonNoteEntity>()

        override fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNoteEntity>> {
            val filtered = notes.filter {
                it.scope == "student" && it.discipline.trim().equals(discipline.trim(), ignoreCase = true)
            }
            return flowOf(filtered)
        }

        override fun getTeacherNotesForDisciplineAndGroup(discipline: String, groupName: String): Flow<List<LessonNoteEntity>> {
            val filtered = notes.filter {
                it.scope == "teacher" &&
                    it.discipline.trim().equals(discipline.trim(), ignoreCase = true) &&
                    it.groupName.trim().equals(groupName.trim(), ignoreCase = true)
            }
            return flowOf(filtered)
        }

        override fun getNotesForLesson(lessonId: String): Flow<List<LessonNoteEntity>> {
            return flowOf(notes.filter { it.lessonId == lessonId })
        }

        override fun getAllNotes(): Flow<List<LessonNoteEntity>> = flowOf(notes)

        override fun getNotesForDisciplineExceptLesson(discipline: String, currentLessonId: String): Flow<List<LessonNoteEntity>> {
            return flowOf(notes.filter { it.discipline == discipline && it.lessonId != currentLessonId })
        }

        override suspend fun insertNote(note: LessonNoteEntity): Long {
            val assignedId = if (note.id == 0L) (notes.size + 1).toLong() else note.id
            val entity = note.copy(id = assignedId)
            notes.add(entity)
            return assignedId
        }

        override suspend fun updateNote(note: LessonNoteEntity) {
            val index = notes.indexOfFirst { it.id == note.id }
            if (index != -1) notes[index] = note
        }

        override suspend fun deleteNoteById(noteId: Long): Int {
            return if (notes.removeIf { it.id == noteId }) 1 else 0
        }

        override suspend fun deleteAllNotes(): Int {
            val count = notes.size
            notes.clear()
            return count
        }

        override fun getTasksForLesson(lessonId: String): Flow<List<HomeworkTaskEntity>> = flowOf(emptyList())
        override suspend fun getTasksForLessonSync(lessonId: String): List<HomeworkTaskEntity> = emptyList()
        override suspend fun insertTask(task: HomeworkTaskEntity): Long = 1
        override suspend fun updateTaskStatus(taskId: Long, isDone: Boolean): Int = 0
        override suspend fun deleteTaskById(taskId: Long): Int = 0
        override fun getNotesCountForLesson(lessonId: String): Flow<Int> = flowOf(0)
        override fun getTotalTasksCountForLesson(lessonId: String): Flow<Int> = flowOf(0)
        override fun getCompletedTasksCountForLesson(lessonId: String): Flow<Int> = flowOf(0)
    }

    @Test
    fun testStudentNotesBoundToDisciplineAndIsolatedFromTeacher() = runBlocking {
        val dao = InMemoryLessonNoteDao()
        val repo = LessonNoteRepositoryImpl(dao)

        // Seed: student note for "Математический анализ"
        dao.insertNote(
            LessonNoteEntity(
                id = 1,
                lessonId = "lesson_student_1",
                scope = "student",
                discipline = "Математический анализ",
                title = "Формула Тейлора",
                content = "Не забыть остаточный член в форме Лагранжа",
                colorHex = 0xFFFF0000
            )
        )

        // Seed: teacher note for the same discipline "Математический анализ" for group "24-1"
        dao.insertNote(
            LessonNoteEntity(
                id = 2,
                lessonId = "lesson_teacher_1",
                scope = "teacher",
                discipline = "Математический анализ",
                groupName = "24-1",
                title = "Контрольная работа",
                content = "Проверить посещаемость группы 24-1",
                colorHex = 0xFF00FF00
            )
        )

        // Student viewing another lesson of "Математический анализ" (different lessonId, group "24-2")
        val studentLesson = Lesson(
            id = "lesson_student_2",
            sectionType = "group",
            target = "24-2",
            group = "24-2",
            discipline = "Математический анализ",
            teacher = "Иванов И.И.",
            classroom = "101",
            startTime = "09:00",
            endTime = "10:30",
            date = "01.09.2026",
            numberPair = 1,
            lessonType = LessonType.LECTURE
        )

        val studentNotes = repo.getNotesForLesson(studentLesson).first()
        assertEquals(1, studentNotes.size)
        assertEquals("Формула Тейлора", studentNotes[0].title)
        assertEquals("student", studentNotes[0].scope)
    }

    @Test
    fun testTeacherNotesBoundToDisciplineAndGroupWithoutCollisions() = runBlocking {
        val dao = InMemoryLessonNoteDao()
        val repo = LessonNoteRepositoryImpl(dao)

        // Teacher note for Group 24-1
        dao.insertNote(
            LessonNoteEntity(
                id = 1,
                lessonId = "l1",
                scope = "teacher",
                discipline = "Физика",
                groupName = "24-1",
                title = "Тест 24-1",
                content = "Дать вариант 1",
                colorHex = 0xFF0000FF
            )
        )

        // Teacher note for Group 24-2
        dao.insertNote(
            LessonNoteEntity(
                id = 2,
                lessonId = "l2",
                scope = "teacher",
                discipline = "Физика",
                groupName = "24-2",
                title = "Тест 24-2",
                content = "Дать вариант 2",
                colorHex = 0xFF0000FF
            )
        )

        // Student note for Физика
        dao.insertNote(
            LessonNoteEntity(
                id = 3,
                lessonId = "l3",
                scope = "student",
                discipline = "Физика",
                groupName = "24-1",
                title = "Шпора по оптике",
                content = "n1 * sin(a) = n2 * sin(b)",
                colorHex = 0xFF0000FF
            )
        )

        // Teacher viewing lesson for group 24-1
        val teacherLessonGroup1 = Lesson(
            id = "l_teach_1",
            sectionType = "teacher",
            target = "Петров П.П.",
            group = "24-1",
            discipline = "Физика",
            teacher = "Петров П.П.",
            classroom = "202",
            startTime = "10:45",
            endTime = "12:15",
            date = "01.09.2026",
            numberPair = 2,
            lessonType = LessonType.PRACTICE
        )

        val teacherNotes1 = repo.getNotesForLesson(teacherLessonGroup1).first()
        assertEquals(1, teacherNotes1.size)
        assertEquals("Тест 24-1", teacherNotes1[0].title)

        // Teacher viewing lesson for group 24-2
        val teacherLessonGroup2 = teacherLessonGroup1.copy(group = "24-2")
        val teacherNotes2 = repo.getNotesForLesson(teacherLessonGroup2).first()
        assertEquals(1, teacherNotes2.size)
        assertEquals("Тест 24-2", teacherNotes2[0].title)

        // Teacher viewing lesson for group 24-3 (no notes for this group)
        val teacherLessonGroup3 = teacherLessonGroup1.copy(group = "24-3")
        val teacherNotes3 = repo.getNotesForLesson(teacherLessonGroup3).first()
        assertTrue(teacherNotes3.isEmpty())
    }

    @Test
    fun testClassroomViewHasZeroNotes() = runBlocking {
        val dao = InMemoryLessonNoteDao()
        val repo = LessonNoteRepositoryImpl(dao)

        dao.insertNote(
            LessonNoteEntity(
                id = 1,
                lessonId = "l_cls",
                scope = "student",
                discipline = "Химия",
                groupName = "24-1",
                title = "Заметка по химии",
                content = "H2O",
                colorHex = 0xFF00FF00
            )
        )

        val classroomLesson = Lesson(
            id = "l_cls",
            sectionType = "classroom",
            target = "Ауд. 305",
            group = "24-1",
            discipline = "Химия",
            teacher = "Сидоров С.С.",
            classroom = "Ауд. 305",
            startTime = "13:00",
            endTime = "14:30",
            date = "01.09.2026",
            numberPair = 3,
            lessonType = LessonType.LAB
        )

        val notes = repo.getNotesForLesson(classroomLesson).first()
        assertTrue("Classroom view should return empty notes list", notes.isEmpty())
    }
}
