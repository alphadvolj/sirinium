package com.dlab.sirinium.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {

    private val RU_MONTHS_GENITIVE = listOf(
        "", "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val RU_MONTHS_NOMINATIVE = listOf(
        "", "январь", "февраль", "март", "апрель", "май", "июнь",
        "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    )

    private val RU_DAYS_SHORT = listOf(
        "пн", "вт", "ср", "чт", "пт", "сб", "вс"
    )

    private val RU_DAYS_FULL = listOf(
        "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
    )

    fun today(): LocalDate {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    fun todayFormatted(): String {
        return formatDate(today())
    }

    fun getWeekMonday(date: LocalDate = today()): LocalDate {
        val dayOfWeekIndex = date.dayOfWeek.ordinal // MONDAY is 0, SUNDAY is 6
        return date.minus(DatePeriod(days = dayOfWeekIndex))
    }

    fun calculateWeekOffset(targetDate: LocalDate, baseDate: LocalDate = today()): Int {
        val baseMonday = getWeekMonday(baseDate)
        val targetMonday = getWeekMonday(targetDate)
        val daysDiff = targetMonday.toEpochDays() - baseMonday.toEpochDays()
        return (daysDiff / 7)
    }

    fun getWeekDatesForOffset(weekOffset: Int, baseDate: LocalDate = today()): List<String> {
        val monday = getWeekMonday(baseDate).plus(DatePeriod(days = weekOffset * 7))
        return (0..6).map { day ->
            formatDate(monday.plus(DatePeriod(days = day)))
        }
    }

    fun formatDate(date: LocalDate): String {
        val d = date.dayOfMonth.toString().padStart(2, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        return "$d.$m.${date.year}"
    }

    fun formatDateShort(date: LocalDate): String {
        val d = date.dayOfMonth.toString().padStart(2, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        return "$d.$m"
    }

    fun formatReadableDate(dateStr: String): String {
        val parsed = parseDate(dateStr) ?: return dateStr
        val day = parsed.dayOfMonth
        val month = RU_MONTHS_GENITIVE.getOrElse(parsed.monthNumber) { "" }
        val dayName = RU_DAYS_FULL.getOrElse(parsed.dayOfWeek.ordinal) { "" }
        return "$day $month, $dayName"
    }

    fun formatShortDate(dateStr: String): String {
        val parsed = parseDate(dateStr) ?: return dateStr
        val day = parsed.dayOfMonth
        val month = RU_MONTHS_GENITIVE.getOrElse(parsed.monthNumber) { "" }
        return "$day $month"
    }

    fun formatRelativeDate(dateStr: String?, baseDate: LocalDate = today()): String {
        val parsed = parseDate(dateStr) ?: return dateStr.orEmpty()
        val daysDiff = parsed.toEpochDays() - baseDate.toEpochDays()
        return when (daysDiff) {
            0 -> "сегодня"
            1 -> "завтра"
            2 -> "послезавтра"
            else -> formatShortDate(dateStr ?: "")
        }
    }

    fun formatLastUpdated(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Никогда"
        val instant = Instant.fromEpochMilliseconds(timestampMillis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val date = ldt.date
        val timeStr = "${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
        val today = today()
        val daysDiff = today.toEpochDays() - date.toEpochDays()
        return when (daysDiff) {
            0 -> "сегодня в $timeStr"
            1 -> "вчера в $timeStr"
            else -> "${formatDateShort(date)} в $timeStr"
        }
    }

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        if (trimmed.contains(".")) {
            val parts = trimmed.split(".")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                val y = parts[2].toIntOrNull() ?: return null
                return try { LocalDate(y, m, d) } catch (_: Exception) { null }
            }
        }
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-")
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toIntOrNull() ?: return null
                    val m = parts[1].toIntOrNull() ?: return null
                    val d = parts[2].toIntOrNull() ?: return null
                    return try { LocalDate(y, m, d) } catch (_: Exception) { null }
                } else {
                    val d = parts[0].toIntOrNull() ?: return null
                    val m = parts[1].toIntOrNull() ?: return null
                    val y = parts[2].toIntOrNull() ?: return null
                    return try { LocalDate(y, m, d) } catch (_: Exception) { null }
                }
            }
        }
        return null
    }

    fun parseTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return try { LocalTime(h, m) } catch (_: Exception) { null }
    }

    fun toLocalDateTime(dateStr: String?, timeStr: String?): LocalDateTime? {
        val date = parseDate(dateStr) ?: return null
        val time = parseTime(timeStr) ?: return null
        return LocalDateTime(date, time)
    }

    fun toEpochMillis(dateStr: String?, timeStr: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long? {
        val dt = toLocalDateTime(dateStr, timeStr) ?: return null
        return dt.toInstant(timeZone).toEpochMilliseconds()
    }

    sealed interface LessonTimeStatus {
        data class Upcoming(val minutesUntilStart: Long) : LessonTimeStatus
        data class Ongoing(val minutesRemaining: Long) : LessonTimeStatus
        data object Finished : LessonTimeStatus
        data object FarAway : LessonTimeStatus
    }

    fun calculateLessonStatus(
        dateStr: String?,
        startTimeStr: String?,
        endTimeStr: String?,
        nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LessonTimeStatus {
        val startMillis = toEpochMillis(dateStr, startTimeStr, timeZone) ?: return LessonTimeStatus.FarAway
        val endMillis = toEpochMillis(dateStr, endTimeStr, timeZone) ?: (startMillis + 90 * 60 * 1000L)

        return when {
            nowMillis > endMillis -> LessonTimeStatus.Finished
            nowMillis < startMillis -> {
                val minsUntil = (startMillis - nowMillis) / (60 * 1000L)
                if (minsUntil <= 180) {
                    LessonTimeStatus.Upcoming(minsUntil)
                } else {
                    LessonTimeStatus.FarAway
                }
            }
            else -> {
                val minsLeft = (endMillis - nowMillis) / (60 * 1000L)
                LessonTimeStatus.Ongoing(minsLeft.coerceAtLeast(1L))
            }
        }
    }

    fun formatStatusText(status: LessonTimeStatus): String? {
        return when (status) {
            is LessonTimeStatus.Upcoming -> {
                when {
                    status.minutesUntilStart <= 0 -> "Начинается прямо сейчас"
                    status.minutesUntilStart < 60 -> "До пары ${status.minutesUntilStart} мин"
                    else -> {
                        val hours = status.minutesUntilStart / 60
                        val mins = status.minutesUntilStart % 60
                        "Через ${hours}ч ${if (mins > 0) "${mins}м" else ""}"
                    }
                }
            }
            is LessonTimeStatus.Ongoing -> {
                "Идёт сейчас (осталось ${status.minutesRemaining} мин)"
            }
            LessonTimeStatus.Finished -> "Завершена"
            LessonTimeStatus.FarAway -> null
        }
    }

    data class StandardPairSlot(
        val pairNumber: Int,
        val startTime: String,
        val endTime: String,
        val startMinutes: Int,
        val endMinutes: Int,
        val timeSlot: String
    )

    val STANDARD_PAIRS: List<StandardPairSlot> = listOf(
        StandardPairSlot(1, "08:45", "10:05", 8 * 60 + 45, 10 * 60 + 5, "08:45 – 10:05"),
        StandardPairSlot(2, "10:20", "11:40", 10 * 60 + 20, 11 * 60 + 40, "10:20 – 11:40"),
        StandardPairSlot(3, "11:55", "13:15", 11 * 60 + 55, 13 * 60 + 15, "11:55 – 13:15"),
        StandardPairSlot(4, "13:30", "14:50", 13 * 60 + 30, 14 * 60 + 50, "13:30 – 14:50"),
        StandardPairSlot(5, "15:05", "16:25", 15 * 60 + 5, 16 * 60 + 25, "15:05 – 16:25"),
        StandardPairSlot(6, "16:40", "18:00", 16 * 60 + 40, 18 * 60 + 0, "16:40 – 18:00"),
        StandardPairSlot(7, "18:15", "19:35", 18 * 60 + 15, 19 * 60 + 35, "18:15 – 19:35"),
        StandardPairSlot(8, "19:50", "21:10", 19 * 60 + 50, 21 * 60 + 10, "19:50 – 21:10")
    )

    val PE_PAIR_1 = StandardPairSlot(1, "09:25", "10:45", 9 * 60 + 25, 10 * 60 + 45, "09:25 – 10:45")

    fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim()
        val parts = clean.split(":")
        if (parts.size < 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    fun timeToMinutes(timeStr: String?): Int = parseTimeToMinutes(timeStr) ?: 0

    fun currentTimeInMinutes(timeZone: TimeZone = TimeZone.currentSystemDefault()): Int {
        val now = Clock.System.now().toLocalDateTime(timeZone)
        return now.hour * 60 + now.minute
    }

    fun isTimeAnomaly(startTime: String?, endTime: String?): Boolean {
        val startMin = parseTimeToMinutes(startTime) ?: return true
        val endMin = parseTimeToMinutes(endTime)
        val validSlots = STANDARD_PAIRS + PE_PAIR_1
        return if (endMin != null) {
            validSlots.none { it.startMinutes == startMin && it.endMinutes == endMin }
        } else {
            validSlots.none { it.startMinutes == startMin }
        }
    }

    fun doesIntervalOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        return start1 < end2 && end1 > start2
    }

    fun doesLessonOverlapInterval(startTime: String?, endTime: String?, slotStartMinutes: Int, slotEndMinutes: Int): Boolean {
        val start = parseTimeToMinutes(startTime) ?: return false
        val end = parseTimeToMinutes(endTime) ?: (start + 80)
        return doesIntervalOverlap(start, end, slotStartMinutes, slotEndMinutes)
    }

    fun doesLessonOverlapSlot(startTime: String?, endTime: String?, slot: StandardPairSlot): Boolean {
        return doesLessonOverlapInterval(startTime, endTime, slot.startMinutes, slot.endMinutes)
    }

    fun formatShortPerson(fullName: String?): String {
        if (fullName.isNullOrBlank()) return ""
        val trimmed = fullName.trim()
        if (trimmed.contains(",")) {
            return trimmed.split(",").joinToString(", ") { formatShortPerson(it) }
        }
        val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> {
                val f = parts[1].firstOrNull()?.uppercaseChar()
                val o = parts[2].firstOrNull()?.uppercaseChar()
                if (f != null && o != null) "${parts[0]} $f. $o."
                else if (f != null) "${parts[0]} $f."
                else parts[0]
            }
            parts.size == 2 -> {
                val f = parts[1].firstOrNull()?.uppercaseChar()
                if (f != null) "${parts[0]} $f." else parts[0]
            }
            else -> trimmed
        }
    }
}
