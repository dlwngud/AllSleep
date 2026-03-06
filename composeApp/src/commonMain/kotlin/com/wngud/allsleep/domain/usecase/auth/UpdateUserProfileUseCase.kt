package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.SleepSyncRepository

class UpdateUserProfileUseCase(
    private val repository: SleepSyncRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        return repository.updateUserProfile(user)
    }
}
