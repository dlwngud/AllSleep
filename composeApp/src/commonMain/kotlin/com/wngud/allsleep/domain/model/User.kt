package com.wngud.allsleep.domain.model

/**
 * 사용자 도메인 모델
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: AuthProvider
)

enum class AuthProvider {
    GOOGLE,
    APPLE,
    KAKAO,
    ANONYMOUS
}
