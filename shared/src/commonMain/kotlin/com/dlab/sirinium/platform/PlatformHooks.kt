package com.dlab.sirinium.platform

import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository

interface PlatformAlarmScheduler {
    fun scheduleAlarm(lesson: Lesson, minutesBefore: Int = 15)
    fun disableAlarmForLesson(lessonId: String)
    fun rescheduleAlarmsForLessons(lessons: List<Lesson>, defaultMinutesBefore: Int = 15)
    fun cancelAllAlarms()
    fun hasExactAlarmPermission(): Boolean = true
}

class NoOpAlarmScheduler : PlatformAlarmScheduler {
    override fun scheduleAlarm(lesson: Lesson, minutesBefore: Int) {}
    override fun disableAlarmForLesson(lessonId: String) {}
    override fun rescheduleAlarmsForLessons(lessons: List<Lesson>, defaultMinutesBefore: Int) {}
    override fun cancelAllAlarms() {}
}

interface PlatformWidgetUpdater {
    fun updateWidgets()
}

class NoOpWidgetUpdater : PlatformWidgetUpdater {
    override fun updateWidgets() {}
}

interface PlatformIconManager {
    fun setCustomIconEnabled(enabled: Boolean) {}
    fun isCustomIconEnabled(): Boolean = false
}

class NoOpIconManager : PlatformIconManager

interface PlatformSyncScheduler {
    fun schedulePeriodicSync(intervalMinutes: Long) {}
    fun cancelPeriodicSync() {}
}

class NoOpSyncScheduler : PlatformSyncScheduler

expect fun createPlatformAlarmScheduler(): PlatformAlarmScheduler

expect fun createPlatformWidgetUpdater(
    settings: PlatformSettings,
    repository: ScheduleRepository
): PlatformWidgetUpdater
