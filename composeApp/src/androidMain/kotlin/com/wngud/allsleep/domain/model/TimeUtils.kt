package com.wngud.allsleep.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun platformTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestampToDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun formatTimestampToTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("a hh:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun isWeekday(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    // 2 (MONDAY) ~ 6 (FRIDAY)
    return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
}
