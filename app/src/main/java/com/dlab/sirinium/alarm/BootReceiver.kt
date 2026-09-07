package com.dlab.sirinium.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.sync.ScheduleSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmScheduler: LessonAlarmScheduler by inject()
    private val database: SiriniumDatabase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d("BootReceiver", "Re-arming alarms after ${intent.action}")
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settingsPrefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
                        val notifyEnabled = settingsPrefs.getBoolean("notify_lessons_enabled", true)
                        if (!notifyEnabled) {
                            Log.d("BootReceiver", "Notifications disabled in settings, cancelling alarms")
                            alarmScheduler.cancelAllAlarms()
                            return@launch
                        }

                        val schedulePrefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                        val currentTarget = schedulePrefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, null)
                        if (currentTarget.isNullOrBlank()) {
                            Log.d("BootReceiver", "No current target set, cancelling alarms")
                            alarmScheduler.cancelAllAlarms()
                            return@launch
                        }

                        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
                        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                        val defaultMins = settingsPrefs.getInt("default_alarm_mins", 15)

                        val upcomingEntities = database.scheduleDao().getUpcomingLessons(
                            target = currentTarget,
                            currentDate = today,
                            currentTime = nowTime,
                            limit = 30
                        )

                        val lessons = upcomingEntities.map { it.toDomain() }
                        alarmScheduler.rescheduleAlarmsForLessons(lessons, minutesBefore = defaultMins)
                        Log.d("BootReceiver", "Successfully rescheduled ${lessons.size} alarms for target: $currentTarget")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Failed to reschedule alarms", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
