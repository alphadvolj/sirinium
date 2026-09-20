package com.dlab.sirinium.platform

import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import kotlinx.datetime.Clock
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosPlatformSettings : PlatformSettings {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (userDefaults.objectForKey(key) != null) userDefaults.boolForKey(key) else defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return if (userDefaults.objectForKey(key) != null) userDefaults.integerForKey(key) else defaultValue
    }

    override fun putLong(key: String, value: Long) {
        userDefaults.setInteger(value, forKey = key)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return if (userDefaults.objectForKey(key) != null) userDefaults.integerForKey(key).toInt() else defaultValue
    }

    override fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        val raw = userDefaults.stringForKey(key) ?: return defaultValue
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        userDefaults.setObject(value.joinToString("\n"), forKey = key)
    }
}

class IosPlatformActions : PlatformActions {
    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun shareText(text: String) {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVc != null) {
            val activityVc = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null
            )
            rootVc.presentViewController(activityVc, animated = true, completion = null)
        } else {
            copyToClipboard(text)
        }
    }

    override fun showToast(message: String) {
        // iOS does not have native toasts; message logged or handled by UI
        println("[iOS Message] $message")
    }

    override fun getPlatformName(): String = "iOS"
}

class IosAlarmScheduler : PlatformAlarmScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound or
                UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { _, _ -> }
    }

    override fun scheduleAlarm(lesson: Lesson, minutesBefore: Int) {
        val epochMillis = DateTimeUtils.toEpochMillis(lesson.date, lesson.startTime) ?: return
        val triggerTime = epochMillis - (minutesBefore * 60 * 1000L)
        val now = Clock.System.now().toEpochMilliseconds()
        val secondsRemaining = (triggerTime - now) / 1000.0

        if (secondsRemaining <= 0) return

        val content = UNMutableNotificationContent().apply {
            setTitle(lesson.discipline)
            setSubtitle("${lesson.startTime} • ${lesson.classroom}")
            setBody("${lesson.rawLessonType.ifBlank { lesson.lessonType.title }} • ${lesson.teacher}")
            setSound(UNNotificationSound.defaultSound())
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = secondsRemaining,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = lesson.id,
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request) { _ -> }
    }

    override fun disableAlarmForLesson(lessonId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(lessonId))
    }

    override fun rescheduleAlarmsForLessons(lessons: List<Lesson>, defaultMinutesBefore: Int) {
        cancelAllAlarms()
        lessons.forEach { scheduleAlarm(it, defaultMinutesBefore) }
    }

    override fun cancelAllAlarms() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }
}

actual fun createPlatformSettings(): PlatformSettings = IosPlatformSettings()
actual fun createPlatformActions(): PlatformActions = IosPlatformActions()
actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = IosAlarmScheduler()

@androidx.compose.runtime.Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}
