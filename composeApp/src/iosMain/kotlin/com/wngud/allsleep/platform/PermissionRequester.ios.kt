package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester {
    return remember {
        object : PermissionRequester {
            override fun requestBasicPermissions() {
                // iOS는 추후 UNUserNotificationCenter 등을 통해 연동
                onResult(true)
            }
            override fun requestAccessibilityPermission() {
                // iOS에서는 Accessibility 권한 모델이 다름
            }
            override fun isIgnoringBatteryOptimizations(): Boolean = true
            override fun requestIgnoreBatteryOptimizations() {
                // iOS에서는 해당 설정이 필요 없음
            }
        }
    }
}
