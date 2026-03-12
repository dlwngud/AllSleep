package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

interface PermissionRequester {
    fun requestBasicPermissions()
    fun requestAccessibilityPermission()
    fun isIgnoringBatteryOptimizations(): Boolean
    fun requestIgnoreBatteryOptimizations()
}

@Composable
expect fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester
