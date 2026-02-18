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
        data object Login : Screen("onboarding/login")
        data object Complete : Screen("onboarding/complete")
    }
    
    // 메인 홈
    data object Home : Screen("home")
    
    // 통계
    data object Stats : Screen("stats")
    
    // 알람
    data object Alarm : Screen("alarm")
    
    // 설정
    data object Settings : Screen("settings")
}
