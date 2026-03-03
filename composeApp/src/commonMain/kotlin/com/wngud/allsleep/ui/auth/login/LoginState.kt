package com.wngud.allsleep.ui.auth.login

import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User

/**
 * 로그인 화면 상태 (MVI State)
 */
data class LoginState(
    val loadingProvider: AuthProvider? = null, // 현재 로딩 중인 로그인 제공자 (null = 로딩 없음)
    val user: User? = null,
    val error: String? = null
)

/**
 * 로그인 화면 의도 (MVI Intent)
 */
sealed interface LoginIntent {
    data object LoginWithGoogle : LoginIntent
    data object LoginWithKakao : LoginIntent
    data object DismissError : LoginIntent
}
