package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.data.repository.AuthRepository

class LoginWithKakaoUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = authRepository.loginWithKakao()
}
