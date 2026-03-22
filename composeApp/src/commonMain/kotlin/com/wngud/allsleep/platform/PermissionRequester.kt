package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

import kotlinx.coroutines.flow.StateFlow

interface PermissionRequester {
    val isBatteryOptimized: StateFlow<Boolean>
    val isAccessibilityEnabled: StateFlow<Boolean>
    val isAlarmPermissionGranted: StateFlow<Boolean>
    val isNotificationPermissionGranted: StateFlow<Boolean>

    fun requestBasicPermissions()
    fun requestAccessibilityPermission()
    fun isIgnoringBatteryOptimizations(): Boolean
    fun requestIgnoreBatteryOptimizations()
    fun requestAlarmPermission()
}

@Composable
expect fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester
