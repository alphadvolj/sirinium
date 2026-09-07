package com.dlab.sirinium.domain.repository

import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.ScheduleFilter
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    /**
     * Offline-first Flow of lessons according to current filter.
     * Emits cached data immediately from Room, then updates from network in background.
     */
    fun getSchedule(filter: ScheduleFilter): Flow<Resource<List<Lesson>>>

    /**
     * Preloads schedule for current week, 3 following weeks, and 3 previous weeks (-3..3).
     */
    suspend fun preloadSchedule(target: String, sectionType: String): Resource<Unit>

    /**
     * Loads schedule for a specific week offset (0 = current week, 1 = next week, -1 = previous, etc.)
     * Replaces only the days of that week in the local cache.
     */
    suspend fun loadWeekSchedule(target: String, sectionType: String, weekOffset: Int): Resource<Unit>

    /**
     * Checks if a given week offset is already loaded for this target.
     */
    fun isWeekLoaded(target: String, weekOffset: Int): Boolean

    /**
     * Marks a given week offset as loaded for this target.
     */
    fun markWeekLoaded(target: String, weekOffset: Int)

    /**
     * Direct network refresh and cache update.
     */
    suspend fun refreshSchedule(target: String, sectionType: String, date: String? = null): Resource<Unit>

    /**
     * Get available groups for search / selector
     */
    suspend fun getGroups(query: String? = null): Resource<List<String>>

    /**
     * Get available teachers for search / selector
     */
    suspend fun getTeachers(query: String? = null): Resource<List<String>>

    /**
     * Get available classrooms for search / selector
     */
    suspend fun getClassrooms(query: String? = null): Resource<List<String>>

    /**
     * Get upcoming lessons for notification scheduling and widget display
     */
    suspend fun getUpcomingLessons(target: String, limit: Int = 10): List<Lesson>

    /**
     * Timestamp (in epoch millis) of the last successful online schedule update for [target]
     */
    fun getLastUpdateTime(target: String): Long

    /**
     * Observe timestamp of the last successful online schedule update for [target]
     */
    fun observeLastUpdateTime(target: String): Flow<Long>

    /**
     * Get cached lessons for a specific group name
     */
    suspend fun getLessonsForGroup(groupName: String): List<Lesson>
}
