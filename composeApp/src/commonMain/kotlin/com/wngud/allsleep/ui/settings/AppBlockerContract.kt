package com.wngud.allsleep.ui.settings

import com.wngud.allsleep.domain.model.AppInfo

data class AppBlockerState(
    val isLoading: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showSystemApps: Boolean = false
)

sealed class AppBlockerIntent {
    data object LoadApps : AppBlockerIntent()
    data class ToggleAppBlock(val packageName: String, val isBlocked: Boolean) : AppBlockerIntent()
    data class UpdateSearchQuery(val query: String) : AppBlockerIntent()
    data class ToggleSystemApps(val show: Boolean) : AppBlockerIntent()
}

sealed class AppBlockerEffect {
    data class ShowError(val message: String) : AppBlockerEffect()
}
