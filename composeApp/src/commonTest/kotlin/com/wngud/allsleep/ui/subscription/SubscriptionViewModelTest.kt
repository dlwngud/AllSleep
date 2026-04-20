package com.wngud.allsleep.ui.subscription

import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.PackageType
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.PurchaseResult
import com.wngud.allsleep.platform.SubscriptionPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SubscriptionViewModel
    private lateinit var billingProvider: FakeBillingProvider

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        billingProvider = FakeBillingProvider()
        viewModel = SubscriptionViewModel(billingProvider)
    }

    @Test
    fun `LoadPackages 성공 시 ANNUAL 플랜이 기본 선택되어야 한다`() = runTest {
        // Given
        val packages = listOf(
            SubscriptionPackage("m", "월", "₩4,900", PackageType.MONTHLY, null, null, false, 0),
            SubscriptionPackage("a", "년", "₩39,000", PackageType.ANNUAL, "인기", "34% 할인", true, 7),
            SubscriptionPackage("l", "평생", "₩99,000", PackageType.LIFETIME, "추천", "영구", false, 0)
        )
        billingProvider.packages = packages

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(3, state.packages.size)
        assertEquals("a", state.selectedPackageId) // ANNUAL (id: "a") 가 자동 선택됨
        assertEquals(null, state.error)
    }

    @Test
    fun `SelectPackage 호출 시 선택된 ID가 업데이트되어야 한다`() = runTest {
        // Given
        val packages = listOf(
            SubscriptionPackage("m", "월", "₩4,900", PackageType.MONTHLY, null, null, false, 0),
            SubscriptionPackage("l", "평생", "₩99,000", PackageType.LIFETIME, "추천", "영구", false, 0)
        )
        billingProvider.packages = packages
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        advanceUntilIdle()

        // When
        viewModel.handleIntent(SubscriptionContract.Intent.SelectPackage("l"))

        // Then
        assertEquals("l", viewModel.state.value.selectedPackageId)
    }
}

class FakeBillingProvider : BillingProvider {
    var packages: List<SubscriptionPackage> = emptyList()
    var purchaseResult: Result<PurchaseResult> = Result.success(PurchaseResult(true, true))

    override suspend fun getOfferings(): List<SubscriptionPackage> = packages
    
    override suspend fun purchasePackage(packageId: String, context: PlatformContext): Result<PurchaseResult> = purchaseResult
    
    override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)
    
    override suspend fun loginUser(uid: String) {}
    
    override suspend fun logoutUser() {}
}
