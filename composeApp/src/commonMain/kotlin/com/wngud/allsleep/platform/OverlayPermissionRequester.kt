package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

/**
 * 다른 앱 위에 표시(SYSTEM_ALERT_WINDOW) 권한을 점검하고 요청하는 역할을 합니다.
 */
interface OverlayPermissionRequester {
    /**
     * 권한이 부여되어 있는지 확인합니다.
     */
    fun isGranted(): Boolean

    /**
     * 권한이 없다면 시스템 설정 창으로 이동시켜 권한을 요구합니다.
     */
    fun requestPermission()
}

@Composable
expect fun rememberOverlayPermissionRequester(onResult: (Boolean) -> Unit): OverlayPermissionRequester
