package com.wngud.allsleep.platform

/**
 * 구독 패키지 정보 모델
 */
data class SubscriptionPackage(
    val id: String,             // 패키지 ID (e.g. "allsleep_premium_monthly")
    val title: String,          // 제목
    val priceString: String,    // 포맷팅된 가격 (e.g. "₩4,900/월")
    val isMonthly: Boolean,     // 월간 플랜 여부
    val hasFreeTrial: Boolean,  // 무료 체험 포함 여부
    val freeTrialDays: Int      // 무료 체험 기간
)

/**
 * 구매 결과 모델
 */
data class PurchaseResult(
    val isSuccess: Boolean,
    val isPremiumNow: Boolean
)

/**
 * 구독 결제 플랫폼 추상화 인터페이스
 */
interface BillingProvider {
    // 상품(Offering) 목록 가져오기
    suspend fun getOfferings(): List<SubscriptionPackage>
    
    // 패키지 구매하기
    suspend fun purchasePackage(
        packageId: String,
        context: PlatformContext
    ): Result<PurchaseResult>
    
    // 구매 내역 복원하기
    suspend fun restorePurchases(): Result<Unit>
    
    // RevenueCat 유저 식별자 연동 (로그인 시)
    suspend fun loginUser(uid: String)
    
    // RevenueCat 유저 식별자 해제 (로그아웃 시)
    suspend fun logoutUser()
}
