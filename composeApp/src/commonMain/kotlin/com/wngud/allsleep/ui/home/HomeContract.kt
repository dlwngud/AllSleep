package com.wngud.allsleep.ui.home

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.UserSleepState

data class HomeState(
    val sleepState: UserSleepState? = null,
    val registeredDevices: List<DeviceState> = emptyList(),
    val isToggleLoading: Boolean = false,
    val error: String? = null,
) {
    val isSleeping: Boolean
        get() = sleepState?.isSleeping == true
}
