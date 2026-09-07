package com.dlab.sirinium

import com.dlab.sirinium.core.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateTimeUtilsTest {

    @Test
    fun testGetWeekMonday() {
        val wednesday = LocalDate.of(2026, 9, 2)
        val monday = DateTimeUtils.getWeekMonday(wednesday)
        assertEquals(LocalDate.of(2026, 8, 31), monday)

        val sunday = LocalDate.of(2026, 9, 6)
        val mondayFromSunday = DateTimeUtils.getWeekMonday(sunday)
        assertEquals(LocalDate.of(2026, 8, 31), mondayFromSunday)

        val mondayItself = LocalDate.of(2026, 8, 31)
        assertEquals(mondayItself, DateTimeUtils.getWeekMonday(mondayItself))
    }

    @Test
    fun testCalculateWeekOffset() {
        val baseDate = LocalDate.of(2026, 9, 5) // Saturday in week 2026-08-31..2026-09-06

        assertEquals(0, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 8, 31), baseDate))
        assertEquals(0, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 2), baseDate))
        assertEquals(0, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 6), baseDate))

        assertEquals(1, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 7), baseDate))
        assertEquals(2, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 14), baseDate))
        assertEquals(3, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 21), baseDate))

        assertEquals(-1, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 8, 24), baseDate))
        assertEquals(-2, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 8, 17), baseDate))
        assertEquals(-3, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 8, 10), baseDate))

        assertEquals(4, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 9, 28), baseDate))
        assertEquals(-4, DateTimeUtils.calculateWeekOffset(LocalDate.of(2026, 8, 3), baseDate))
    }

    @Test
    fun testGetWeekDatesForOffset() {
        val baseDate = LocalDate.of(2026, 9, 5)

        val currentWeekDates = DateTimeUtils.getWeekDatesForOffset(0, baseDate)
        assertEquals(7, currentWeekDates.size)
        assertEquals("31.08.2026", currentWeekDates[0])
        assertEquals("06.09.2026", currentWeekDates[6])

        val nextWeekDates = DateTimeUtils.getWeekDatesForOffset(1, baseDate)
        assertEquals(7, nextWeekDates.size)
        assertEquals("07.09.2026", nextWeekDates[0])
        assertEquals("13.09.2026", nextWeekDates[6])

        val prevWeekDates = DateTimeUtils.getWeekDatesForOffset(-1, baseDate)
        assertEquals(7, prevWeekDates.size)
        assertEquals("24.08.2026", prevWeekDates[0])
        assertEquals("30.08.2026", prevWeekDates[6])
    }

    @Test
    fun testStandardPairsDefinition() {
        assertEquals(8, DateTimeUtils.STANDARD_PAIRS.size)
        assertEquals("08:45 – 10:05", DateTimeUtils.STANDARD_PAIRS[0].timeSlot)
        assertEquals("10:20 – 11:40", DateTimeUtils.STANDARD_PAIRS[1].timeSlot)
        assertEquals("11:55 – 13:15", DateTimeUtils.STANDARD_PAIRS[2].timeSlot)
        assertEquals("13:30 – 14:50", DateTimeUtils.STANDARD_PAIRS[3].timeSlot)
        assertEquals("15:05 – 16:25", DateTimeUtils.STANDARD_PAIRS[4].timeSlot)
        assertEquals("16:40 – 18:00", DateTimeUtils.STANDARD_PAIRS[5].timeSlot)
        assertEquals("18:15 – 19:35", DateTimeUtils.STANDARD_PAIRS[6].timeSlot)
        assertEquals("19:50 – 21:10", DateTimeUtils.STANDARD_PAIRS[7].timeSlot)

        assertEquals("09:25 – 10:45", DateTimeUtils.PE_PAIR_1.timeSlot)
    }

    @Test
    fun testIsTimeAnomaly() {
        // Standard pairs should NOT be anomalies
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("08:45", "10:05"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("8:45", "10:05"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("09:25", "10:45")) // PE 1st pair
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("9:25", "10:45"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("10:20", "11:40"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("11:55", "13:15"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("13:30", "14:50"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("15:05", "16:25"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("16:40", "18:00"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("18:15", "19:35"))
        org.junit.Assert.assertFalse(DateTimeUtils.isTimeAnomaly("19:50", "21:10"))

        // Irregular times MUST be anomalies
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("13:30", "14:45")) // old 14:45 is now anomaly
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("09:00", "10:30")) // old schedule
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("10:45", "12:15")) // old schedule
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("12:00", "13:30"))
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("14:00", "15:00"))
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly(null, null))
        org.junit.Assert.assertTrue(DateTimeUtils.isTimeAnomaly("", ""))
    }

    @Test
    fun testTimeIntervalOverlap() {
        val slot1 = DateTimeUtils.STANDARD_PAIRS[0] // 08:45 - 10:05 (525..605)
        val slot2 = DateTimeUtils.STANDARD_PAIRS[1] // 10:20 - 11:40 (620..700)
        val slot3 = DateTimeUtils.STANDARD_PAIRS[2] // 11:55 - 13:15 (715..795)

        // Lesson matching slot 1
        org.junit.Assert.assertTrue(DateTimeUtils.doesLessonOverlapSlot("08:45", "10:05", slot1))
        org.junit.Assert.assertFalse(DateTimeUtils.doesLessonOverlapSlot("08:45", "10:05", slot2))

        // Physical Education 09:25 - 10:45 overlaps slot 1 (08:45-10:05) AND slot 2 (10:20-11:40)
        org.junit.Assert.assertTrue(DateTimeUtils.doesLessonOverlapSlot("09:25", "10:45", slot1))
        org.junit.Assert.assertTrue(DateTimeUtils.doesLessonOverlapSlot("09:25", "10:45", slot2))
        org.junit.Assert.assertFalse(DateTimeUtils.doesLessonOverlapSlot("09:25", "10:45", slot3))

        // Irregular lesson 10:00 - 10:30 overlaps slot 1 and slot 2
        org.junit.Assert.assertTrue(DateTimeUtils.doesLessonOverlapSlot("10:00", "10:30", slot1))
        org.junit.Assert.assertTrue(DateTimeUtils.doesLessonOverlapSlot("10:00", "10:30", slot2))

        // Lesson ending right when slot starts does NOT overlap (e.g. 10:00 - 10:20 and slot 2 starting at 10:20)
        org.junit.Assert.assertFalse(DateTimeUtils.doesLessonOverlapSlot("09:00", "10:20", slot2))
    }

    @Test
    fun testDeducePairNumber() {
        assertEquals(1, com.dlab.sirinium.data.remote.dto.deducePairNumber("08:45"))
        assertEquals(1, com.dlab.sirinium.data.remote.dto.deducePairNumber("09:25"))
        assertEquals(2, com.dlab.sirinium.data.remote.dto.deducePairNumber("10:20"))
        assertEquals(3, com.dlab.sirinium.data.remote.dto.deducePairNumber("11:55"))
        assertEquals(4, com.dlab.sirinium.data.remote.dto.deducePairNumber("13:30"))
        assertEquals(5, com.dlab.sirinium.data.remote.dto.deducePairNumber("15:05"))
        assertEquals(6, com.dlab.sirinium.data.remote.dto.deducePairNumber("16:40"))
        assertEquals(7, com.dlab.sirinium.data.remote.dto.deducePairNumber("18:15"))
        assertEquals(8, com.dlab.sirinium.data.remote.dto.deducePairNumber("19:50"))
        assertEquals(8, com.dlab.sirinium.data.remote.dto.deducePairNumber("20:30"))
    }
}
