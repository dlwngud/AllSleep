package com.wngud.allsleep.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.wngud.allsleep.ui.home.HomeScreen

/**
 * 메인 네비게이션 그래프
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 온보딩
        onboardingGraph(navController)
        
        // 홈 (메인 쉘 역할을 수행할 수 있도록 HomeScreen 수정 필요)
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        // 통계
        composable(Screen.Stats.route) {
            HomeScreen(navController, initialTab = 1)
        }
        
        // 알람
        composable(Screen.Alarm.route) {
            HomeScreen(navController, initialTab = 2)
        }
        
        // 설정
        composable(Screen.Settings.route) {
            HomeScreen(navController, initialTab = 3)
        }
    }
}
