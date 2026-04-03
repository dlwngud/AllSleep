package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.repository.SleepSyncRepository

class UpdateUserSleepStateUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(
        uid: String, 
        isSleeping: Boolean? = null, 
        targetWakeUpTime: Long? = null,
        weekdayBedtime: String? = null,
        weekdayWakeTime: String? = null,
        isWeekdaySleepEnabled: Boolean? = null,
        isWeekdayWakeEnabled: Boolean? = null,
        weekendBedtime: String? = null,
        weekendWakeTime: String? = null,
        isWeekendSleepEnabled: Boolean? = null,
        isWeekendWakeEnabled: Boolean? = null
    ): Result<Unit> {
        return sleepSyncRepository.updateUserSleepState(
            uid = uid,
            isSleeping = isSleeping,
            targetWakeUpTime = targetWakeUpTime,
            weekdayBedtime = weekdayBedtime,
            weekdayWakeTime = weekdayWakeTime,
            isWeekdaySleepEnabled = isWeekdaySleepEnabled,
            isWeekdayWakeEnabled = isWeekdayWakeEnabled,
            weekendBedtime = weekendBedtime,
            weekendWakeTime = weekendWakeTime,
            isWeekendSleepEnabled = isWeekendSleepEnabled,
            isWeekendWakeEnabled = isWeekendWakeEnabled
        )
    }
}
