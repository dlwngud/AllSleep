package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberOverlayPermissionRequester(onResult: (Boolean) -> Unit): OverlayPermissionRequester {
    return remember {
        object : OverlayPermissionRequester {
            override fun isGranted(): Boolean {
                // iOS는 이런 형태의 화면 오버레이 시스템 권한이 존재하지 않으므로 기본 true 반환
                return true
            }

            override fun requestPermission() {
                onResult(true)
            }
        }
    }
}
