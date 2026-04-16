package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository

class LoginWithEmailUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return repository.loginWithEmail(email, password)
    }
}
