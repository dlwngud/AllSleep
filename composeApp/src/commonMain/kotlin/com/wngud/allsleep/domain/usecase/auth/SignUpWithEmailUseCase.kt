package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository

class SignUpWithEmailUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> {
        return repository.signUpWithEmail(email, password, name)
    }
}
