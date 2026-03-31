package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.SleepStats
import kotlin.math.roundToInt

/**
 * 수면 기록 목록을 바탕으로 통계 지표를 계산하는 UseCase
 * (순수 함수 로직 - Repository 의존 없음)
 */
class GetSleepStatsUseCase {
    
    operator fun invoke(records: List<SleepRecord>, targetMinutesPerDay: Int = 480): SleepStats {
        if (records.isEmpty()) return SleepStats()

        val count = records.size
        val totalDuration = records.sumOf { it.durationMinutes }
        val totalEfficiency = records.sumOf { it.sleepEfficiency.toDouble() }
        val goalAchievedDays = records.count { it.achievementRate >= 100f }
        
        // 1. 기본 지표
        val averageDurationMinutes = (totalDuration / count)
        val averageEfficiency = (totalEfficiency / count).toFloat()
        val goalAchievementRate = (goalAchievedDays.toFloat() / count.toFloat()) * 100f
        
        // 2. 추이 차트용 데이터 (date, duration)
        val dailyDurations = records.sortedBy { it.date }.map { it.date to it.durationMinutes }
        
        // 3. 요일별 평균 (DayOfWeek -> avgDuration)
        // 주의: 요일 계산 로직이 필요함 (여기서는 생략하거나 간단히 구현)
        // 일단 emptyMap으로 두고 나중에 필요 시 고도화
        
        // 4. 베스트/워스트 (수면 효율 기준)
        val bestRecord = records.maxByOrNull { it.sleepEfficiency }
        val worstRecord = records.minByOrNull { it.sleepEfficiency }
        
        // 5. 프리미엄 지표 (간이 계산식)
        val sleepScore = calculateSleepScore(records)
        val sleepScoreGrade = when {
            sleepScore >= 90 -> "매우 좋음"
            sleepScore >= 80 -> "좋음"
            sleepScore >= 60 -> "보통"
            else -> "개선 필요"
        }
        
        // 수면 부채: 이번 주(최근 7일) 목표 대비 부족분
        val recent7Days = records.takeLast(7)
        val weeklyTarget = recent7Days.size * targetMinutesPerDay
        val weeklyActual = recent7Days.sumOf { it.durationMinutes }
        val sleepDebtMinutes = (weeklyTarget - weeklyActual).coerceAtLeast(0)
        
        // 잠금 스트릭: 어제부터 역순으로 isLockUsed 체크 (데이터가 연속적이라고 가정)
        var streak = 0
        records.reversed().forEach { 
            if (it.isLockUsed) streak++ else return@forEach
        }

        return SleepStats(
            averageDurationMinutes = averageDurationMinutes,
            averageEfficiency = averageEfficiency,
            goalAchievementRate = goalAchievementRate,
            goalAchievedDays = goalAchievedDays,
            totalDays = count,
            dailyDurations = dailyDurations,
            bestRecord = bestRecord,
            worstRecord = worstRecord,
            sleepScore = sleepScore,
            sleepScoreGrade = sleepScoreGrade,
            sleepDebtMinutes = sleepDebtMinutes,
            lockStreak = streak,
            insightText = "일정한 시간에 취침하는 습관이 생겼습니다! 😊", // TODO: 고도화
            insightType = "CONSISTENCY",
            allRecords = records,
            monthlyRecords = records.associateBy { it.date }
        )
    }

    /**
     * 간이 수면 점수 계산 (각 25점 만점, 총 100점)
     * 1. 시간 충족도 (targetMinutes 대비)
     * 2. 수면 효율 (계산된 efficiency)
     * 3. 취침 일관성 (가상 점수)
     * 4. 잠금 앱 사용 여부 (25점 보너스)
     */
    private fun calculateSleepScore(records: List<SleepRecord>): Int {
        val latest = records.lastOrNull() ?: return 0
        
        val scoreTime = (minOf(latest.achievementRate, 100f) / 100f * 25).toInt()
        val scoreEff = (minOf(latest.sleepEfficiency, 100f) / 100f * 25).toInt()
        val scoreLock = if (latest.isLockUsed) 25 else 0
        val scoreConsistency = 20 // 임시 고정값
        
        return (scoreTime + scoreEff + scoreLock + scoreConsistency).coerceIn(0, 100)
    }
}
