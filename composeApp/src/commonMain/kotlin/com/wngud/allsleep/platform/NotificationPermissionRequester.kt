package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

/**
 * 알림 전송 권한(POST_NOTIFICATIONS, API 33+)을 요청합니다.
 * Android 12 이하에서는 항상 isGranted = true를 반환합니다.
 */
interface NotificationPermissionRequester {
    fun isGranted(): Boolean
    fun requestPermission()
    fun openSettings()
}

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester
