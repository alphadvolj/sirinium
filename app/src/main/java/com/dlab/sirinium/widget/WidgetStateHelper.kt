package com.dlab.sirinium.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.sync.ScheduleSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CompactWidgetState(
    val target: String,
    val sectionType: String,
    val nextLesson: Lesson?
)

data class ListWidgetState(
    val target: String,
    val sectionType: String,
    val formattedDate: String,
    val lessons: List<Lesson>
)

object WidgetStateHelper {
    private const val TAG = "WidgetStateHelper"

    // In-memory state flow to instantly notify active widget sessions within the same process
    private val inMemoryTarget = MutableStateFlow<Pair<String, String>?>(null)

    fun updateCurrentTarget(target: String, sectionType: String) {
        inMemoryTarget.value = Pair(target, sectionType)
    }

    private fun getTargetConfigFlow(context: Context): Flow<Pair<String, String>> = callbackFlow {
        val prefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)

        fun readCurrent(): Pair<String, String> {
            val target = prefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, ScheduleSyncWorker.DEFAULT_TARGET)
                ?: ScheduleSyncWorker.DEFAULT_TARGET
            val section = prefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, ScheduleSyncWorker.DEFAULT_SECTION)
                ?: ScheduleSyncWorker.DEFAULT_SECTION
            return Pair(target, section)
        }

        trySend(readCurrent())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ScheduleSyncWorker.KEY_CURRENT_TARGET || key == ScheduleSyncWorker.KEY_CURRENT_SECTION) {
                trySend(readCurrent())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun observeTargetConfig(context: Context): Flow<Pair<String, String>> {
        return combine(
            getTargetConfigFlow(context),
            inMemoryTarget
        ) { prefConfig, memConfig ->
            memConfig ?: prefConfig
        }.distinctUntilChanged()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCompactWidgetState(
        context: Context,
        database: SiriniumDatabase,
        scheduleRepository: ScheduleRepository
    ): Flow<CompactWidgetState> {
        return observeTargetConfig(context)
            .flatMapLatest { (target, sectionType) ->
                database.scheduleDao().getAllScheduleForTarget(target)
                    .transform { entities ->
                        if (entities.isEmpty() && target.isNotBlank() && !scheduleRepository.isWeekLoaded(target, 0)) {
                            try {
                                scheduleRepository.loadWeekSchedule(target, sectionType, 0)
                            } catch (e: Exception) {
                                Log.e(TAG, "On-demand load failed for target=", e)
                            }
                        }

                        val todayDate = LocalDate.now()
                        val timeNow = LocalTime.now()

                        val parsedEntities = entities.mapNotNull { entity ->
                            val d = DateTimeUtils.parseDate(entity.date) ?: return@mapNotNull null
                            val endT = try { LocalTime.parse(entity.endTime) } catch (_: Exception) { null }
                            val startT = try { LocalTime.parse(entity.startTime) } catch (_: Exception) { null }
                            Triple(entity, d, endT ?: startT ?: LocalTime.MAX)
                        }.sortedWith(
                            compareBy<Triple<com.dlab.sirinium.data.local.entity.ScheduleEntity, LocalDate, LocalTime>> { it.second }
                                .thenBy { it.third }
                        )

                        val nextEntity = parsedEntities.firstOrNull { (_, date, endTime) ->
                            date.isAfter(todayDate) || (date.isEqual(todayDate) && !endTime.isBefore(timeNow))
                        }?.first

                        emit(
                            CompactWidgetState(
                                target = target,
                                sectionType = sectionType,
                                nextLesson = nextEntity?.toDomain()
                            )
                        )
                    }
            }
            .flowOn(Dispatchers.IO)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeListWidgetState(
        context: Context,
        database: SiriniumDatabase,
        scheduleRepository: ScheduleRepository
    ): Flow<ListWidgetState> {
        return observeTargetConfig(context)
            .flatMapLatest { (target, sectionType) ->
                database.scheduleDao().getAllScheduleForTarget(target)
                    .transform { entities ->
                        if (entities.isEmpty() && target.isNotBlank() && !scheduleRepository.isWeekLoaded(target, 0)) {
                            try {
                                scheduleRepository.loadWeekSchedule(target, sectionType, 0)
                            } catch (e: Exception) {
                                Log.e(TAG, "On-demand load failed for target=", e)
                            }
                        }

                        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
                        val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru")))
                        val todayLessons = entities.filter { it.date == today }
                            .sortedBy { it.startTime }
                            .map { it.toDomain() }

                        emit(
                            ListWidgetState(
                                target = target,
                                sectionType = sectionType,
                                formattedDate = formattedDate,
                                lessons = todayLessons
                            )
                        )
                    }
            }
            .flowOn(Dispatchers.IO)
    }
}
