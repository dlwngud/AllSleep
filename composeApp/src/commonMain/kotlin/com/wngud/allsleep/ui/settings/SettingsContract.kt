package com.wngud.allsleep.ui.settings

/**
 * 설정 화면 State/Intent (MVI 패턴)
 *
 * Stitch 디자인 기준:
 * - 프로필 영역
 * - 수면 설정 (취침/기상 시간, 알림, 방해 금지)
 * - 앱 설정 (테마, 언어, 알람음)
 * - 계정 (연결된 기기, 데이터 동기화, 로그아웃, 계정 삭제)
 */

enum class AppTheme { DARK, LIGHT, SYSTEM }
enum class AppLanguage { KOREAN, ENGLISH }

data class SettingsState(
    val isPremium: Boolean = false, // 프리미엄 구독 여부

    // 수면 설정
    val isNotificationEnabled: Boolean = true,
    val isDndEnabled: Boolean = true,
    // 앱 설정
    val appTheme: AppTheme = AppTheme.DARK,
    val appLanguage: AppLanguage = AppLanguage.KOREAN,
    // 다이얼로그
    val showDeleteAccountDialog: Boolean = false,
    val showLogoutDialog: Boolean = false
)

sealed interface SettingsIntent {
    data object UpgradePremium : SettingsIntent
    data object ManageSubscription : SettingsIntent
    data object NavigateSleepTime : SettingsIntent
    data object NavigateWakeTime : SettingsIntent
    data class ToggleNotification(val enabled: Boolean) : SettingsIntent
    data class ToggleDnd(val enabled: Boolean) : SettingsIntent
    data object NavigateAlarmSound : SettingsIntent
    data class ChangeTheme(val theme: AppTheme) : SettingsIntent
    data class ChangeLanguage(val language: AppLanguage) : SettingsIntent
    data object NavigateDeviceManagement : SettingsIntent
    data object NavigateDataSync : SettingsIntent
    data object ShowLogoutDialog : SettingsIntent
    data object ConfirmLogout : SettingsIntent
    data object ShowDeleteAccountDialog : SettingsIntent
    data object ConfirmDeleteAccount : SettingsIntent
    data object DismissDialog : SettingsIntent
}
