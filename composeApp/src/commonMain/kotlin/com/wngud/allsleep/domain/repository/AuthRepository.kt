package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.platform.PlatformContext

/**
 * 인증 Repository 인터페이스 (commonMain)
 *
 * PlatformContext를 통해 플랫폼 의존성 추상화:
 * Android: PlatformContext = Activity
 * iOS:     PlatformContext = UIViewController
 */
interface AuthRepository {
    suspend fun loginWithKakao(): Result<User>
    suspend fun loginWithGoogle(context: PlatformContext): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): User?
    fun isLoggedIn(): Boolean
}
