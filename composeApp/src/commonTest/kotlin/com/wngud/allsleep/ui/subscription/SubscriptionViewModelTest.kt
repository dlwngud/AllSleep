package com.wngud.allsleep.ui.subscription

import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.PurchaseResult
import com.wngud.allsleep.platform.SubscriptionPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBillingProvider : BillingProvider {
        var offerings = listOf(
            SubscriptionPackage(
                id = "monthly_id",
                title = "Monthly Premium",
                priceString = "\$4.99",
                isMonthly = true,
                hasFreeTrial = true,
                freeTrialDays = 7
            )
        )

        override suspend fun loginUser(uid: String) {}
        override suspend fun logoutUser() {}
        override suspend fun getOfferings(): List<SubscriptionPackage> = offerings
        
        override suspend fun purchasePackage(
            packageId: String,
            context: PlatformContext
        ): Result<PurchaseResult> {
            return Result.success(PurchaseResult(isSuccess = true, isPremiumNow = true))
        }

        override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)
    }

    private lateinit var viewModel: SubscriptionViewModel
    private lateinit var billingProvider: FakeBillingProvider

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        billingProvider = FakeBillingProvider()
        viewModel = SubscriptionViewModel(billingProvider)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadPackages_updates_state_with_packages() = runTest {
        // Given
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        
        // When
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.packages.size)
        assertEquals("monthly_id", viewModel.state.value.selectedPackageId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun selectPackage_updates_selected_id() = runTest {
        // When
        viewModel.handleIntent(SubscriptionContract.Intent.SelectPackage("new_id"))
        
        // Then
        assertEquals("new_id", viewModel.state.value.selectedPackageId)
    }
}
