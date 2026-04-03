package com.wngud.allsleep.domain.model

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.autoupdatingCurrentLocale
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun platformTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatTimestampToDate(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale.autoupdatingCurrentLocale
    }
    return formatter.stringFromDate(date)
}

actual fun formatTimestampToTime(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "a hh:mm"
        locale = NSLocale.autoupdatingCurrentLocale
    }
    return formatter.stringFromDate(date)
}

actual fun isWeekday(timestamp: Long): Boolean {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val calendar = NSCalendar.currentCalendar
    val weekday = calendar.component(NSCalendarUnitWeekday, fromDate = date).toInt()
    // Objective-C 기반의 평일 판단: 1=일, 2=월, ..., 6=금, 7=토
    return weekday in 2..6
}
