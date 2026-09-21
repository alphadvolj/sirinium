package com.dlab.sirinium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.ui.theme.expressiveBounceClick
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DateItem(
    val dateStr: String,
    val dayOfMonth: String,
    val dayOfWeek: String,
    val isToday: Boolean
)

private fun getMonthShortCode(monthValue: Int): String {
    return when (monthValue) {
        1 -> "ЯНВ"
        2 -> "ФЕВ"
        3 -> "МАР"
        4 -> "АПР"
        5 -> "МАЙ"
        6 -> "ИЮН"
        7 -> "ИЮЛ"
        8 -> "АВГ"
        9 -> "СЕН"
        10 -> "ОКТ"
        11 -> "НОЯ"
        12 -> "ДЕК"
        else -> ""
    }
}

private fun getDayOfWeekShort(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "ПН"
        DayOfWeek.TUESDAY -> "ВТ"
        DayOfWeek.WEDNESDAY -> "СР"
        DayOfWeek.THURSDAY -> "ЧТ"
        DayOfWeek.FRIDAY -> "ПТ"
        DayOfWeek.SATURDAY -> "СБ"
        DayOfWeek.SUNDAY -> "ВС"
        else -> ""
    }
}

sealed interface DateSelectorElement {
    val key: String

    data class MonthHeader(
        val monthCode: String,
        val firstDateStr: String,
        val year: Int,
        val month: Int
    ) : DateSelectorElement {
        override val key: String = "month_${year}_${month}"
    }

    data class Day(
        val dateItem: DateItem
    ) : DateSelectorElement {
        override val key: String = dateItem.dateStr
    }
}

/**
 * Expressive Date Selector Row (Material 3 Expressive)
 * Dynamic weekly strip with spring-animated tactile buttons, month boundary chips, and static calendar button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorRow(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { DateTimeUtils.today() }
    val todayStr = remember { DateTimeUtils.formatDate(today) }

    var showDatePicker by remember { mutableStateOf(false) }

    // Generate 180-day window centered stably around today, inserting month headers before each month starts
    val elements = remember(today) {
        val result = mutableListOf<DateSelectorElement>()
        var lastMonth = -1

        for (offset in -90..90) {
            val date = today.plus(DatePeriod(days = offset))
            val dateStr = DateTimeUtils.formatDate(date)
            val month = date.monthNumber
            val year = date.year

            if (month != lastMonth) {
                val monthCode = getMonthShortCode(month)
                result.add(
                    DateSelectorElement.MonthHeader(
                        monthCode = monthCode,
                        firstDateStr = dateStr,
                        year = year,
                        month = month
                    )
                )
                lastMonth = month
            }

            result.add(
                DateSelectorElement.Day(
                    DateItem(
                        dateStr = dateStr,
                        dayOfMonth = date.dayOfMonth.toString().padStart(2, '0'),
                        dayOfWeek = getDayOfWeekShort(date.dayOfWeek),
                        isToday = dateStr == todayStr
                    )
                )
            )
        }
        result
    }

    val initialIndex = remember {
        val idx = elements.indexOfFirst { it is DateSelectorElement.Day && it.dateItem.dateStr == selectedDateStr }
        (idx - 2).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(selectedDateStr) {
        val index = elements.indexOfFirst { it is DateSelectorElement.Day && it.dateItem.dateStr == selectedDateStr }
        if (index >= 0) {
            val targetScroll = (index - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                items(elements, key = { it.key }) { element ->
                    when (element) {
                        is DateSelectorElement.MonthHeader -> {
                            Column(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .expressiveBounceClick(scaleDown = 0.92f) {
                                        onDateSelected(element.firstDateStr)
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = element.monthCode,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        is DateSelectorElement.Day -> {
                            val item = element.dateItem
                            val isSelected = item.dateStr == selectedDateStr

                            val cardWidth by animateDpAsState(
                                targetValue = if (isSelected) 54.dp else 48.dp,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                ),
                                label = "card_width"
                            )

                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                ),
                                label = "container_color"
                            )

                            val onContainerColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                ),
                                label = "on_container_color"
                            )

                            val shape = remember(isSelected) {
                                if (isSelected) RoundedCornerShape(18.dp) else RoundedCornerShape(14.dp)
                            }

                            Column(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(68.dp)
                                    .clip(shape)
                                    .background(containerColor)
                                    .expressiveBounceClick(scaleDown = 0.92f) { onDateSelected(item.dateStr) }
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.dayOfWeek,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = onContainerColor.copy(alpha = if (isSelected) 0.85f else 0.6f),
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = item.dayOfMonth,
                                    fontSize = if (isSelected) 18.sp else 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onContainerColor
                                )

                                if (item.isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .width(48.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .expressiveBounceClick(scaleDown = 0.90f) { showDatePicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "Выбрать дату в календаре",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        val initialEpochMillis = remember(selectedDateStr) {
            val localDate = DateTimeUtils.parseDate(selectedDateStr) ?: today
            localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            onDateSelected(DateTimeUtils.formatDate(selectedLocalDate))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
