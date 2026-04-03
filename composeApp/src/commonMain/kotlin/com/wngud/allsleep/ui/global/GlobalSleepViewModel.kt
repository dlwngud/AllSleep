package com.wngud.allsleep.ui.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase
import com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import com.wngud.allsleep.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveRegisteredDevicesUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.sleep.RegisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UnregisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.platform.DeviceInfoProvider
import com.wngud.allsleep.platform.SleepScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * 전역 수면 상태(App State)를 관리하는 ViewModel (MVI)
 */
class GlobalSleepViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeUserUseCase: ObserveUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val observeOnboardingCompletedUseCase: ObserveOnboardingCompletedUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val observeUserSleepStateUseCase: ObserveUserSleepStateUseCase,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val observeRegisteredDevicesUseCase: ObserveRegisteredDevicesUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val validateSessionUseCase: com.wngud.allsleep.domain.usecase.auth.ValidateSessionUseCase,
    private val recordSleepSessionUseCase: com.wngud.allsleep.domain.usecase.sleep.RecordSleepSessionUseCase,
    private val sleepSettingsRepository: com.wngud.allsleep.domain.repository.SleepSettingsRepository,
    private val sleepScheduler: SleepScheduler,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSleepContract.State())
    val state: StateFlow<GlobalSleepContract.State> = _state.asStateFlow()

    private val _effect = kotlinx.coroutines.channels.Channel<GlobalSleepContract.Effect>()
    val effect = _effect.receiveAsFlow()
    
    private var currentUid: String? = null
    
    private var observeJob: Job? = null
    private var devicesJob: Job? = null
    private var deviceRegisterJob: Job? = null
    private var hasBeenRegisteredInThisSession = false

    init {
        handleIntent(GlobalSleepContract.Intent.RequestInitialize)
    }

    fun handleIntent(intent: GlobalSleepContract.Intent) {
        when (intent) {
            is GlobalSleepContract.Intent.RequestInitialize -> initializeData()
            is GlobalSleepContract.Intent.RequestLogout -> logout()
            is GlobalSleepContract.Intent.ToggleSleepState -> toggleSleepState(intent.isSleeping, intent.targetWakeUpTime)
            is GlobalSleepContract.Intent.CompleteOnboarding -> completeOnboarding(intent.bedtime, intent.wakeTime)
            is GlobalSleepContract.Intent.ReplaceDevice -> forceReplaceDevice()
            is GlobalSleepContract.Intent.CancelDeviceRegistration -> cancelDeviceRegistration()
            is GlobalSleepContract.Intent.UpgradeToPremium -> upgradeToPremium()
        }
    }

    private data class RoutineData(
        val bedtime: String,
        val wakeTime: String,
        val sleepEnabled: Boolean,
        val wakeEnabled: Boolean
    )

    private fun upgradeToPremium() {
        _state.update { it.copy(showDeviceLimitDialog = false) }
        viewModelScope.launch {
            _effect.send(GlobalSleepContract.Effect.NavigateToSubscription)
        }
    }

    private fun cancelDeviceRegistration() {
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }
        handleIntent(GlobalSleepContract.Intent.RequestLogout)
    }

    private fun forceReplaceDevice() {
        val uid = currentUid ?: return
        val deviceToRegister = _state.value.cachedDeviceStateToRegister ?: return
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }
        viewModelScope.launch {
            try {
                val devices = observeRegisteredDevicesUseCase(uid).first()
                devices.forEach { device -> unregisterDeviceUseCase(uid, device.deviceId) }
                registerDeviceUseCase(uid, deviceToRegister, isPremium = true).onFailure { e ->
                    _state.update { it.copy(error = "기기 교체 실패: ${e.message}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "기기 교체 중 오류: ${e.message}") }
            }
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
            _state.update { it.copy(isStateInitialized = true) }

            setupAuthObserver()
            setupOnboardingObserver()
            setupSettingsSync()
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
                    val wasCompleted = _state.value.isOnboardingCompleted
                    _state.update { it.copy(isOnboardingCompleted = completed) }
                    if (completed && !wasCompleted && currentUid != null) {
                        syncScheduleToFirestore()
                    }
                }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupSettingsSync() {
        viewModelScope.launch {
            val weekdayFlow = combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime,
                sleepSettingsRepository.isWeekdaySleepEnabled,
                sleepSettingsRepository.isWeekdayWakeEnabled
            ) { b, w, se, we -> RoutineData(b, w, se, we) }
            
            val weekendFlow = combine(
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime,
                sleepSettingsRepository.isWeekendSleepEnabled,
                sleepSettingsRepository.isWeekendWakeEnabled
            ) { b, w, se, we -> RoutineData(b, w, se, we) }

            combine(weekdayFlow, weekendFlow) { wd, we -> wd to we }
                .distinctUntilChanged()
                .debounce(500L)
                .collect { (wd, we) ->
                    val uid = currentUid ?: return@collect
                    val remoteState = _state.value.sleepState ?: return@collect
                    
                    val isChanged = remoteState.weekdayBedtime != wd.bedtime || 
                                    remoteState.weekdayWakeTime != wd.wakeTime ||
                                    remoteState.isWeekdaySleepEnabled != wd.sleepEnabled ||
                                    remoteState.isWeekdayWakeEnabled != wd.wakeEnabled ||
                                    remoteState.weekendBedtime != we.bedtime ||
                                    remoteState.weekendWakeTime != we.wakeTime ||
                                    remoteState.isWeekendSleepEnabled != we.sleepEnabled ||
                                    remoteState.isWeekendWakeEnabled != we.wakeEnabled

                    if (isChanged) {
                        updateUserSleepStateUseCase(
                            uid = uid,
                            weekdayBedtime = wd.bedtime,
                            weekdayWakeTime = wd.wakeTime,
                            isWeekdaySleepEnabled = wd.sleepEnabled,
                            isWeekdayWakeEnabled = wd.wakeEnabled,
                            weekendBedtime = we.bedtime,
                            weekendWakeTime = we.wakeTime,
                            isWeekendSleepEnabled = we.sleepEnabled,
                            isWeekendWakeEnabled = we.wakeEnabled
                        )
                    }
                }
        }
    }

    private suspend fun syncScheduleToFirestore() {
        val uid = currentUid ?: return
        updateUserSleepStateUseCase(
            uid = uid,
            weekdayBedtime = sleepSettingsRepository.weekdayBedtime.first(),
            weekdayWakeTime = sleepSettingsRepository.weekdayWakeTime.first(),
            isWeekdaySleepEnabled = sleepSettingsRepository.isWeekdaySleepEnabled.first(),
            isWeekdayWakeEnabled = sleepSettingsRepository.isWeekdayWakeEnabled.first(),
            weekendBedtime = sleepSettingsRepository.weekendBedtime.first(),
            weekendWakeTime = sleepSettingsRepository.weekendWakeTime.first(),
            isWeekendSleepEnabled = sleepSettingsRepository.isWeekendSleepEnabled.first(),
            isWeekendWakeEnabled = sleepSettingsRepository.isWeekendWakeEnabled.first()
        )
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
                viewModelScope.launch { updateUserProfileUseCase(user) }
                startObserving(user.uid)
                registerCurrentUIDDevice(user.uid)
            }
        } else {
            currentUid = null
            _state.update { it.copy(sleepState = null, registeredDevices = emptyList()) }
            observeJob?.cancel()
            devicesJob?.cancel()
        }
    }

    private fun startObserving(uid: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeUserSleepStateUseCase(uid)
                .catch { logout() }
                .collect { state ->
                    val oldState = _state.value.sleepState
                    if (state == null && currentUid != null) {
                        logout()
                        return@collect
                    }
                    
                    if (oldState?.isSleeping == true && state?.isSleeping == false) {
                        launch {
                            val sleepStartAt = sleepSettingsRepository.activeSleepStartAt.first()
                            if (sleepStartAt > 0L) {
                                val currentIsWeekday = isWeekday()
                                val targetBedtime = if (currentIsWeekday) sleepSettingsRepository.weekdayBedtime.first() else sleepSettingsRepository.weekendBedtime.first()
                                val targetWakeTime = if (currentIsWeekday) sleepSettingsRepository.weekdayWakeTime.first() else sleepSettingsRepository.weekendWakeTime.first()

                                recordSleepSessionUseCase(
                                    uid = uid,
                                    date = formatCurrentDate(sleepStartAt),
                                    sleepStartAt = sleepStartAt,
                                    wakeTimeMs = platformTimeMillis(),
                                    targetBedtime = targetBedtime,
                                    targetWakeTime = targetWakeTime,
                                    isLockUsed = true
                                )
                                sleepSettingsRepository.saveActiveSleepStartAt(0L)
                            }
                        }
                    }

                    _state.update { it.copy(sleepState = state) }
                    
                    if (state != null) {
                        launch {
                            val isDefaultServerData = state.lastUpdatedAt == 0L
                            val isDifferent = state.weekdayBedtime != sleepSettingsRepository.weekdayBedtime.first() || 
                                            state.weekdayWakeTime != sleepSettingsRepository.weekdayWakeTime.first() ||
                                            state.isWeekdaySleepEnabled != sleepSettingsRepository.isWeekdaySleepEnabled.first() ||
                                            state.isWeekdayWakeEnabled != sleepSettingsRepository.isWeekdayWakeEnabled.first() ||
                                            state.weekendBedtime != sleepSettingsRepository.weekendBedtime.first() ||
                                            state.weekendWakeTime != sleepSettingsRepository.weekendWakeTime.first() ||
                                            state.isWeekendSleepEnabled != sleepSettingsRepository.isWeekendSleepEnabled.first() ||
                                            state.isWeekendWakeEnabled != sleepSettingsRepository.isWeekendWakeEnabled.first()
                            
                            if (isDifferent && !isDefaultServerData) {
                                sleepSettingsRepository.saveWeekdaySchedule(state.weekdayBedtime, state.weekdayWakeTime)
                                sleepSettingsRepository.saveWeekdaySleepEnabled(state.isWeekdaySleepEnabled)
                                sleepSettingsRepository.saveWeekdayWakeEnabled(state.isWeekdayWakeEnabled)
                                sleepSettingsRepository.saveWeekendSchedule(state.weekendBedtime, state.weekendWakeTime)
                                sleepSettingsRepository.saveWeekendSleepEnabled(state.isWeekendSleepEnabled)
                                sleepSettingsRepository.saveWeekendWakeEnabled(state.isWeekendWakeEnabled)
                            }

                            sleepScheduler.scheduleNextEvents(
                                state.weekdayBedtime, state.weekdayWakeTime, state.isWeekdaySleepEnabled,
                                state.weekendBedtime, state.weekendWakeTime, state.isWeekendSleepEnabled
                            )
                        }
                    }
                }
        }
        
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            observeRegisteredDevicesUseCase(uid)
                .catch { logout() }
                .collect { devices ->
                    _state.update { it.copy(registeredDevices = devices) }
                    val currentId = deviceInfoProvider.getDeviceId()
                    if (devices.any { it.deviceId == currentId }) {
                        hasBeenRegisteredInThisSession = true
                    } else if (hasBeenRegisteredInThisSession && devices.isNotEmpty()) {
                        logout()
                    }
                }
        }
    }

    private fun registerCurrentUIDDevice(uid: String) {
        deviceRegisterJob?.cancel()
        deviceRegisterJob = viewModelScope.launch {
            try {
                val cachedName = sleepSettingsRepository.deviceName.first()
                val deviceName = cachedName ?: deviceInfoProvider.getDeviceName()
                val deviceState = DeviceState(
                    deviceId = deviceInfoProvider.getDeviceId(),
                    deviceName = deviceName,
                    fcmToken = deviceInfoProvider.getPushToken(),
                    platform = deviceInfoProvider.getPlatform(),
                    lastActiveForSleepLocking = 0L,
                    isMainAlarmDevice = false
                )
                registerDeviceUseCase(uid, deviceState, _state.value.isPremium).onFailure { e ->
                    if (e is com.wngud.allsleep.domain.model.exception.PremiumRequiredException) {
                        _state.update { it.copy(showDeviceLimitDialog = true, cachedDeviceStateToRegister = deviceState) }
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun logout() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            unregisterDeviceUseCase(uid, deviceInfoProvider.getDeviceId())
            signOutUseCase()
            currentUid = null
            hasBeenRegisteredInThisSession = false
            _state.update { it.copy(currentUser = null, sleepState = null, registeredDevices = emptyList()) }
        }
    }

    private fun toggleSleepState(isSleeping: Boolean, targetWakeUpTime: Long? = null) {
        if (_state.value.isToggleLoading) return
        val uid = currentUid ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isToggleLoading = true) }
            try {
                val wdB = sleepSettingsRepository.weekdayBedtime.first()
                val wdW = sleepSettingsRepository.weekdayWakeTime.first()
                val wdSE = sleepSettingsRepository.isWeekdaySleepEnabled.first()
                val wdWE = sleepSettingsRepository.isWeekdayWakeEnabled.first()
                
                val weB = sleepSettingsRepository.weekendBedtime.first()
                val weW = sleepSettingsRepository.weekendWakeTime.first()
                val weSE = sleepSettingsRepository.isWeekendSleepEnabled.first()
                val weWE = sleepSettingsRepository.isWeekendWakeEnabled.first()
                
                val now = platformTimeMillis()

                if (isSleeping) {
                    sleepSettingsRepository.saveActiveSleepStartAt(now)
                }

                _state.update { it.copy(
                    sleepState = it.sleepState?.copy(
                        isSleeping = isSleeping,
                        targetWakeUpTime = targetWakeUpTime,
                        lastUpdatedAt = 0L
                    ) ?: UserSleepState(
                        uid = uid, isSleeping = isSleeping, targetWakeUpTime = targetWakeUpTime,
                        weekdayBedtime = wdB, weekdayWakeTime = wdW, weekendBedtime = weB, weekendWakeTime = weW,
                        isWeekdaySleepEnabled = wdSE, isWeekdayWakeEnabled = wdWE,
                        isWeekendSleepEnabled = weSE, isWeekendWakeEnabled = weWE
                    )
                ) }

                updateUserSleepStateUseCase(
                    uid = uid, 
                    isSleeping = isSleeping, 
                    targetWakeUpTime = targetWakeUpTime, 
                    weekdayBedtime = wdB,
                    weekdayWakeTime = wdW,
                    isWeekdaySleepEnabled = wdSE,
                    isWeekdayWakeEnabled = wdWE,
                    weekendBedtime = weB,
                    weekendWakeTime = weW,
                    isWeekendSleepEnabled = weSE,
                    isWeekendWakeEnabled = weWE
                )
            } finally {
                _state.update { it.copy(isToggleLoading = false) }
            }
        }
    }

    private fun isWeekday(): Boolean {
        return com.wngud.allsleep.domain.model.isWeekday(platformTimeMillis())
    }

    private fun platformTimeMillis(): Long = com.wngud.allsleep.domain.model.platformTimeMillis()
    private fun formatCurrentDate(timestamp: Long): String = com.wngud.allsleep.domain.model.formatTimestampToDate(timestamp)

    private fun completeOnboarding(bedtime: String, wakeTime: String) {
        viewModelScope.launch {
            completeOnboardingUseCase(bedtime, wakeTime)
            _state.update { it.copy(isOnboardingCompleted = true) }
        }
    }
}
