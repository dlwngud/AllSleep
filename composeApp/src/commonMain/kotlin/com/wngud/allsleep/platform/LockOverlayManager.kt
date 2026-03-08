package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

/**
 * 잠금 화면(오버레이 뷰)을 표시하고 숨기는 매니저입니다.
 * Android에서는 WindowManager를 통해 전체 화면을 덮고, 타 플랫폼에서는 Stub으로 동작합니다.
 */
interface LockOverlayManager {
    val isShowing: Boolean

    fun showOverlay()
    fun hideOverlay()
}

@Composable
expect fun rememberLockOverlayManager(): LockOverlayManager
