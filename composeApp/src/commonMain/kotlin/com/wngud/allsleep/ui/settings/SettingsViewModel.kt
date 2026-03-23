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

import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.wngud.allsleep.domain.usecase.auth.DeleteAccountUseCase

/**
 * 설정 탭 ViewModel
 * 프로필, 수면·앱·계정 설정 상태를 관리합니다.
 */
class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    // ... (init block remains the same)
    init {
        // 로컬 수면 설정값 관찰
        viewModelScope.launch {
            sleepSettingsRepository.bedtime.collectLatest { bedtime ->
                _state.update { it.copy(bedtime = bedtime) }
            }
        }
        viewModelScope.launch {
            sleepSettingsRepository.wakeTime.collectLatest { wakeTime ->
                _state.update { it.copy(wakeTime = wakeTime) }
            }
        }
    }

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

            is SettingsIntent.UpdateBedtime -> updateBedtime(intent.time)
            is SettingsIntent.UpdateWakeTime -> updateWakeTime(intent.time)

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
                performDeleteAccount()
            }

            // Navigation 이벤트는 TODO: NavController 연결 시 처리
            else -> Unit
        }
    }
    
    private fun performDeleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = deleteAccountUseCase()
            result.onSuccess {
                // UI 레이어(SettingsScreen)에서 로그인 화면으로 이동하는 처리를 위해
                // 진행 중인 로딩 상태는 풀고, 성공 이벤트는 별도의 Flow나 
                // Navigation Event로 처리하는 것이 좋으나, 
                // 현재는 로그아웃과 동일하게 세션 종료를 감지하여 처리하도록 유도하거나 
                // error 없이 isLoading이 false가 되는 것을 감지할 수 있습니다.
                _state.update { it.copy(isLoading = false) }
                // TODO: UI 레이어에서 세션 종료 감지 후 로그인 화면으로 이동 필요
            }.onFailure { e ->
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "계정 삭제에 실패했습니다."
                    ) 
                }
            }
        }
    }

    private fun updateBedtime(time: String) {
        viewModelScope.launch {
            sleepSettingsRepository.saveSleepSchedule(time, _state.value.wakeTime)
        }
    }

    private fun updateWakeTime(time: String) {
        viewModelScope.launch {
            sleepSettingsRepository.saveSleepSchedule(_state.value.bedtime, time)
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    // 1. 현재 기기 등록 해제
                    val deviceId = deviceInfoProvider.getDeviceId()
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
