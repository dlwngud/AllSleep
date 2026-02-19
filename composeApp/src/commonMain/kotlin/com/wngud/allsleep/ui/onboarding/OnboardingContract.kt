package com.wngud.allsleep.ui.onboarding

import com.wngud.allsleep.domain.model.User

/**
 * 온보딩 화면의 State와 Intent 정의 (MVI 패턴)
 */

data class OnboardingState(
    val bedtime: String = "23:00",
    val wakeTime: String = "07:00",
    val userName: String = "사용자",
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface OnboardingIntent {
    data object LoadCurrentUser : OnboardingIntent
    data class UpdateBedtime(val time: String) : OnboardingIntent
    data class UpdateWakeTime(val time: String) : OnboardingIntent
    data object CompleteOnboarding : OnboardingIntent
}
