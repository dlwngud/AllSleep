package com.wngud.allsleep.domain.model.stats

import com.wngud.allsleep.domain.model.SleepRecord

data class SleepReport(
    val weeklyBars: List<Float> = emptyList(),
    val weeklyLabels: List<String> = emptyList(),
    val weeklyAverageMinutes: Int = 0,
    val sleepScore: Int = 0,
    val scoreLabel: String = "기록을 기다리고 있어요",
    val sleepDebtMinutes: Int = 0,
    val sleepDebtLevel: SleepDebtLevel = SleepDebtLevel.GOOD,
    val achievementCount: Int = 0,
    val streakDays: Int = 0,
    val aiMessage: String = "수면 모드를 사용하면 개인화 인사이트를 준비할게요.",
    val weeklyDeltaMinutes: Int = 0,
    val bedtimeConsistencyMinutes: Int = 0,
    val weekendDriftMinutes: Int = 0,
    val bestRecord: SleepRecord? = null,
    val worstRecord: SleepRecord? = null,
    val premiumSummary: String = "기록이 쌓이면 장기 패턴을 분석할 수 있어요.",
)
