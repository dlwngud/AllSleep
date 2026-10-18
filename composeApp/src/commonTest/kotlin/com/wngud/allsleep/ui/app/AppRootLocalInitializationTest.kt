package com.wngud.allsleep.ui.app

import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.data.repository.*
import com.wngud.allsleep.domain.model.*
import com.wngud.allsleep.domain.repository.*
import com.wngud.allsleep.domain.usecase.auth.*
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import com.wngud.allsleep.domain.usecase.sleep.*
import com.wngud.allsleep.domain.usecase.subscription.SyncPremiumStatusUseCase
import com.wngud.allsleep.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppRootLocalInitializationTest {
    @Test fun remoteQueryThatNeverReturnsDoesNotBlockLocalEntry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val auth = OfflineAuth(hangs = true)
        val vm = createRoot(auth)
        try {
            runCurrent()
            assertTrue(vm.state.value.isStateInitialized)
            assertNull(vm.state.value.currentUser)
            assertNull(vm.state.value.localInitializationError)
            assertEquals(0, auth.logouts)
            assertEquals(0, auth.validations)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun authAndServiceFailureLeaveGuestLocalEntryAvailable() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val auth = OfflineAuth(hangs = false)
        val vm = createRoot(auth)
        try {
            runCurrent()
            assertTrue(vm.state.value.isStateInitialized)
            assertNull(vm.state.value.localInitializationError)
            assertNotNull(vm.state.value.error)
            assertEquals(0, auth.logouts)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun lateAccountQueryCannotRestoreAnObservedSignedOutSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val response = CompletableDeferred<User?>()
        val observedUser = MutableStateFlow<User?>(null)
        val auth = object : AuthRepository by OfflineAuth(hangs = true) {
            override suspend fun getCurrentUser() = response.await()
            override fun observeUser(): Flow<User?> = observedUser
        }
        val vm = createRoot(auth)
        try {
            runCurrent()
            assertTrue(vm.state.value.isStateInitialized)
            response.complete(User("stale-account", null, "Stale account", null, AuthProvider.EMAIL))
            runCurrent()
            assertNull(vm.state.value.currentUser)
            assertNull(vm.state.value.localInitializationError)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    private fun createRoot(auth: AuthRepository): AppRootViewModel {
        val store = MemoryPreferencesStore()
        val settings = SleepSettingsRepositoryImpl(store)
        val remote = OfflineSync()
        val billing = object : BillingProvider {
            override suspend fun getOfferings() = emptyList<SubscriptionPackage>()
            override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> = Result.failure(IllegalStateException("offline"))
            override suspend fun purchasePackage(packageId: String, context: PlatformContext): Result<PurchaseResult> = error("Must not purchase")
            override suspend fun restorePurchases(): Result<Unit> = error("Must not restore")
            override suspend fun loginUser(uid: String) = Unit
            override suspend fun logoutUser() = Unit
        }
        val device = object : DeviceInfoProvider {
            override fun getDeviceId() = "test-device"
            override fun getDeviceName() = "Test"
            override fun getPlatform() = "Android"
            override suspend fun getPushToken() = ""
        }
        return AppRootViewModel(
            GetCurrentUserUseCase(auth), ObserveUserUseCase(auth), UpdateUserProfileUseCase(remote),
            ObserveOnboardingCompletedUseCase(settings), RegisterDeviceUseCase(remote), ObserveRegisteredDevicesUseCase(remote),
            UnregisterDeviceUseCase(remote), SignOutUseCase(auth), ValidateSessionUseCase(auth),
            SyncPremiumStatusUseCase(BillingRepositoryImpl(billing), GetCurrentUserUseCase(auth), UpdateUserProfileUseCase(remote), settings),
            settings, billing, device, LocalBedtimeRepositoryImpl(store) { "guest:offline" },
        )
    }
}

private class OfflineAuth(private val hangs: Boolean) : AuthRepository {
    var logouts = 0
    var validations = 0
    override suspend fun getCurrentUser(): User? = if (hangs) awaitCancellation() else error("offline account service")
    override fun observeUser(): Flow<User?> = if (hangs) emptyFlow() else flow { error("offline observer") }
    override fun isLoggedIn() = false
    override suspend fun logout(): Result<Unit> { logouts++; return Result.success(Unit) }
    override suspend fun validateSession(): Result<Unit> { validations++; return Result.failure(IllegalStateException("offline")) }
    override suspend fun deleteAccount(): Result<Unit> = error("Must not delete")
    override suspend fun loginWithKakao(): Result<User> = error("Must not login")
    override suspend fun loginWithGoogle(context: PlatformContext): Result<User> = error("Must not login")
    override suspend fun loginWithEmail(email: String, password: String): Result<User> = error("Must not login")
    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> = error("Must not signup")
}

private class OfflineSync : SleepSyncRepository {
    override fun observeUserSleepState(uid: String): Flow<UserSleepState?> = error("Must not observe remote guest state")
    override suspend fun updateUserSleepState(uid: String, isSleeping: Boolean?, targetWakeUpTime: Long?, weekdayBedtime: String?, weekdayWakeTime: String?, isWeekdaySleepEnabled: Boolean?, isWeekdayWakeEnabled: Boolean?, weekendBedtime: String?, weekendWakeTime: String?, isWeekendSleepEnabled: Boolean?, isWeekendWakeEnabled: Boolean?): Result<Unit> = error("Must not update remote guest state")
    override suspend fun updateUserProfile(user: User): Result<Unit> = error("Must not write guest profile")
    override suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit> = error("Must not register guest")
    override suspend fun getRegisteredDevices(uid: String): Result<List<DeviceState>> = error("Must not query devices")
    override suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit> = error("Must not set remote alarm")
    override suspend fun unregisterDevice(uid: String, deviceId: String): Result<Unit> = error("Must not unregister guest")
    override fun observeRegisteredDevices(uid: String): Flow<List<DeviceState>> = error("Must not observe devices")
}
