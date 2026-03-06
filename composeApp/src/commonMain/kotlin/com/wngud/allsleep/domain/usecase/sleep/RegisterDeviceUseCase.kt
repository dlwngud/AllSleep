package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.data.repository.SleepSyncRepository

class RegisterDeviceUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    suspend operator fun invoke(uid: String, deviceState: DeviceState): Result<Unit> {
        return sleepSyncRepository.registerDevice(uid, deviceState)
    }
}
