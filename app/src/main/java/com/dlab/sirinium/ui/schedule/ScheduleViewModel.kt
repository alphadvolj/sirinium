package com.dlab.sirinium.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlab.sirinium.alarm.LessonAlarmScheduler
import com.dlab.sirinium.core.model.Resource
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonMarkerInfo
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.domain.model.ScheduleFilter
import com.dlab.sirinium.domain.repository.LessonNoteRepository
import com.dlab.sirinium.domain.repository.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.net.NetworkCapabilities
import android.net.Network
import android.net.NetworkRequest
import com.dlab.sirinium.sync.ScheduleSyncWorker
import com.dlab.sirinium.widget.ScheduleGlanceWidget
import com.dlab.sirinium.widget.ScheduleListGlanceWidget
import com.dlab.sirinium.widget.WidgetUpdateHelper
import com.dlab.sirinium.widget.WidgetStateHelper
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val lessonNoteRepository: LessonNoteRepository,
    private val alarmScheduler: LessonAlarmScheduler,
    private val context: Context
) : ViewModel() {

    companion object {
        const val KEY_FAVORITE_TARGETS = "key_favorite_targets_set"
    }

    private val prefs = context.getSharedPreferences(ScheduleSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)

    private val initialTarget = prefs.getString(ScheduleSyncWorker.KEY_CURRENT_TARGET, "")
        ?: ""
    private val initialSection = prefs.getString(ScheduleSyncWorker.KEY_CURRENT_SECTION, "group")
        ?: "group"

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            filter = ScheduleFilter(
                target = initialTarget,
                sectionType = initialSection
            )
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val filterStateFlow = MutableStateFlow(_uiState.value.filter)
    private var notesJob: Job? = null
    private var tasksJob: Job? = null
    private var recommendedNotesJob: Job? = null
    private var extraGroupJob: Job? = null
    private val autoLoadJobs = java.util.concurrent.ConcurrentHashMap<Int, Job>()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        _uiState.update { it.copy(favoriteTargets = loadFavorites()) }
        observeSchedule()
        observeNetworkConnectivity()
        observeLastUpdateTime()
        observeLessonMarkers()
        refreshAvailableSelectors()
        checkAndAutoLoadWeek(_uiState.value.filter.selectedDate)
    }

    private fun checkIsOnline(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun observeNetworkConnectivity() {
        val cm = connectivityManager
        _uiState.update { it.copy(isOffline = !checkIsOnline()) }
        if (cm == null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _uiState.update { it.copy(isOffline = false) }
            }

            override fun onLost(network: Network) {
                _uiState.update { it.copy(isOffline = !checkIsOnline()) }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _uiState.update { it.copy(isOffline = !hasInternet) }
            }
        }
        networkCallback = callback
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            android.util.Log.w("ScheduleViewModel", "Could not register network callback", e)
        }
    }

    private fun observeLastUpdateTime() {
        filterStateFlow
            .map { it.target }
            .distinctUntilChanged()
            .flatMapLatest { target ->
                if (target.isBlank()) kotlinx.coroutines.flow.flowOf(0L)
                else scheduleRepository.observeLastUpdateTime(target)
            }
            .onEach { timestamp ->
                _uiState.update { it.copy(lastUpdateTime = timestamp) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLessonMarkers() {
        combine(
            lessonNoteRepository.getAllNotes(),
            lessonNoteRepository.getAllTasks()
        ) { notes, tasks ->
            val markersMap = mutableMapOf<String, LessonMarkerInfo>()

            val notesByLesson = notes.filter { it.lessonId.isNotBlank() }.groupBy { it.lessonId }
            val tasksByLesson = tasks.filter { it.lessonId.isNotBlank() }.groupBy { it.lessonId }

            val allLessonIds = (notesByLesson.keys + tasksByLesson.keys)
            for (lessonId in allLessonIds) {
                val lessonNotes = notesByLesson[lessonId].orEmpty()
                val lessonTasks = tasksByLesson[lessonId].orEmpty()
                val completedTasks = lessonTasks.count { it.isDone }
                markersMap[lessonId] = LessonMarkerInfo(
                    notesCount = lessonNotes.size,
                    totalTasksCount = lessonTasks.size,
                    completedTasksCount = completedTasks
                )
            }
            markersMap
        }
        .onEach { markers ->
            _uiState.update { it.copy(lessonMarkers = markers) }
        }
        .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
    }

    private fun observeSchedule() {
        filterStateFlow
            .map { Pair(it.target, it.sectionType) }
            .distinctUntilChanged()
            .flatMapLatest { (target, sectionType) ->
                if (target.isBlank()) {
                    kotlinx.coroutines.flow.flowOf(Resource.Success(emptyList()))
                } else {
                    scheduleRepository.getSchedule(ScheduleFilter(target = target, sectionType = sectionType))
                }
            }
            .onEach { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                allLessons = resource.data,
                                errorMessage = null
                            )
                        }

                        // Reschedule alarms for current target only if enabled
                        val settingsPrefs = context.getSharedPreferences("sirinium_settings", Context.MODE_PRIVATE)
                        val notifyEnabled = settingsPrefs.getBoolean("notify_lessons_enabled", true)
                        val defaultMins = settingsPrefs.getInt("default_alarm_mins", 15)
                        if (notifyEnabled) {
                            alarmScheduler.rescheduleAlarmsForLessons(resource.data, defaultMins)
                        } else {
                            alarmScheduler.cancelAllAlarms()
                        }

                        // Keep widgets updated with latest lessons
                        viewModelScope.launch {
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun checkAndAutoLoadWeek(dateStr: String) {
        val currentFilter = _uiState.value.filter
        val target = currentFilter.target
        val sectionType = currentFilter.sectionType
        if (target.isBlank()) return

        val date = DateTimeUtils.parseDate(dateStr) ?: return
        val weekOffset = DateTimeUtils.calculateWeekOffset(date)

        if (scheduleRepository.isWeekLoaded(target, weekOffset)) {
            return
        }

        if (autoLoadJobs[weekOffset]?.isActive == true) {
            return
        }

        val job = viewModelScope.launch {
            _uiState.update { it.copy(loadingWeekOffsets = it.loadingWeekOffsets + weekOffset) }
            try {
                scheduleRepository.loadWeekSchedule(target, sectionType, weekOffset)
                WidgetUpdateHelper.updateAllWidgets(context)
            } catch (e: Exception) {
                android.util.Log.e("ScheduleViewModel", "Failed to auto-load week $weekOffset", e)
            } finally {
                _uiState.update { it.copy(loadingWeekOffsets = it.loadingWeekOffsets - weekOffset) }
                autoLoadJobs.remove(weekOffset)
            }
        }
        autoLoadJobs[weekOffset] = job
    }

    fun refreshAvailableSelectors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSelectors = true) }
            try {
                coroutineScope {
                    launch {
                        when (val res = scheduleRepository.getGroups()) {
                            is Resource.Success -> _uiState.update { it.copy(availableGroups = res.data) }
                            else -> {}
                        }
                    }
                    launch {
                        when (val res = scheduleRepository.getTeachers()) {
                            is Resource.Success -> _uiState.update { it.copy(availableTeachers = res.data) }
                            else -> {}
                        }
                    }
                    launch {
                        when (val res = scheduleRepository.getClassrooms()) {
                            is Resource.Success -> _uiState.update { it.copy(availableClassrooms = res.data) }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScheduleViewModel", "Failed refreshing selectors", e)
            } finally {
                _uiState.update { it.copy(isRefreshingSelectors = false) }
            }
        }
    }

    fun onIntent(intent: ScheduleUiIntent) {
        when (intent) {
            is ScheduleUiIntent.ChangeDate -> {
                updateFilter { it.copy(selectedDate = intent.date) }
                checkAndAutoLoadWeek(intent.date)
            }
            is ScheduleUiIntent.ChangeTarget -> {
                // Cancel pending auto-load jobs for previous target
                autoLoadJobs.values.forEach { it.cancel() }
                autoLoadJobs.clear()
                _uiState.update { it.copy(loadingWeekOffsets = emptySet()) }

                // Cancel all alarms from previous target immediately
                alarmScheduler.cancelAllAlarms()

                // Synchronously commit target & section for WorkManager and Glance widgets
                prefs.edit()
                    .putString(ScheduleSyncWorker.KEY_CURRENT_TARGET, intent.target)
                    .putString(ScheduleSyncWorker.KEY_CURRENT_SECTION, intent.sectionType)
                    .commit()

                // Notify in-memory flow so active widget sessions update immediately
                WidgetStateHelper.updateCurrentTarget(intent.target, intent.sectionType)

                updateFilter { it.copy(target = intent.target, sectionType = intent.sectionType) }
                checkAndAutoLoadWeek(_uiState.value.filter.selectedDate)

                // Update widgets immediately
                viewModelScope.launch {
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            is ScheduleUiIntent.AddFavoriteTarget -> addFavorite(intent.target, intent.sectionType)
            is ScheduleUiIntent.RemoveFavoriteTarget -> removeFavorite(intent.target, intent.sectionType)
            is ScheduleUiIntent.ToggleFavoriteTarget -> toggleFavorite(intent.target, intent.sectionType)
            is ScheduleUiIntent.SetSearchQuery -> updateFilter { it.copy(searchQuery = intent.query) }
            ScheduleUiIntent.Refresh -> refreshData()
            ScheduleUiIntent.RefreshSelectors -> refreshAvailableSelectors()
            is ScheduleUiIntent.SelectLessonForDetails -> selectLesson(intent.lesson)
            is ScheduleUiIntent.ScheduleAlarmForLesson -> scheduleAlarm(intent.lesson, intent.minutesBefore)
            is ScheduleUiIntent.CancelAlarmForLesson -> cancelAlarm(intent.lesson)
            is ScheduleUiIntent.SaveNote -> saveNote(intent.title, intent.content, intent.colorHex)
            is ScheduleUiIntent.DeleteNote -> deleteNote(intent.noteId)
            is ScheduleUiIntent.AddHomeworkTask -> addHomeworkTask(intent.text)
            is ScheduleUiIntent.ToggleHomeworkTask -> toggleHomeworkTask(intent.taskId, intent.isDone)
            is ScheduleUiIntent.DeleteHomeworkTask -> deleteHomeworkTask(intent.taskId)
            ScheduleUiIntent.ReloadFavorites -> reloadFavorites()
            ScheduleUiIntent.DismissUserMessage -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun updateFilter(transform: (ScheduleFilter) -> ScheduleFilter) {
        val newFilter = transform(_uiState.value.filter)
        _uiState.update { it.copy(filter = newFilter) }
        filterStateFlow.value = newFilter
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val currentFilter = _uiState.value.filter
            val result = scheduleRepository.refreshSchedule(
                target = currentFilter.target,
                sectionType = currentFilter.sectionType,
                date = currentFilter.selectedDate.ifBlank { null }
            )
            if (result is Resource.Error) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        userMessage = "Не удалось обновить: ${result.message}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        userMessage = "Расписание успешно синхронизировано"
                    )
                }
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }

    private fun selectLesson(lesson: Lesson?) {
        _uiState.update { it.copy(selectedLessonForDetails = lesson) }
        notesJob?.cancel()
        tasksJob?.cancel()
        recommendedNotesJob?.cancel()
        extraGroupJob?.cancel()

        if (lesson != null) {
            preloadNextWeeksForLesson(lesson)

            // If viewing teacher or classroom schedule, load lessons for the lesson's group
            val groupTarget = lesson.group.trim()
            if (groupTarget.isNotBlank() && _uiState.value.filter.sectionType != "group") {
                extraGroupJob = viewModelScope.launch {
                    try {
                        val groupLessons = scheduleRepository.getLessonsForGroup(groupTarget)
                        _uiState.update { it.copy(extraGroupLessons = groupLessons) }
                    } catch (e: Exception) {
                        Log.w("ScheduleViewModel", "Failed to fetch group lessons for $groupTarget", e)
                    }
                }
            } else {
                _uiState.update { it.copy(extraGroupLessons = emptyList()) }
            }

            if (lesson.sectionType == "classroom") {
                // Classrooms view has no notes or homework tasks
                _uiState.update {
                    it.copy(
                        selectedLessonNotes = emptyList(),
                        recommendedLessonNotes = emptyList(),
                        selectedLessonTasks = emptyList()
                    )
                }
                return
            }

            notesJob = lessonNoteRepository.getNotesForLesson(lesson)
                .onEach { notes ->
                    _uiState.update { it.copy(selectedLessonNotes = notes) }
                }
                .launchIn(viewModelScope)

            _uiState.update { it.copy(recommendedLessonNotes = emptyList()) }

            tasksJob = lessonNoteRepository.getTasksForLesson(lesson.id)
                .onEach { tasks ->
                    _uiState.update { it.copy(selectedLessonTasks = tasks) }
                }
                .launchIn(viewModelScope)
        } else {
            _uiState.update {
                it.copy(
                    selectedLessonNotes = emptyList(),
                    recommendedLessonNotes = emptyList(),
                    selectedLessonTasks = emptyList(),
                    extraGroupLessons = emptyList()
                )
            }
        }
    }

    private fun preloadNextWeeksForLesson(lesson: Lesson) {
        val date = DateTimeUtils.parseDate(lesson.date) ?: return
        val currentWeek = DateTimeUtils.calculateWeekOffset(date)
        val currentTarget = _uiState.value.filter.target
        val currentSection = _uiState.value.filter.sectionType
        val groupTarget = lesson.group.trim()

        viewModelScope.launch {
            for (offset in currentWeek..(currentWeek + 3)) {
                // Ensure target schedule is loaded for next 3 weeks
                if (currentTarget.isNotBlank() && !scheduleRepository.isWeekLoaded(currentTarget, offset)) {
                    try {
                        scheduleRepository.loadWeekSchedule(currentTarget, currentSection, offset)
                    } catch (e: Exception) {
                        Log.w("ScheduleViewModel", "Failed to preload week $offset for $currentTarget", e)
                    }
                }
                // If viewing teacher/classroom, also preload the group's schedule for next 3 weeks
                if (groupTarget.isNotBlank() && (currentSection != "group" || currentTarget != groupTarget)) {
                    if (!scheduleRepository.isWeekLoaded(groupTarget, offset)) {
                        try {
                            scheduleRepository.loadWeekSchedule(groupTarget, "group", offset)
                        } catch (e: Exception) {
                            Log.w("ScheduleViewModel", "Failed to preload group week $offset for $groupTarget", e)
                        }
                    }
                }
            }

            // After preloading, refresh extra group lessons if needed
            if (groupTarget.isNotBlank() && currentSection != "group") {
                try {
                    val groupLessons = scheduleRepository.getLessonsForGroup(groupTarget)
                    _uiState.update { it.copy(extraGroupLessons = groupLessons) }
                } catch (_: Exception) {}
            }
        }
    }

    private fun scheduleAlarm(lesson: Lesson, minutesBefore: Int) {
        alarmScheduler.scheduleAlarm(lesson, minutesBefore)
        _uiState.update {
            it.copy(userMessage = "Напоминание установлено за $minutesBefore минут до начала")
        }
    }

    private fun cancelAlarm(lesson: Lesson) {
        alarmScheduler.disableAlarmForLesson(lesson.id)
        _uiState.update {
            it.copy(userMessage = "Уведомление для пары отключено")
        }
    }

    private fun saveNote(title: String, content: String, colorHex: Long) {
        val lesson = _uiState.value.selectedLessonForDetails ?: return
        if (lesson.sectionType == "classroom") return
        val noteScope = if (lesson.sectionType == "teacher") "teacher" else "student"
        viewModelScope.launch {
            lessonNoteRepository.saveNote(
                LessonNote(
                    lessonId = lesson.id,
                    scope = noteScope,
                    groupName = lesson.group.trim(),
                    target = lesson.target.trim(),
                    discipline = lesson.discipline.trim(),
                    lessonType = lesson.lessonType.title,
                    lessonDate = lesson.date,
                    classroom = lesson.classroom,
                    title = title,
                    content = content,
                    colorHex = colorHex
                )
            )
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            lessonNoteRepository.deleteNote(noteId)
        }
    }

    private fun addHomeworkTask(text: String) {
        val lesson = _uiState.value.selectedLessonForDetails ?: return
        if (lesson.sectionType == "classroom") return
        viewModelScope.launch {
            lessonNoteRepository.saveTask(
                HomeworkTask(
                    lessonId = lesson.id,
                    text = text
                )
            )
        }
    }

    private fun toggleHomeworkTask(taskId: Long, isDone: Boolean) {
        viewModelScope.launch {
            lessonNoteRepository.toggleTaskCompletion(taskId, isDone)
        }
    }

    private fun deleteHomeworkTask(taskId: Long) {
        viewModelScope.launch {
            lessonNoteRepository.deleteTask(taskId)
        }
    }

    fun reloadFavorites() {
        _uiState.update { it.copy(favoriteTargets = loadFavorites()) }
    }

    private fun loadFavorites(): List<FavoriteTarget> {
        val rawSet = prefs.getStringSet(KEY_FAVORITE_TARGETS, null)
        return if (rawSet.isNullOrEmpty()) {
            emptyList()
        } else {
            rawSet.mapNotNull { FavoriteTarget.fromSerializedString(it) }
        }
    }

    private fun saveFavorites(list: List<FavoriteTarget>) {
        val serializedSet = list.map { it.toSerializedString() }.toSet()
        prefs.edit().putStringSet(KEY_FAVORITE_TARGETS, serializedSet).apply()
    }

    private fun addFavorite(target: String, sectionType: String) {
        val current = loadFavorites().toMutableList()
        val exists = current.any { it.target.equals(target, ignoreCase = true) && it.sectionType == sectionType }
        if (!exists) {
            current.add(FavoriteTarget(target = target, sectionType = sectionType))
            saveFavorites(current)
            _uiState.update { it.copy(favoriteTargets = current, userMessage = "Добавлено в избранное: $target") }
        }
    }

    private fun removeFavorite(target: String, sectionType: String) {
        val current = loadFavorites().filterNot {
            it.target.equals(target, ignoreCase = true) && it.sectionType == sectionType
        }
        saveFavorites(current)
        _uiState.update { it.copy(favoriteTargets = current, userMessage = "Удалено из избранного: $target") }
    }

    private fun toggleFavorite(target: String, sectionType: String) {
        val current = loadFavorites()
        val exists = current.any {
            it.target.equals(target, ignoreCase = true) && it.sectionType == sectionType
        }
        if (exists) {
            removeFavorite(target, sectionType)
        } else {
            addFavorite(target, sectionType)
        }
    }
}
