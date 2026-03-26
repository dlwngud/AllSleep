package com.wngud.allsleep.ui.settings

import com.wngud.allsleep.domain.model.DeviceState

/**
 * 설정 화면 State/Intent (MVI 패턴)
 *
 * 최근 개편 기준:
 * - 수면 설정 (취침/기상 시간, 권한(알림/접근성))
 * - 계정/기기 관리 (동기화된 기기 목록, 로그아웃, 계정 삭제)
 */

data class SettingsState(
    val user: com.wngud.allsleep.domain.model.User? = null, // 사용자 정보
    val isPremium: Boolean = false, // 프리미엄 구독 여부

    // 수면 설정 및 권한
    val isNotificationEnabled: Boolean = true,
    val isAccessibilityEnabled: Boolean = false, // 커스텀 접근성 서비스 상태
    val bedtime: String = "23:00",
    val wakeTime: String = "07:00",
    
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
    data class UpdateBedtime(val time: String) : SettingsIntent
    data class UpdateWakeTime(val time: String) : SettingsIntent
    
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

