package com.wngud.allsleep.domain.model

import java.text.SimpleDateFormat
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
