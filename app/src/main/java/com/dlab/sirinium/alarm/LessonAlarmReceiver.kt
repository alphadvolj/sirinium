package com.dlab.sirinium.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dlab.sirinium.notification.ScheduleNotificationManager
import com.dlab.sirinium.sync.ScheduleSyncWorker

class LessonAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LessonAlarmScheduler.ACTION_LESSON_ALARM) {
            val settingsPrefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
            val notifyEnabled = settingsPrefs.getBoolean("notify_lessons_enabled", true)
            if (!notifyEnabled) {
                Log.d("LessonAlarmReceiver", "Notification dropped: notifications are disabled in settings")
                return
            }

            val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
            val currentTarget = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, null)
            val lessonTarget = intent.getStringExtra(LessonAlarmScheduler.EXTRA_TARGET)

            // Only show notifications for the last selected entity
            if (!currentTarget.isNullOrBlank() && !lessonTarget.isNullOrBlank() &&
                !lessonTarget.equals(currentTarget, ignoreCase = true)
            ) {
                Log.w(
                    "LessonAlarmReceiver",
                    "Notification dropped: alarm target '$lessonTarget' does not match last selected target '$currentTarget'"
                )
                return
            }

            val lessonId = intent.getStringExtra(LessonAlarmScheduler.EXTRA_LESSON_ID) ?: return
            val discipline = intent.getStringExtra(LessonAlarmScheduler.EXTRA_DISCIPLINE) ?: "Пара"
            val lessonType = intent.getStringExtra(LessonAlarmScheduler.EXTRA_LESSON_TYPE) ?: "Занятие"
            val classroom = intent.getStringExtra(LessonAlarmScheduler.EXTRA_CLASSROOM) ?: ""
            val teacher = intent.getStringExtra(LessonAlarmScheduler.EXTRA_TEACHER) ?: ""
            val startTime = intent.getStringExtra(LessonAlarmScheduler.EXTRA_START_TIME) ?: ""
            val address = intent.getStringExtra(LessonAlarmScheduler.EXTRA_ADDRESS) ?: ""
            val minutesBefore = intent.getIntExtra(LessonAlarmScheduler.EXTRA_MINUTES_BEFORE, 15)

            Log.d("LessonAlarmReceiver", "Alarm triggered for: $discipline ($lessonType) target=$lessonTarget")

            val notificationManager = ScheduleNotificationManager(context)
            notificationManager.showLessonNotification(
                lessonId = lessonId,
                discipline = discipline,
                lessonType = lessonType,
                classroom = classroom,
                teacher = teacher,
                startTime = startTime,
                address = address,
                minutesBefore = minutesBefore
            )
        }
    }
}
