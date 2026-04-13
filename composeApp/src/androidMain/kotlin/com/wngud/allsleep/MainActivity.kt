package com.wngud.allsleep
 
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wngud.allsleep.platform.AppOpenAdManager
import com.wngud.allsleep.service.SleepLockService
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private val globalSleepViewModel: GlobalSleepViewModel by viewModel()
    private val appOpenAdManager: AppOpenAdManager by inject()
    private var lastSleepingState: Boolean? = null
    
    // 콜드 스타트 완료 여부를 추적하는 상태값
    private var isColdStartFinished by mutableStateOf(false)

    // 알림 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission GRANTED")
        } else {
            android.util.Log.w("MainActivity", "Notification permission DENIED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Compose 그리기 전에 AndroidX 네이티브 스플래시 스크린 초기화
        val splashScreen = installSplashScreen()

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

        // Android 13 이상 알림 권한 요청
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 광고나 타임아웃이 끝날 때까지 기기의 기본 아이콘 스플래시 화면을 유지
        splashScreen.setKeepOnScreenCondition { !isColdStartFinished }

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

        // 콜드 스타트 광고 요청 (최대 3초 대기)
        appOpenAdManager.requestColdStartAd {
            isColdStartFinished = true
        }

        setContent {
            // 대기화면은 네이티브 스플래시가 책임지므로, 여기서는 무조건 App()만 호출 (바로 안 보임)
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // 포그라운드 복귀 시 쿨타임 확인 후 광고 노출
        appOpenAdManager.checkAndShowAdIfAvailable()
    }
}