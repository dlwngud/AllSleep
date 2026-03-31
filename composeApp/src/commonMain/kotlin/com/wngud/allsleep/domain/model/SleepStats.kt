package com.wngud.allsleep.domain.model

/**
 * 통계 대시보드 및 인사이트 화면을 위한 집계 결과 모델
 */
data class SleepStats(
    // 기본 지표
    val averageDurationMinutes: Int = 0,
    val averageEfficiency: Float = 0f,
    val goalAchievementRate: Float = 0f,   // 달성일 / 전체일
    val goalAchievedDays: Int = 0,
    val totalDays: Int = 0,
    
    // 추이 차트용
    val dailyDurations: List<Pair<String, Int>> = emptyList(), // (date, minutes)
    
    // 요일별 히트맵용
    val weekdayAverages: Map<Int, Int> = emptyMap(), // DayOfWeek(1=Mon) → avgMinutes
    
    // 베스트/워스트
    val bestRecord: SleepRecord? = null,
    val worstRecord: SleepRecord? = null,
    
    // 차별화 인사이트 지표 (Premium)
    val sleepScore: Int = 0,               // 0~100 수면 점수
    val sleepScoreGrade: String = "",      // "매우 좋음", "좋음" 등
    val sleepDebtMinutes: Int = 0,         // 이번 주 수면 부채 (분)
    val lockStreak: Int = 0,               // 잠금 연속 사용 스트릭
    val insightText: String = "",          // AI 분석 문구
    val insightType: String = "",          // 분석 타입 (JET_LAG 등)
    
    // 원시 데이터 목록
    val allRecords: List<SleepRecord> = emptyList(),
    val monthlyRecords: Map<String, SleepRecord> = emptyMap() // 캘린더 뷰 매핑용
)
