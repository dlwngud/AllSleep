package com.wngud.allsleep.ui.home

/**
 * 홈 화면의 State와 Intent 정의 (MVI 패턴)
 * selectedTab 제거: 탭 전환은 Nav2(NavController)가 담당
 */

data class HomeState(
    val connectedDevicesCount: Int = 3,
    val isSleepReady: Boolean = true,
    val sleepGoal: String = "8h 30m"
)

sealed interface HomeIntent {
    data object StartSleep : HomeIntent
    data object Refresh : HomeIntent
}
