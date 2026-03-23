package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.repository.AuthRepository

class ValidateSessionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.validateSession()
    }
}
