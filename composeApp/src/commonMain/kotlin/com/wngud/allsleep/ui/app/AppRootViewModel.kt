package com.wngud.allsleep.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.model.exception.PremiumRequiredException
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase
import com.wngud.allsleep.domain.usecase.auth.ValidateSessionUseCase
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveRegisteredDevicesUseCase
import com.wngud.allsleep.domain.usecase.sleep.RegisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UnregisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.subscription.SyncPremiumStatusUseCase
import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.DeviceInfoProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppRootViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeUserUseCase: ObserveUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val observeOnboardingCompletedUseCase: ObserveOnboardingCompletedUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val observeRegisteredDevicesUseCase: ObserveRegisteredDevicesUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val validateSessionUseCase: ValidateSessionUseCase,
    private val syncPremiumStatusUseCase: SyncPremiumStatusUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val billingProvider: BillingProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AppRootState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AppRootEffect>()
    val effect = _effect.receiveAsFlow()

    private var currentUid: String? = null
    private var devicesJob: Job? = null
    private var deviceRegisterJob: Job? = null
    private var hasBeenRegisteredInThisSession = false
    private var isReplacingDevice = false
    private var isLoggingOut = false

    init {
        handleIntent(AppRootIntent.RequestInitialize)
    }

    fun handleIntent(intent: AppRootIntent) {
        when (intent) {
            AppRootIntent.RequestInitialize -> initializeData()
            AppRootIntent.RequestLogout -> logout()
            AppRootIntent.RefreshPremiumStatus -> refreshPremiumStatus()
            AppRootIntent.ReplaceDevice -> forceReplaceDevice()
            AppRootIntent.CancelDeviceRegistration -> cancelDeviceRegistration()
            AppRootIntent.UpgradeToPremium -> upgradeToPremium()
        }
    }

    private fun initializeData() {
        viewModelScope.launch {
            val cachedIsPremium = sleepSettingsRepository.isPremium.first()
            _state.update { it.copy(isPremium = cachedIsPremium) }

            val initialOnboardingState = observeOnboardingCompletedUseCase().first()
            _state.update { it.copy(isOnboardingCompleted = initialOnboardingState) }

            val user = getCurrentUserUseCase()
            if (user != null) {
                validateSessionUseCase().onFailure {
                    logout()
                    _state.update { it.copy(isStateInitialized = true) }
                    return@launch
                }
            }

            handleUserSessionChange(user)
            if (user != null) {
                refreshPremiumStatus()
            }
            _state.update { it.copy(isStateInitialized = true) }

            setupAuthObserver()
            setupOnboardingObserver()
        }
    }

    private fun setupAuthObserver() {
        viewModelScope.launch {
            observeUserUseCase().collect { user -> handleUserSessionChange(user) }
        }
    }

    private fun setupOnboardingObserver() {
        viewModelScope.launch {
            observeOnboardingCompletedUseCase()
                .distinctUntilChanged()
                .collect { completed ->
                    _state.update { it.copy(isOnboardingCompleted = completed) }
                }
        }
    }

    private fun handleUserSessionChange(user: User?) {
        val wasPremium = _state.value.isPremium
        val isNowPremium = user?.isPremium ?: _state.value.isPremium
        _state.update { it.copy(currentUser = user, isPremium = isNowPremium) }
        user?.let { viewModelScope.launch { sleepSettingsRepository.savePremiumStatus(it.isPremium) } }

        if (!wasPremium && isNowPremium) {
            val pendingDevice = _state.value.cachedDeviceStateToRegister
            val uid = user?.uid
            if (pendingDevice != null && uid != null) {
                _state.update { it.copy(cachedDeviceStateToRegister = null, showDeviceLimitDialog = false) }
                viewModelScope.launch {
                    registerDeviceUseCase(uid, pendingDevice, isPremium = true)
                }
            }
        }

        if (user != null) {
            if (user.uid != currentUid) {
                currentUid = user.uid
                hasBeenRegisteredInThisSession = false
                viewModelScope.launch {
                    updateUserProfileUseCase(user)
                    billingProvider.loginUser(user.uid)
                }
                observeRegisteredDevices(user.uid)
                registerCurrentDevice(user.uid)
            }
        } else {
            currentUid = null
            devicesJob?.cancel()
            deviceRegisterJob?.cancel()
        }
    }

    private fun refreshPremiumStatus() {
        viewModelScope.launch {
            val isPremiumNow = syncPremiumStatusUseCase() ?: return@launch
            run {
                val currentUser = getCurrentUserUseCase()
                _state.update {
                    it.copy(
                        currentUser = currentUser?.copy(isPremium = isPremiumNow) ?: currentUser,
                        isPremium = isPremiumNow,
                    )
                }
            }
        }
    }

    private fun observeRegisteredDevices(uid: String) {
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            observeRegisteredDevicesUseCase(uid)
                .catch { handleRemoteSessionClosed() }
                .collect { devices ->
                    val currentId = deviceInfoProvider.getDeviceId()
                    if (devices.any { it.deviceId == currentId }) {
                        hasBeenRegisteredInThisSession = true
                    } else if (hasBeenRegisteredInThisSession && devices.isNotEmpty()) {
                        if (!isReplacingDevice) {
                            handleRemoteSessionClosed()
                        }
                    }
                }
        }
    }

    private fun registerCurrentDevice(uid: String) {
        deviceRegisterJob?.cancel()
        deviceRegisterJob = viewModelScope.launch {
            val cachedName = sleepSettingsRepository.deviceName.first()
            val deviceName = cachedName ?: deviceInfoProvider.getDeviceName()
            val deviceState = DeviceState(
                deviceId = deviceInfoProvider.getDeviceId(),
                deviceName = deviceName,
                fcmToken = deviceInfoProvider.getPushToken(),
                platform = deviceInfoProvider.getPlatform(),
                lastActiveForSleepLocking = 0L,
                isMainAlarmDevice = false,
            )

            registerDeviceUseCase(uid, deviceState, _state.value.isPremium).onFailure { e ->
                if (e is PremiumRequiredException) {
                    _state.update {
                        it.copy(
                            showDeviceLimitDialog = true,
                            cachedDeviceStateToRegister = deviceState,
                        )
                    }
                }
            }
        }
    }

    private fun forceReplaceDevice() {
        val uid = currentUid ?: return
        val deviceToRegister = _state.value.cachedDeviceStateToRegister ?: return
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }

        isReplacingDevice = true
        viewModelScope.launch {
            try {
                val devices = observeRegisteredDevicesUseCase(uid).first()
                val currentId = deviceInfoProvider.getDeviceId()
                devices.filter { it.deviceId != currentId }.forEach { device ->
                    unregisterDeviceUseCase(uid, device.deviceId)
                }

                registerDeviceUseCase(uid, deviceToRegister, isPremium = true).onFailure { e ->
                    isReplacingDevice = false
                    _state.update { it.copy(error = "기기 교체 실패: ${e.message}") }
                    return@launch
                }
                isReplacingDevice = false
            } catch (e: Exception) {
                isReplacingDevice = false
                _state.update { it.copy(error = "기기 교체 중 오류: ${e.message}") }
            }
        }
    }

    private fun cancelDeviceRegistration() {
        if (isReplacingDevice) return
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }
        handleIntent(AppRootIntent.RequestLogout)
    }

    private fun upgradeToPremium() {
        _state.update { it.copy(showDeviceLimitDialog = false) }
        viewModelScope.launch {
            _effect.send(AppRootEffect.NavigateToSubscription)
        }
    }

    private fun handleRemoteSessionClosed() {
        if (isLoggingOut) return
        logout()
    }

    private fun logout() {
        if (isLoggingOut) return
        val uid = currentUid ?: return
        isLoggingOut = true
        devicesJob?.cancel()
        deviceRegisterJob?.cancel()
        viewModelScope.launch {
            unregisterDeviceUseCase(uid, deviceInfoProvider.getDeviceId())
                .onFailure { e -> println("[AppRoot] 로그아웃 중 기기 등록 해제 실패: ${e.message}") }
            signOutUseCase()
                .onFailure { e -> println("[AppRoot] 로그아웃 실패: ${e.message}") }
            runCatching { billingProvider.logoutUser() }
            currentUid = null
            hasBeenRegisteredInThisSession = false
            _state.update { it.copy(currentUser = null) }
            isLoggingOut = false
        }
    }
}
