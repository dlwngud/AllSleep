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
                globalSleepViewModel.state.collectLatest { globalState ->
                    val state = globalState.sleepState
                    val isSleeping = state?.isSleeping ?: false
                    
                    // 상태가 실제로 변했을 때만 서비스 제어 (무한 루프 방지)
                    if (lastSleepingState != null && isSleeping != lastSleepingState) {
                        android.util.Log.d("MainActivity", "Sleep status changed: $isSleeping (sync from Remote/Local)")
                        if (isSleeping) {
                            SleepLockService.start(this@MainActivity)
                        } else {
                            // Firestore에서 관찰된 상태가 이미 false이므로, 
                            // Service.stop() 내부에서 Firestore를 다시 덮어쓸 필요가 없음.
                            SleepLockService.stop(this@MainActivity, updateFirestore = false)
                        }
                    } else if (lastSleepingState == null) {
                        // 최초 진입 시, 수면 모드 상태에 맞게 서비스 동기화 (단, Firestore 강제 덮어쓰기는 방지)
                        android.util.Log.d("MainActivity", "Initial Sleep status: $isSleeping")
                        if (isSleeping) {
                            SleepLockService.start(this@MainActivity)
                        } else {
                            SleepLockService.stop(this@MainActivity, updateFirestore = false)
                        }
                    }
                    lastSleepingState = isSleeping
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