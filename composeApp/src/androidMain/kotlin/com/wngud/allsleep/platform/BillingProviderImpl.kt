package com.wngud.allsleep.platform

import android.app.Application
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.models.Period
import com.wngud.allsleep.BuildConfig
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

class BillingProviderImpl(
    private val application: Application
) : BillingProvider {

    override suspend fun loginUser(uid: String) {
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logInWith(
                appUserID = uid,
                onError = { /* 로거 연동 필요 */ },
                onSuccess = { _, _ -> continuation.resume(Unit) }
            )
        }
    }

    override suspend fun logoutUser() {
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logOutWith(
                onError = { /* 에러 처리 */ },
                onSuccess = { continuation.resume(Unit) }
            )
        }
    }

    override suspend fun getOfferings(): List<SubscriptionPackage> {
        val offerings = suspendCancellableCoroutine<Offerings?> { continuation ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    continuation.resumeWithException(Exception(error.message))
                },
                onSuccess = { offerings ->
                    continuation.resume(offerings)
                }
            )
        }

        return offerings?.selectOfferingForBuild()?.availablePackages?.map { rcPackage ->
            rcPackage.toSubscriptionPackage()
        } ?: emptyList()
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> = runCatching {
        val customerInfo = suspendCancellableCoroutine<CustomerInfo?> { continuation ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    continuation.resumeWithException(Exception(error.message))
                },
                onSuccess = { customerInfo ->
                    continuation.resume(customerInfo)
                }
            )
        } ?: throw Exception("Customer info not available")

        val premiumEntitlement = customerInfo.entitlements["premium"]
            ?: customerInfo.entitlements.active.values.firstOrNull()
        SubscriptionStatus(
            isPremiumActive = premiumEntitlement?.isActive == true,
            entitlementId = premiumEntitlement?.identifier,
            productIdentifier = premiumEntitlement?.productIdentifier,
            productPlanIdentifier = premiumEntitlement?.productPlanIdentifier,
            willRenew = premiumEntitlement?.willRenew,
            periodType = premiumEntitlement?.periodType?.name,
            expirationDateMillis = premiumEntitlement?.expirationDate?.time,
            latestPurchaseDateMillis = premiumEntitlement?.latestPurchaseDate?.time,
            originalPurchaseDateMillis = premiumEntitlement?.originalPurchaseDate?.time,
            store = premiumEntitlement?.store?.name,
            managementUrl = customerInfo.managementURL?.toString(),
            isSandbox = premiumEntitlement?.isSandbox == true,
            unsubscribeDetectedAtMillis = premiumEntitlement?.unsubscribeDetectedAt?.time,
            billingIssueDetectedAtMillis = premiumEntitlement?.billingIssueDetectedAt?.time
        )
    }

    override suspend fun purchasePackage(
        packageId: String,
        context: PlatformContext
    ): Result<PurchaseResult> = runCatching {
        // 1. 현재 빌드에서 사용할 Offering 안에서만 패키지 탐색
        val offerings = suspendCancellableCoroutine<Offerings?> { continuation ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { continuation.resumeWithException(Exception(it.message)) },
                onSuccess = { continuation.resume(it) }
            )
        }
        val rcPackage = offerings
            ?.selectOfferingForBuild()
            ?.availablePackages
            ?.find { it.identifier == packageId }
            ?: throw Exception("Package not found")

        // 2. 결제 실행
        val customerInfo = suspendCancellableCoroutine<CustomerInfo?> { continuation ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(context, rcPackage).build(),
                onError = { error, userCancelled ->
                    if (userCancelled) {
                        continuation.resume(null)
                    } else {
                        continuation.resumeWithException(Exception(error.message))
                    }
                },
                onSuccess = { _, customerInfo ->
                    continuation.resume(customerInfo)
                }
            )
        }

        PurchaseResult(
            isSuccess = customerInfo != null,
            isPremiumNow = customerInfo?.entitlements?.get("premium")?.isActive == true ||
                customerInfo?.entitlements?.active?.values?.firstOrNull()?.isActive == true
        )
    }

    override suspend fun restorePurchases(): Result<Unit> = runCatching {
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { continuation.resumeWithException(Exception(it.message)) },
                onSuccess = { continuation.resume(Unit) }
            )
        }
    }
}

private fun Offerings.selectOfferingForBuild(): Offering? {
    return if (BuildConfig.REVENUECAT_USE_TEST_STORE) {
        all["test_store"] ?: current ?: all["default"]
    } else {
        current ?: all["default"]
    }
}

private fun com.revenuecat.purchases.Package.toSubscriptionPackage(): SubscriptionPackage {
    val type = when (packageType) {
        com.revenuecat.purchases.PackageType.MONTHLY -> PackageType.MONTHLY
        com.revenuecat.purchases.PackageType.ANNUAL -> PackageType.ANNUAL
        com.revenuecat.purchases.PackageType.LIFETIME -> PackageType.LIFETIME
        else -> PackageType.UNKNOWN
    }

    val badge = when (type) {
        PackageType.ANNUAL -> "가장 인기"
        PackageType.LIFETIME -> "최고의 가치"
        else -> null
    }

    val subDescription = when (type) {
        PackageType.ANNUAL -> "34% 할인 혜택"
        PackageType.LIFETIME -> "영구 소장 (단 한 번 결제)"
        PackageType.MONTHLY -> "부담 없이 시작하기"
        else -> null
    }
    val freeTrialDays = product.defaultOption
        ?.freePhase
        ?.billingPeriod
        ?.toTrialDays()
        ?: 0

    return SubscriptionPackage(
        id = identifier,
        title = product.title,
        priceString = product.price.formatted,
        type = type,
        productId = product.id,
        badge = badge,
        subDescription = subDescription,
        hasFreeTrial = freeTrialDays > 0,
        freeTrialDays = freeTrialDays
    )
}

private fun Period.toTrialDays(): Int {
    return when (unit) {
        Period.Unit.DAY -> value
        Period.Unit.WEEK -> value * 7
        Period.Unit.MONTH -> value * 30
        Period.Unit.YEAR -> value * 365
        Period.Unit.UNKNOWN -> 0
    }
}
