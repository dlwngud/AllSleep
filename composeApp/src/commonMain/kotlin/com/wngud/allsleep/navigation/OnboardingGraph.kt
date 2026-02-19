package com.wngud.allsleep.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.wngud.allsleep.ui.onboarding.OnboardingCompleteScreen
import com.wngud.allsleep.ui.onboarding.OnboardingLoginScreen
import com.wngud.allsleep.ui.onboarding.OnboardingProblemScreen
import com.wngud.allsleep.ui.onboarding.OnboardingSolutionScreen
import com.wngud.allsleep.ui.onboarding.OnboardingTimeScreen
import com.wngud.allsleep.ui.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.remember

/**
 * 온보딩 네비게이션 그래프
 * 
 * - Screen.Onboarding ("onboarding") 루트 하위에 중첩 그래프 구성
 * - OnboardingViewModel을 그래프 스코프로 공유
 */
fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    navigation(
        startDestination = Screen.Onboarding.Problem.route,
        route = Screen.Onboarding.route
    ) {
        // ViewModel을 그래프 스코프에서 생성/공유하기 위한 트릭
        // (compose-navigation factory 함수가 없으므로 각 화면에서 같은 인스턴스를 주입받도록 처리하거나,
        //  상위 레벨에서 주입받아 넘겨줄 수 있음. 여기서는 각 화면에서 koinViewModel() 호출 시 
        //  싱글톤이 아닌 이상 별개 인스턴스가 될 수 있으므로, ViewModelStoreOwner를 그래프로 설정해야 함)
        
        // Koin을 이용한 Shared ViewModel 패턴:
        // 일반적인 방식은 `val viewModel: OnboardingViewModel = koinViewModel()`을 
        // `navigation` 블록 바로 아래에서는 호출할 수 없음 (Composable이 아님).
        // 따라서 각 composable 내부에서 `koinViewModel(viewModelStoreOwner = ...)`를 사용해야 함.
        // 편의상 여기서는 각 화면에서 같은 ViewModel을 쓰도록 `sharedViewModel` 변수로 관리하기 어려움.
        // 대안: OnboardingScreen이라는 부모 Composable을 두고 그 안에서 NavHost를 또 만들거나,
        //       AppModule에서 Single로 선언하거나,
        //       혹은 아래처럼 각 화면에서 주입받되, Scoped Injection을 활용해야 함.
        
        composable(Screen.Onboarding.Problem.route) {
            OnboardingProblemScreen(
                onNext = { navController.navigate(Screen.Onboarding.Solution.route) }
            )
        }
        
        composable(Screen.Onboarding.Solution.route) {
            OnboardingSolutionScreen(
                onNext = { navController.navigate(Screen.Onboarding.Time.route) }
            )
        }
        
        composable(Screen.Onboarding.Time.route) { backStackEntry ->
            // 그래프 스코프의 ViewModel 가져오기
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            
            OnboardingTimeScreen(
                onNext = { navController.navigate(Screen.Onboarding.Login.route) },
                bedtime = state.bedtime,
                wakeTime = state.wakeTime,
                onBedtimeChange = { viewModel.handleIntent(com.wngud.allsleep.ui.onboarding.OnboardingIntent.UpdateBedtime(it)) },
                onWakeTimeChange = { viewModel.handleIntent(com.wngud.allsleep.ui.onboarding.OnboardingIntent.UpdateWakeTime(it)) }
            )
        }
        
        composable(Screen.Onboarding.Login.route) { backStackEntry ->
            // 그래프 스코프의 ViewModel 가져오기
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            // LoginScreen에서 로컬 모드 여부 등을 ViewModel에 저장할 수 있음
             val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
             
            OnboardingLoginScreen(
                onNext = { navController.navigate(Screen.Onboarding.Complete.route) },
                onSkip = { navController.navigate(Screen.Onboarding.Complete.route) },
                onKakaoLogin = { /* TODO */ },
                onAppleLogin = { /* TODO */ },
                onEmailLogin = { /* TODO */ }
            )
        }
        
        composable(Screen.Onboarding.Complete.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            
            // 화면 진입 시 사용자 정보 갱신
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.handleIntent(com.wngud.allsleep.ui.onboarding.OnboardingIntent.LoadCurrentUser)
            }
            
            OnboardingCompleteScreen(
                onStart = {
                    viewModel.handleIntent(com.wngud.allsleep.ui.onboarding.OnboardingIntent.CompleteOnboarding)
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


