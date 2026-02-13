package com.wngud.allsleep.ui.auth.login

import com.wngud.allsleep.domain.model.User

/**
 * 로그인 화면 상태 (MVI State)
 */
data class LoginState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

/**
 * 로그인 화면 의도 (MVI Intent)
 */
sealed interface LoginIntent {
    data object LoginWithGoogle : LoginIntent
    data object DismissError : LoginIntent
}
