package com.dlab.sirinium.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

enum class TutorialTargetKey {
    SCHEDULE_DATE_ROW,    // Полоса выбора дат в расписании
    SCHEDULE_SWIPE_AREA,  // Область расписания со свайпом
    FIRST_LESSON_CARD,    // Карточка первой пары (или hero card)
    COMPARE_HEADER,       // Блок сравнения сущностей на странице «Окна»
    CLASSROOMS_PAIRS,     // Полоса выбора пар на странице аудиторий
    BOTTOM_NAV_BAR        // Капсула нижней панели навигации
}

val LocalTutorialBoundsRecorder = compositionLocalOf<(TutorialTargetKey, Rect) -> Unit> {
    { _, _ -> }
}

fun Modifier.tutorialTarget(key: TutorialTargetKey): Modifier = composed {
    val recorder = LocalTutorialBoundsRecorder.current
    this.onGloballyPositioned { coords ->
        if (coords.isAttached) {
            val bounds = coords.boundsInRoot()
            if (bounds.width > 0 && bounds.height > 0) {
                recorder(key, bounds)
            }
        }
    }
}