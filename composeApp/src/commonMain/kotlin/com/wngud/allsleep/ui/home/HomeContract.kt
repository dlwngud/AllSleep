package com.wngud.allsleep.ui.home

/**
 * 홈 화면의 State와 Intent 정의 (MVI 패턴)
 */

data class HomeState(
    val selectedTab: Int = 0,
    val connectedDevicesCount: Int = 3,
    val isSleepReady: Boolean = true,
    val rotation1: Float = 0f, // 애니메이션 상태는 필요 시 추가 (지금은 UI 내부 애니메이션으로 유지 가능)
    val sleepGoal: String = "8h 30m"
)

sealed interface HomeIntent {
    data class SelectTab(val tabIndex: Int) : HomeIntent
    data object StartSleep : HomeIntent
    data object Refresh : HomeIntent
}
