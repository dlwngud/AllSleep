package com.wngud.allsleep.platform

import android.app.Activity
import android.app.Application
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

class BillingProviderImpl(
    private val application: Application
) : BillingProvider {

    override suspend fun loginUser(uid: String) {
        /*
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logInWith(
                appUserID = uid,
                onError = { /* 로깅 정도만 수행 */ },
                onSuccess = { _, _ -> continuation.resume(Unit) }
            )
        }
        */
    }

    override suspend fun logoutUser() {
        /*
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logOutWith(
                onError = { /* 로깅 */ },
                onSuccess = { continuation.resume(Unit) }
            )
        }
        */
    }

    override suspend fun getOfferings(): List<SubscriptionPackage> {
        /*
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

        return offerings?.current?.availablePackages?.map { rcPackage ->
            val type = when (rcPackage.packageType) {
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

            SubscriptionPackage(
                id = rcPackage.identifier,
                title = rcPackage.product.title,
                priceString = rcPackage.product.price.formatted,
                type = type,
                badge = badge,
                subDescription = subDescription,
                hasFreeTrial = rcPackage.product.period?.let { true } ?: false,
                freeTrialDays = 7
            )
        } ?: emptyList()
        */
        return emptyList()
    }

    override suspend fun purchasePackage(
        packageId: String,
        context: PlatformContext
    ): Result<PurchaseResult> = runCatching {
        /*
        val activity = context as? Activity ?: throw Exception("Activity context is required for purchase")
        
        // 1. 패키지 찾기
        val offerings = suspendCancellableCoroutine<Offerings?> { continuation ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { continuation.resumeWithException(Exception(it.message)) },
                onSuccess = { continuation.resume(it) }
            )
        }
        val rcPackageArr = offerings?.all?.values?.flatMap { it.availablePackages }
        val rcPackage = rcPackageArr?.find { it.identifier == packageId }
            ?: throw Exception("Package not found")

        // 2. 결제 실행
        val customerInfo = suspendCancellableCoroutine<CustomerInfo?> { continuation ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, rcPackage).build(),
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
            isPremiumNow = customerInfo?.entitlements?.get("premium")?.isActive == true
        )
        */
        PurchaseResult(isSuccess = false, isPremiumNow = false)
    }

    override suspend fun restorePurchases(): Result<Unit> = runCatching {
        /*
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { continuation.resumeWithException(Exception(it.message)) },
                onSuccess = { continuation.resume(Unit) }
            )
        }
        */
    }
}
