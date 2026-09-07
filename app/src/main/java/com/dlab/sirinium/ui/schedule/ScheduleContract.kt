package com.dlab.sirinium.ui.schedule

import com.dlab.sirinium.domain.model.FavoriteTarget
import com.dlab.sirinium.domain.model.HomeworkTask
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonMarkerInfo
import com.dlab.sirinium.domain.model.LessonNote
import com.dlab.sirinium.domain.model.ScheduleFilter

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isRefreshingSelectors: Boolean = false,
    val loadingWeekOffsets: Set<Int> = emptySet(),
    val filter: ScheduleFilter = ScheduleFilter(),
    val allLessons: List<Lesson> = emptyList(),
    val availableGroups: List<String> = emptyList(),
    val availableTeachers: List<String> = emptyList(),
    val availableClassrooms: List<String> = emptyList(),
    val favoriteTargets: List<FavoriteTarget> = emptyList(),
    val selectedLessonForDetails: Lesson? = null,
    val selectedLessonNotes: List<LessonNote> = emptyList(),
    val recommendedLessonNotes: List<LessonNote> = emptyList(),
    val selectedLessonTasks: List<HomeworkTask> = emptyList(),
    val lessonMarkers: Map<String, LessonMarkerInfo> = emptyMap(),
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val isOffline: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val extraGroupLessons: List<Lesson> = emptyList()
) {
    /**
     * Lessons for currently selected date
     */
    val lessons: List<Lesson>
        get() = lessonsForDate(filter.selectedDate)

    /**
     * Lessons for a specific date with optional search query applied
     */
    fun lessonsForDate(dateStr: String): List<Lesson> {
        val query = filter.searchQuery.trim()
        return allLessons.filter { lesson ->
            val matchesDate = dateStr.isBlank() || lesson.date == dateStr
            val matchesQuery = query.isBlank() ||
                    lesson.discipline.contains(query, ignoreCase = true) ||
                    lesson.teacher.contains(query, ignoreCase = true) ||
                    lesson.classroom.contains(query, ignoreCase = true)
            matchesDate && matchesQuery
        }
    }
}

sealed interface ScheduleUiIntent {
    data class ChangeDate(val date: String) : ScheduleUiIntent
    data class ChangeTarget(val target: String, val sectionType: String = "group") : ScheduleUiIntent
    data class AddFavoriteTarget(val target: String, val sectionType: String) : ScheduleUiIntent
    data class RemoveFavoriteTarget(val target: String, val sectionType: String) : ScheduleUiIntent
    data class ToggleFavoriteTarget(val target: String, val sectionType: String) : ScheduleUiIntent
    data class SetSearchQuery(val query: String) : ScheduleUiIntent
    data object Refresh : ScheduleUiIntent
    data object RefreshSelectors : ScheduleUiIntent
    data class SelectLessonForDetails(val lesson: Lesson?) : ScheduleUiIntent
    data class ScheduleAlarmForLesson(val lesson: Lesson, val minutesBefore: Int = 15) : ScheduleUiIntent
    data class CancelAlarmForLesson(val lesson: Lesson) : ScheduleUiIntent
    data class SaveNote(val title: String, val content: String, val colorHex: Long) : ScheduleUiIntent
    data class DeleteNote(val noteId: Long) : ScheduleUiIntent
    data class AddHomeworkTask(val text: String) : ScheduleUiIntent
    data class ToggleHomeworkTask(val taskId: Long, val isDone: Boolean) : ScheduleUiIntent
    data class DeleteHomeworkTask(val taskId: Long) : ScheduleUiIntent
    data object ReloadFavorites : ScheduleUiIntent
    data object DismissUserMessage : ScheduleUiIntent
}
