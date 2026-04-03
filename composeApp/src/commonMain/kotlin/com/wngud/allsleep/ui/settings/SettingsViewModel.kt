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

import com.wngud.allsleep.domain.usecase.sleep.ObserveRegisteredDevicesUseCase
import com.wngud.allsleep.domain.usecase.sleep.RenameDeviceUseCase
import com.wngud.allsleep.domain.usecase.auth.DeleteAccountUseCase
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

/**
 * 설정 탭 ViewModel
 * 프로필, 수면 설정(알림/권한) 및 계정/기기 관리 상태를 관리합니다.
 */
class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeUserUseCase: com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val observeRegisteredDevicesUseCase: ObserveRegisteredDevicesUseCase,
    private val renameDeviceUseCase: RenameDeviceUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateUserProfileUseCase: com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        // 로컬 수면 설정값 관찰 (평일/주말 루틴 정보 요약용)
        viewModelScope.launch {
            val weekdayFlow = combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime
            ) { b, w -> b to w }
            
            val weekendFlow = combine(
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime
            ) { b, w -> b to w }

            combine(weekdayFlow, weekendFlow) { wd, we -> wd to we }
                .collectLatest { (wd, we) ->
                    _state.update { 
                        it.copy(
                            weekdayBedtime = wd.first,
                            weekdayWakeTime = wd.second,
                            weekendBedtime = we.first,
                            weekendWakeTime = we.second
                        )
                    }
                }
        }
        
        // 유저 정보 실시간 관찰
        viewModelScope.launch {
            observeUserUseCase().collectLatest { user ->
                _state.update { it.copy(user = user, isPremium = user?.isPremium ?: false) }
                
                // 유저가 있는 경우 기기 목록 관찰 시작
                if (user != null) {
                    observeRegisteredDevicesUseCase(user.uid).collectLatest { devices ->
                        _state.update { it.copy(devices = devices) }
                    }
                }
            }
        }
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleNotification ->
                _state.update { it.copy(isNotificationEnabled = intent.enabled) }

            is SettingsIntent.ShowEditNameDialog -> {
                _state.update { it.copy(showEditNameDialog = true) }
            }
            is SettingsIntent.UpdateDisplayName -> {
                _state.update { it.copy(showEditNameDialog = false) }
                updateDisplayName(intent.name)
            }

            is SettingsIntent.RenameDevice -> renameDevice(intent.device, intent.newName)
            
            is SettingsIntent.ShowUnregisterDialog -> {
                _state.update { it.copy(showUnregisterDialog = true, deviceToUnregister = intent.device) }
            }
            is SettingsIntent.ConfirmUnregisterDevice -> {
                val device = _state.value.deviceToUnregister
                if (device != null) {
                    unregisterDevice(device)
                    _state.update { it.copy(showUnregisterDialog = false, deviceToUnregister = null) }
                }
            }

            is SettingsIntent.ShowLogoutDialog ->
                _state.update { it.copy(showLogoutDialog = true) }

            is SettingsIntent.ShowDeleteAccountDialog ->
                _state.update { it.copy(showDeleteAccountDialog = true) }

            is SettingsIntent.DismissDialog ->
                _state.update { it.copy(
                    showLogoutDialog = false, 
                    showDeleteAccountDialog = false,
                    showUnregisterDialog = false,
                    showEditNameDialog = false,
                    deviceToUnregister = null
                ) }

            is SettingsIntent.ConfirmLogout -> {
                _state.update { it.copy(showLogoutDialog = false) }
                performLogout()
            }

            is SettingsIntent.ConfirmDeleteAccount -> {
                _state.update { it.copy(showDeleteAccountDialog = false) }
                performDeleteAccount()
            }

            // OpenAccessibilitySettings 등 UI Navigation/System Call 영역은 UI단에서 처리
            else -> Unit
        }
    }

    /** SettingsScreen에서 AccessibilityPermissionRequester.isGranted() 결과를 동기화 */
    fun updateAccessibilityStatus(isEnabled: Boolean) {
        _state.update { it.copy(isAccessibilityEnabled = isEnabled) }
    }

    /** 알림 권한 결과(granted) 처리: 상태를 저장 */
    fun onNotificationPermissionResult(granted: Boolean) {
        _state.update { it.copy(isNotificationEnabled = granted) }
    }

    private fun updateDisplayName(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        
        val currentUser = _state.value.user ?: return
        if (currentUser.displayName == trimmedName) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val updatedUser = currentUser.copy(displayName = trimmedName)
            updateUserProfileUseCase(updatedUser)
                .onSuccess {
                    _state.update { it.copy(user = updatedUser, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = "프로필 업데이트 실패: ${e.message}") }
                }
        }
    }

    private fun renameDevice(device: com.wngud.allsleep.domain.model.DeviceState, newName: String) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _state.update { it.copy(isLoading = true, error = null) }
            val result = renameDeviceUseCase(user.uid, device, newName, _state.value.devices)
            result.onSuccess {
                _state.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun unregisterDevice(device: com.wngud.allsleep.domain.model.DeviceState) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            unregisterDeviceUseCase(user.uid, device.deviceId)
        }
    }
    
    private fun performDeleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = deleteAccountUseCase()
            result.onSuccess {
                _state.update { it.copy(isLoading = false) }
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

    private fun performLogout() {
        viewModelScope.launch {
            try {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    val deviceId = deviceInfoProvider.getDeviceId()
                    unregisterDeviceUseCase(user.uid, deviceId)
                }
                signOutUseCase()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
}
