package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.data.repository.AuthRepository

class SignOutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
