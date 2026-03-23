package com.wngud.allsleep.domain.usecase.auth

import com.wngud.allsleep.domain.repository.AuthRepository

/**
 * 계정 삭제 UseCase
 * Firestore 데이터 삭제 및 Firebase Auth 계정 삭제를 수행합니다.
 */
class DeleteAccountUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.deleteAccount()
    }
}
