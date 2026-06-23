package com.wngud.allsleep.domain.usecase.subscription

import com.wngud.allsleep.domain.repository.BillingRepository
import com.wngud.allsleep.platform.PlatformContext

class PurchaseSubscriptionUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(packageId: String, context: PlatformContext) =
        billingRepository.purchasePackage(packageId, context)
}
