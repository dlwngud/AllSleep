package com.wngud.allsleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.ui.components.BottomNavScaffold
import com.wngud.allsleep.ui.theme.AllSleepTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomNavRoutes = setOf(
            Screen.Home.route,
            Screen.Stats.route,
            Screen.Alarm.route,
            Screen.Settings.route
        )
        val showBottomNav = currentRoute in bottomNavRoutes

        AllSleepTheme {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { _ ->
                if (showBottomNav) {
                    // 탭 화면: BottomNavScaffold → AppNavigation으로 paddingValues 전달
                    BottomNavScaffold(navController = navController) { paddingValues ->
                        AppNavigation(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            contentPadding = paddingValues
                        )
                    }
                } else {
                    // 온보딩 등 바텀바 없는 화면
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}