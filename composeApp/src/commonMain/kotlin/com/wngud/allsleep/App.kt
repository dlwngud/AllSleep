package com.wngud.allsleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.ui.components.BottomNavScaffold
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import com.wngud.allsleep.ui.theme.AllSleepTheme
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val globalSleepViewModel: GlobalSleepViewModel = koinViewModel()
        
        val isInitialized by globalSleepViewModel.isStateInitialized.collectAsState()
        
        // 데이터(온보딩 여부, 로그인 여부)를 불러오는 동안에는 빈 화면(또는 스플래시)을 보여주어 "깜빡임" 현상을 완전히 제거
        if (!isInitialized) {
            AllSleepTheme {
                Scaffold(containerColor = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) // 완전한 빈 화면 (나중에 로고 추가 가능)
                }
            }
            return@KoinContext
        }

        val user by globalSleepViewModel.currentUser.collectAsState()
        val isOnboardingCompleted by globalSleepViewModel.isOnboardingCompleted.collectAsState()
        
        // 모든 정보가 준비되었을 때, 동적으로 딱 알맞은 Start 화면을 결정함. (온보딩을 잠깐 거치는 일 방지)
        val initialStartDestination = remember {
            if (user != null) {
                Screen.Home.route
            } else if (isOnboardingCompleted) {
                Screen.Auth.Login.route
            } else {
                Screen.Onboarding.route
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavRoutes = setOf(
            Screen.Home.route,
            Screen.Stats.route,
            Screen.Alarm.route,
            Screen.Settings.route
        )
        val showBottomNav = currentRoute in bottomNavRoutes

        // 이후 로그아웃을 하거나 상태가 변했을 때의 내비게이션 이동 트리거
        LaunchedEffect(user, isOnboardingCompleted) {
            val route = currentRoute ?: return@LaunchedEffect

            if (user == null) {
                // 비로그인 상태일 때, 잘못된 경로(홈 화면 등)에 있다면 쫓아냄
                val isOnboarding = route.startsWith("onboarding")
                val isLogin = route == Screen.Auth.Login.route
                
                if (!isOnboarding && !isLogin) {
                    if (isOnboardingCompleted) {
                        navController.navigate(Screen.Auth.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            } else {
                // 로그인 상태일 때, 로그인/온보딩 화면에 있다면 홈으로 보냄
                val isOnboarding = route.startsWith("onboarding")
                val isLogin = route == Screen.Auth.Login.route

                if (isOnboarding || isLogin) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        AllSleepTheme {
            Scaffold(containerColor = MaterialTheme.colorScheme.background) { _ ->
                if (showBottomNav) {
                    BottomNavScaffold(navController = navController) { paddingValues ->
                        AppNavigation(
                            navController = navController,
                            startDestination = initialStartDestination,
                            contentPadding = paddingValues
                        )
                    }
                } else {
                    AppNavigation(
                        navController = navController,
                        startDestination = initialStartDestination
                    )
                }
            }
        }
    }
}
