package com.wngud.allsleep.domain.usecase.onboarding

import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveOnboardingCompletedUseCase(private val repository: SleepSettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isOnboardingCompleted
}
