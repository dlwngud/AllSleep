package com.wngud.allsleep.domain.model

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.autoupdatingCurrentLocale

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
