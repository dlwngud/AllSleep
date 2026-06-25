package com.wngud.allsleep.domain.usecase.ad

import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import kotlinx.coroutines.flow.first

class CanShowAppOpenAdUseCase(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeOnboardingCompletedUseCase: ObserveOnboardingCompletedUseCase,
) {
    suspend operator fun invoke(): Boolean {
        val user = getCurrentUserUseCase() ?: return false
        if (user.isPremium) return false

        return observeOnboardingCompletedUseCase().first()
    }
}
