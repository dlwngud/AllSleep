package com.wngud.allsleep.ui.settings

import com.wngud.allsleep.domain.model.DeviceState

/**
 * 설정 화면 State/Intent (MVI 패턴)
 */

data class SettingsState(
    val user: com.wngud.allsleep.domain.model.User? = null, // 사용자 정보
    val isPremium: Boolean = false, // 프리미엄 구독 여부

    // 수면 설정 및 권한
    val isNotificationEnabled: Boolean = true,
    val isAccessibilityEnabled: Boolean = false, // 커스텀 접근성 서비스 상태
    
    // 평일 루틴
    val weekdayBedtime: String = "23:00",
    val weekdayWakeTime: String = "07:00",
    
    // 주말 루틴
    val weekendBedtime: String = "00:00",
    val weekendWakeTime: String = "09:00",
    
    // 연결된 기기 목록
    val devices: List<DeviceState> = emptyList(),

    // 다이얼로그
    val showDeleteAccountDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showUnregisterDialog: Boolean = false,
    val showEditNameDialog: Boolean = false,
    val deviceToUnregister: DeviceState? = null,
    
    // 로딩 상태
    val isLoading: Boolean = false,
    
    // 이벤트 처리용 (단회성 에러 메시지 등)
    val error: String? = null
)

sealed interface SettingsIntent {
    data object UpgradePremium : SettingsIntent
    data object ManageSubscription : SettingsIntent
    data class ToggleNotification(val enabled: Boolean) : SettingsIntent
    data object OpenAccessibilitySettings : SettingsIntent // 접근성 설정 화면 이동
    
    data object ShowEditNameDialog : SettingsIntent // 프로필 이름 수정 다이얼로그 노출
    data class UpdateDisplayName(val name: String) : SettingsIntent // 프로필 이름 수정 수행
    
    data class RenameDevice(val device: DeviceState, val newName: String) : SettingsIntent // 기기 이름 변경
    data object NavigateDeviceManagement : SettingsIntent // 기기 관리 바텀시트 열기
    
    data class ShowUnregisterDialog(val device: DeviceState) : SettingsIntent
    data object ConfirmUnregisterDevice : SettingsIntent

    data object ShowLogoutDialog : SettingsIntent
    data object ConfirmLogout : SettingsIntent
    data object ShowDeleteAccountDialog : SettingsIntent
    data object ConfirmDeleteAccount : SettingsIntent
    data object DismissDialog : SettingsIntent
}
