package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.platform.PlatformContext

class LoginWithGoogleUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(context: PlatformContext): Result<User> =
        authRepository.loginWithGoogle(context)
}
