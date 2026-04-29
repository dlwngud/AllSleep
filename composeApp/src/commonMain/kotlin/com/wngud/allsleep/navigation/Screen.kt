package com.wngud.allsleep.navigation

/**
 * 앱 네비게이션 라우트 정의
 */
sealed class Screen(val route: String) {
    // 온보딩
    data object Onboarding : Screen("onboarding") {
        data object Problem : Screen("onboarding/problem")
        data object Solution : Screen("onboarding/solution")
        data object Time : Screen("onboarding/time")
        data object Alerts : Screen("onboarding/alerts")
        data object Overlay : Screen("onboarding/overlay")
        data object Battery : Screen("onboarding/battery")
        data object Accessibility : Screen("onboarding/accessibility")
        data object Auth : Screen("onboarding/auth")
    }

    // 인증 (로그아웃 후 재로그인 등)
    data object Auth : Screen("auth") {
        data object Login : Screen("auth/login")
        data object EmailLogin : Screen("auth/email_login")
        data object EmailSignup : Screen("auth/email_signup")
    }
    
    // 메인 홈
    data object Home : Screen("home")
    
    // 통계
    data object Stats : Screen("stats")
    
    // 알람
    data object Alarm : Screen("alarm")
    
    // 설정
    data object Settings : Screen("settings")
    
    // 프리미엄 구독
    data object Subscription : Screen("subscription") {
        data object Purchase : Screen("subscription/purchase")
        data object Manage : Screen("subscription/manage")
    }
}
