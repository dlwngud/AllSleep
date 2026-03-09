package com.wngud.allsleep.domain.usecase.onboarding

import com.wngud.allsleep.domain.repository.SleepSettingsRepository

class CompleteOnboardingUseCase(private val repository: SleepSettingsRepository) {
    suspend operator fun invoke() {
        repository.saveOnboardingCompleted(true)
    }
}
