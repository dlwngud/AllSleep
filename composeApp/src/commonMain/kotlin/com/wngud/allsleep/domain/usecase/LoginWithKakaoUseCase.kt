package com.wngud.allsleep.domain.usecase

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository

/**
 * 카카오 로그인 Use Case
 */
class LoginWithKakaoUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<User> {
        return authRepository.loginWithKakao()
    }
}
