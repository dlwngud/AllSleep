package com.wngud.allsleep.domain.usecase.subscription

import com.wngud.allsleep.domain.repository.BillingRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase

class SyncPremiumStatusUseCase(
    private val billingRepository: BillingRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
) {
    suspend operator fun invoke(): Boolean? {
        val statusResult = billingRepository.getSubscriptionStatus()
        if (statusResult.isFailure) return null

        val isPremiumNow = statusResult.getOrThrow().isPremiumActive
        val currentUser = getCurrentUserUseCase()
        if (currentUser != null && currentUser.isPremium != isPremiumNow) {
            updateUserProfileUseCase(currentUser.copy(isPremium = isPremiumNow))
        }
        sleepSettingsRepository.savePremiumStatus(isPremiumNow)
        return isPremiumNow
    }
}
