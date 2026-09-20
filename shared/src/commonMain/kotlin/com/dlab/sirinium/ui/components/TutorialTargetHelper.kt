package com.dlab.sirinium.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

enum class TutorialTargetKey {
    SCHEDULE_DATE_ROW,
    SCHEDULE_SWIPE_AREA,
    FIRST_LESSON_CARD,
    COMPARE_HEADER,
    CLASSROOMS_PAIRS,
    BOTTOM_NAV_BAR
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
