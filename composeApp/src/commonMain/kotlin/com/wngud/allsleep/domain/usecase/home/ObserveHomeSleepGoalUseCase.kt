package com.wngud.allsleep.domain.usecase.home

import com.wngud.allsleep.domain.model.HomeSleepGoal
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ObserveHomeSleepGoalUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val todayProvider: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
) {
    operator fun invoke(): Flow<HomeSleepGoal> {
        return combine(
            sleepSettingsRepository.weekdayBedtime,
            sleepSettingsRepository.weekdayWakeTime,
            sleepSettingsRepository.weekendBedtime,
            sleepSettingsRepository.weekendWakeTime
        ) { weekdayBedtime, weekdayWakeTime, weekendBedtime, weekendWakeTime ->
            val today = todayProvider()
            val isWeekdayTonight = today.dayOfWeek.ordinal < 5
            val targetMinutes = if (isWeekdayTonight) {
                calculateTimeDiffMinutes(weekdayBedtime, weekdayWakeTime)
            } else {
                calculateTimeDiffMinutes(weekendBedtime, weekendWakeTime)
            }
            HomeSleepGoal(
                targetMinutes = targetMinutes,
                displayText = formatGoalDuration(targetMinutes),
            )
        }
    }

    private fun calculateTimeDiffMinutes(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }
            val startTotal = startParts[0] * 60 + startParts[1]
            var endTotal = endParts[0] * 60 + endParts[1]
            if (endTotal <= startTotal) endTotal += 24 * 60
            endTotal - startTotal
        } catch (_: Exception) {
            480
        }
    }

    private fun formatGoalDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
    }
}
