package com.dlab.sirinium.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dlab.sirinium.data.local.dao.LessonNoteDao
import com.dlab.sirinium.data.local.dao.ScheduleDao
import com.dlab.sirinium.data.local.entity.HomeworkTaskEntity
import com.dlab.sirinium.data.local.entity.LessonNoteEntity
import com.dlab.sirinium.data.local.entity.ScheduleEntity

@Database(
    entities = [
        ScheduleEntity::class,
        LessonNoteEntity::class,
        HomeworkTaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SiriniumDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun lessonNoteDao(): LessonNoteDao

    companion object {
        const val DATABASE_NAME = "sirinium_schedule.db"
    }
}
