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
    val trendRecords: List<SleepRecord> = emptyList(),  // 통계 분석용 전체 수면 기록 목록
    val selectedPeriodIndex: Int = 0,           // 트렌드 탭 기간 (0=이번주, 1=이번달, 2=올해, 3=전체)
    
    // 계산된 통계 지표들
    val latestRecord: SleepRecord? = null,      // 헤더 "어젯밤" 용 최신 수면 데이터
    val weeklyTrend: List<Float> = emptyList(), // 최근 7일간의 수면 시간(시간 단위) 리스트
    val trendDates: List<String> = emptyList(), // 차트 바벨 레이블용 ("월", "화" 등)
    val avgSleepMinutes: Int = 0,               // 평균 수면 분
    val avgEfficiency: Float = 0f,              // 평균 수면 효율 (0~1)
    val achievementCount: Int = 0,              // 목표 달성 일수
    val streakDays: Int = 0,                    // 잠금 모드 연속 사용 일수
    
    // 수면 분석 점수 (인사이트용 실데이터)
    val sleepScore: Int = 0,                    // 종합 점수 (0~100)
    val consistencyScore: Float = 0f,           // 취침 일관성 (0~1)
    val durationScore: Float = 0f,              // 수면 시간 충족도 (0~1)
    val lockComplianceScore: Float = 0f,        // 잠금 모드 준수율 (0~1)
    
    // 기간별 트렌드 (주/월/년/전체 연동)
    val periodBars: List<Float> = emptyList(),  // 선택된 기간의 막대 데이터
    val periodLabels: List<String> = emptyList(), // 선택된 기간의 레이블
    
    // 추가 지표 (히트맵 및 베스트/워스트)
    val heatmapData: List<Float> = emptyList(), // 요일별 평균 수면 시간 (index 0=월, 6=일)
    val bestRecord: SleepRecord? = null,
    val worstRecord: SleepRecord? = null,
    
    // 선택 기간별 동적 지표 (ViewModel에서 계산)
    val metricLabel1: String = "평균 수면",
    val metricValue1: String = "0h 0m",
    val metricLabel2: String = "수면 효율",
    val metricValue2: String = "0%",
    val metricLabel3: String = "목표 달성",
    val metricValue3: String = "0일",
    
    // AI 인사이트 및 수면 부채
    val aiMessage: String = "데이터 분석 중...",
    val aiSymbol: String = "✨",
    val recentAvgMinutes: Int = 0,               // 최근 7일간의 실제 평균 수면 시간 (분)
    val recentSevenDaysDebt: Int = 0,           // 최근 7일간의 총 수면 부채 (분)
    
    // 설정에서 가져온 현재 목표 수면 시간 (분)
    val currentTargetMinutes: Int = 480,         // 기본값 8시간
    
    val error: String? = null
)

sealed interface StatsIntent {
    data class SelectTab(val tab: StatsTab) : StatsIntent
    data class SelectDate(val date: String) : StatsIntent
    data class NavigateMonth(val yearMonth: String) : StatsIntent
    data class SelectPeriod(val index: Int) : StatsIntent
}
