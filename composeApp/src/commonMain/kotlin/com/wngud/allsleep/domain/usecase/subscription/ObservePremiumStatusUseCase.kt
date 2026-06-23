package com.wngud.allsleep.domain.usecase.subscription

import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObservePremiumStatusUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = sleepSettingsRepository.isPremium
}
