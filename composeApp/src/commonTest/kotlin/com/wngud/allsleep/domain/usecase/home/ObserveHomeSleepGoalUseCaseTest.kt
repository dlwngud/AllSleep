package com.wngud.allsleep.domain.usecase.home

import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveHomeSleepGoalUseCaseTest {

    @Test
    fun weekday_should_use_weekday_schedule() = runTest {
        val useCase = ObserveHomeSleepGoalUseCase(
            sleepSettingsRepository = FakeSleepSettingsRepository(
                weekdayBedtimeValue = "23:00",
                weekdayWakeTimeValue = "07:00",
                weekendBedtimeValue = "01:00",
                weekendWakeTimeValue = "09:30",
            ),
            todayProvider = { LocalDate(2026, 6, 22) },
        )

        val result = useCase().first()

        assertEquals(480, result.targetMinutes)
        assertEquals("8h", result.displayText)
    }

    @Test
    fun weekend_should_use_weekend_schedule() = runTest {
        val useCase = ObserveHomeSleepGoalUseCase(
            sleepSettingsRepository = FakeSleepSettingsRepository(
                weekdayBedtimeValue = "23:00",
                weekdayWakeTimeValue = "07:00",
                weekendBedtimeValue = "01:00",
                weekendWakeTimeValue = "09:30",
            ),
            todayProvider = { LocalDate(2026, 6, 27) },
        )

        val result = useCase().first()

        assertEquals(510, result.targetMinutes)
        assertEquals("8h 30m", result.displayText)
    }

    @Test
    fun invalid_time_should_keep_existing_fallback() = runTest {
        val useCase = ObserveHomeSleepGoalUseCase(
            sleepSettingsRepository = FakeSleepSettingsRepository(
                weekdayBedtimeValue = "invalid",
                weekdayWakeTimeValue = "07:00",
            ),
            todayProvider = { LocalDate(2026, 6, 22) },
        )

        val result = useCase().first()

        assertEquals(480, result.targetMinutes)
        assertEquals("8h", result.displayText)
    }

    private class FakeSleepSettingsRepository(
        weekdayBedtimeValue: String = "23:00",
        weekdayWakeTimeValue: String = "07:00",
        weekendBedtimeValue: String = "23:00",
        weekendWakeTimeValue: String = "07:00",
    ) : SleepSettingsRepository {
        override val weekdayBedtime: Flow<String> = flowOf(weekdayBedtimeValue)
        override val weekdayWakeTime: Flow<String> = flowOf(weekdayWakeTimeValue)
        override val isWeekdaySleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekdayWakeEnabled: Flow<Boolean> = flowOf(true)
        override val weekendBedtime: Flow<String> = flowOf(weekendBedtimeValue)
        override val weekendWakeTime: Flow<String> = flowOf(weekendWakeTimeValue)
        override val isWeekendSleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekendWakeEnabled: Flow<Boolean> = flowOf(true)
        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val deviceName: Flow<String?> = flowOf(null)
        override val isPremium: Flow<Boolean> = flowOf(false)
        override val activeSleepStartAt: Flow<Long> = flowOf(0L)
        override val lastAppOpenAdShownAt: Flow<Long> = flowOf(0L)
        override suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String) {}
        override suspend fun saveWeekdaySleepEnabled(enabled: Boolean) {}
        override suspend fun saveWeekdayWakeEnabled(enabled: Boolean) {}
        override suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String) {}
        override suspend fun saveWeekendSleepEnabled(enabled: Boolean) {}
        override suspend fun saveWeekendWakeEnabled(enabled: Boolean) {}
        override suspend fun saveOnboardingCompleted(completed: Boolean) {}
        override suspend fun saveDeviceName(name: String) {}
        override suspend fun savePremiumStatus(isPremium: Boolean) {}
        override suspend fun saveActiveSleepStartAt(startTime: Long) {}
        override suspend fun saveLastAppOpenAdShownAt(time: Long) {}
        override suspend fun clear() {}
    }
}
