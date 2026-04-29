package com.wngud.allsleep.ui.subscription

import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.PackageType
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.PurchaseResult
import com.wngud.allsleep.platform.SubscriptionPackage
import com.wngud.allsleep.platform.SubscriptionStatus
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SubscriptionViewModel
    private lateinit var billingProvider: FakeBillingProvider
    private lateinit var sleepSyncRepository: FakeSleepSyncRepository
    private lateinit var sleepSettingsRepository: FakeSleepSettingsRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        billingProvider = FakeBillingProvider()
        sleepSyncRepository = FakeSleepSyncRepository()
        sleepSettingsRepository = FakeSleepSettingsRepository()
        viewModel = SubscriptionViewModel(
            billingProvider = billingProvider,
            getCurrentUserUseCase = GetCurrentUserUseCase(FakeAuthRepository()),
            updateUserProfileUseCase = UpdateUserProfileUseCase(sleepSyncRepository),
            sleepSettingsRepository = sleepSettingsRepository
        )
    }

    @Test
    fun `LoadPackages 성공 시 ANNUAL 플랜이 기본 선택되어야 한다`() = runTest {
        // Given
        val packages = listOf(
            subscriptionPackage("m", PackageType.MONTHLY),
            subscriptionPackage("a", PackageType.ANNUAL, badge = "인기", subDescription = "34% 할인", hasFreeTrial = true, freeTrialDays = 7),
            subscriptionPackage("l", PackageType.LIFETIME, badge = "추천", subDescription = "영구")
        )
        billingProvider.packages = packages

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(3, state.packages.size)
        assertEquals("a", state.selectedPackageId) // ANNUAL (id: "a") 가 자동 선택됨
        assertEquals(null, state.error)
    }

    @Test
    fun `SelectPackage 호출 시 선택된 ID가 업데이트되어야 한다`() = runTest {
        // Given
        val packages = listOf(
            subscriptionPackage("m", PackageType.MONTHLY),
            subscriptionPackage("l", PackageType.LIFETIME, badge = "추천", subDescription = "영구")
        )
        billingProvider.packages = packages
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        advanceUntilIdle()

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.SelectPackage("l"))

        // Then
        assertEquals("l", viewModel.state.value.selectedPackageId)
    }

    @Test
    fun `복원 성공 후 활성 구독이면 앱 상태와 캐시에 프리미엄이 반영되어야 한다`() = runTest {
        // Given
        billingProvider.subscriptionStatusResult = Result.success(
            SubscriptionStatus(
                isPremiumActive = true,
                entitlementId = "premium",
                productIdentifier = "allsleep_premium_annual",
                willRenew = true
            )
        )

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.RestorePurchases)
        advanceUntilIdle()

        // Then
        assertEquals(1, billingProvider.restoreCalls)
        assertEquals(1, billingProvider.subscriptionStatusCalls)
        assertEquals(listOf(true), sleepSyncRepository.updatedPremiumStatuses)
        assertEquals(listOf(true), sleepSettingsRepository.savedPremiumStatuses)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `복원 성공 후 구독이 비활성화되면 앱 상태를 false로 유지해야 한다`() = runTest {
        // Given
        billingProvider.subscriptionStatusResult = Result.success(
            SubscriptionStatus(
                isPremiumActive = false,
                entitlementId = null,
                productIdentifier = null,
                willRenew = false
            )
        )

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.RestorePurchases)
        advanceUntilIdle()

        // Then
        assertEquals(1, billingProvider.restoreCalls)
        assertEquals(1, billingProvider.subscriptionStatusCalls)
        assertEquals(listOf(false), sleepSettingsRepository.savedPremiumStatuses)
        assertTrue(sleepSyncRepository.updatedPremiumStatuses.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }
}

class FakeBillingProvider : BillingProvider {
    var packages: List<SubscriptionPackage> = emptyList()
    var purchaseResult: Result<PurchaseResult> = Result.success(PurchaseResult(true, true))
    var subscriptionStatusResult: Result<SubscriptionStatus> = Result.success(
        SubscriptionStatus(
            isPremiumActive = true,
            entitlementId = "premium",
            productIdentifier = "allsleep_premium_annual"
        )
    )
    var restoreCalls = 0
    var subscriptionStatusCalls = 0
    var purchaseCalls = 0
    var lastPurchasedPackageId: String? = null

    override suspend fun getOfferings(): List<SubscriptionPackage> = packages

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        subscriptionStatusCalls += 1
        return subscriptionStatusResult
    }
    
    override suspend fun purchasePackage(packageId: String, context: PlatformContext): Result<PurchaseResult> {
        purchaseCalls += 1
        lastPurchasedPackageId = packageId
        return purchaseResult
    }
    
    override suspend fun restorePurchases(): Result<Unit> {
        restoreCalls += 1
        return Result.success(Unit)
    }
    
    override suspend fun loginUser(uid: String) {}
    
    override suspend fun logoutUser() {}
}

private class FakeAuthRepository : AuthRepository {
    private var currentUser: User? = User(
        uid = "uid",
        email = "test@example.com",
        displayName = "tester",
        photoUrl = null,
        provider = AuthProvider.EMAIL,
        isPremium = false
    )

    override suspend fun loginWithKakao(): Result<User> = Result.success(currentUser!!)
    override suspend fun loginWithGoogle(context: PlatformContext): Result<User> = Result.success(currentUser!!)
    override suspend fun loginWithEmail(email: String, password: String): Result<User> = Result.success(currentUser!!)
    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> = Result.success(currentUser!!)
    override suspend fun logout(): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override suspend fun validateSession(): Result<Unit> = Result.success(Unit)
    override suspend fun getCurrentUser(): User? = currentUser
    override fun isLoggedIn(): Boolean = currentUser != null
    override fun observeUser(): Flow<User?> = flowOf(currentUser)
}

private class FakeSleepSyncRepository : SleepSyncRepository {
    val updatedPremiumStatuses = mutableListOf<Boolean>()

    override fun observeUserSleepState(uid: String) = flowOf(null)
    override suspend fun updateUserSleepState(
        uid: String,
        isSleeping: Boolean?,
        targetWakeUpTime: Long?,
        weekdayBedtime: String?,
        weekdayWakeTime: String?,
        isWeekdaySleepEnabled: Boolean?,
        isWeekdayWakeEnabled: Boolean?,
        weekendBedtime: String?,
        weekendWakeTime: String?,
        isWeekendSleepEnabled: Boolean?,
        isWeekendWakeEnabled: Boolean?
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        updatedPremiumStatuses += user.isPremium
        return Result.success(Unit)
    }
    override suspend fun registerDevice(uid: String, deviceState: com.wngud.allsleep.domain.model.DeviceState): Result<Unit> = Result.success(Unit)
    override suspend fun getRegisteredDevices(uid: String): Result<List<com.wngud.allsleep.domain.model.DeviceState>> = Result.success(emptyList())
    override suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun unregisterDevice(uid: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override fun observeRegisteredDevices(uid: String): Flow<List<com.wngud.allsleep.domain.model.DeviceState>> = flowOf(emptyList())
}

private class FakeSleepSettingsRepository : com.wngud.allsleep.domain.repository.SleepSettingsRepository {
    val savedPremiumStatuses = mutableListOf<Boolean>()

    override val weekdayBedtime = flowOf("23:00")
    override val weekdayWakeTime = flowOf("07:00")
    override val isWeekdaySleepEnabled = flowOf(true)
    override val isWeekdayWakeEnabled = flowOf(true)
    override val weekendBedtime = flowOf("00:00")
    override val weekendWakeTime = flowOf("09:00")
    override val isWeekendSleepEnabled = flowOf(true)
    override val isWeekendWakeEnabled = flowOf(true)
    override val isOnboardingCompleted = flowOf(true)
    override val deviceName = flowOf(null)
    override val isPremium = flowOf(false)
    override val activeSleepStartAt = flowOf(0L)
    override val lastAppOpenAdShownAt = flowOf(0L)

    override suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String) {}
    override suspend fun saveWeekdaySleepEnabled(enabled: Boolean) {}
    override suspend fun saveWeekdayWakeEnabled(enabled: Boolean) {}
    override suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String) {}
    override suspend fun saveWeekendSleepEnabled(enabled: Boolean) {}
    override suspend fun saveWeekendWakeEnabled(enabled: Boolean) {}
    override suspend fun saveOnboardingCompleted(completed: Boolean) {}
    override suspend fun saveDeviceName(name: String) {}
    override suspend fun savePremiumStatus(isPremium: Boolean) {
        savedPremiumStatuses += isPremium
    }
    override suspend fun saveActiveSleepStartAt(startTime: Long) {}
    override suspend fun saveLastAppOpenAdShownAt(time: Long) {}
    override suspend fun clear() {}
}

private fun subscriptionPackage(
    id: String,
    type: PackageType,
    badge: String? = null,
    subDescription: String? = null,
    hasFreeTrial: Boolean = false,
    freeTrialDays: Int = 0
): SubscriptionPackage {
    val title = when (type) {
        PackageType.MONTHLY -> "월"
        PackageType.ANNUAL -> "년"
        PackageType.LIFETIME -> "평생"
        PackageType.UNKNOWN -> "패키지"
    }

    val priceString = when (type) {
        PackageType.MONTHLY -> "₩4,900"
        PackageType.ANNUAL -> "₩39,000"
        PackageType.LIFETIME -> "₩99,000"
        PackageType.UNKNOWN -> "₩0"
    }

    return SubscriptionPackage(
        id = id,
        title = title,
        priceString = priceString,
        type = type,
        productId = "",
        badge = badge,
        subDescription = subDescription,
        hasFreeTrial = hasFreeTrial,
        freeTrialDays = freeTrialDays
    )
}
