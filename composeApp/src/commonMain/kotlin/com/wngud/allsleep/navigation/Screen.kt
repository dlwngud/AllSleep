package com.wngud.allsleep.navigation

/**
 * 앱 네비게이션 라우트 정의
 */
sealed class Screen(val route: String) {
    // 온보딩
    data object Onboarding : Screen("onboarding")
    
    // 메인 홈
    data object Home : Screen("home")
}
