package com.wngud.allsleep.ui.stats

/**
 * 통계 화면의 State와 Intent 정의 (MVI 패턴)
 */

data class StatsState(
    val timePeriodIndex: Int = 0 // 0: 주, 1: 월, 2: 년, 3: 전체
)

sealed interface StatsIntent {
    data class SelectTimePeriod(val index: Int) : StatsIntent
}
