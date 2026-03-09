package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLockOverlayManager(): LockOverlayManager {
    return remember {
        object : LockOverlayManager {
            override val isShowing: Boolean = false
            override fun showOverlay() {}
            override fun hideOverlay() {}
        }
    }
}
