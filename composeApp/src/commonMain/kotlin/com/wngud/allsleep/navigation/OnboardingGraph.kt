package com.wngud.allsleep.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.wngud.allsleep.ui.onboarding.OnboardingCompleteScreen
import com.wngud.allsleep.ui.onboarding.*
import com.wngud.allsleep.platform.rememberPermissionRequester
import com.wngud.allsleep.platform.rememberOverlayPermissionRequester
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    navigation(
        startDestination = Screen.Onboarding.Problem.route,
        route = Screen.Onboarding.route
    ) {
        // Step 1: 문제 공감
        composable(Screen.Onboarding.Problem.route) {
            OnboardingProblemScreen(onNext = { navController.navigate(Screen.Onboarding.Solution.route) })
        }
        // Step 2: 해결책 제시
        composable(Screen.Onboarding.Solution.route) {
            OnboardingSolutionScreen(onNext = { navController.navigate(Screen.Onboarding.Time.route) })
        }
        // Step 3: 수면 시간 설정
        composable(Screen.Onboarding.Time.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            OnboardingTimeScreen(
                onNext = { navController.navigate(Screen.Onboarding.Alerts.route) },
                bedtime = state.bedtime,
                wakeTime = state.wakeTime,
                onBedtimeChange = { viewModel.handleIntent(OnboardingIntent.UpdateBedtime(it)) },
                onWakeTimeChange = { viewModel.handleIntent(OnboardingIntent.UpdateWakeTime(it)) }
            )
        }
        // Step 4: [필수] 알림 및 정기 예약
        composable(Screen.Onboarding.Alerts.route) {
            val permissionRequester = rememberPermissionRequester { isGranted ->
                // Basic permissions 결과 (Notification)
            }
            val isNotiGranted by permissionRequester.isNotificationPermissionGranted.collectAsState(initial = true)
            val isAlarmGranted by permissionRequester.isAlarmPermissionGranted.collectAsState(initial = true)

            // 둘 다 허용 시 자동 이동
            androidx.compose.runtime.LaunchedEffect(isNotiGranted, isAlarmGranted) {
                if (isNotiGranted && isAlarmGranted) {
                    navController.navigate(Screen.Onboarding.Overlay.route)
                }
            }

            OnboardingPermissionStep(
                stepNumber = 4,
                icon = "🔔",
                title = "수면 성공을 함께 기뻐하고\n지켜드릴게요",
                whyContent = "매일 아침 수면 리포트를 정확히 전달하고, 설정된 수면 시간에 오차 없이 수면 모드를 시작하기 위해 필요합니다.",
                whenContent = "아침 수면 분석 리포트 알림 및 밤사이 수면 예약 엔진 가동 시 사용합니다.",
                howContent = "허용 버튼 클릭 후 나타나는 시스템 팝업에서 모두 허용을 선택해주세요.",
                isAllowed = isNotiGranted && isAlarmGranted,
                onAllow = { 
                    if (!isNotiGranted) {
                        permissionRequester.requestBasicPermissions()
                    } else if (!isAlarmGranted) {
                        permissionRequester.requestAlarmPermission()
                    }
                }
            )
        }
        // Step 5: [필수] 화면 보호
        composable(Screen.Onboarding.Overlay.route) {
            val overlayRequester = rememberOverlayPermissionRequester { isGranted ->
                if (isGranted) navController.navigate(Screen.Onboarding.Battery.route)
            }
            val isGranted = overlayRequester.isGranted()

            OnboardingPermissionStep(
                stepNumber = 5,
                icon = "🛡️",
                title = "당신의 의지를\n끝까지 지켜드릴게요",
                whyContent = "수면 중에 무의식적으로 다른 앱을 사용하는 것을 방지하기 위해 화면 잠금 기능을 실행해야 합니다.",
                whenContent = "설정한 수면 목표 시간이 시작되면 자동으로 화면 위에 잠금 레이어를 띄울 때 사용합니다.",
                howContent = "설정 화면에서 AllSleep을 찾아 [다른 앱 위에 표시] 권한을 허용으로 변경해주세요.",
                isAllowed = isGranted,
                onAllow = { overlayRequester.requestPermission() }
            )
        }
        // Step 6: [필수] 실행 유지
        composable(Screen.Onboarding.Battery.route) {
            val permissionRequester = rememberPermissionRequester { }
            val isBatteryOptimized by permissionRequester.isBatteryOptimized.collectAsState(initial = true)

            // 허용(최적화 제외) 시 자동 이동
            androidx.compose.runtime.LaunchedEffect(isBatteryOptimized) {
                if (isBatteryOptimized) {
                    navController.navigate(Screen.Onboarding.Accessibility.route)
                }
            }

            OnboardingPermissionStep(
                stepNumber = 6,
                icon = "🔋",
                title = "밤새 꺼지지 않고\n당신을 분석할게요",
                whyContent = "시스템에 의해 앱이 강제로 종료되지 않아야 정확한 수면 분석과 잠금 기능이 유지될 수 있습니다.",
                whenContent = "앱이 백그라운드에서도 중단 없이 수면 데이터를 수집하고 기기를 제어할 때 사용합니다.",
                howContent = "화면 상단에서 [전체]를 선택 후 'AllSleep'을 검색하여, 사용 안 함 토글을 선택해 주세요.",
                isAllowed = isBatteryOptimized,
                onAllow = { permissionRequester.requestIgnoreBatteryOptimizations() }
            )
        }
        // Step 7: [권장] 강력 차단
        composable(Screen.Onboarding.Accessibility.route) {
            val permissionRequester = rememberPermissionRequester { }
            val isAccessibilityEnabled by permissionRequester.isAccessibilityEnabled.collectAsState(initial = false)

            // 허용 시 자동 이동
            androidx.compose.runtime.LaunchedEffect(isAccessibilityEnabled) {
                if (isAccessibilityEnabled) {
                    navController.navigate(Screen.Onboarding.Auth.route)
                }
            }

            OnboardingPermissionStep(
                stepNumber = 7,
                icon = "🔒",
                title = "어떤 유혹도\n빈틈없이 차단할게요",
                whyContent = "상단바 설정이나 홈 버튼 등으로 잠금 화면을 우회하려는 행위를 근본적으로 방지하기 위해 필요합니다.",
                whenContent = "수면 중에 억지로 다른 앱을 실행하려 할 때 즉시 수면 화면으로 되돌려보낼 때 사용합니다.",
                howContent = "설정 > 접근성 > 설치된 앱 > AllSleep에서 사용으로 변경해 주세요.",
                isAllowed = isAccessibilityEnabled,
                onAllow = { permissionRequester.requestAccessibilityPermission() },
                onSkip = {
                    navController.navigate(Screen.Onboarding.Auth.route)
                }
            )
        }
        // Step 8: 로그인 및 시작
        composable(Screen.Onboarding.Auth.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Onboarding.route)
            }
            val viewModel: OnboardingViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val state by viewModel.state.collectAsState()
            
            // 로그인 상태 확인
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.handleIntent(OnboardingIntent.LoadCurrentUser)
            }

            if (state.user == null && state.userName == "사용자") {
                // 로그인 화면 (8단계 중 8단계)
                OnboardingLoginScreen(
                    onNext = { /* state.user 변화로 인해 하단의 else 블록으로 재생성됨 */ },
                    onSkip = {
                        // 게스트로 시작 (Complete 화면으로)
                        viewModel.handleIntent(OnboardingIntent.UpdateUserName("손님"))
                    },
                    onKakaoLogin = { },
                    onAppleLogin = { },
                    onEmailLogin = { 
                        navController.navigate(Screen.Auth.EmailLogin.route)
                    }
                )
            } else {
                // 준비 완료 화면 (8단계 중 8단계 최종)
                OnboardingCompleteScreen(
                    onStart = {
                        viewModel.handleIntent(OnboardingIntent.CompleteOnboarding)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    bedtime = state.bedtime,
                    wakeTime = state.wakeTime,
                    userName = if (state.userName != "사용자") state.userName else (state.user?.displayName ?: "사용자")
                )
            }
        }
    }
}
