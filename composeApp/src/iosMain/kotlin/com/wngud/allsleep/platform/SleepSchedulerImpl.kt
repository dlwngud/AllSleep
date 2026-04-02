package com.wngud.allsleep.platform

class SleepSchedulerImpl : SleepScheduler {
    override fun scheduleNextEvents(
        bedtime: String,
        wakeTime: String,
        sleepDays: Set<Int>,
        wakeDays: Set<Int>
    ) {
        // iOS implementation will be added later
    }

    override fun cancelAll() {
        // iOS implementation will be added later
    }
}
