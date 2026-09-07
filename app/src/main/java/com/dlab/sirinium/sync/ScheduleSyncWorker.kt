package com.dlab.sirinium.sync

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.widget.WidgetUpdateHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScheduleSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val scheduleRepository: ScheduleRepository by inject()
    private val alarmScheduler: LessonAlarmScheduler by inject()

    companion object {
        const val PREFS_NAME = "sirinium_prefs"
        const val KEY_CURRENT_TARGET = "pref_current_target"
        const val KEY_CURRENT_SECTION = "pref_current_section"
        const val DEFAULT_TARGET = "К1609-241"
        const val DEFAULT_SECTION = "group"
    }

    override suspend fun doWork(): Result {
        Log.d("ScheduleSyncWorker", "Starting background schedule sync...")

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val target = prefs.getString(KEY_CURRENT_TARGET, DEFAULT_TARGET) ?: DEFAULT_TARGET
        val sectionType = prefs.getString(KEY_CURRENT_SECTION, DEFAULT_SECTION) ?: DEFAULT_SECTION

        return try {
            val result = scheduleRepository.refreshSchedule(target, sectionType)
            if (result is Resource.Success) {
                Log.d("ScheduleSyncWorker", "Schedule refreshed successfully for $target")

                // Update exact alarms for upcoming lessons of the current target
                val settingsPrefs = applicationContext.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
                val notifyEnabled = settingsPrefs.getBoolean("notify_lessons_enabled", true)
                val defaultMins = settingsPrefs.getInt("default_alarm_mins", 15)
                if (notifyEnabled) {
                    val upcoming = scheduleRepository.getUpcomingLessons(target, limit = 20)
                    alarmScheduler.rescheduleAlarmsForLessons(upcoming, minutesBefore = defaultMins)
                } else {
                    alarmScheduler.cancelAllAlarms()
                }

                // Refresh Glance widgets immediately
                WidgetUpdateHelper.updateAllWidgets(applicationContext)

                Result.success()
            } else {
                Log.w("ScheduleSyncWorker", "Schedule refresh returned non-success, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ScheduleSyncWorker", "Error during schedule sync", e)
            Result.retry()
        }
    }
}
