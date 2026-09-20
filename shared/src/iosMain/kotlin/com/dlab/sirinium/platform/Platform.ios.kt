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
    private var isAuthRequested = false

    private fun requestNotificationPermissionIfNeeded() {
        if (!isAuthRequested) {
            isAuthRequested = true
            val options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionSound or
                    UNAuthorizationOptionBadge
            center.requestAuthorizationWithOptions(options) { _, _ -> }
        }
    }

    override fun scheduleAlarm(lesson: Lesson, minutesBefore: Int) {
        requestNotificationPermissionIfNeeded()
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

@kotlinx.serialization.Serializable
private data class IosWidgetPayload(
    val target: String,
    val sectionType: String,
    val updatedAt: Long,
    val lessons: List<IosWidgetLessonPayload>
)

@kotlinx.serialization.Serializable
private data class IosWidgetLessonPayload(
    val id: String,
    val discipline: String,
    val startTime: String,
    val endTime: String,
    val classroom: String,
    val teacher: String,
    val rawLessonType: String,
    val numberPair: Int,
    val date: String
)

class IosWidgetUpdater(
    private val settings: PlatformSettings,
    private val repository: com.dlab.sirinium.domain.repository.ScheduleRepository
) : PlatformWidgetUpdater {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
    private val jsonFormatter = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun updateWidgets() {
        val target = settings.getString("pref_current_target", "")
        val section = settings.getString("pref_current_section", "group").ifBlank { "group" }
        if (target.isBlank()) return

        scope.launch {
            try {
                val upcoming = repository.getUpcomingLessons(target, limit = 20)
                val payload = IosWidgetPayload(
                    target = target,
                    sectionType = section,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    lessons = upcoming.map {
                        IosWidgetLessonPayload(
                            id = it.id,
                            discipline = it.discipline,
                            startTime = it.startTime,
                            endTime = it.endTime,
                            classroom = it.classroom,
                            teacher = it.teacher,
                            rawLessonType = it.rawLessonType.ifBlank { it.lessonType.title },
                            numberPair = it.numberPair,
                            date = it.date
                        )
                    }
                )
                val jsonString = jsonFormatter.encodeToString(payload)

                // 1. App Group shared container for WidgetExtension
                val groupDefaults = NSUserDefaults(suiteName = "group.com.dlab.sirinium")
                groupDefaults?.setObject(jsonString, forKey = "widget_schedule_data")
                groupDefaults?.synchronize()

                // 2. Standard user defaults container as fallback
                val standardDefaults = NSUserDefaults.standardUserDefaults
                standardDefaults.setObject(jsonString, forKey = "widget_schedule_data")
                standardDefaults.synchronize()

                // 3. Post notification for Swift to call WidgetCenter.shared.reloadAllTimelines()
                platform.Foundation.NSNotificationCenter.defaultCenter.postNotificationName("ReloadWidgetsNotification", null)
            } catch (e: Exception) {
                println("[IosWidgetUpdater] Error updating widget: ${e.message}")
            }
        }
    }
}

actual fun createPlatformSettings(): PlatformSettings = IosPlatformSettings()
actual fun createPlatformActions(): PlatformActions = IosPlatformActions()
actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = IosAlarmScheduler()
actual fun createPlatformWidgetUpdater(
    settings: PlatformSettings,
    repository: com.dlab.sirinium.domain.repository.ScheduleRepository
): PlatformWidgetUpdater = IosWidgetUpdater(settings, repository)

@androidx.compose.runtime.Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}
