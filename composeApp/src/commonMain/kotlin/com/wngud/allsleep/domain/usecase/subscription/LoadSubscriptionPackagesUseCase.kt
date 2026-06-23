package com.wngud.allsleep.domain.usecase.subscription

import com.wngud.allsleep.domain.repository.BillingRepository

class LoadSubscriptionPackagesUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke() = billingRepository.getOfferings()
}
