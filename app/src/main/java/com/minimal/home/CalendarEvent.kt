package com.minimal.home

import java.util.Date

data class CalendarEvent(
    val title: String,
    val startTime: Date?,
    val endTime: Date?,
    val isAllDay: Boolean = false
)
