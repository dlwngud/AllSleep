package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.PurchaseResult
import com.wngud.allsleep.platform.SubscriptionPackage
import com.wngud.allsleep.platform.SubscriptionStatus

interface BillingRepository {
    suspend fun getOfferings(): List<SubscriptionPackage>
    suspend fun getSubscriptionStatus(): Result<SubscriptionStatus>
    suspend fun purchasePackage(packageId: String, context: PlatformContext): Result<PurchaseResult>
    suspend fun restorePurchases(): Result<Unit>
    suspend fun loginUser(uid: String)
    suspend fun logoutUser()
}
