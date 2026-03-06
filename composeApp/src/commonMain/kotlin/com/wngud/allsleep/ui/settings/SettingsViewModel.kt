package com.wngud.allsleep.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.domain.usecase.sleep.UnregisterDeviceUseCase
import com.wngud.allsleep.platform.DeviceInfoProvider
import kotlinx.coroutines.launch

/**
 * 설정 탭 ViewModel
 * 프로필, 수면·앱·계정 설정 상태를 관리합니다.
 */
class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleNotification ->
                _state.update { it.copy(isNotificationEnabled = intent.enabled) }

            is SettingsIntent.ToggleDnd ->
                _state.update { it.copy(isDndEnabled = intent.enabled) }

            is SettingsIntent.ChangeTheme ->
                _state.update { it.copy(appTheme = intent.theme) }

            is SettingsIntent.ChangeLanguage ->
                _state.update { it.copy(appLanguage = intent.language) }

            is SettingsIntent.ShowLogoutDialog ->
                _state.update { it.copy(showLogoutDialog = true) }

            is SettingsIntent.ShowDeleteAccountDialog ->
                _state.update { it.copy(showDeleteAccountDialog = true) }

            is SettingsIntent.DismissDialog ->
                _state.update { it.copy(showLogoutDialog = false, showDeleteAccountDialog = false) }

            is SettingsIntent.ConfirmLogout -> {
                _state.update { it.copy(showLogoutDialog = false) }
                performLogout()
            }

            is SettingsIntent.ConfirmDeleteAccount -> {
                _state.update { it.copy(showDeleteAccountDialog = false) }
                // TODO: 계정 삭제 UseCase 연결
            }

            // Navigation 이벤트는 TODO: NavController 연결 시 처리
            else -> Unit
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    // 1. 현재 기기 등록 해제
                    val deviceId = DeviceInfoProvider.getDeviceId()
                    unregisterDeviceUseCase(user.uid, deviceId)
                }
                
                // 2. 실제 로그아웃 수행
                signOutUseCase()
                
                // TODO: UI 레이어에서 세션 종료 감지 후 로그인 화면으로 이동 필요
            } catch (e: Exception) {
                // 에러 처리 (로그 출력 등)
            }
        }
    }
}
