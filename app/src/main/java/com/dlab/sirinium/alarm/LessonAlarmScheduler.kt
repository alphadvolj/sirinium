package com.dlab.sirinium.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.sync.ScheduleSyncWorker

class LessonAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("sirinium_alarm_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_LESSON_ALARM = "com.dlab.sirinium.ACTION_LESSON_ALARM"
        const val EXTRA_LESSON_ID = "extra_lesson_id"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_DISCIPLINE = "extra_discipline"
        const val EXTRA_LESSON_TYPE = "extra_lesson_type"
        const val EXTRA_CLASSROOM = "extra_classroom"
        const val EXTRA_TEACHER = "extra_teacher"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
        private const val KEY_SCHEDULED_LESSON_IDS = "scheduled_lesson_ids"
        private const val KEY_DISABLED_LESSON_IDS = "disabled_lesson_ids"
    }

    fun getDisabledLessonIds(): MutableSet<String> {
        return prefs.getStringSet(KEY_DISABLED_LESSON_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun disableAlarmForLesson(lessonId: String) {
        cancelAlarm(lessonId)
        val disabled = getDisabledLessonIds()
        if (disabled.add(lessonId)) {
            prefs.edit().putStringSet(KEY_DISABLED_LESSON_IDS, disabled).apply()
        }
    }

    fun enableAlarmForLesson(lessonId: String) {
        val disabled = getDisabledLessonIds()
        if (disabled.remove(lessonId)) {
            prefs.edit().putStringSet(KEY_DISABLED_LESSON_IDS, disabled).apply()
        }
    }

    private fun getScheduledLessonIds(): MutableSet<String> {
        return prefs.getStringSet(KEY_SCHEDULED_LESSON_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun addScheduledLessonId(lessonId: String) {
        val ids = getScheduledLessonIds()
        if (ids.add(lessonId)) {
            prefs.edit().putStringSet(KEY_SCHEDULED_LESSON_IDS, ids).apply()
        }
    }

    private fun removeScheduledLessonId(lessonId: String) {
        val ids = getScheduledLessonIds()
        if (ids.remove(lessonId)) {
            prefs.edit().putStringSet(KEY_SCHEDULED_LESSON_IDS, ids).apply()
        }
    }

    /**
     * Check if app has permission to schedule exact alarms (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules exact alarm for a single lesson.
     * Validates that the lesson matches the last selected target.
     * @param minutesBefore reminders 10–15 minutes before lesson starts
     */
    fun scheduleAlarm(lesson: Lesson, minutesBefore: Int = 15) {
        enableAlarmForLesson(lesson.id)
        val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
        val currentTarget = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, "") ?: ""
        if (currentTarget.isNotBlank() && lesson.target.isNotBlank() && !lesson.target.equals(currentTarget, ignoreCase = true)) {
            Log.d("AlarmScheduler", "Skipping alarm for ${lesson.discipline} (${lesson.target}) because current target is $currentTarget")
            return
        }

        val lessonStartEpoch = DateTimeUtils.toEpochMillis(lesson.date, lesson.startTime) ?: return
        val triggerAtMillis = lessonStartEpoch - (minutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (triggerAtMillis <= now) {
            // Already in the past
            return
        }

        val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
            action = ACTION_LESSON_ALARM
            putExtra(EXTRA_LESSON_ID, lesson.id)
            putExtra(EXTRA_TARGET, lesson.target)
            putExtra(EXTRA_DISCIPLINE, lesson.discipline)
            putExtra(EXTRA_LESSON_TYPE, lesson.rawLessonType.ifBlank { lesson.lessonType.title })
            putExtra(EXTRA_CLASSROOM, lesson.classroom)
            putExtra(EXTRA_TEACHER, lesson.teacher)
            putExtra(EXTRA_START_TIME, lesson.startTime)
            putExtra(EXTRA_ADDRESS, lesson.address)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lesson.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Exact alarm scheduled for ${lesson.discipline} (${lesson.target}) at $triggerAtMillis")
            } else {
                // Inexact fallback if user revoked exact alarm permission
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.w("AlarmScheduler", "Scheduled inexact alarm (exact permission missing)")
            }
            addScheduledLessonId(lesson.id)
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule alarm: permission denied", e)
        }
    }

    fun cancelAlarm(lessonId: String) {
        val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
            action = ACTION_LESSON_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        removeScheduledLessonId(lessonId)
    }

    /**
     * Cancels all previously scheduled alarms in AlarmManager
     */
    fun cancelAllAlarms() {
        val ids = getScheduledLessonIds()
        for (id in ids) {
            val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
                action = ACTION_LESSON_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        prefs.edit().remove(KEY_SCHEDULED_LESSON_IDS).apply()
        Log.d("AlarmScheduler", "Cancelled all ${ids.size} scheduled alarms")
    }

    fun scheduleAlarmsForLessons(lessons: List<Lesson>, minutesBefore: Int = 15) {
        val disabled = getDisabledLessonIds()
        lessons.forEach { lesson ->
            if (!disabled.contains(lesson.id)) {
                scheduleAlarm(lesson, minutesBefore)
            }
        }
    }

    /**
     * Clears all old alarms and schedules alarms for the current target's lessons
     */
    fun rescheduleAlarmsForLessons(lessons: List<Lesson>, minutesBefore: Int = 15) {
        cancelAllAlarms()
        scheduleAlarmsForLessons(lessons, minutesBefore)
    }
}
