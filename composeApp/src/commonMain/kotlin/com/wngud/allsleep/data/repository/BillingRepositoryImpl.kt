package com.wngud.allsleep.data.repository

import com.wngud.allsleep.domain.repository.BillingRepository
import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.PurchaseResult
import com.wngud.allsleep.platform.SubscriptionPackage
import com.wngud.allsleep.platform.SubscriptionStatus

class BillingRepositoryImpl(
    private val billingProvider: BillingProvider,
) : BillingRepository {
    override suspend fun getOfferings(): List<SubscriptionPackage> {
        return billingProvider.getOfferings()
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        return billingProvider.getSubscriptionStatus()
    }

    override suspend fun purchasePackage(
        packageId: String,
        context: PlatformContext,
    ): Result<PurchaseResult> {
        return billingProvider.purchasePackage(packageId, context)
    }

    override suspend fun restorePurchases(): Result<Unit> {
        return billingProvider.restorePurchases()
    }

    override suspend fun loginUser(uid: String) {
        billingProvider.loginUser(uid)
    }

    override suspend fun logoutUser() {
        billingProvider.logoutUser()
    }
}
