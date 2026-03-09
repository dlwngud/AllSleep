package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wngud.allsleep.service.SleepLockService

@Composable
actual fun rememberSleepServiceController(): SleepServiceController {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        object : SleepServiceController {
            override fun start() {
                SleepLockService.start(context)
            }

            override fun stop() {
                SleepLockService.stop(context)
            }
        }
    }
}
