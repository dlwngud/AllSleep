package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

/**
 * 접근성 서비스 활성화 상태를 확인하고, 없으면 시스템 설정으로 유도합니다.
 */
interface AccessibilityPermissionRequester {
    fun isGranted(): Boolean
    fun requestPermission()
}

@Composable
expect fun rememberAccessibilityPermissionRequester(onResult: (Boolean) -> Unit): AccessibilityPermissionRequester
