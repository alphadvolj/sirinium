package com.dlab.sirinium.di

import android.annotation.SuppressLint
import androidx.room.Room
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.data.remote.api.SiriusScheduleApi
import com.dlab.sirinium.data.repository.LessonNoteRepositoryImpl
import com.dlab.sirinium.data.repository.ScheduleRepositoryImpl
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.notification.ScheduleNotificationManager
import com.dlab.sirinium.ui.classrooms.FreeClassroomsViewModel
import com.dlab.sirinium.ui.compare.CompareViewModel
import com.dlab.sirinium.ui.onboarding.OnboardingViewModel
import com.dlab.sirinium.ui.schedule.ScheduleViewModel
import com.dlab.sirinium.ui.settings.SettingsViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val appModule = module {

    // Network & Retrofit
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            prettyPrint = false
        }
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Trust manager tolerant to time skew (CertificateNotYetValidException) and custom certificates
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        val json: Json = get()
        val client: OkHttpClient = get()
        Retrofit.Builder()
            .baseUrl("https://sirinium.avh-vless.work/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiriusScheduleApi::class.java)
    }

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

    // Repositories
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

    // ViewModels
    viewModel {
        ScheduleViewModel(
            scheduleRepository = get(),
            lessonNoteRepository = get(),
            alarmScheduler = get(),
            context = androidContext()
        )
    }

    viewModel {
        CompareViewModel(
            context = androidContext(),
            api = get(),
            repository = get()
        )
    }

    viewModel {
        FreeClassroomsViewModel(
            api = get(),
            repository = get(),
            scheduleDao = get()
        )
    }

    viewModel {
        SettingsViewModel(
            context = androidContext(),
            api = get(),
            alarmScheduler = get(),
            repository = get(),
            lessonNoteRepository = get()
        )
    }

    viewModel {
        OnboardingViewModel(
            context = androidContext(),
            scheduleRepository = get(),
            alarmScheduler = get()
        )
    }

    viewModel {
        com.dlab.sirinium.ui.update.AppUpdateViewModel(
            api = get(),
            context = androidContext()
        )
    }
}
