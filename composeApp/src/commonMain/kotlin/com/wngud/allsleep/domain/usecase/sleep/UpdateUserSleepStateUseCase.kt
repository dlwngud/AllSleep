package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.repository.SleepSyncRepository

class UpdateUserSleepStateUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(
        uid: String, 
        isSleeping: Boolean? = null, 
        targetWakeUpTime: Long? = null,
        bedtime: String? = null,
        wakeTime: String? = null,
        sleepAlarmDays: Set<Int>? = null,
        wakeAlarmDays: Set<Int>? = null,
        isSleepAlarmEnabled: Boolean? = null,
        isWakeAlarmEnabled: Boolean? = null
    ): Result<Unit> {
        return sleepSyncRepository.updateUserSleepState(
            uid, isSleeping, targetWakeUpTime, bedtime, wakeTime,
            sleepAlarmDays, wakeAlarmDays, isSleepAlarmEnabled, isWakeAlarmEnabled
        )
    }
}
