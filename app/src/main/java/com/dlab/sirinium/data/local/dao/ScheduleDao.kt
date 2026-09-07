package com.dlab.sirinium.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dlab.sirinium.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("""
        SELECT * FROM schedule_items 
        WHERE target = :target 
        AND (:date IS NULL OR date = :date)
        ORDER BY 
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) ASC, 
            startTime ASC, 
            numberPair ASC
    """)
    fun getSchedule(target: String, date: String?): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT * FROM schedule_items 
        WHERE target = :target 
        ORDER BY 
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) ASC, 
            startTime ASC, 
            numberPair ASC
    """)
    fun getAllScheduleForTarget(target: String): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleEntity>)

    @Query("DELETE FROM schedule_items WHERE target = :target")
    suspend fun deleteByTarget(target: String): Int

    @Query("DELETE FROM schedule_items WHERE target = :target AND date = :date")
    suspend fun deleteByTargetAndDate(target: String, date: String): Int

    @Query("DELETE FROM schedule_items WHERE target = :target AND date IN (:dates)")
    suspend fun deleteByTargetAndDates(target: String, dates: List<String>): Int

    @Transaction
    suspend fun replaceScheduleForTarget(target: String, items: List<ScheduleEntity>) {
        deleteByTarget(target)
        insertAll(items)
    }

    @Transaction
    suspend fun replaceScheduleForDates(target: String, dates: List<String>, items: List<ScheduleEntity>) {
        if (dates.isNotEmpty()) {
            deleteByTargetAndDates(target, dates)
        }
        insertAll(items)
    }

    @Query("""
        SELECT * FROM schedule_items
        WHERE target = :target
        AND (
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) > 
            (substr(:currentDate, 7, 4) || '-' || substr(:currentDate, 4, 2) || '-' || substr(:currentDate, 1, 2))
            OR (date = :currentDate AND endTime >= :currentTime)
        )
        ORDER BY 
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) ASC, 
            startTime ASC
        LIMIT :limit
    """)
    suspend fun getUpcomingLessons(
        target: String,
        currentDate: String,
        currentTime: String,
        limit: Int
    ): List<ScheduleEntity>

    @Query("""
        SELECT * FROM schedule_items
        WHERE (
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) > 
            (substr(:currentDate, 7, 4) || '-' || substr(:currentDate, 4, 2) || '-' || substr(:currentDate, 1, 2))
            OR (date = :currentDate AND endTime >= :currentTime)
        )
        ORDER BY 
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) ASC, 
            startTime ASC
        LIMIT :limit
    """)
    suspend fun getAllUpcomingLessons(
        currentDate: String,
        currentTime: String,
        limit: Int = 50
    ): List<ScheduleEntity>

    @Query("""
        SELECT * FROM schedule_items 
        WHERE target = :target 
        AND date = :date
        ORDER BY startTime ASC, numberPair ASC
    """)
    suspend fun getScheduleForTargetAndDate(target: String, date: String): List<ScheduleEntity>

    @Query("""
        SELECT * FROM schedule_items 
        WHERE date = :date
        ORDER BY startTime ASC, numberPair ASC
    """)
    suspend fun getScheduleForDate(date: String): List<ScheduleEntity>

    @Query("SELECT DISTINCT teacher FROM schedule_items WHERE teacher != '' ORDER BY teacher ASC")
    suspend fun getDistinctTeachers(): List<String>

    @Query("SELECT DISTINCT target FROM schedule_items WHERE target != '' ORDER BY target ASC")
    suspend fun getDistinctGroups(): List<String>

    @Query("SELECT DISTINCT classroom FROM schedule_items WHERE classroom != '' ORDER BY classroom ASC")
    suspend fun getDistinctClassrooms(): List<String>

    @Query("""
        SELECT * FROM schedule_items 
        WHERE (target = :groupName OR groupName = :groupName)
        ORDER BY 
            (substr(date, 7, 4) || '-' || substr(date, 4, 2) || '-' || substr(date, 1, 2)) ASC, 
            startTime ASC, 
            numberPair ASC
    """)
    suspend fun getLessonsForGroup(groupName: String): List<ScheduleEntity>
}
