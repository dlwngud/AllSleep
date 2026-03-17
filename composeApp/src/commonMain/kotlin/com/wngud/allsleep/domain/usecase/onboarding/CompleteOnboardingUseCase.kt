package com.wngud.allsleep.domain.usecase.onboarding

import com.wngud.allsleep.domain.repository.SleepSettingsRepository

class CompleteOnboardingUseCase(private val repository: SleepSettingsRepository) {
    suspend operator fun invoke(bedtime: String, wakeTime: String) {
        repository.saveSleepSchedule(bedtime, wakeTime)
        repository.saveOnboardingCompleted(true)
    }
}
