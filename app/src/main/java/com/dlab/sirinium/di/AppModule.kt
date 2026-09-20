package com.dlab.sirinium.di

import android.content.Context
import androidx.room.Room
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.core.util.AppIconManager
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.repository.LessonNoteRepositoryImpl
import com.dlab.sirinium.data.repository.ScheduleRepositoryImpl
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.notification.ScheduleNotificationManager
import com.dlab.sirinium.platform.PlatformActions
import com.dlab.sirinium.platform.PlatformAlarmScheduler
import com.dlab.sirinium.platform.PlatformIconManager
import com.dlab.sirinium.platform.PlatformSettings
import com.dlab.sirinium.platform.PlatformSyncScheduler
import com.dlab.sirinium.platform.PlatformWidgetUpdater
import com.dlab.sirinium.platform.createPlatformActions
import com.dlab.sirinium.platform.createPlatformSettings
import com.dlab.sirinium.sync.SyncScheduler
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.onboarding.OnboardingViewModel
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsViewModel
import com.dlab.sirinium.ui.update.AppUpdateViewModel
import com.dlab.sirinium.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class AndroidAlarmScheduler(
    private val scheduler: LessonAlarmScheduler
) : PlatformAlarmScheduler {
    override fun scheduleAlarm(lesson: Lesson, minutesBefore: Int) =
        scheduler.scheduleAlarm(lesson, minutesBefore)

    override fun disableAlarmForLesson(lessonId: String) =
        scheduler.disableAlarmForLesson(lessonId)

    override fun rescheduleAlarmsForLessons(lessons: List<Lesson>, defaultMinutesBefore: Int) =
        scheduler.rescheduleAlarmsForLessons(lessons, defaultMinutesBefore)

    override fun cancelAllAlarms() =
        scheduler.cancelAllAlarms()

    override fun hasExactAlarmPermission(): Boolean =
        scheduler.canScheduleExactAlarms()
}

class AndroidWidgetUpdater(
    private val context: Context
) : PlatformWidgetUpdater {
    override fun updateWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }
}

class AndroidIconManager(
    private val context: Context
) : PlatformIconManager {
    override fun setCustomIconEnabled(enabled: Boolean) {
        AppIconManager.setCustomIconEnabled(context, enabled)
    }

    override fun isCustomIconEnabled(): Boolean {
        return AppIconManager.isCustomIconEnabled(context)
    }
}

class AndroidSyncScheduler(
    private val context: Context
) : PlatformSyncScheduler {
    override fun schedulePeriodicSync(intervalMinutes: Long) {
        SyncScheduler.schedulePeriodicSync(context, intervalMinutes)
    }

    override fun cancelPeriodicSync() {
        SyncScheduler.cancelPeriodicSync(context)
    }
}

val appModule = module {

    // Platform Adapters
    single<PlatformAlarmScheduler> { AndroidAlarmScheduler(get()) }
    single<PlatformWidgetUpdater> { AndroidWidgetUpdater(androidContext()) }
    single<PlatformIconManager> { AndroidIconManager(androidContext()) }
    single<PlatformSyncScheduler> { AndroidSyncScheduler(androidContext()) }
    single<PlatformSettings> { createPlatformSettings() }
    single<PlatformActions> { createPlatformActions() }

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            SiriniumDatabase::class.java,
            SiriniumDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
    }

    single { get<SiriniumDatabase>().scheduleDao() }
    single { get<SiriniumDatabase>().lessonNoteDao() }

    // Repositories (Android Room-backed implementations)
    single<ScheduleRepository> {
        ScheduleRepositoryImpl(
            context = androidContext(),
            api = get(),
            scheduleDao = get(),
            lessonNoteDao = get()
        )
    }

    single<LessonNoteRepository> {
        LessonNoteRepositoryImpl(
            lessonNoteDao = get()
        )
    }

    // Notifications & Alarms
    single { ScheduleNotificationManager(androidContext()) }
    single { LessonAlarmScheduler(androidContext()) }

    // Multiplatform ViewModels
    viewModel {
        ScheduleViewModel(
            scheduleRepository = get(),
            lessonNoteRepository = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            settings = get()
        )
    }

    viewModel {
        CompareViewModel(
            settings = get(),
            api = get(),
            repository = get()
        )
    }

    viewModel {
        FreeClassroomsViewModel(
            api = get(),
            repository = get()
        )
    }

    viewModel {
        SettingsViewModel(
            api = get(),
            alarmScheduler = get(),
            repository = get(),
            lessonNoteRepository = get(),
            widgetUpdater = get(),
            iconManager = get(),
            syncScheduler = get(),
            settings = get()
        )
    }

    viewModel {
        OnboardingViewModel(
            scheduleRepository = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            settings = get()
        )
    }

    viewModel {
        AppUpdateViewModel(
            api = get(),
            settings = get()
        )
    }
}
