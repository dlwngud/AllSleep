package com.wngud.allsleep.ui.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.sleep.RegisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.platform.DeviceInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 전역 수면 상태(App State)를 관리하는 ViewModel
 * - 앱 시작 시 현재 사용자 UID를 확인 후 기기 정보를 Firestore에 등록
 * - 실시간으로 수면 상태를 구독하여 전역 UI 상태로 제공
 */
class GlobalSleepViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeUserSleepStateUseCase: ObserveUserSleepStateUseCase,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase
) : ViewModel() {

    private val _sleepState = MutableStateFlow<UserSleepState?>(null)
    val sleepState: StateFlow<UserSleepState?> = _sleepState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 현재 사용자 캐싱용 변수
    private var currentUid: String? = null

    init {
        // ViewModel 생성 시점에 현재 사용자를 확인하고 관찰 시작 + 기기 등록
        viewModelScope.launch {
            android.util.Log.d("GlobalSleepVM", "[init] ViewModel 생성됨")
            val user = getCurrentUserUseCase()
            android.util.Log.d("GlobalSleepVM", "[init] user=$user")
            if (user != null) {
                currentUid = user.uid
                android.util.Log.d("GlobalSleepVM", "[init] uid=${user.uid}, 기기 등록 시작")
                // 1. 실시간 수면 상태 구독 시작
                startObserving(user.uid)
                // 2. 현재 기기를 Firestore에 등록/갱신 (1-3 구현)
                registerCurrentDevice(user.uid)
            } else {
                android.util.Log.w("GlobalSleepVM", "[init] 로그인 사용자 없음 - 기기 등록 skip")
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
            android.util.Log.d("GlobalSleepVM", "[registerDevice] deviceId=${deviceState.deviceId}, name=${deviceState.deviceName}")
            registerDeviceUseCase(uid, deviceState)
                .onSuccess { 
                    android.util.Log.d("GlobalSleepVM", "[registerDevice] Firestore 저장 성공! (UID=$uid)") 
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
