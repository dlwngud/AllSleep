package com.wngud.allsleep.ui.global

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.model.UserSleepState

/**
 * 전역 수면 상태 관리를 위한 MVI 컨트랙트
 */
class GlobalSleepContract {
    
    // 1. 상태 (State) - 단일 데이터 클래스로 모든 상태 통합
    data class State(
        val currentUser: User? = null,
        val isPremium: Boolean = false,
        val sleepState: UserSleepState? = null,
        val registeredDevices: List<DeviceState> = emptyList(),
        val isOnboardingCompleted: Boolean = false,
        val isStateInitialized: Boolean = false,
        val isToggleLoading: Boolean = false,
        val showDeviceLimitDialog: Boolean = false,
        val cachedDeviceStateToRegister: DeviceState? = null,
        val error: String? = null
    )

    // 2. 의도 (Intent) - 사용자의 액션 또는 내부 이벤트 정의
    sealed interface Intent {
        data object RequestInitialize : Intent
        data object RequestLogout : Intent
        data class ToggleSleepState(
            val isSleeping: Boolean, 
            val targetWakeUpTime: Long? = null
        ) : Intent
        data class CompleteOnboarding(
            val bedtime: String, 
            val wakeTime: String
        ) : Intent
        
        // 기기 한도 초과 처리용 Intent
        data object ReplaceDevice : Intent
        data object CancelDeviceRegistration : Intent
        data object UpgradeToPremium : Intent
    }

    // 3. 효과 (Effect) - 일회성 이벤트 정의
    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data object NavigateToSubscription : Effect
    }
}
