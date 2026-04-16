package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester {
    return object : NotificationPermissionRequester {
        override fun isGranted() = true
        override fun requestPermission() { onResult(true) }
        override fun openSettings() {
            val url = platform.Foundation.NSURL.URLWithString(platform.UIKit.UIApplicationOpenSettingsURLString)
            if (url != null) {
                platform.UIKit.UIApplication.sharedApplication.openURL(url)
            }
        }
    }
}
