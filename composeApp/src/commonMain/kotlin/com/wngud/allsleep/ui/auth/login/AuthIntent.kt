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
    
    // 이메일 로그인 관련
    data class UpdateEmail(val email: String) : AuthIntent
    data class UpdatePassword(val password: String) : AuthIntent
    data class UpdateConfirmPassword(val confirmPassword: String) : AuthIntent
    data class UpdateName(val name: String) : AuthIntent
    data object LoginWithEmail : AuthIntent
    data object SignUpWithEmail : AuthIntent
    data object ToggleAuthMode : AuthIntent
}
