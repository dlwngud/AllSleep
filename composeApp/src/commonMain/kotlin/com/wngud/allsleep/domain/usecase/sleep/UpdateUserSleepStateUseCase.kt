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
        wakeTime: String? = null
    ): Result<Unit> {
        return sleepSyncRepository.updateUserSleepState(uid, isSleeping, targetWakeUpTime, bedtime, wakeTime)
    }
}
