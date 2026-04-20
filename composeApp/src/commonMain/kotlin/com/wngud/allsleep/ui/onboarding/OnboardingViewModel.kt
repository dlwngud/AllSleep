package com.wngud.allsleep.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.onboarding.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.LoadCurrentUser -> loadCurrentUser()
            is OnboardingIntent.UpdateBedtime -> updateBedtime(intent.time)
            is OnboardingIntent.UpdateWakeTime -> updateWakeTime(intent.time)
            is OnboardingIntent.UpdateUserName -> updateUserName(intent.name)
            is OnboardingIntent.CompleteOnboarding -> completeOnboarding()
        }
    }

    private fun updateUserName(name: String) {
        _state.update { it.copy(userName = name, user = null) } // user = null for guest
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = getCurrentUserUseCase()
            if (user != null) {
                val displayName = user.displayName ?: user.email?.split("@")?.get(0) ?: "사용자"
                _state.update { it.copy(user = user, userName = displayName, isLoading = false) }
                
                // 로그인에 성공했다면 온보딩 과정은 완료된 것으로 간주하여 저장 (회귀 방지)
                completeOnboardingUseCase(
                    bedtime = _state.value.bedtime,
                    wakeTime = _state.value.wakeTime
                )
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }


    private fun updateBedtime(time: String) = _state.update { it.copy(bedtime = time) }
    private fun updateWakeTime(time: String) = _state.update { it.copy(wakeTime = time) }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            completeOnboardingUseCase(
                bedtime = _state.value.bedtime,
                wakeTime = _state.value.wakeTime
            )
            _state.update { it.copy(isLoading = false) }
        }
    }
}
