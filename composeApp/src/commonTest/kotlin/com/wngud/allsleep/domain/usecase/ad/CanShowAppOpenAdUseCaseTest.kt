package com.wngud.allsleep.domain.usecase.ad

import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import com.wngud.allsleep.platform.PlatformContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanShowAppOpenAdUseCaseTest {
    @Test
    fun not_logged_in_user_cannot_show_app_open_ad() = runTest {
        val useCase = createUseCase(user = null, isOnboardingCompleted = true)

        assertFalse(useCase())
    }

    @Test
    fun premium_user_cannot_show_app_open_ad() = runTest {
        val useCase = createUseCase(
            user = testUser(isPremium = true),
            isOnboardingCompleted = true,
        )

        assertFalse(useCase())
    }

    @Test
    fun onboarding_incomplete_free_user_cannot_show_app_open_ad() = runTest {
        val useCase = createUseCase(
            user = testUser(isPremium = false),
            isOnboardingCompleted = false,
        )

        assertFalse(useCase())
    }

    @Test
    fun onboarding_completed_free_user_can_show_app_open_ad() = runTest {
        val useCase = createUseCase(
            user = testUser(isPremium = false),
            isOnboardingCompleted = true,
        )

        assertTrue(useCase())
    }

    private fun createUseCase(
        user: User?,
        isOnboardingCompleted: Boolean,
    ): CanShowAppOpenAdUseCase {
        return CanShowAppOpenAdUseCase(
            getCurrentUserUseCase = GetCurrentUserUseCase(FakeAuthRepository(user)),
            observeOnboardingCompletedUseCase = ObserveOnboardingCompletedUseCase(
                FakeSleepSettingsRepository(isOnboardingCompleted),
            ),
        )
    }

    private fun testUser(isPremium: Boolean): User {
        return User(
            uid = "test_uid",
            email = "test@example.com",
            displayName = "Tester",
            photoUrl = null,
            provider = AuthProvider.EMAIL,
            isPremium = isPremium,
        )
    }

    private class FakeAuthRepository(
        private val currentUser: User?,
    ) : AuthRepository {
        override suspend fun loginWithKakao(): Result<User> = error("Not used")
        override suspend fun loginWithGoogle(context: PlatformContext): Result<User> = error("Not used")
        override suspend fun loginWithEmail(email: String, password: String): Result<User> = error("Not used")
        override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> = error("Not used")
        override suspend fun logout(): Result<Unit> = error("Not used")
        override suspend fun deleteAccount(): Result<Unit> = error("Not used")
        override suspend fun validateSession(): Result<Unit> = error("Not used")
        override suspend fun getCurrentUser(): User? = currentUser
        override fun isLoggedIn(): Boolean = currentUser != null
        override fun observeUser(): Flow<User?> = flowOf(currentUser)
    }

    private class FakeSleepSettingsRepository(
        isOnboardingCompletedValue: Boolean,
    ) : SleepSettingsRepository {
        override val weekdayBedtime: Flow<String> = flowOf("23:00")
        override val weekdayWakeTime: Flow<String> = flowOf("07:00")
        override val isWeekdaySleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekdayWakeEnabled: Flow<Boolean> = flowOf(true)
        override val weekendBedtime: Flow<String> = flowOf("23:00")
        override val weekendWakeTime: Flow<String> = flowOf("07:00")
        override val isWeekendSleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekendWakeEnabled: Flow<Boolean> = flowOf(true)
        override val isOnboardingCompleted: Flow<Boolean> = flowOf(isOnboardingCompletedValue)
        override val deviceName: Flow<String?> = flowOf(null)
        override val isPremium: Flow<Boolean> = flowOf(false)
        override val activeSleepStartAt: Flow<Long> = flowOf(0L)
        override val lastAppOpenAdShownAt: Flow<Long> = flowOf(0L)
        override suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String) = Unit
        override suspend fun saveWeekdaySleepEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekdayWakeEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String) = Unit
        override suspend fun saveWeekendSleepEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekendWakeEnabled(enabled: Boolean) = Unit
        override suspend fun saveOnboardingCompleted(completed: Boolean) = Unit
        override suspend fun saveDeviceName(name: String) = Unit
        override suspend fun savePremiumStatus(isPremium: Boolean) = Unit
        override suspend fun saveActiveSleepStartAt(startTime: Long) = Unit
        override suspend fun saveLastAppOpenAdShownAt(time: Long) = Unit
        override suspend fun clear() = Unit
    }
}
