package com.wngud.allsleep.domain.usecase.stats

import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.stats.SleepDebtLevel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateSleepReportUseCaseTest {

    private val today = LocalDate(2026, 6, 23)
    private val useCase = CalculateSleepReportUseCase(
        todayProvider = { today },
        timeZoneProvider = { TimeZone.UTC },
    )

    @Test
    fun empty_records_should_keep_existing_empty_summary() {
        val report = useCase(records = emptyList(), currentTargetMinutes = 480)

        assertEquals(0, report.weeklyAverageMinutes)
        assertEquals(0, report.sleepScore)
        assertEquals(0, report.sleepDebtMinutes)
        assertEquals(SleepDebtLevel.GOOD, report.sleepDebtLevel)
        assertEquals("수면 모드를 사용하면 분석이 시작돼요", report.scoreLabel)
        assertEquals("3일 이상 기록하면 수면 패턴을 더 정확히 분석할 수 있어요.", report.aiMessage)
    }

    @Test
    fun target_met_records_should_match_existing_summary_formula() {
        val records = recentRecords(durationMinutes = 480, days = 7)

        val report = useCase(records = records, currentTargetMinutes = 480)

        assertEquals(480, report.weeklyAverageMinutes)
        assertEquals(0, report.sleepDebtMinutes)
        assertEquals(SleepDebtLevel.GOOD, report.sleepDebtLevel)
        assertEquals(7, report.achievementCount)
        assertTrue(report.sleepScore >= 90)
        assertEquals("이번 주 수면 리듬이 안정적이에요. 지금 패턴을 유지하면 좋아요.", report.aiMessage)
    }

    @Test
    fun under_target_records_should_calculate_existing_sleep_debt() {
        val records = recentRecords(durationMinutes = 300, days = 3)

        val report = useCase(records = records, currentTargetMinutes = 480)

        assertEquals(300, report.weeklyAverageMinutes)
        assertEquals(540, report.sleepDebtMinutes)
        assertEquals(SleepDebtLevel.WARNING, report.sleepDebtLevel)
    }

    private fun recentRecords(durationMinutes: Int, days: Int): List<SleepRecord> {
        return (0 until days).map { offset ->
            val date = today.minus(offset, DateTimeUnit.DAY)
            SleepRecord(
                id = date.toString(),
                uid = "test_uid",
                date = date.toString(),
                bedtime = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                wakeTime = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() + durationMinutes * 60_000L,
                targetBedtime = "23:00",
                targetWakeTime = "07:00",
                targetMinutes = 480,
                durationMinutes = durationMinutes,
                sleepEfficiency = 92f,
                achievementRate = (durationMinutes / 480f * 100f).coerceAtMost(100f),
                isLockUsed = true,
            )
        }
    }
}
