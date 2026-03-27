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

    init {
        handleIntent(GlobalSleepContract.Intent.RequestInitialize)
    }

    fun handleIntent(intent: GlobalSleepContract.Intent) {
        when (intent) {
            is GlobalSleepContract.Intent.RequestInitialize -> initializeData()
            is GlobalSleepContract.Intent.RequestLogout -> logout()
            is GlobalSleepContract.Intent.ToggleSleepState -> toggleSleepState(intent.isSleeping, intent.targetWakeUpTime)
            is GlobalSleepContract.Intent.CompleteOnboarding -> completeOnboarding(intent.bedtime, intent.wakeTime)
        }
    }

    private fun initializeData() {
        viewModelScope.launch {
            println("GlobalSleepVM: [init] ViewModel 초기화 시작")
            
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
            
            // 3. 마이그레이션 (로그인된 경우 온보딩 강제 완료 처리)
            if (user != null && !initialOnboardingState) {
                completeOnboarding(bedtime = "23:00", wakeTime = "07:00")
                _state.update { it.copy(isOnboardingCompleted = true) }
            }
            
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
                sleepSettingsRepository.bedtime,
                sleepSettingsRepository.wakeTime
            ) { b, w -> b to w }
                .distinctUntilChanged()
                .debounce(500L)
                .collect { (bedtime, wakeTime) ->
                    val uid = currentUid ?: return@collect
                    val remoteState = _state.value.sleepState ?: return@collect
                    
                    if (remoteState.bedtime != bedtime || remoteState.wakeTime != wakeTime) {
                        println("SleepBounceDebug: [UpstreamSync] 스케줄 변경 감지! Cloud 업데이트")
                        updateUserSleepStateUseCase(
                            uid = uid,
                            isSleeping = null,
                            bedtime = bedtime,
                            wakeTime = wakeTime
                        )
                    }
                }
        }
    }

    private fun handleUserSessionChange(user: User?) {
        _state.update { it.copy(currentUser = user) }
        if (user != null) {
            if (user.uid != currentUid) {
                currentUid = user.uid
                // 새 유저인 경우 프로필 갱신 및 관찰 시작
                viewModelScope.launch { updateUserProfileUseCase(user) }
                startObserving(user.uid)
                registerCurrentUIDDevice(user.uid)
            }
        } else {
            currentUid = null
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
        updateUserSleepStateUseCase(
            uid = uid,
            isSleeping = null,
            bedtime = bedtime,
            wakeTime = wakeTime
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
            registerDeviceUseCase(uid, deviceState).onFailure { e ->
                _state.update { it.copy(error = "기기 등록 실패: ${e.message}") }
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
                    println("SleepBounceDebug: [startObserving] Firestore snapshot 받음: isSleeping=${state?.isSleeping}")
                    
                    if (state == null && currentUid != null) {
                        println("GlobalSleepVM: [startObserving] 서버 데이터 삭제 확인 -> 로그아웃 수행")
                        logout()
                        return@collect
                    }

                    if (_state.value.isToggleLoading) {
                        val current = _state.value.sleepState
                        if (current?.lastUpdatedAt == 0L && state != null && state.isSleeping != current.isSleeping) {
                            println("SleepBounceDebug: [startObserving] Stale 스냅샷 무시됨")
                            return@collect
                        }
                    }
                    
                    _state.update { it.copy(sleepState = state) }
                    
                    if (state != null) {
                        sleepScheduler.scheduleNextEvents(state.bedtime, state.wakeTime)
                        launch {
                            val localBedtime = sleepSettingsRepository.bedtime.first()
                            val localWakeTime = sleepSettingsRepository.wakeTime.first()
                            if (state.bedtime != localBedtime || state.wakeTime != localWakeTime) {
                                sleepSettingsRepository.saveSleepSchedule(state.bedtime, state.wakeTime)
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
                        
                        if (!isStillRegistered && devices.isNotEmpty()) {
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
        if (_state.value.isToggleLoading) return
        val uid = currentUid ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isToggleLoading = true) }
            try {
                val bedtime = sleepSettingsRepository.bedtime.first()
                val wakeTime = sleepSettingsRepository.wakeTime.first()

                val optimisticState = _state.value.sleepState?.copy(
                    isSleeping = isSleeping,
                    targetWakeUpTime = targetWakeUpTime,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    lastUpdatedAt = 0L
                ) ?: UserSleepState(uid, isSleeping, targetWakeUpTime, bedtime, wakeTime)
                
                _state.update { it.copy(sleepState = optimisticState) }

                val result = updateUserSleepStateUseCase(uid, isSleeping, targetWakeUpTime, bedtime, wakeTime)
                result.onFailure { e -> 
                    _state.update { it.copy(error = "업데이트 실패: ${e.message}") }
                }
            } finally {
                _state.update { it.copy(isToggleLoading = false) }
            }
        }
    }

    private fun completeOnboarding(bedtime: String, wakeTime: String) {
        viewModelScope.launch {
            completeOnboardingUseCase(bedtime, wakeTime)
            _state.update { it.copy(isOnboardingCompleted = true) }
        }
    }
}

