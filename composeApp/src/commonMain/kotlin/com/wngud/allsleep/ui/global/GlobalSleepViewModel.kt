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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * 전역 수면 상태(App State)를 관리하는 ViewModel
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
    private val sleepSettingsRepository: com.wngud.allsleep.domain.repository.SleepSettingsRepository
) : ViewModel() {

    private val _sleepState = MutableStateFlow<UserSleepState?>(null)
    val sleepState: StateFlow<UserSleepState?> = _sleepState.asStateFlow()

    private val _registeredDevices = MutableStateFlow<List<DeviceState>>(emptyList())
    val registeredDevices: StateFlow<List<DeviceState>> = _registeredDevices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isToggleLoading = MutableStateFlow(false)
    val isToggleLoading: StateFlow<Boolean> = _isToggleLoading.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
    // 앱 시작 시 로컬 데이터(Auth, DataStore)를 완전히 읽을 때까지 대기하기 위한 상태
    private val _isStateInitialized = MutableStateFlow(false)
    val isStateInitialized: StateFlow<Boolean> = _isStateInitialized.asStateFlow()
    
    // 현재 사용자 캐싱용 변수
    private var currentUid: String? = null
    
    // 중복 구독 방지를 위한 Job 관리
    private var observeJob: Job? = null
    private var devicesJob: Job? = null
    private var deviceRegisterJob: Job? = null

    init {
        // ViewModel 생성 시점에 현재 사용자를 확인하고 관찰 시작 + 기기 등록
        viewModelScope.launch {
            android.util.Log.d("GlobalSleepVM", "[init] ViewModel 생성됨 - 초기화 시작")
            
            // 1. DataStore에서 온보딩 완료 상태 동기화 대기 (Flow의 첫 번째 값 읽기)
            val initialOnboardingState = observeOnboardingCompletedUseCase().first()
            _isOnboardingCompleted.value = initialOnboardingState
            
            // 2. Firebase에서 현재 로그인 세션 확인
            val user = getCurrentUserUseCase()
            _currentUser.value = user
            android.util.Log.d("GlobalSleepVM", "[init] user=$user, onboarding=$initialOnboardingState")
            
            // 3. 기존 로그인된 유저가 온보딩 완료 값이 false라면 마이그레이션 (기기별 동기화)
            if (user != null && !initialOnboardingState) {
                android.util.Log.d("GlobalSleepVM", "[init] 기존 로그인 유저 확인. 온보딩 완료 상태를 로컬 저장소에 갱신함")
                completeOnboardingUseCase(bedtime = "23:00", wakeTime = "07:00")
                _isOnboardingCompleted.value = true
            }
            
            if (user != null) {
                currentUid = user.uid
                android.util.Log.d("GlobalSleepVM", "[init] uid=${user.uid}, 기기/프로필 등록 시작")
                launch { updateUserProfileUseCase(user) }
                startObserving(user.uid)
                registerCurrentUIDDevice(user.uid)
            } else {
                android.util.Log.w("GlobalSleepVM", "[init] 로그인 사용자 없음 - 기기 등록 skip")
            }
            
            // 초기 스토리지 로딩이 모두 완료되었음을 알림
            _isStateInitialized.value = true

            // 이후부터는 실시간 관찰 (구독) 시작
            launch {
                observeUserUseCase().collect { observedUser ->
                    android.util.Log.d("GlobalSleepVM", "[observeUser] 세션 변경 감지: user=$observedUser")
                    _currentUser.value = observedUser
                    if (observedUser != null) {
                        currentUid = observedUser.uid
                        // 로그인 성공 시 "온보딩 완료"로 자동 간주하고 로컬 스토리지에 기록 (로그아웃해도 온보딩 안 뜨게)
                        if (!_isOnboardingCompleted.value) {
                            completeOnboardingUseCase(bedtime = "23:00", wakeTime = "07:00")
                            _isOnboardingCompleted.value = true
                        }
                        launch { updateUserProfileUseCase(observedUser) }
                        startObserving(observedUser.uid)
                        registerCurrentUIDDevice(observedUser.uid)
                    } else {
                        currentUid = null
                        _sleepState.value = null
                        _registeredDevices.value = emptyList()
                    }
                }
            }
            
            launch {
                observeOnboardingCompletedUseCase()
                    .distinctUntilChanged()
                    .collect { completed ->
                        val wasCompleted = _isOnboardingCompleted.value
                        _isOnboardingCompleted.value = completed
                        
                        // 온보딩이 "실제로 완료되는 순간"(false -> true)에만 Firestore에 취침/기상 스케줄 최초 동기화
                        // (앱 시작 시는 실행하지 않음 - 이미 Firestore 에서 Downstream Sync로 처리되므로)
                        if (completed && !wasCompleted && currentUid != null) {
                            android.util.Log.d("GlobalSleepVM", "Onboarding just completed. Syncing initial schedule to Firestore (schedule only)")
                            val uid = currentUid ?: return@collect
                            val bedtime = sleepSettingsRepository.bedtime.first()
                            val wakeTime = sleepSettingsRepository.wakeTime.first()
                            // 수면 모드 상태는 건드리지 않고 스케줄 정보만 동기화
                            updateUserSleepStateUseCase(
                                uid = uid,
                                isSleeping = null,
                                bedtime = bedtime,
                                wakeTime = wakeTime
                            )
                        }
                    }
            }

            // [Upstream Sync] 로컬 DataStore 변경 감지 시 Firestore 동기화 (Step 4 핵심)
            launch {
                @OptIn(kotlinx.coroutines.FlowPreview::class)
                combine(
                    sleepSettingsRepository.bedtime,
                    sleepSettingsRepository.wakeTime
                ) { bedtime, wakeTime ->
                    bedtime to wakeTime
                }
                .debounce(300L)
                .collect { (bedtime, wakeTime) ->
                    val uid = currentUid ?: return@collect
                    val currentState = _sleepState.value ?: return@collect
                    
                    // Firestore에 저장된 값과 다를 때만 업데이트 (무한 루프 방지)
                    if (currentState.bedtime != bedtime || currentState.wakeTime != wakeTime) {
                        android.util.Log.w("SleepBounceDebug", "[UpstreamSync] 스케줄 변경 감지! 서버 전송: bedtime=$bedtime, isSleeping(current)=${currentState.isSleeping}")
                        updateUserSleepStateUseCase(
                            uid = uid,
                            isSleeping = null, // 수면 모드 상태는 건드리지 않음 (Ping-pong 방지)
                            bedtime = bedtime,
                            wakeTime = wakeTime
                        )
                    }
                }
            }
        }
    }

    /**
     * 현재 기기 정보를 Firestore users/{uid}/devices/{deviceId}에 등록합니다.
     * 이미 등록된 경우 덮어쓰기(갱신) 처리됩니다.
     */
    private suspend fun registerCurrentDevice(uid: String) {
        try {
            val deviceState = DeviceState(
                deviceId = DeviceInfoProvider.getDeviceId(),
                deviceName = DeviceInfoProvider.getDeviceName(),
                fcmToken = DeviceInfoProvider.getPushToken(),
                platform = DeviceInfoProvider.getPlatform(),
                lastActiveForSleepLocking = 0L,
                isMainAlarmDevice = false
            )
            val provider = _currentUser.value?.provider?.name ?: "UNKNOWN"
            android.util.Log.d("GlobalSleepVM", "[registerDevice] 시작: uid=$uid, provider=$provider, deviceId=${deviceState.deviceId}")
            registerDeviceUseCase(uid, deviceState)
                .onSuccess { 
                    android.util.Log.d("GlobalSleepVM", "[registerDevice] Firestore 저장 성공! (UID=$uid, Provider=$provider)") 
                }
                .onFailure { e ->
                    android.util.Log.e("GlobalSleepVM", "[registerDevice] 저장 실패: ${e.message}", e)
                    _error.value = "기기 등록 실패: ${e.message}"
                }
        } catch (e: Exception) {
            android.util.Log.e("GlobalSleepVM", "[registerDevice] 예외 발생: ${e.message}", e)
            _error.value = "기기 등록 중 오류: ${e.message}"
        }
    }

    /**
     * Firebase 인증 후 UID 획득 시 호출하여 상태 구독을 시작합니다.
     */
    fun startObserving(uid: String) {
        if (uid == currentUid && observeJob?.isActive == true) return
        
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                observeUserSleepStateUseCase(uid).collect { state ->
                    android.util.Log.w("SleepBounceDebug", "[startObserving] Firestore snapshot 받음: isSleeping=${state?.isSleeping}, isToggleLoading=${_isToggleLoading.value}")
                    
                    // [Bounce 방지]: toggleSleepState 진행 중에 Firestore에서 낙관적 상태와 다른 스냅샷이 오면 무시
                    // (isPersistenceEnabled=false여도 이전 쓰기 확인 전 중간 상태가 올 수 있음)
                    if (_isToggleLoading.value) {
                        val currentOptimistic = _sleepState.value
                        if (currentOptimistic?.lastUpdatedAt == 0L && state?.isSleeping != currentOptimistic.isSleeping) {
                            android.util.Log.w("SleepBounceDebug", "[startObserving] 토글 중 stale 스냅샷 무시: snapshot=${state?.isSleeping} vs optimistic=${currentOptimistic.isSleeping}")
                            return@collect
                        }
                    }
                    
                    _sleepState.value = state
                    
                    // Firestore에서 최신 스케줄 정보가 오면
                    if (state != null) {
                        // 1. 알람 재스케줄링 (Step 2 핵심)
                        com.wngud.allsleep.platform.SleepScheduler.scheduleNextEvents(
                            bedtime = state.bedtime,
                            wakeTime = state.wakeTime
                        )

                        // 2. [Downstream Sync] Firestore 값이 로컬과 다르면 로컬 갱신 (Step 4 핵심)
                        launch {
                            val localBedtime = sleepSettingsRepository.bedtime.first()
                            val localWakeTime = sleepSettingsRepository.wakeTime.first()
                            
                            if (state.bedtime != localBedtime || state.wakeTime != localWakeTime) {
                                android.util.Log.d("GlobalSleepVM", "Syncing Cloud -> Local: ${state.bedtime}, ${state.wakeTime}")
                                sleepSettingsRepository.saveSleepSchedule(state.bedtime, state.wakeTime)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to observe state: ${e.message}"
            }
        }
        
        // 기기 목록 실시간 구독 추가
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            try {
                observeRegisteredDevicesUseCase(uid).collect { devices ->
                    _registeredDevices.value = devices
                }
            } catch (e: Exception) {
                android.util.Log.e("GlobalSleepVM", "Failed to observe devices: ${e.message}")
            }
        }
    }

    /**
     * 현재 기기 정보를 등록하는 작업을 Job으로 관리하여 중복 실행을 방지합니다.
     */
    private fun registerCurrentUIDDevice(uid: String) {
        deviceRegisterJob?.cancel()
        deviceRegisterJob = viewModelScope.launch {
            registerCurrentDevice(uid)
        }
    }

    /**
     * 로그아웃 처리 (현재 기기 정보 삭제 후 사인아웃)
     */
    fun logout(onComplete: () -> Unit = {}) {
        val uid = currentUid ?: return
        
        // 관찰 중이던 Job들 취소
        observeJob?.cancel()
        devicesJob?.cancel()
        deviceRegisterJob?.cancel()

        viewModelScope.launch {
            try {
                // 1. 현재 기기 등록 해제
                val deviceId = DeviceInfoProvider.getDeviceId()
                unregisterDeviceUseCase(uid, deviceId)
                
                // 2. 실제 로그아웃 수행
                signOutUseCase()
                
                // 3. 상태 초기화
                currentUid = null
                _currentUser.value = null
                _sleepState.value = null
                _registeredDevices.value = emptyList()
                
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("GlobalSleepVM", "Logout failed: ${e.message}")
            }
        }
    }

    /**
     * 수면 상태를 수동으로 변경 (낙관적 UI 반영)
     */
    fun toggleSleepState(isSleeping: Boolean, targetWakeUpTime: Long? = null) {
        if (_isToggleLoading.value) return // 중복 실행 방지
        val uid = currentUid ?: return
        
        viewModelScope.launch {
            _isToggleLoading.value = true
            try {
                // 로컬 스토리지의 최신 설정값 읽어오기
                val bedtime = sleepSettingsRepository.bedtime.first()
                val wakeTime = sleepSettingsRepository.wakeTime.first()

                // [낙관적 제어] 서버 응답 전 로컬 상태 먼저 강제 업데이트하여 즉시 애니메이션 전환 유도
                val optimisticState = _sleepState.value?.copy(
                    isSleeping = isSleeping,
                    targetWakeUpTime = targetWakeUpTime,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    lastUpdatedAt = 0L
                ) ?: UserSleepState(
                    uid = uid, 
                    isSleeping = isSleeping, 
                    targetWakeUpTime = targetWakeUpTime,
                    bedtime = bedtime,
                    wakeTime = wakeTime
                )
                
                _sleepState.value = optimisticState
                android.util.Log.w("SleepBounceDebug", "[toggleSleepState] 낙관적 상태 설정: isSleeping=$isSleeping")

                // 실제 서버 전송 (전역 스케줄 정보 포함)
                val result = updateUserSleepStateUseCase(
                    uid = uid, 
                    isSleeping = isSleeping, 
                    targetWakeUpTime = targetWakeUpTime,
                    bedtime = bedtime,
                    wakeTime = wakeTime
                )
                result.onFailure {
                    _error.value = "Failed to update state: ${it.message}"
                }
            } finally {
                _isToggleLoading.value = false
            }
        }
    }
}
