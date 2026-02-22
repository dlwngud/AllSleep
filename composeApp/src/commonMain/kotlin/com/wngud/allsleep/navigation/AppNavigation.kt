package com.wngud.allsleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.wngud.allsleep.ui.home.HomeScreen
import com.wngud.allsleep.ui.home.AlarmPlaceholder
import com.wngud.allsleep.ui.home.SettingsPlaceholder
import com.wngud.allsleep.ui.stats.StatsScreen

/**
 * 메인 네비게이션 그래프
 *
 * 바텀 네비게이션 탭은 각각 독립적인 NavGraph destination으로 등록합니다.
 * saveState/restoreState로 탭 전환 시 UI 상태(스크롤 위치 등)를 보존하며,
 * 딥링크 및 표준 Back Stack 처리를 Nav2에 위임합니다.
 *
 * @param contentPadding BottomNavScaffold의 paddingValues. 탭 화면 최하단 바텀바 높이 확보에 사용됩니다.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route,
    contentPadding: PaddingValues = PaddingValues()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 온보딩
        onboardingGraph(navController)

        // 홈 탭
        composable(Screen.Home.route) {
            HomeScreen(contentPadding = contentPadding)
        }

        // 통계 탭
        composable(Screen.Stats.route) {
            StatsScreen(modifier = Modifier.padding(contentPadding))
        }

        // 알람 탭
        composable(Screen.Alarm.route) {
            AlarmPlaceholder(modifier = Modifier.padding(contentPadding))
        }

        // 설정 탭
        composable(Screen.Settings.route) {
            SettingsPlaceholder(modifier = Modifier.padding(contentPadding))
        }
    }
}

/**
 * 바텀 네비게이션 탭 전환 헬퍼
 * saveState/restoreState로 탭 상태를 보존하며 단일 인스턴스를 유지합니다.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
