package com.wngud.allsleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.wngud.allsleep.ui.alarm.AlarmScreen
import com.wngud.allsleep.ui.auth.login.GlobalLoginScreen
import com.wngud.allsleep.ui.home.HomeScreen
import com.wngud.allsleep.ui.settings.SettingsScreen
import com.wngud.allsleep.ui.stats.StatsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route,
    contentPadding: PaddingValues = PaddingValues()
) {
    NavHost(navController = navController, startDestination = startDestination) {
        onboardingGraph(navController)
        composable(Screen.Home.route) { HomeScreen(contentPadding = contentPadding) }
        composable(Screen.Stats.route) { StatsScreen(contentPadding = contentPadding) }
        composable(Screen.Alarm.route) { AlarmScreen(contentPadding = contentPadding) }
        composable(Screen.Settings.route) { SettingsScreen(contentPadding = contentPadding) }
        
        composable(Screen.Auth.Login.route) {
            GlobalLoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.Login.route) { inclusive = true }
                }
            })
        }
    }
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().route!!) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
