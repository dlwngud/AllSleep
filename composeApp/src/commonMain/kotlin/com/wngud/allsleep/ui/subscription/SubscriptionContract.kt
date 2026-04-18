package com.wngud.allsleep.ui.subscription

import com.wngud.allsleep.platform.SubscriptionPackage

/**
 * 구독 결제 관련 MVI 규약
 */
interface SubscriptionContract {
    data class State(
        val isLoading: Boolean = false,
        val isPurchasing: Boolean = false,
        val isSuccess: Boolean = false,
        val packages: List<SubscriptionPackage> = emptyList(),
        val selectedPackageId: String? = null,
        val error: String? = null
    )

    sealed interface Intent {
        object LoadPackages : Intent
        data class SelectPackage(val id: String) : Intent
        object PurchaseSelected : Intent
        object RestorePurchases : Intent
        object DismissError : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        object NavigateBack : Effect
    }
}
