package com.dlab.sirinium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.util.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DaySelectorBar(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = DateTimeUtils.parseDate(selectedDateStr) ?: LocalDate.now()
    val today = LocalDate.now()

    // Find start of week (Monday)
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val daysOfWeek = (0L..6L).map { startOfWeek.plusDays(it) }

    val monthYearText = selectedDate.format(DateTimeUtils.MONTH_YEAR_FORMATTER)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Month and Week Switcher Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = monthYearText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedDate != today) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onDateSelected(today.format(DateTimeUtils.DATE_FORMATTER)) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Сегодня",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Сегодня", fontSize = 12.sp)
                    }
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val prevWeek = selectedDate.minusWeeks(1)
                        onDateSelected(prevWeek.format(DateTimeUtils.DATE_FORMATTER))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Предыдущая неделя"
                    )
                }

                IconButton(
                    onClick = {
                        val nextWeek = selectedDate.plusWeeks(1)
                        onDateSelected(nextWeek.format(DateTimeUtils.DATE_FORMATTER))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Следующая неделя"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row of 7 Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val dateStr = date.format(DateTimeUtils.DATE_FORMATTER)
                val dayShortName = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "ПН"
                    DayOfWeek.TUESDAY -> "ВТ"
                    DayOfWeek.WEDNESDAY -> "СР"
                    DayOfWeek.THURSDAY -> "ЧТ"
                    DayOfWeek.FRIDAY -> "ПТ"
                    DayOfWeek.SATURDAY -> "СБ"
                    DayOfWeek.SUNDAY -> "ВС"
                    else -> ""
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(dateStr) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayShortName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (isToday && !isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
