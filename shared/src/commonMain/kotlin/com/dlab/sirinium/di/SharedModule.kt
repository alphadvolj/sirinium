package com.dlab.sirinium.di

import com.dlab.sirinium.data.remote.api.KtorSiriusScheduleApi
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.remote.api.createConfiguredHttpClient
import com.dlab.sirinium.data.repository.MultiplatformLessonNoteRepository
import com.dlab.sirinium.data.repository.MultiplatformScheduleRepository
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.platform.NoOpAlarmScheduler
import com.dlab.sirinium.platform.NoOpIconManager
import com.dlab.sirinium.platform.NoOpSyncScheduler
import com.dlab.sirinium.platform.NoOpWidgetUpdater
import com.dlab.sirinium.platform.PlatformActions
import com.dlab.sirinium.platform.PlatformAlarmScheduler
import com.dlab.sirinium.platform.PlatformIconManager
import com.dlab.sirinium.platform.PlatformSettings
import com.dlab.sirinium.platform.PlatformSyncScheduler
import com.dlab.sirinium.platform.PlatformWidgetUpdater
import com.dlab.sirinium.platform.createPlatformActions
import com.dlab.sirinium.platform.createPlatformAlarmScheduler
import com.dlab.sirinium.platform.createPlatformSettings
import com.dlab.sirinium.platform.createPlatformWidgetUpdater
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.onboarding.OnboardingViewModel
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsViewModel
import com.dlab.sirinium.ui.update.AppUpdateViewModel
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> { createConfiguredHttpClient() }
    single<PlatformSettings> { createPlatformSettings() }
    single<PlatformActions> { createPlatformActions() }
    single<PlatformAlarmScheduler> { createPlatformAlarmScheduler() }
    single<PlatformIconManager> { NoOpIconManager() }
    single<PlatformSyncScheduler> { NoOpSyncScheduler() }
    single<SiriusScheduleApi> { KtorSiriusScheduleApi(get()) }
    single<LessonNoteRepository> { MultiplatformLessonNoteRepository() }
    single<ScheduleRepository> {
        MultiplatformScheduleRepository(
            api = get(),
            settings = get(),
            noteRepository = get()
        )
    }
    single<PlatformWidgetUpdater> { createPlatformWidgetUpdater(get(), get()) }

    factory {
        ScheduleViewModel(
            scheduleRepository = get(),
            lessonNoteRepository = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            settings = get()
        )
    }
    factory {
        CompareViewModel(
            settings = get(),
            api = get(),
            repository = get()
        )
    }
    factory {
        FreeClassroomsViewModel(
            api = get(),
            repository = get()
        )
    }
    factory {
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
    factory {
        OnboardingViewModel(
            scheduleRepository = get(),
            alarmScheduler = get(),
            widgetUpdater = get(),
            settings = get()
        )
    }
    factory {
        AppUpdateViewModel(
            api = get(),
            settings = get()
        )
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(sharedModule)
    }
