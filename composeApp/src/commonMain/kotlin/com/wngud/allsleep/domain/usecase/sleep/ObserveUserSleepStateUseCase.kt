package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserSleepStateUseCase(
    private val sleepSyncRepository: SleepSyncRepository
) {
    operator fun invoke(uid: String): Flow<UserSleepState?> {
        return sleepSyncRepository.observeUserSleepState(uid)
    }
}
