package com.wngud.allsleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.ui.components.BottomNavScaffold
import com.wngud.allsleep.ui.app.AppRootEffect
import com.wngud.allsleep.ui.app.AppRootIntent
import com.wngud.allsleep.ui.app.AppRootViewModel
import com.wngud.allsleep.ui.theme.AllSleepTheme
import org.koin.compose.KoinContext
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val appRootViewModel: AppRootViewModel = koinViewModel()
        val lifecycleOwner = LocalLifecycleOwner.current
        
        val snackbarHostState = remember { SnackbarHostState() }
        val state by appRootViewModel.state.collectAsState()
        val isInitialized = state.isStateInitialized
        
        // 전역 Effect 수집
        LaunchedEffect(appRootViewModel) {
            appRootViewModel.effect.collect { effect ->
                when (effect) {
                    is AppRootEffect.NavigateToSubscription -> {
                        navController.navigate(Screen.Subscription.Purchase.route) {
                            launchSingleTop = true
                        }
                    }
                    is AppRootEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }

        DisposableEffect(lifecycleOwner, appRootViewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    appRootViewModel.handleIntent(AppRootIntent.RefreshPremiumStatus)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        
        if (!isInitialized || state.localInitializationError != null) {
            AllSleepTheme {
                Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
                    com.wngud.allsleep.ui.components.BedtimePage(padding) {
                        com.wngud.allsleep.ui.components.BedtimeStatus(
                            title = if (isInitialized) "설정을 확인해 주세요" else "설정을 불러오는 중",
                            message = state.localInitializationError ?: "이 기기에 저장된 약속을 확인하고 있어요.",
                            action = if (isInitialized) ({ com.wngud.allsleep.ui.components.BedtimeTextButton({ appRootViewModel.handleIntent(AppRootIntent.RequestInitialize) }) { Text("다시 확인") } }) else null,
                        )
                    }
                }
            }
            return@KoinContext
        }

        val user = state.currentUser
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // 계정 기기 한도는 계정 관리 맥락에서만 안내하며 로컬 탭을 막지 않는다.
        val shouldShowLimitDialog = user != null && currentRoute == Screen.AccountSettings.route &&
            (state.showDeviceLimitDialog || state.cachedDeviceStateToRegister != null)

        val initialStartDestination = remember { Screen.Home.route }

        val bottomNavRoutes = setOf(
            Screen.Home.route,
            Screen.Stats.route,
            Screen.Alarm.route,
            Screen.Settings.route
        )
        val showBottomNav = currentRoute in bottomNavRoutes

        AllSleepTheme {
            if (shouldShowLimitDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        // 빈 여백(Scrim) 터치나 뒤로가기 시 인증 취소 및 로그아웃 처리
                        appRootViewModel.handleIntent(AppRootIntent.CancelDeviceRegistration)
                    },
                    title = { 
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("기기 1대 연결 한도 초과", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) 
                            androidx.compose.material3.IconButton(
                                onClick = { appRootViewModel.handleIntent(AppRootIntent.CancelDeviceRegistration) }
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
                            onClick = { appRootViewModel.handleIntent(AppRootIntent.UpgradeToPremium) }
                        ) {
                            Text("👑 프리미엄 구독", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { appRootViewModel.handleIntent(AppRootIntent.ReplaceDevice) }
                        ) {
                            Text("기존 기기 연결 끊기", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BottomNavScaffold(
                navController = navController,
                snackbarHostState = snackbarHostState,
                showBottomBar = showBottomNav,
            ) { paddingValues ->
                AppNavigation(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    startDestination = initialStartDestination,
                    contentPadding = paddingValues,
                    accountName = user?.displayName ?: user?.email?.substringBefore('@') ?: user?.let { "로그인한 계정" },
                )
            }
        }
    }
}
