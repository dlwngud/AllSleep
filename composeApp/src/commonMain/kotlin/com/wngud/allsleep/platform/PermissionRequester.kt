package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

import kotlinx.coroutines.flow.StateFlow

interface PermissionRequester {
    val isBatteryOptimized: StateFlow<Boolean>
    fun requestBasicPermissions()
    fun requestAccessibilityPermission()
    fun isIgnoringBatteryOptimizations(): Boolean
    fun requestIgnoreBatteryOptimizations()
}

@Composable
expect fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester
