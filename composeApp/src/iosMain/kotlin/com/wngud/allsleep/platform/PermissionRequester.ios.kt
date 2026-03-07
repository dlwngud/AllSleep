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
        }
    }
}
