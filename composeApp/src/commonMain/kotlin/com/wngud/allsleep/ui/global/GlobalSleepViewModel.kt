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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _sleepState = MutableStateFlow<UserSleepState?>(null)
    val sleepState: StateFlow<UserSleepState?> = _sleepState.asStateFlow()

    private val _registeredDevices = MutableStateFlow<List<DeviceState>>(emptyList())
    val registeredDevices: StateFlow<List<DeviceState>> = _registeredDevices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
    // 앱 시작 시 로컬 데이터(Auth, DataStore)를 완전히 읽을 때까지 대기하기 위한 상태
    private val _isStateInitialized = MutableStateFlow(false)
    val isStateInitialized: StateFlow<Boolean> = _isStateInitialized.asStateFlow()
    
    // 현재 사용자 캐싱용 변수
    private var currentUid: String? = null

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
                completeOnboardingUseCase()
                _isOnboardingCompleted.value = true
            }
            
            if (user != null) {
                currentUid = user.uid
                android.util.Log.d("GlobalSleepVM", "[init] uid=${user.uid}, 기기/프로필 등록 시작")
                launch { updateUserProfileUseCase(user) }
                startObserving(user.uid)
                registerCurrentDevice(user.uid)
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
                            completeOnboardingUseCase()
                            _isOnboardingCompleted.value = true
                        }
                        launch { updateUserProfileUseCase(observedUser) }
                        startObserving(observedUser.uid)
                        registerCurrentDevice(observedUser.uid)
                    } else {
                        currentUid = null
                        _sleepState.value = null
                        _registeredDevices.value = emptyList()
                    }
                }
            }
            
            launch {
                observeOnboardingCompletedUseCase().collect { completed ->
                    _isOnboardingCompleted.value = completed
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
        viewModelScope.launch {
            try {
                observeUserSleepStateUseCase(uid).collect { state ->
                    _sleepState.value = state
                }
            } catch (e: Exception) {
                _error.value = "Failed to observe state: ${e.message}"
            }
        }
        
        // 기기 목록 실시간 구독 추가
        viewModelScope.launch {
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
     * 로그아웃 처리 (현재 기기 정보 삭제 후 사인아웃)
     */
    fun logout(onComplete: () -> Unit = {}) {
        val uid = currentUid ?: return
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
        val uid = currentUid ?: return
        
        viewModelScope.launch {
            // [낙관적 제어] 서버 응답 전 로컬 상태 먼저 강제 업데이트하여 즉시 애니메이션 전환 유도
            val optimisticState = _sleepState.value?.copy(
                isSleeping = isSleeping,
                targetWakeUpTime = targetWakeUpTime,
                lastUpdatedAt = 0L
            ) ?: UserSleepState(uid = uid, isSleeping = isSleeping, targetWakeUpTime = targetWakeUpTime)
            
            _sleepState.value = optimisticState

            // 실제 서버 전송
            val result = updateUserSleepStateUseCase(uid, isSleeping, targetWakeUpTime)
            result.onFailure {
                _error.value = "Failed to update state: ${it.message}"
            }
        }
    }
}
