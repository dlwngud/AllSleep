package com.wngud.allsleep.ui.stats

import com.wngud.allsleep.domain.model.SleepRecord

/**
 * 통계 화면 MVI 계약 — 3탭 구조 (기록 | 트렌드 | 인사이트)
 */

enum class StatsTab { RECORD, TREND, INSIGHT }

data class StatsState(
    val isLoading: Boolean = false,
    val selectedTab: StatsTab = StatsTab.RECORD,
    val selectedDate: String? = null,           // 캘린더에서 선택된 날짜
    val selectedYearMonth: String = "",         // 캘린더 현재 표시 월 (초기화 시 설정)
    val records: Map<String, SleepRecord> = emptyMap(), // 날짜별 수면 기록 (Key: "yyyy-MM-dd")
    val selectedPeriodIndex: Int = 0,           // 트렌드 탭 기간 (0=이번주, 1=이번달, 2=올해, 3=전체)
    
    // 계산된 통계 지표들
    val latestRecord: SleepRecord? = null,      // 헤더 "어젯밤" 용 최신 수면 데이터
    val weeklyTrend: List<Float> = emptyList(), // 최근 7일간의 수면 시간(시간 단위) 리스트
    val trendDates: List<String> = emptyList(), // 차트 바벨 레이블용 ("월", "화" 등)
    val avgSleepMinutes: Int = 0,               // 평균 수면 분
    val avgEfficiency: Float = 0f,              // 평균 수면 효율 (0~1)
    val achievementCount: Int = 0,              // 목표 달성 일수
    val streakDays: Int = 0,                    // 잠금 모드 연속 사용 일수
    
    // 추가 지표 (히트맵 및 베스트/워스트)
    val heatmapData: List<Float> = emptyList(), // 요일별 평균 수면 시간 (index 0=월, 6=일)
    val bestRecord: SleepRecord? = null,
    val worstRecord: SleepRecord? = null,
    
    val error: String? = null
)

sealed interface StatsIntent {
    data class SelectTab(val tab: StatsTab) : StatsIntent
    data class SelectDate(val date: String) : StatsIntent
    data class NavigateMonth(val yearMonth: String) : StatsIntent
    data class SelectPeriod(val index: Int) : StatsIntent
}
