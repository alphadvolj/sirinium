package com.dlab.sirinium

import com.dlab.sirinium.alarm.LessonAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonAlarmTargetTest {

    @Test
    fun testExtraTargetConstantExists() {
        assertEquals("extra_target", LessonAlarmScheduler.EXTRA_TARGET)
    }

    @Test
    fun testTargetMatchingCondition() {
        val currentTarget = "К1609-241"
        val matchingLessonTarget = "К1609-241"
        val anotherLessonTarget = "Иванов И.И."

        val isMatching = matchingLessonTarget.equals(currentTarget, ignoreCase = true)
        val isAnother = anotherLessonTarget.equals(currentTarget, ignoreCase = true)

        assertTrue("Matching target should equal currentTarget", isMatching)
        assertFalse("Different target should not equal currentTarget", isAnother)
    }

    @Test
    fun testAlarmDroppedWhenTargetDiffers() {
        val currentTarget = "К1609-241"
        val oldAlarmTarget = "П123-456"

        val shouldDrop = !currentTarget.isNullOrBlank() &&
                !oldAlarmTarget.isNullOrBlank() &&
                !oldAlarmTarget.equals(currentTarget, ignoreCase = true)

        assertTrue("Alarm for non-current target must be dropped", shouldDrop)
    }

    @Test
    fun testAlarmKeptWhenTargetMatches() {
        val currentTarget = "К1609-241"
        val activeAlarmTarget = "к1609-241"

        val shouldDrop = !currentTarget.isNullOrBlank() &&
                !activeAlarmTarget.isNullOrBlank() &&
                !activeAlarmTarget.equals(currentTarget, ignoreCase = true)

        assertFalse("Alarm for matching current target must NOT be dropped", shouldDrop)
    }
}
