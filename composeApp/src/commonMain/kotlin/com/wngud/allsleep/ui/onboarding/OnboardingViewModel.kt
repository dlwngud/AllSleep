package com.wngud.allsleep.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 온보딩 ViewModel (MVI 패턴 적용)
 * - 단일 State 관리 (OnboardingState)
 * - Intent 처리 (OnboardingIntent)
 */
class OnboardingViewModel(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.LoadCurrentUser -> loadCurrentUser()
            is OnboardingIntent.UpdateBedtime -> updateBedtime(intent.time)
            is OnboardingIntent.UpdateWakeTime -> updateWakeTime(intent.time)
            is OnboardingIntent.CompleteOnboarding -> completeOnboarding()
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            if (user != null) {
                val displayName = user.displayName ?: user.email?.split("@")?.get(0) ?: "사용자"
                _state.update { 
                    it.copy(
                        user = user,
                        userName = displayName,
                        isLoading = false
                    ) 
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateBedtime(time: String) {
        _state.update { it.copy(bedtime = time) }
    }

    private fun updateWakeTime(time: String) {
        _state.update { it.copy(wakeTime = time) }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sleepSettingsRepository.saveSleepSchedule(_state.value.bedtime, _state.value.wakeTime)
            _state.update { it.copy(isLoading = false) }
        }
    }
}
