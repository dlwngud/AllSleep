package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester {
    return object : NotificationPermissionRequester {
        override fun isGranted() = true
        override fun requestPermission() { onResult(true) }
    }
}
