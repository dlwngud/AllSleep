package com.wngud.allsleep
 
import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wngud.allsleep.service.SleepLockService
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val globalSleepViewModel: GlobalSleepViewModel by viewModel()
    private var lastSleepingState: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 잠금 화면 위에서도 앱이 나타나고 화면을 강제로 켜도록 설정 (Android 10+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 전역 수면 상태 관찰 시작 (실시간 동기화 핵심)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                globalSleepViewModel.sleepState.collectLatest { state ->
                    val isSleeping = state?.isSleeping ?: false
                    
                    // 상태가 실제로 변했을 때만 서비스 제어 (무한 루프 방지)
                    if (isSleeping != lastSleepingState) {
                        android.util.Log.d("MainActivity", "Sleep status changed: $isSleeping (sync from Remote/Local)")
                        if (isSleeping) {
                            SleepLockService.start(this@MainActivity)
                        } else {
                            // STOP_SLEEP 시에는 앱이 화면 전면으로 와야 하므로 stop 호출
                            SleepLockService.stop(this@MainActivity)
                        }
                        lastSleepingState = isSleeping
                    }
                }
            }
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("AllSleepFCM", "New Token: ${task.result}")
            }
        }

        setContent {
            App()
        }
    }
}