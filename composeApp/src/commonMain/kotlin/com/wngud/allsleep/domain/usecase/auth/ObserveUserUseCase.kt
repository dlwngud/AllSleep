package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<User?> = authRepository.observeUser()
}
