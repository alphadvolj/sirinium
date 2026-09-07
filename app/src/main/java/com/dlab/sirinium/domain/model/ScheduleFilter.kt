package com.dlab.sirinium.domain.model

import com.dlab.sirinium.core.util.DateTimeUtils

data class ScheduleFilter(
    val sectionType: String = "group",
    val target: String = "",
    val selectedDate: String = DateTimeUtils.todayFormatted(),
    val searchQuery: String = ""
)
