package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSleepServiceController(): SleepServiceController {
    return remember {
        object : SleepServiceController {
            override fun start() {
                // iOS는 별도의 Foreground 서비스 구현이 없거나 다름
            }

            override fun stop() {
                // iOS stub
            }

            override fun isRunning(): Boolean = false
        }
    }
}
