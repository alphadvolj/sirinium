package com.dlab.sirinium

import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.data.local.dao.LessonNoteDao
import com.dlab.sirinium.data.local.dao.ScheduleDao
import com.dlab.sirinium.data.local.entity.HomeworkTaskEntity
import com.dlab.sirinium.data.local.entity.LessonNoteEntity
import com.dlab.sirinium.data.local.entity.ScheduleEntity
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.dto.*
import com.dlab.sirinium.data.repository.ScheduleRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ScheduleRepositoryTest {

    private class FakeScheduleApi : SiriusScheduleApi {
        val requestedWeekOffsets = mutableListOf<Int?>()

        override suspend fun getSchedule(
            group: String?,
            teacher: String?,
            classroom: String?,
            date: String?,
            weekOffset: Int?,
            dateFrom: String?,
            dateTo: String?,
            discipline: String?,
            sectionType: String?,
            search: String?,
            limit: Int?,
            offset: Int?
        ): List<ScheduleItemDto> = emptyList()

        override suspend fun getGroupSchedule(groupName: String, date: String?, weekOffset: Int?): List<ScheduleItemDto> {
            requestedWeekOffsets.add(weekOffset)
            return listOf(
                ScheduleItemDto(
                    group = groupName,
                    target = groupName,
                    date = "01.09.2026",
                    discipline = "Test Discipline",
                    startTime = "09:00",
                    endTime = "10:30"
                )
            )
        }

        override suspend fun getGroupScheduleByQuery(group: String?, name: String?, date: String?, weekOffset: Int?): List<ScheduleItemDto> = emptyList()
        override suspend fun getTeacherSchedule(teacher: String, date: String?, weekOffset: Int?): List<ScheduleItemDto> = emptyList()
        override suspend fun getTeacherScheduleByQuery(teacher: String?, name: String?, date: String?, weekOffset: Int?): List<ScheduleItemDto> = emptyList()
        override suspend fun getClassroomSchedule(classroomName: String, date: String?, weekOffset: Int?): List<ScheduleItemDto> = emptyList()
        override suspend fun getClassroomScheduleByQuery(classroom: String?, name: String?, date: String?, weekOffset: Int?): List<ScheduleItemDto> = emptyList()
        override suspend fun getGroups(query: String?): List<String> = listOf("Group1")
        override suspend fun getTeachers(query: String?): List<TeacherDto> = emptyList()
        override suspend fun getClassrooms(query: String?): List<String> = emptyList()
        override suspend fun getStatus(): StatusResponseDto = StatusResponseDto(status = "ok")
        override suspend fun triggerRefresh(): RefreshResponseDto = RefreshResponseDto(status = "ok", message = "refreshed")
        override suspend fun checkGroup(query: String?, q: String?): CheckGroupResponseDto = CheckGroupResponseDto(found = true)
        override suspend fun checkGroupByPath(groupQuery: String): CheckGroupResponseDto = CheckGroupResponseDto(found = true)
        override suspend fun checkTeacher(query: String?, q: String?): CheckTeacherResponseDto = CheckTeacherResponseDto(found = true)
        override suspend fun checkTeacherByPath(teacherQuery: String): CheckTeacherResponseDto = CheckTeacherResponseDto(found = true)
    }

    private class FakeScheduleDao : ScheduleDao {
        val replacedDates = mutableListOf<List<String>>()
        val storedEntities = mutableListOf<ScheduleEntity>()

        override fun getSchedule(target: String, date: String?): Flow<List<ScheduleEntity>> = flowOf(storedEntities)
        override fun getAllScheduleForTarget(target: String): Flow<List<ScheduleEntity>> = flowOf(storedEntities)
        override suspend fun insertAll(items: List<ScheduleEntity>) { storedEntities.addAll(items) }
        override suspend fun deleteByTarget(target: String): Int = 0
        override suspend fun deleteByTargetAndDate(target: String, date: String): Int = 0
        override suspend fun deleteByTargetAndDates(target: String, dates: List<String>): Int = 0

        override suspend fun replaceScheduleForDates(target: String, dates: List<String>, items: List<ScheduleEntity>) {
            replacedDates.add(dates)
            storedEntities.addAll(items)
        }

        override suspend fun getUpcomingLessons(target: String, currentDate: String, currentTime: String, limit: Int): List<ScheduleEntity> = emptyList()
        override suspend fun getAllUpcomingLessons(currentDate: String, currentTime: String, limit: Int): List<ScheduleEntity> = emptyList()
        override suspend fun getScheduleForTargetAndDate(target: String, date: String): List<ScheduleEntity> = emptyList()
        override suspend fun getDistinctTeachers(): List<String> = emptyList()
        override suspend fun getDistinctGroups(): List<String> = emptyList()
        override suspend fun getDistinctClassrooms(): List<String> = emptyList()
    }

    private class FakeLessonNoteDao : LessonNoteDao {
        override fun getStudentNotesForDiscipline(discipline: String): Flow<List<LessonNoteEntity>> = flowOf(emptyList())
        override fun getTeacherNotesForDisciplineAndGroup(discipline: String, groupName: String): Flow<List<LessonNoteEntity>> = flowOf(emptyList())
        override fun getNotesForLesson(lessonId: String): Flow<List<LessonNoteEntity>> = flowOf(emptyList())
        override fun getAllNotes(): Flow<List<LessonNoteEntity>> = flowOf(emptyList())
        override fun getNotesForDisciplineExceptLesson(discipline: String, currentLessonId: String): Flow<List<LessonNoteEntity>> = flowOf(emptyList())
        override suspend fun insertNote(note: LessonNoteEntity): Long = 1
        override suspend fun updateNote(note: LessonNoteEntity) {}
        override suspend fun deleteNoteById(noteId: Long): Int = 0
        override suspend fun deleteAllNotes(): Int = 0
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
    fun testPreloadScheduleLoadsSevenWeeks() = runBlocking {
        val fakeApi = FakeScheduleApi()
        val fakeDao = FakeScheduleDao()
        val fakeNoteDao = FakeLessonNoteDao()
        val repository = ScheduleRepositoryImpl(fakeApi, fakeDao, fakeNoteDao)

        val target = "K0109-26"
        assertFalse(repository.isWeekLoaded(target, 0))
        assertFalse(repository.isWeekLoaded(target, 3))
        assertFalse(repository.isWeekLoaded(target, -3))

        val result = repository.preloadSchedule(target, "group")
        assertTrue(result is Resource.Success)

        // All 7 weeks (-3, -2, -1, 0, 1, 2, 3) must be requested
        val requestedSet = fakeApi.requestedWeekOffsets.filterNotNull().toSet()
        val expectedSet = setOf(-3, -2, -1, 0, 1, 2, 3)
        assertEquals(expectedSet, requestedSet)

        // All 7 weeks must be marked as loaded
        for (offset in -3..3) {
            assertTrue("Week " + offset + " should be marked loaded", repository.isWeekLoaded(target, offset))
        }

        // Weeks outside -3..3 should NOT be marked loaded
        assertFalse(repository.isWeekLoaded(target, 4))
        assertFalse(repository.isWeekLoaded(target, -4))
    }

    @Test
    fun testLoadWeekScheduleLoadsSingleWeek() = runBlocking {
        val fakeApi = FakeScheduleApi()
        val fakeDao = FakeScheduleDao()
        val fakeNoteDao = FakeLessonNoteDao()
        val repository = ScheduleRepositoryImpl(fakeApi, fakeDao, fakeNoteDao)

        val target = "K0109-26"
        val result = repository.loadWeekSchedule(target, "group", 4)
        assertTrue(result is Resource.Success)

        assertTrue(fakeApi.requestedWeekOffsets.contains(4))
        assertTrue(repository.isWeekLoaded(target, 4))
        assertFalse(repository.isWeekLoaded(target, 5))
    }
}
