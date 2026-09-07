package com.dlab.sirinium.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_items",
    indices = [
        Index(value = ["target", "date"]),
        Index(value = ["sectionType", "target"]),
        Index(value = ["date", "startTime"])
    ]
)
data class ScheduleEntity(
    @PrimaryKey
    val id: String,
    val sectionType: String,
    val target: String,
    val date: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val discipline: String,
    val lessonType: String,
    val teacher: String,
    val classroom: String,
    val address: String,
    val onlineUrl: String?,
    val groupName: String,
    val numberPair: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
