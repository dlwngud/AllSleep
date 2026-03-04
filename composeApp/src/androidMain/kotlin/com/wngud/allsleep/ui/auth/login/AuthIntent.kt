package com.wngud.allsleep.ui.auth.login

import com.wngud.allsleep.platform.PlatformContext

/**
 * 로그인 화면 Intent (MVI)
 * PlatformContext = Activity (Android) / UIViewController (iOS)
 */
sealed interface AuthIntent {
    data class LoginWithGoogle(val context: PlatformContext) : AuthIntent
    data object LoginWithKakao : AuthIntent
    data object DismissError : AuthIntent
}
