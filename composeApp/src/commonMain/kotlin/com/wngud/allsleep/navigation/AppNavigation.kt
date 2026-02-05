package com.wngud.allsleep.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wngud.allsleep.ui.home.HomeScreen
import com.wngud.allsleep.ui.onboarding.OnboardingScreen

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
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        // 온보딩 화면을 백스택에서 제거
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        
        // 홈
        composable(Screen.Home.route) {
            HomeScreen()
        }
    }
}
