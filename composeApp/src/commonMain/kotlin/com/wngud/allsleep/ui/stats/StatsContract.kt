package com.wngud.allsleep.ui.stats

/**
 * 통계 화면 MVI 계약 — 3탭 구조 (기록 | 트렌드 | 인사이트)
 */

enum class StatsTab { RECORD, TREND, INSIGHT }

data class StatsState(
    val selectedTab: StatsTab = StatsTab.RECORD,
    val selectedDate: String? = "2026-03-15",   // 캘린더에서 선택된 날짜
    val selectedYearMonth: String = "2026-03",  // 캘린더 현재 표시 월
    val selectedPeriodIndex: Int = 0,           // 트렌드 탭 기간 (0=이번주, 1=이번달, 2=올해, 3=전체)
)

sealed interface StatsIntent {
    data class SelectTab(val tab: StatsTab) : StatsIntent
    data class SelectDate(val date: String) : StatsIntent
    data class NavigateMonth(val yearMonth: String) : StatsIntent
    data class SelectPeriod(val index: Int) : StatsIntent
}
