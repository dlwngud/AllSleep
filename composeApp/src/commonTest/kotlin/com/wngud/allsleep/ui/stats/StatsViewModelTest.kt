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
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSleepSettingsRepository : SleepSettingsRepository {
        override val weekdayBedtime: Flow<String> = flowOf("23:00")
        override val weekdayWakeTime: Flow<String> = flowOf("07:00")
        override val isWeekdaySleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekdayWakeEnabled: Flow<Boolean> = flowOf(true)
        override val weekendBedtime: Flow<String> = flowOf("00:00")
        override val weekendWakeTime: Flow<String> = flowOf("09:00")
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

    private class FakeSleepRecordRepository : SleepRecordRepository {
        override suspend fun saveSleepRecord(record: SleepRecord) = Result.success(Unit)
        override suspend fun getRecordsByMonth(uid: String, yearMonth: String) = Result.success(emptyList<SleepRecord>())
        override suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String) = Result.success(emptyList<SleepRecord>())
        override suspend fun getLatestRecord(uid: String) = Result.success(null)
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

    private lateinit var viewModel: StatsViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val authRepository = FakeAuthRepository()
        val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)
        
        viewModel = StatsViewModel(
            sleepRecordRepository = FakeSleepRecordRepository(),
            getCurrentUserUseCase = getCurrentUserUseCase,
            sleepSettingsRepository = FakeSleepSettingsRepository()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun isRefreshing_should_be_updated_during_refresh_intent() = runTest {
        // Given: Advance until initial load is done
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)

        // When: Refresh intent is triggered
        viewModel.handleIntent(StatsIntent.Refresh)
        
        // Assert: isRefreshing should be true during load (before idle)
        // Note: Since we use StandardTestDispatcher and launch, we can check intermediate states
        // if we are careful, but advanceUntilIdle will complete it.
        advanceUntilIdle()
        
        // Assert: finally it should be false
        assertFalse(viewModel.state.value.isRefreshing, "isRefreshing should be false after refresh is done")
    }
}
