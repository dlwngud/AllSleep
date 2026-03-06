package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.data.repository.SleepSyncRepository

class UpdateUserSleepStateUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(
        uid: String, 
        isSleeping: Boolean, 
        targetWakeUpTime: Long? = null
    ): Result<Unit> {
        return sleepSyncRepository.updateUserSleepState(uid, isSleeping, targetWakeUpTime)
    }
}
