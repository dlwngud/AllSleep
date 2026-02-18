package com.wngud.allsleep.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 온보딩 ViewModel
 * - 온보딩 과정의 상태 관리 (시간 설정 등)
 * - 완료 시 데이터 저장 (Repository 호출)
 */
class OnboardingViewModel(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val authRepository: com.wngud.allsleep.domain.repository.AuthRepository
) : ViewModel() {

    private val _bedtime = MutableStateFlow("23:00")
    val bedtime = _bedtime.asStateFlow()

    private val _wakeTime = MutableStateFlow("07:00")
    val wakeTime = _wakeTime.asStateFlow()

    private val _userName = MutableStateFlow("사용자")
    val userName = _userName.asStateFlow()

    fun fetchCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _userName.value = user.displayName ?: user.email?.split("@")?.get(0) ?: "사용자"
            }
        }
    }

    fun updateBedtime(time: String) {
        _bedtime.value = time
    }

    fun updateWakeTime(time: String) {
        _wakeTime.value = time
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            sleepSettingsRepository.saveSleepSchedule(_bedtime.value, _wakeTime.value)
        }
    }
}
