package com.wngud.allsleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import com.wngud.allsleep.ui.global.GlobalSleepContract
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val globalSleepViewModel: GlobalSleepViewModel = org.koin.compose.koinInject()
        
        val snackbarHostState = remember { SnackbarHostState() }
        val state by globalSleepViewModel.state.collectAsState()
        val isInitialized = state.isStateInitialized
        
        // 전역 Effect 수집
        LaunchedEffect(globalSleepViewModel) {
            globalSleepViewModel.effect.collect { effect ->
                when (effect) {
                    is GlobalSleepContract.Effect.NavigateToSubscription -> {
                        navController.navigate(Screen.Subscription.route)
                    }
                    is GlobalSleepContract.Effect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
        
        // 데이터(온보딩 여부, 로그인 여부)를 불러오는 동안에는 빈 화면(또는 스플래시)을 보여주어 "깜빡임" 현상을 완전히 제거
        if (!isInitialized) {
            AllSleepTheme {
                Scaffold(containerColor = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) // 완전한 빈 화면 (나중에 로고 추가 가능)
                }
            }
            return@KoinContext
        }

        val user = state.currentUser
        val isOnboardingCompleted = state.isOnboardingCompleted
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isSubscriptionScreen = currentRoute == Screen.Subscription.route
        val isLoginScreen = currentRoute == Screen.Auth.Login.route
        
        // 기기 한도 초과 다이얼로그 (구독 화면 들어갔다가 그냥 뒤로 가기 눌렀을 때 얍삽하게 사용하는 것 방지)
        val shouldShowLimitDialog = state.showDeviceLimitDialog || 
            (state.cachedDeviceStateToRegister != null && !isSubscriptionScreen && !isLoginScreen && user != null)
            
        
        // 모든 정보가 준비되었을 때, 동적으로 딱 알맞은 Start 화면을 결정함. (온보딩을 잠깐 거치는 일 방지)
        // 시작 지점은 앱 구동 시 단 한 번만 결정하고 고정하여, 이후 상태 변화로 인해 NavHost가 초기화되는 것을 방지합니다.
        val initialStartDestination = remember {
            if (user != null) {
                Screen.Home.route
            } else if (isOnboardingCompleted) {
                Screen.Auth.Login.route
            } else {
                Screen.Onboarding.route
            }
        }

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
                // 1. 비로그인 상태일 때
                val isInsideOnboarding = route.startsWith("onboarding")
                val isLogin = route == Screen.Auth.Login.route
                val isEmailAuth = route.startsWith("auth/")
                
                // 홈화면 등 잘못된 위치에 있다면 로그인 혹은 온보딩으로 쫓아냄
                if (!isInsideOnboarding && !isLogin && !isEmailAuth) {
                    if (isOnboardingCompleted) {
                        // 이미 온보딩을 마친 사람이라면 로그인 화면으로
                        navController.navigate(Screen.Auth.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // 처음 온 사람이라면 온보딩으로
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            } else {
                // 2. 로그인 상태일 때
                // 로그인/회원가입 화면에 머물러 있다면 홈으로 보냄
                // (온보딩 화면은 '시작하기' 버튼을 직접 누를 수 있도록 자동 이동시키지 않음)
                val isLogin = route == Screen.Auth.Login.route
                val isEmailAuth = route.startsWith("auth/")
                
                if (isLogin || isEmailAuth) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }


        AllSleepTheme {
            if (shouldShowLimitDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        // 빈 여백(Scrim) 터치나 뒤로가기 시 인증 취소 및 로그아웃 처리
                        globalSleepViewModel.handleIntent(GlobalSleepContract.Intent.CancelDeviceRegistration) 
                    },
                    title = { 
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("기기 1대 연결 한도 초과", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) 
                            androidx.compose.material3.IconButton(
                                onClick = { globalSleepViewModel.handleIntent(GlobalSleepContract.Intent.CancelDeviceRegistration) }
                            ) {
                                Text(
                                    text = "✕",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    },
                    text = { Text("무료 버전은 기기 1대만 연결할 수 있습니다.\n계속하시려면 현재 기기로 완전히 교체하거나 프리미엄으로 업그레이드하세요.") },
                    confirmButton = {
                        TextButton(
                            onClick = { globalSleepViewModel.handleIntent(GlobalSleepContract.Intent.UpgradeToPremium) }
                        ) {
                            Text("👑 프리미엄 구독", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { globalSleepViewModel.handleIntent(GlobalSleepContract.Intent.ReplaceDevice) }
                        ) {
                            Text("기존 기기 연결 끊기", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { if (!showBottomNav) SnackbarHost(snackbarHostState) }
            ) { _ ->
                if (showBottomNav) {
                    BottomNavScaffold(
                        navController = navController,
                        snackbarHostState = snackbarHostState
                    ) { paddingValues ->
                        AppNavigation(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            startDestination = initialStartDestination,
                            contentPadding = paddingValues
                        )
                    }
                } else {
                    AppNavigation(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        startDestination = initialStartDestination
                    )
                }
            }
        }
    }
}
