package com.wngud.allsleep.ui.app

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.User

data class AppRootState(
    val currentUser: User? = null,
    val isPremium: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isStateInitialized: Boolean = false,
    val showDeviceLimitDialog: Boolean = false,
    val cachedDeviceStateToRegister: DeviceState? = null,
    val error: String? = null,
)

sealed interface AppRootIntent {
    data object RequestInitialize : AppRootIntent
    data object RequestLogout : AppRootIntent
    data object RefreshPremiumStatus : AppRootIntent
    data object ReplaceDevice : AppRootIntent
    data object CancelDeviceRegistration : AppRootIntent
    data object UpgradeToPremium : AppRootIntent
}

sealed interface AppRootEffect {
    data class ShowSnackbar(val message: String) : AppRootEffect
    data object NavigateToSubscription : AppRootEffect
}
