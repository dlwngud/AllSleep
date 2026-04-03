package com.wngud.allsleep.platform

class SleepSchedulerImpl : SleepScheduler {
    override fun scheduleNextEvents(
        weekdayBedtime: String,
        weekdayWakeTime: String,
        isWeekdayEnabled: Boolean,
        weekendBedtime: String,
        weekendWakeTime: String,
        isWeekendEnabled: Boolean
    ) {
        // iOS implementation will be added later
    }

    override fun cancelAll() {
        // iOS implementation will be added later
    }
}
