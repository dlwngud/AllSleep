package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import kotlin.math.min

/**
 * 수면 종료 시 수면 데이터를 계산하고 저장하는 UseCase
 */
class RecordSleepSessionUseCase(
    private val repository: SleepRecordRepository
) {
    /**
     * @param uid 사용자 ID
     * @param date 기록 날짜 ("yyyy-MM-dd")
     * @param sleepStartAt 수면 시작 시각 (ms)
     * @param wakeTimeMs 수면 종료(기상) 시각 (ms)
     * @param targetBedtime 목표 취침 시간 ("HH:mm")
     * @param targetWakeTime 목표 기상 시간 ("HH:mm")
     * @param isLockUsed 앱 잠금 사용 여부
     */
    suspend operator fun invoke(
        uid: String,
        date: String,
        sleepStartAt: Long,
        wakeTimeMs: Long,
        targetBedtime: String,
        targetWakeTime: String,
        isLockUsed: Boolean
    ): Result<Unit> {
        return try {
            val durationMinutes = ((wakeTimeMs - sleepStartAt) / 60000).toInt().coerceAtLeast(0)
            val targetMinutes = calculateTimeDiffMinutes(targetBedtime, targetWakeTime)
            
            // 달성률: (실제 수면 시간 / 목표 수면 시간) * 100
            val achievementRate = if (targetMinutes > 0) {
                min(durationMinutes.toFloat() / targetMinutes.toFloat(), 1.0f) * 100f
            } else 0f
            
            // 수면 효율: (목표 시간 / 실제 수면 시간) * 100 (최대 100%)
            // (실제로 더 많이 잤더라도 목표 대비 효율로 계산하거나, 단순 수식 적용)
            val sleepEfficiency = if (durationMinutes > 0) {
                min(targetMinutes.toFloat() / durationMinutes.toFloat(), 1.0f) * 100f
            } else 0f

            val record = SleepRecord(
                id = date,
                uid = uid,
                date = date,
                bedtime = sleepStartAt,
                wakeTime = wakeTimeMs,
                targetBedtime = targetBedtime,
                targetWakeTime = targetWakeTime,
                targetMinutes = targetMinutes,
                durationMinutes = durationMinutes,
                sleepEfficiency = sleepEfficiency,
                achievementRate = achievementRate,
                isLockUsed = isLockUsed
            )

            repository.saveSleepRecord(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * "HH:mm" 형식의 두 시간 차이를 분 단위로 계산 (취침~기상)
     * 예: "23:00", "07:00" -> 480분 (8시간)
     */
    private fun calculateTimeDiffMinutes(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }
            
            val startTotal = startParts[0] * 60 + startParts[1]
            var endTotal = endParts[0] * 60 + endParts[1]
            
            if (endTotal <= startTotal) {
                endTotal += 24 * 60 // 다음날인 경우
            }
            
            endTotal - startTotal
        } catch (e: Exception) {
            480 // 파싱 실패 시 기본 8시간
        }
    }
}
