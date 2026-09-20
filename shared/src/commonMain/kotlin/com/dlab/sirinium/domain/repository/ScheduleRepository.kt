package com.dlab.sirinium.domain.repository

import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.ScheduleFilter
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getSchedule(filter: ScheduleFilter): Flow<Resource<List<Lesson>>>
    suspend fun preloadSchedule(target: String, sectionType: String): Resource<Unit>
    suspend fun loadWeekSchedule(target: String, sectionType: String, weekOffset: Int): Resource<Unit>
    fun isWeekLoaded(target: String, weekOffset: Int): Boolean
    fun markWeekLoaded(target: String, weekOffset: Int)
    suspend fun refreshSchedule(target: String, sectionType: String, date: String? = null): Resource<Unit>
    suspend fun getGroups(query: String? = null): Resource<List<String>>
    suspend fun getTeachers(query: String? = null): Resource<List<String>>
    suspend fun getClassrooms(query: String? = null): Resource<List<String>>
    suspend fun getUpcomingLessons(target: String, limit: Int = 10): List<Lesson>
    fun getLastUpdateTime(target: String): Long
    fun observeLastUpdateTime(target: String): Flow<Long>
    suspend fun getLessonsForGroup(groupName: String): List<Lesson>
}
