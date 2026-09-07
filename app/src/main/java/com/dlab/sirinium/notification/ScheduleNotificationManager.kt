package com.dlab.sirinium.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dlab.sirinium.MainActivity
import com.dlab.sirinium.R

class ScheduleNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "channel_sirinium_lessons"
        const val EXTRA_LESSON_ID = "extra_lesson_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_lessons_name)
            val descriptionText = context.getString(R.string.channel_lessons_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLessonNotification(
        lessonId: String,
        discipline: String,
        lessonType: String,
        classroom: String,
        teacher: String,
        startTime: String,
        address: String,
        minutesBefore: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LESSON_ID, lessonId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            lessonId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (minutesBefore > 0) {
            "Через $minutesBefore мин: $discipline"
        } else {
            "Пара начинается: $discipline"
        }

        val shortText = "$lessonType • Ауд. $classroom • в $startTime"
        val bigText = buildString {
            append("$lessonType в $startTime\n")
            append("📍 Аудитория: $classroom")
            if (address.isNotBlank()) {
                append(" ($address)")
            }
            if (teacher.isNotBlank()) {
                append("\n👨‍🏫 Преподаватель: $teacher")
            }
        }

        val isCustom = com.dlab.sirinium.core.util.AppIconManager.isCustomIconEnabled(context)
        val iconRes = if (isCustom) R.mipmap.ic_launcher_custom else R.mipmap.ic_launcher

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                iconRes,
                "Перейти к занятию",
                pendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(lessonId.hashCode(), notification)
    }
}
