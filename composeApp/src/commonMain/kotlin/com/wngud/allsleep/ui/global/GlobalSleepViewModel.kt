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
 * - 앱 시작 시 현재 사용자 UID를 확인 후 기기 정보를 Firestore에 등록
 * - 실시간으로 수면 상태를 구독하여 전역 UI 상태로 제공
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

    // 원격 로그아웃 감지를 위한 플래그: 현재 세션 중에 한 번이라도 성공적으로 기기가 등록되었는지 추적
    private var hasBeenRegisteredInThisSession = false

    init {
        handleIntent(GlobalSleepContract.Intent.RequestInitialize)
    }

    fun handleIntent(intent: GlobalSleepContract.Intent) {
        println("[SleepDebug] Intent 수신: $intent")
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

    private fun upgradeToPremium() {
        _state.update { it.copy(showDeviceLimitDialog = false) }
        viewModelScope.launch {
            _effect.send(GlobalSleepContract.Effect.NavigateToSubscription)
        }
    }

    private fun cancelDeviceRegistration() {
        _state.update { 
            it.copy(
                showDeviceLimitDialog = false,
                cachedDeviceStateToRegister = null
            ) 
        }
        // 새 기기 등록을 취소했으므로 로그아웃 처리하여 서비스를 이용하지 못하게 함
        handleIntent(GlobalSleepContract.Intent.RequestLogout)
    }

    private fun forceReplaceDevice() {
        val uid = currentUid ?: return
        val deviceToRegister = _state.value.cachedDeviceStateToRegister ?: return
        
        _state.update { it.copy(showDeviceLimitDialog = false, cachedDeviceStateToRegister = null) }
        
        viewModelScope.launch {
            try {
                // 1. 기존 기기들 모두 삭제
                val devices = observeRegisteredDevicesUseCase(uid).first()
                devices.forEach { device ->
                    unregisterDeviceUseCase(uid, device.deviceId)
                }
                
                // 2. 새 기기 등록 (이때는 강제로 프리미엄 체크 패스하거나 size가 0이므로 성공)
                // RegisterDeviceUseCase에 isPremium = true를 넘겨서 강제 등록
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
            println("GlobalSleepVM: [init] ViewModel 초기화 시작")
            
            // 0. 로컬에 저장된 구독 상태 초기 로딩 (최우선 반영)
            val cachedIsPremium = sleepSettingsRepository.isPremium.first()
            _state.update { it.copy(isPremium = cachedIsPremium) }

            // 1. 초기 온보딩 상태 로드
            val initialOnboardingState = observeOnboardingCompletedUseCase().first()
            _state.update { it.copy(isOnboardingCompleted = initialOnboardingState) }
            
            // 2. 초기 사용자 세션 확인
            val user = getCurrentUserUseCase()
            
            // [추가] 기동 시 서버에서 계정 생존 여부 강제 확인 (타 기기 탈퇴 대응)
            if (user != null) {
                println("GlobalSleepVM: [init] 기존 세션 발견, 서버 유효성 검사 시작")
                validateSessionUseCase().onFailure { e ->
                    println("GlobalSleepVM: [init] 세션 유효성 검사 실패 (계정 삭제됨으로 판단): ${e.message}")
                    logout()
                    _state.update { it.copy(isStateInitialized = true) }
                    return@launch
                }
                println("GlobalSleepVM: [init] 세션 유효성 검사 통과")
            }
            
            handleUserSessionChange(user)
            
            _state.update { it.copy(isStateInitialized = true) }

            // 4. 실시간 관찰자 설정 (Modularized)
            setupAuthObserver()
            setupOnboardingObserver()
            setupSettingsSync()
        }
    }

    private fun setupAuthObserver() {
        viewModelScope.launch {
            observeUserUseCase().collect { user ->
                println("GlobalSleepVM: [observeUser] 세션 변경 감지: user=$user")
                handleUserSessionChange(user)
            }
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

    private fun setupSettingsSync() {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(
                listOf(
                    sleepSettingsRepository.bedtime,
                    sleepSettingsRepository.wakeTime,
                    sleepSettingsRepository.sleepAlarmDays,
                    sleepSettingsRepository.wakeAlarmDays,
                    sleepSettingsRepository.isSleepAlarmEnabled,
                    sleepSettingsRepository.isWakeAlarmEnabled
                )
            ) { values ->
                val bedtime = values[0] as String
                val wakeTime = values[1] as String
                val sleepDays = values[2] as Set<Int>
                val wakeDays = values[3] as Set<Int>
                val sleepEnabled = values[4] as Boolean
                val wakeEnabled = values[5] as Boolean
                
                arrayOf(bedtime, wakeTime, sleepDays, wakeDays, sleepEnabled, wakeEnabled)
            }
                .distinctUntilChanged { old, new -> old.contentDeepEquals(new) }
                .debounce(500L)
                .collect { values ->
                    val bedtime = values[0] as String
                    val wakeTime = values[1] as String
                    val sleepDays = values[2] as Set<Int>
                    val wakeDays = values[3] as Set<Int>
                    val sleepEnabled = values[4] as Boolean
                    val wakeEnabled = values[5] as Boolean
                    
                    val uid = currentUid ?: return@collect
                    val remoteState = _state.value.sleepState ?: return@collect
                    
                    val isChanged = remoteState.bedtime != bedtime || 
                                    remoteState.wakeTime != wakeTime ||
                                    remoteState.sleepAlarmDays != sleepDays ||
                                    remoteState.wakeAlarmDays != wakeDays ||
                                    remoteState.isSleepAlarmEnabled != sleepEnabled ||
                                    remoteState.isWakeAlarmEnabled != wakeEnabled

                    if (isChanged) {
                        println("SleepBounceDebug: [UpstreamSync] 알람 설정 변경 감지! Cloud 업데이트")
                        updateUserSleepStateUseCase(
                            uid = uid,
                            isSleeping = null,
                            bedtime = bedtime,
                            wakeTime = wakeTime,
                            sleepAlarmDays = sleepDays,
                            wakeAlarmDays = wakeDays,
                            isSleepAlarmEnabled = sleepEnabled,
                            isWakeAlarmEnabled = wakeEnabled
                        )
                    }
                }
        }
    }

    private fun handleUserSessionChange(user: User?) {
        val wasPremium = _state.value.isPremium
        val isNowPremium = user?.isPremium ?: _state.value.isPremium
        
        _state.update { 
            it.copy(
                currentUser = user,
                isPremium = isNowPremium
            ) 
        }
        
        // Firestore에서 받은 최신 구독 상태를 로컬 캐시에 저장
        user?.let {
            viewModelScope.launch {
                sleepSettingsRepository.savePremiumStatus(it.isPremium)
            }
        }
        
        // 사용자가 구독 결제에 성공하여 처음으로 프리미엄 상태가 되었을 때 보류되었던 기기 강제 등록
        if (!wasPremium && isNowPremium) {
            val pendingDevice = _state.value.cachedDeviceStateToRegister
            val uid = user?.uid
            if (pendingDevice != null && uid != null) {
                _state.update { it.copy(cachedDeviceStateToRegister = null, showDeviceLimitDialog = false) }
                viewModelScope.launch {
                    try {
                        registerDeviceUseCase(uid, pendingDevice, isPremium = true).onFailure { e ->
                            _state.update { it.copy(error = "구독 후 자동 기기 등록 실패: ${e.message}") }
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(error = "자동 등록 중 오류: ${e.message}") }
                    }
                }
            }
        }

        if (user != null) {
            if (user.uid != currentUid) {
                currentUid = user.uid
                hasBeenRegisteredInThisSession = false // 유저 변경 시 초기화
                // 새 유저인 경우 프로필 갱신 및 관찰 시작
                viewModelScope.launch { updateUserProfileUseCase(user) }
                startObserving(user.uid)
                registerCurrentUIDDevice(user.uid)
            }
        } else {
            currentUid = null
            hasBeenRegisteredInThisSession = false
            _state.update { 
                it.copy(
                    sleepState = null,
                    registeredDevices = emptyList()
                )
            }
            observeJob?.cancel()
            devicesJob?.cancel()
        }
    }

    private suspend fun syncScheduleToFirestore() {
        val uid = currentUid ?: return
        val bedtime = sleepSettingsRepository.bedtime.first()
        val wakeTime = sleepSettingsRepository.wakeTime.first()
        val sleepDays = sleepSettingsRepository.sleepAlarmDays.first()
        val wakeDays = sleepSettingsRepository.wakeAlarmDays.first()
        val sleepEnabled = sleepSettingsRepository.isSleepAlarmEnabled.first()
        val wakeEnabled = sleepSettingsRepository.isWakeAlarmEnabled.first()

        updateUserSleepStateUseCase(
            uid = uid,
            isSleeping = null,
            bedtime = bedtime,
            wakeTime = wakeTime,
            sleepAlarmDays = sleepDays,
            wakeAlarmDays = wakeDays,
            isSleepAlarmEnabled = sleepEnabled,
            isWakeAlarmEnabled = wakeEnabled
        )
    }

    private suspend fun registerCurrentDevice(uid: String) {
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
            val isPremium = _state.value.isPremium
            registerDeviceUseCase(uid, deviceState, isPremium).onFailure { e ->
                if (e is com.wngud.allsleep.domain.model.exception.PremiumRequiredException) {
                    _state.update { 
                        it.copy(
                            showDeviceLimitDialog = true,
                            cachedDeviceStateToRegister = deviceState
                        ) 
                    }
                } else {
                    _state.update { it.copy(error = "기기 등록 실패: ${e.message}") }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "기기 등록 중 오류: ${e.message}") }
        }
    }

    private fun startObserving(uid: String) {
        if (uid != currentUid) currentUid = uid 
        
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeUserSleepStateUseCase(uid)
                .catch { e ->
                    println("GlobalSleepVM: [observeUserState] 에러 발생, 로그아웃 처리: ${e.message}")
                    logout()
                }
                .collect { state ->
                    val oldState = _state.value.sleepState
                    println("SleepBounceDebug: [startObserving] Firestore snapshot 받음: isSleeping=${state?.isSleeping} (이전: ${oldState?.isSleeping})")
                    
                    if (state == null && currentUid != null) {
                        println("GlobalSleepVM: [startObserving] 서버 데이터 삭제 확인 -> 로그아웃 수행")
                        logout()
                        return@collect
                    }

                    if (_state.value.isToggleLoading) {
                        if (oldState?.lastUpdatedAt == 0L && state != null && state.isSleeping != oldState.isSleeping) {
                            println("SleepBounceDebug: [startObserving] Stale 스냅샷 무시됨")
                            return@collect
                        }
                    }
                    
                    // 수면 종료 감지 (상태 전이: Sleeping -> Awake)
                    if (oldState?.isSleeping == true && state?.isSleeping == false) {
                        println("[SleepDebug] 상태 전환 감지: 수면 종료됨 -> 세션 기록 시도")
                        launch {
                            val localStartAt = sleepSettingsRepository.activeSleepStartAt.first()
                            println("[SleepDebug] 관찰자 내 로컬 시작 시각 확인: $localStartAt")

                            val sleepStartAt = if (localStartAt > 0L) localStartAt else oldState.sleepStartAt
                            if (sleepStartAt > 0L) {
                                val wakeNow = platformTimeMillis()
                                val bedtime = sleepSettingsRepository.bedtime.first()
                                val wakeTime = sleepSettingsRepository.wakeTime.first()
                                
                                println("[SleepDebug] 관찰자 주도로 세션 저장 시작: 시작=$sleepStartAt, 종료=$wakeNow")
                                recordSleepSessionUseCase(
                                    uid = uid,
                                    date = formatCurrentDate(sleepStartAt),
                                    sleepStartAt = sleepStartAt,
                                    wakeTimeMs = wakeNow,
                                    targetBedtime = bedtime,
                                    targetWakeTime = wakeTime,
                                    isLockUsed = true
                                )
                                // 기록 완료 후 로컬 세션 초기화
                                sleepSettingsRepository.saveActiveSleepStartAt(0L)
                                println("[SleepDebug] 관찰자 주도 기록 완료 및 로컬 초기화")
                            } else {
                                println("[SleepDebug] 시작 시각을 찾을 수 없어 기록을 건너뜁니다.")
                            }
                        }
                    }

                    _state.update { it.copy(sleepState = state) }
                    
                    if (state != null) {
                        launch {
                            val bedtime = state.bedtime
                            val wakeTime = state.wakeTime
                            val isSleepEnabled = sleepSettingsRepository.isSleepAlarmEnabled.first()
                            val sleepDays = if (isSleepEnabled) sleepSettingsRepository.sleepAlarmDays.first() else emptySet()
                            
                            val isWakeEnabled = sleepSettingsRepository.isWakeAlarmEnabled.first()
                            val wakeDays = if (isWakeEnabled) sleepSettingsRepository.wakeAlarmDays.first() else emptySet()
                            
                            sleepScheduler.scheduleNextEvents(bedtime, wakeTime, sleepDays, wakeDays)
                            
                            // [Bug Fix] 서버 데이터가 기본값(lastUpdatedAt=0)이고 로컬에 이미 설정이 있다면 덮어쓰지 않음
                            val localBedtime = sleepSettingsRepository.bedtime.first()
                            val localWakeTime = sleepSettingsRepository.wakeTime.first()
                            
                            val isDefaultServerData = state.lastUpdatedAt == 0L
                            
                            val localSleepDays = sleepSettingsRepository.sleepAlarmDays.first()
                            val localWakeDays = sleepSettingsRepository.wakeAlarmDays.first()
                            val localSleepEnabled = sleepSettingsRepository.isSleepAlarmEnabled.first()
                            val localWakeEnabled = sleepSettingsRepository.isWakeAlarmEnabled.first()
                            
                            val isDifferent = state.bedtime != localBedtime || 
                                            state.wakeTime != localWakeTime ||
                                            state.sleepAlarmDays != localSleepDays ||
                                            state.wakeAlarmDays != localWakeDays ||
                                            state.isSleepAlarmEnabled != localSleepEnabled ||
                                            state.isWakeAlarmEnabled != localWakeEnabled
                            
                            if (isDifferent) {
                                if (!isDefaultServerData) {
                                    println("GlobalSleepVM: 서버 설정으로 로컬 동기화 (Server wins)")
                                    sleepSettingsRepository.saveSleepSchedule(state.bedtime, state.wakeTime)
                                    sleepSettingsRepository.saveSleepAlarmDays(state.sleepAlarmDays)
                                    sleepSettingsRepository.saveWakeAlarmDays(state.wakeAlarmDays)
                                    sleepSettingsRepository.saveSleepAlarmEnabled(state.isSleepAlarmEnabled)
                                    sleepSettingsRepository.saveWakeAlarmEnabled(state.isWakeAlarmEnabled)
                                } else {
                                    println("GlobalSleepVM: 서버가 기본값이므로 로컬 설정을 유지하고 업로드를 대기함 (Local wins)")
                                }
                            }
                        }
                    }
                }
        }
        
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            observeRegisteredDevicesUseCase(uid)
                .catch { e ->
                    println("GlobalSleepVM: [devicesSync] 에러 발생, 로그아웃 처리: ${e.message}")
                    logout()
                }
                .collect { devices ->
                    _state.update { it.copy(registeredDevices = devices) }

                    // [원격 로그아웃 체크] 기기 목록에 내 기기가 사라졌는지 확인
                    if (currentUid != null) {
                        val currentId = deviceInfoProvider.getDeviceId()
                        val isStillRegistered = devices.any { it.deviceId == currentId }
                        
                        if (isStillRegistered) {
                            hasBeenRegisteredInThisSession = true
                        } else if (hasBeenRegisteredInThisSession && devices.isNotEmpty()) {
                            println("GlobalSleepVM: [RemoteLogout] 다른 기기에 의해 현재 기기가 등록 해제됨 확인 -> 로그아웃 수행")
                            logout()
                        }
                    }
                }
        }
    }

    private fun registerCurrentUIDDevice(uid: String) {
        deviceRegisterJob?.cancel()
        deviceRegisterJob = viewModelScope.launch {
            registerCurrentDevice(uid)
        }
    }

    private fun logout() {
        val uid = currentUid ?: return
        observeJob?.cancel()
        devicesJob?.cancel()
        deviceRegisterJob?.cancel()

        viewModelScope.launch {
            try {
                val deviceId = deviceInfoProvider.getDeviceId()
                unregisterDeviceUseCase(uid, deviceId)
                signOutUseCase()
                currentUid = null
                hasBeenRegisteredInThisSession = false
                _state.update { 
                    it.copy(
                        currentUser = null,
                        sleepState = null,
                        registeredDevices = emptyList()
                    )
                }
            } catch (e: Exception) {
                println("GlobalSleepVM: Logout failed: ${e.message}")
            }
        }
    }

    private fun toggleSleepState(isSleeping: Boolean, targetWakeUpTime: Long? = null) {
        if (_state.value.isToggleLoading) {
            println("[SleepDebug] 호출 거부: 이미 토글 중입니다.")
            return
        }
        val uid = currentUid ?: run {
            println("[SleepDebug] 호출 거부: currentUid가 없습니다.")
            return
        }
        
        println("[SleepDebug] toggleSleepState 실행: isSleeping=$isSleeping")
        viewModelScope.launch {
            _state.update { it.copy(isToggleLoading = true) }
            try {
                val bedtime = sleepSettingsRepository.bedtime.first()
                val wakeTime = sleepSettingsRepository.wakeTime.first()
                val now = platformTimeMillis()

                if (isSleeping) {
                    println("[SleepDebug] 수면 시작 처리: $now")
                    // 수면 시작 시 로컬 DataStore에 시작 시각 즉시 저장 (백업용)
                    sleepSettingsRepository.saveActiveSleepStartAt(now)
                }

                val optimisticState = _state.value.sleepState?.copy(
                    isSleeping = isSleeping,
                    targetWakeUpTime = targetWakeUpTime,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    sleepStartAt = if (isSleeping) now else 0L,
                    lastUpdatedAt = 0L
                ) ?: UserSleepState(
                    uid = uid, 
                    isSleeping = isSleeping, 
                    targetWakeUpTime = targetWakeUpTime, 
                    bedtime = bedtime, 
                    wakeTime = wakeTime,
                    sleepStartAt = if (isSleeping) now else 0L
                )
                
                _state.update { it.copy(sleepState = optimisticState) }

                println("[SleepDebug] 수면 상태 업데이트 시도: isSleeping=$isSleeping")
                val result = updateUserSleepStateUseCase(
                    uid = uid, 
                    isSleeping = isSleeping, 
                    targetWakeUpTime = targetWakeUpTime, 
                    bedtime = bedtime, 
                    wakeTime = wakeTime,
                    sleepAlarmDays = _state.value.sleepState?.sleepAlarmDays,
                    wakeAlarmDays = _state.value.sleepState?.wakeAlarmDays,
                    isSleepAlarmEnabled = _state.value.sleepState?.isSleepAlarmEnabled,
                    isWakeAlarmEnabled = _state.value.sleepState?.isWakeAlarmEnabled
                )
                result.onSuccess {
                    println("[SleepDebug] 수면 상태 업데이트 성공: isSleeping=$isSleeping")
                    // 수면 기록 생성이 이제 '관찰자(startObserving)'에서 이루어지므로 여기서는 상태 변경만 관리합니다.
                }.onFailure { e -> 
                    println("[SleepDebug] 업데이트 실패: ${e.message}")
                    _state.update { it.copy(error = "업데이트 실패: ${e.message}") }
                }
            } catch (e: Exception) {
                println("[SleepDebug] 예외 발생: ${e.message}")
                _state.update { it.copy(error = "수면 상태 변경 실패: ${e.message}") }
            } finally {
                _state.update { it.copy(isToggleLoading = false) }
            }
        }
    }

    /** KMP 환경에서 현재 시각을 구하거나 날짜를 포맷팅하기 위한 헬퍼 (나중에 유틸로 분리 권장) */
    private fun platformTimeMillis(): Long = com.wngud.allsleep.domain.model.platformTimeMillis()
    
    private fun formatCurrentDate(timestamp: Long): String {
        return com.wngud.allsleep.domain.model.formatTimestampToDate(timestamp)
    }

    private fun completeOnboarding(bedtime: String, wakeTime: String) {
        viewModelScope.launch {
            completeOnboardingUseCase(bedtime, wakeTime)
            _state.update { it.copy(isOnboardingCompleted = true) }
        }
    }
}
