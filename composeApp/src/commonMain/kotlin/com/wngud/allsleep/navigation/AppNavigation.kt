package com.wngud.allsleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.wngud.allsleep.ui.alarm.AlarmScreen
import com.wngud.allsleep.ui.auth.login.GlobalLoginScreen
import com.wngud.allsleep.ui.auth.login.EmailLoginScreen
import com.wngud.allsleep.ui.auth.login.EmailSignupScreen
import com.wngud.allsleep.ui.home.HomeScreen
import com.wngud.allsleep.ui.settings.SettingsScreen
import com.wngud.allsleep.ui.stats.StatsScreen
import com.wngud.allsleep.ui.subscription.SubscriptionScreen
import com.wngud.allsleep.ui.subscription.SubscriptionManageScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    startDestination: String = Screen.Onboarding.route,
    contentPadding: PaddingValues = PaddingValues()
) {
    NavHost(navController = navController, startDestination = startDestination) {
        onboardingGraph(navController)
        composable(Screen.Home.route) { HomeScreen(contentPadding = contentPadding, snackbarHostState = snackbarHostState) }
        composable(Screen.Stats.route) { 
            StatsScreen(
                contentPadding = contentPadding,
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.Purchase.route) {
                        launchSingleTop = true
                    }
                }
            ) 
        }
        composable(Screen.Alarm.route) { AlarmScreen(contentPadding = contentPadding) }
        composable(Screen.Settings.route) { 
            SettingsScreen(
                navController = navController, 
                onNavigateToSubscription = { isPremium ->
                    navController.navigate(
                        if (isPremium) Screen.Subscription.Manage.route else Screen.Subscription.Purchase.route
                    ) { launchSingleTop = true }
                },
                contentPadding = contentPadding,
                snackbarHostState = snackbarHostState
            ) 
        }
        
        composable(Screen.Subscription.Purchase.route) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Subscription.Manage.route) {
            SubscriptionManageScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Auth.Login.route) {
            GlobalLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.Login.route) { inclusive = true }
                    }
                },
                onEmailLogin = {
                    navController.navigate(Screen.Auth.EmailLogin.route)
                }
            )
        }

        composable(Screen.Auth.EmailLogin.route) {
            EmailLoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Auth.EmailSignup.route)
                }
            )
        }

        composable(Screen.Auth.EmailSignup.route) {
            EmailSignupScreen(
                onBack = { navController.popBackStack() },
                onSignupSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Auth.EmailLogin.route)
                }
            )
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
