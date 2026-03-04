package com.wngud.allsleep.ui.auth.login

import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User

/**
 * 로그인 화면 상태 (MVI State)
 */
data class LoginState(
    val loadingProvider: AuthProvider? = null,
    val user: User? = null,
    val error: String? = null
)
