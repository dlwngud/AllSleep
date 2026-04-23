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
    private val billingProvider: com.wngud.allsleep.platform.BillingProvider,
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
    private var isReplacingDevice = false

    init {
        handleIntent(GlobalSleepContract.Intent.RequestInitialize)
    }

    fun handleIntent(intent: GlobalSleepContract.Intent) {
        when (intent) {
            is GlobalSleepContract.Intent.RequestInitialize -> initializeData()
            is GlobalSleepContract.Intent.RequestLogout -> logout()
            is GlobalSleepContract.Intent.ToggleSleepState -> toggleSleepState(intent.isSleeping, intent.targetWakeUpTime)
            is GlobalSleepContract.Intent.CompleteOnboarding -> completeOnboarding(intent.bedtime, intent.wakeTime)
            is GlobalSleepContract.Intent.RefreshPremiumStatus -> refreshPremiumStatus()
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
        if (isReplacingDevice) return
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }
        handleIntent(GlobalSleepContract.Intent.RequestLogout)
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
                
                // 1. 현재 기기가 아닌 다른 기기들만 모두 등록 해제
                devices.filter { it.deviceId != currentId }.forEach { device -> 
                    unregisterDeviceUseCase(uid, device.deviceId) 
                }
                
                // 2. 새로운 현재 기기 등록 시도
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
        // 온보딩 등에서 로컬로 설정한 최신 정보를 서버(FireStore)에 강제로 덮어씌움
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
                viewModelScope.launch { 
                    updateUserProfileUseCase(user)
                    billingProvider.loginUser(user.uid) 
                }
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

    private fun refreshPremiumStatus() {
        viewModelScope.launch {
            val statusResult = billingProvider.getSubscriptionStatus()
            if (statusResult.isFailure) return@launch

            val isPremiumNow = statusResult.getOrThrow().isPremiumActive
            val currentUser = getCurrentUserUseCase()

            if (currentUser != null && currentUser.isPremium != isPremiumNow) {
                val updatedUser = currentUser.copy(isPremium = isPremiumNow)
                updateUserProfileUseCase(updatedUser)
                _state.update { it.copy(currentUser = updatedUser, isPremium = isPremiumNow) }
            } else {
                _state.update { it.copy(isPremium = isPremiumNow) }
                currentUser?.let { _state.update { state -> state.copy(currentUser = it) } }
            }

            sleepSettingsRepository.savePremiumStatus(isPremiumNow)
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
                                val wakeTimeMs = platformTimeMillis()

                                recordSleepSessionUseCase(
                                    uid = uid,
                                    date = formatCurrentDate(wakeTimeMs),
                                    sleepStartAt = sleepStartAt,
                                    wakeTimeMs = wakeTimeMs,
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
                            
                            val isOnboardingDone = sleepSettingsRepository.isOnboardingCompleted.first()
                            
                            // 온보딩이 완료된 경우에만 서버 데이터를 로컬로 가져옴 (로그인 직후 온보딩 설정값 보호)
                            if (isDifferent && !isDefaultServerData && isOnboardingDone) {
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
                        // 교체 중(isReplacingDevice == true)일 때는 서버 상태가 불안정하므로 로그아웃 트리거를 일시 중지함.
                        if (!isReplacingDevice) {
                            logout()
                        }
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
            billingProvider.logoutUser()
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
                } else {
                    saveCompletedSleepRecord(
                        uid = uid,
                        wakeTimeMs = now,
                        weekdayBedtime = wdB,
                        weekdayWakeTime = wdW,
                        weekendBedtime = weB,
                        weekendWakeTime = weW
                    )
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

    private suspend fun saveCompletedSleepRecord(
        uid: String,
        wakeTimeMs: Long,
        weekdayBedtime: String,
        weekdayWakeTime: String,
        weekendBedtime: String,
        weekendWakeTime: String
    ) {
        val sleepStartAt = sleepSettingsRepository.activeSleepStartAt.first()
        if (sleepStartAt <= 0L || wakeTimeMs <= sleepStartAt) return

        val isWeekdaySession = com.wngud.allsleep.domain.model.isWeekday(sleepStartAt)
        val targetBedtime = if (isWeekdaySession) weekdayBedtime else weekendBedtime
        val targetWakeTime = if (isWeekdaySession) weekdayWakeTime else weekendWakeTime

        recordSleepSessionUseCase(
            uid = uid,
            date = formatCurrentDate(wakeTimeMs),
            sleepStartAt = sleepStartAt,
            wakeTimeMs = wakeTimeMs,
            targetBedtime = targetBedtime,
            targetWakeTime = targetWakeTime,
            isLockUsed = true
        ).onSuccess {
            sleepSettingsRepository.saveActiveSleepStartAt(0L)
        }.onFailure { e ->
            println("[SleepDebug] 수면 기록 저장 실패: ${e.message}")
        }
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
