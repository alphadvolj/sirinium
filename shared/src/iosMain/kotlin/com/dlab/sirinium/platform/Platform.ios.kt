package com.dlab.sirinium.platform

import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIPasteboard
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

internal fun getAppGroupSuites(): List<String> {
    val list = mutableListOf("group.com.dlab.sirinium", "group.com.dlab.sirinium.ios")
    val bundleId = NSBundle.mainBundle.bundleIdentifier
    if (!bundleId.isNullOrBlank()) {
        list.add("group.$bundleId")
    }
    return list
}

internal fun mirrorTargetAndSection(key: String, value: String) {
    if (value.isBlank()) return
    val suites = getAppGroupSuites()
    val altKey = if (key == "pref_current_target") "widget_active_target" else "widget_active_section"
    val fileName = if (key == "pref_current_target") "widget_active_target.txt" else "widget_active_section.txt"

    for (suite in suites) {
        val defs = NSUserDefaults(suiteName = suite)
        defs?.setObject(value, forKey = key)
        defs?.setObject(value, forKey = altKey)
        defs?.synchronize()

        try {
            val containerUrl = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(suite)
            val fileUrl = containerUrl?.URLByAppendingPathComponent(fileName)
            val path = fileUrl?.path
            val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            if (path != null && data != null) {
                data.writeToFile(path, atomically = true)
            }
        } catch (_: Exception) {}
    }
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = altKey)
    NSUserDefaults.standardUserDefaults.synchronize()
    NSNotificationCenter.defaultCenter.postNotificationName("ReloadWidgetsNotification", null)
}

class IosPlatformSettings : PlatformSettings {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
        userDefaults.synchronize()
        if (key == "pref_current_target" || key == "pref_current_section") {
            mirrorTargetAndSection(key, value)
        }
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
    private var activePickerDelegate: NSObject? = null

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

    override fun shareFeedback(subject: String, body: String, attachments: List<String>) {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVc != null) {
            val items = mutableListOf<Any>(subject + "\n\n" + body)
            for (path in attachments) {
                val fileUrl = NSURL.fileURLWithPath(path)
                items.add(fileUrl)
            }
            val activityVc = UIActivityViewController(
                activityItems = items,
                applicationActivities = null
            )
            rootVc.presentViewController(activityVc, animated = true, completion = null)
        } else {
            copyToClipboard("$subject\n\n$body")
        }
    }

    override fun pickImages(maxCount: Int, onResult: (List<String>) -> Unit) {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                picker.dismissViewControllerAnimated(true, completion = null)
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                if (image != null) {
                    val data = UIImageJPEGRepresentation(image, 0.8)
                    if (data != null) {
                        val path = NSTemporaryDirectory() + "feedback_${NSUUID().UUIDString}.jpg"
                        data.writeToFile(path, atomically = true)
                        onResult(listOf(path))
                        return
                    }
                }
                onResult(emptyList())
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, completion = null)
                onResult(emptyList())
            }
        }
        activePickerDelegate = delegate

        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            this.delegate = delegate
        }
        rootVc.presentViewController(picker, animated = true, completion = null)
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
    val group: String = "",
    val rawLessonType: String,
    val numberPair: Int,
    val date: String
)

class IosWidgetUpdater(
    private val settings: PlatformSettings,
    private val repository: ScheduleRepository
) : PlatformWidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun updateWidgets() {
        val target = settings.getString("pref_current_target", "")
        val section = settings.getString("pref_current_section", "group").ifBlank { "group" }
        if (target.isBlank()) return

        // 1. Immediately mirror active target and section to all suites and containers
        mirrorTargetAndSection("pref_current_target", target)
        mirrorTargetAndSection("pref_current_section", section)

        scope.launch {
            try {
                val upcoming = repository.getUpcomingLessons(target, limit = 25)
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
                            group = it.group,
                            rawLessonType = it.rawLessonType.ifBlank { it.lessonType.title },
                            numberPair = it.numberPair,
                            date = it.date
                        )
                    }
                )
                val jsonString = jsonFormatter.encodeToString(IosWidgetPayload.serializer(), payload)
                val jsonData = (jsonString as NSString).dataUsingEncoding(NSUTF8StringEncoding)

                val suites = getAppGroupSuites()
                for (suite in suites) {
                    val groupDefaults = NSUserDefaults(suiteName = suite)
                    groupDefaults?.setObject(jsonString, forKey = "widget_schedule_data")
                    groupDefaults?.setObject(target, forKey = "widget_active_target")
                    groupDefaults?.setObject(section, forKey = "widget_active_section")
                    groupDefaults?.synchronize()

                    try {
                        val containerUrl = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(suite)
                        val fileUrl = containerUrl?.URLByAppendingPathComponent("widget_schedule_data.json")
                        val path = fileUrl?.path
                        if (path != null && jsonData != null) {
                            jsonData.writeToFile(path, atomically = true)
                        }
                    } catch (_: Exception) {}
                }

                val standardDefaults = NSUserDefaults.standardUserDefaults
                standardDefaults.setObject(jsonString, forKey = "widget_schedule_data")
                standardDefaults.setObject(target, forKey = "widget_active_target")
                standardDefaults.setObject(section, forKey = "widget_active_section")
                standardDefaults.synchronize()

                NSNotificationCenter.defaultCenter.postNotificationName("ReloadWidgetsNotification", null)
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
    repository: ScheduleRepository
): PlatformWidgetUpdater = IosWidgetUpdater(settings, repository)

@androidx.compose.runtime.Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}
