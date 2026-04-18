package com.wngud.allsleep.ui.subscription

import com.wngud.allsleep.platform.SubscriptionPackage

/**
 * 구독(Premium) 화면의 MVI 컨트랙트
 */
class SubscriptionContract {

    data class State(
        val isLoading: Boolean = false,
        val isPurchasing: Boolean = false,
        val packages: List<SubscriptionPackage> = emptyList(),
        val selectedPackageId: String? = null,
        val error: String? = null,
        val isSuccess: Boolean = false
    )

    sealed interface Intent {
        data object LoadPackages : Intent
        data class SelectPackage(val id: String) : Intent
        data object PurchaseSelected : Intent
        data object RestorePurchases : Intent
        data object DismissError : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
        data object NavigateBack : Effect
    }
}
