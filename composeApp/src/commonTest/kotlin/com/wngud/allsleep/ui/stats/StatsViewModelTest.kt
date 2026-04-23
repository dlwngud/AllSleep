package com.wngud.allsleep.ui.stats

import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSleepSettingsRepository : SleepSettingsRepository {
        override val weekdayBedtime: Flow<String> = flowOf("23:00")
        override val weekdayWakeTime: Flow<String> = flowOf("07:00")
        override val isWeekdaySleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekdayWakeEnabled: Flow<Boolean> = flowOf(true)
        override val weekendBedtime: Flow<String> = flowOf("23:00")
        override val weekendWakeTime: Flow<String> = flowOf("07:00")
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

    private class FakeSleepRecordRepository(
        private val records: List<SleepRecord> = emptyList()
    ) : SleepRecordRepository {
        override suspend fun saveSleepRecord(record: SleepRecord) = Result.success(Unit)
        override suspend fun getRecordsByMonth(uid: String, yearMonth: String) =
            Result.success(records.filter { it.date.startsWith(yearMonth) })
        override suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String) =
            Result.success(records.filter { it.date >= startDate && it.date <= endDate })
        override suspend fun getLatestRecord(uid: String) =
            Result.success(records.maxByOrNull { it.date })
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun loginWithKakao() = TODO()
        override suspend fun loginWithGoogle(context: PlatformContext) = TODO()
        override suspend fun loginWithEmail(email: String, password: String) = TODO()
        override suspend fun signUpWithEmail(email: String, password: String, name: String) = TODO()
        override suspend fun logout() = TODO()
        override suspend fun deleteAccount() = TODO()
        override suspend fun validateSession() = TODO()
        override suspend fun getCurrentUser() = User(
            uid = "test_uid",
            email = "test@test.com",
            displayName = "Tester",
            photoUrl = null,
            provider = AuthProvider.EMAIL,
            isPremium = false
        )
        override fun isLoggedIn() = true
        override fun observeUser() = TODO()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun isLoading_should_be_false_after_initial_load() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading, "isLoading should be false after data is loaded")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun weekly_summary_should_reflect_recent_records() = runTest {
        val records = recentRecords(durationMinutes = 480, days = 7)
        val viewModel = createViewModel(records)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(480, state.weeklyAverageMinutes)
        assertEquals(0, state.sleepDebtMinutes)
        assertEquals(7, state.achievementCount)
        assertTrue(state.sleepScore >= 90)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun sleep_debt_should_be_calculated_from_recent_records() = runTest {
        val records = recentRecords(durationMinutes = 300, days = 3)
        val viewModel = createViewModel(records)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(300, state.weeklyAverageMinutes)
        assertEquals(540, state.sleepDebtMinutes)
        assertEquals(SleepDebtLevel.WARNING, state.sleepDebtLevel)
    }

    private fun createViewModel(records: List<SleepRecord> = emptyList()): StatsViewModel {
        return StatsViewModel(
            sleepRecordRepository = FakeSleepRecordRepository(records),
            getCurrentUserUseCase = GetCurrentUserUseCase(FakeAuthRepository()),
            sleepSettingsRepository = FakeSleepSettingsRepository()
        )
    }

    private fun recentRecords(durationMinutes: Int, days: Int): List<SleepRecord> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return (0 until days).map { offset ->
            val date = today.minus(offset, DateTimeUnit.DAY).toString()
            SleepRecord(
                id = date,
                uid = "test_uid",
                date = date,
                bedtime = 0L,
                wakeTime = 0L,
                targetBedtime = "23:00",
                targetWakeTime = "07:00",
                targetMinutes = 480,
                durationMinutes = durationMinutes,
                sleepEfficiency = 92f,
                achievementRate = (durationMinutes / 480f * 100f).coerceAtMost(100f),
                isLockUsed = true
            )
        }
    }
}
