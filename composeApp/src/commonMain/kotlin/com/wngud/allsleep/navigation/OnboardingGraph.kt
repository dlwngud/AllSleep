package com.wngud.allsleep.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.wngud.allsleep.ui.onboarding.OnboardingCompleteScreen
import com.wngud.allsleep.ui.onboarding.OnboardingIntent
import com.wngud.allsleep.ui.onboarding.OnboardingLoginScreen
import com.wngud.allsleep.ui.onboarding.OnboardingProblemScreen
import com.wngud.allsleep.ui.onboarding.OnboardingSolutionScreen
import com.wngud.allsleep.ui.onboarding.OnboardingTimeScreen
import com.wngud.allsleep.ui.onboarding.OnboardingPermissionsScreen
import com.wngud.allsleep.ui.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    navigation(
        startDestination = Screen.Onboarding.Problem.route,
        route = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.Problem.route) {
            OnboardingProblemScreen(onNext = { navController.navigate(Screen.Onboarding.Solution.route) })
        }
        composable(Screen.Onboarding.Solution.route) {
            OnboardingSolutionScreen(onNext = { navController.navigate(Screen.Onboarding.Time.route) })
        }
        composable(Screen.Onboarding.Time.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            OnboardingTimeScreen(
                onNext = { navController.navigate(Screen.Onboarding.Permissions.route) },
                bedtime = state.bedtime,
                wakeTime = state.wakeTime,
                onBedtimeChange = { viewModel.handleIntent(OnboardingIntent.UpdateBedtime(it)) },
                onWakeTimeChange = { viewModel.handleIntent(OnboardingIntent.UpdateWakeTime(it)) }
            )
        }
        composable(Screen.Onboarding.Permissions.route) {
            OnboardingPermissionsScreen(
                onAllow = {
                    // TODO: 실제 권한 요청 액션 추가 (Phase 2-1)
                    // 현재는 승인/거절 모두 다음 화면(Login)으로 넘어감
                    navController.navigate(Screen.Onboarding.Login.route)
                },
                onSkip = {
                    navController.navigate(Screen.Onboarding.Login.route)
                }
            )
        }
        composable(Screen.Onboarding.Login.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            OnboardingLoginScreen(
                onNext = { navController.navigate(Screen.Onboarding.Complete.route) },
                onSkip = { navController.navigate(Screen.Onboarding.Complete.route) },
                onKakaoLogin = { },
                onAppleLogin = { },
                onEmailLogin = { }
            )
        }
        composable(Screen.Onboarding.Complete.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.handleIntent(OnboardingIntent.LoadCurrentUser)
            }
            OnboardingCompleteScreen(
                onStart = {
                    viewModel.handleIntent(OnboardingIntent.CompleteOnboarding)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                bedtime = state.bedtime,
                wakeTime = state.wakeTime,
                userName = state.userName
            )
        }
    }
}
