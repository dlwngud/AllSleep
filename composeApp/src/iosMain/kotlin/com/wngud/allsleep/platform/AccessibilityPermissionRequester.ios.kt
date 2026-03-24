package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAccessibilityPermissionRequester(onResult: (Boolean) -> Unit): AccessibilityPermissionRequester {
    return remember {
        object : AccessibilityPermissionRequester {
            override fun isGranted(): Boolean {
                return true
            }

            override fun requestPermission() {
                onResult(true)
            }
        }
    }
}
