package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

/**
 * 수면 잠금 모드(Foreground Service)를 제어하는 컨트롤러 인터페이스입니다.
 */
interface SleepServiceController {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}

@Composable
expect fun rememberSleepServiceController(): SleepServiceController
