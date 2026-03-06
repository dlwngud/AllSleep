package com.wngud.allsleep.data.repository

import com.wngud.allsleep.data.datasource.auth.FirebaseAuthDataSource
import com.wngud.allsleep.data.datasource.auth.GoogleAuthDataSource
import com.wngud.allsleep.data.datasource.auth.KakaoAuthDataSource
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.data.repository.AuthRepository
import com.wngud.allsleep.platform.PlatformContext

class AuthRepositoryImpl(
    private val kakaoAuthDataSource: KakaoAuthDataSource,
    private val googleAuthDataSource: GoogleAuthDataSource,
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun loginWithKakao(): Result<User> =
        kakaoAuthDataSource.signIn()

    override suspend fun loginWithGoogle(context: PlatformContext): Result<User> =
        googleAuthDataSource.signIn(context)

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseAuthDataSource.signOut()
    }

    override suspend fun getCurrentUser(): User? =
        firebaseAuthDataSource.getCurrentUser()

    override fun isLoggedIn(): Boolean =
        firebaseAuthDataSource.isLoggedIn()
}
