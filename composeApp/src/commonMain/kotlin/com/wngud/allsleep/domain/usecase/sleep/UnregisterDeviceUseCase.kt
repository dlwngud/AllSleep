package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.repository.SleepSyncRepository

class UnregisterDeviceUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(uid: String, deviceId: String): Result<Unit> {
        return sleepSyncRepository.unregisterDevice(uid, deviceId)
    }
}
