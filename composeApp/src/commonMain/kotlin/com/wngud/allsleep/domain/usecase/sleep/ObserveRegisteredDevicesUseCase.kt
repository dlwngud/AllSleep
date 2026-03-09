package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import kotlinx.coroutines.flow.Flow

class ObserveRegisteredDevicesUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    operator fun invoke(uid: String): Flow<List<DeviceState>> {
        return sleepSyncRepository.observeRegisteredDevices(uid)
    }
}
