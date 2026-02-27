package com.wngud.allsleep.domain.usecase

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository

/**
 * Google 로그인 Use Case
 * 비즈니스 로직을 캡슐화
 */
class LoginWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<User> {
        return authRepository.loginWithGoogle()
    }
}
