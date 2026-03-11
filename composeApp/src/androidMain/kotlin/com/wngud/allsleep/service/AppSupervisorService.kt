package com.wngud.allsleep.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.wngud.allsleep.domain.repository.AppBlockerRepository
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

class AppSupervisorService : AccessibilityService() {

    private val appBlockerRepository: AppBlockerRepository by inject()
    private val sleepSyncRepository: SleepSyncRepository by inject()
    private val authRepository: AuthRepository by inject()
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // 이전에 호출했던 패키지명 임시 캐싱 (중복 호출 방지)
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // 시스템 UI이거나 자기 자신 앱(AllSleep)이면 통과
            if (packageName == "com.android.systemui" || packageName == this.packageName) {
                return
            }
            
            // 너무 빈번한 호출 방지 (1초 이내 동일 패키지)
            val currentTime = System.currentTimeMillis()
            if (packageName == lastBlockedPackage && currentTime - lastBlockTime < 1000) {
                return
            }
            
            checkAndBlockApp(packageName, currentTime)
        }
    }

    private fun checkAndBlockApp(targetPackageName: String, currentTime: Long) {
        serviceScope.launch {
            try {
                // 1. 현재 수면 모드인지 확인
                val currentUser = authRepository.getCurrentUser()
                val uid = currentUser?.uid
                if (uid == null) return@launch
                
                val userSleepState = sleepSyncRepository.observeUserSleepState(uid).first()
                val isSleeping = userSleepState?.isSleeping == true
                if (!isSleeping) return@launch

                // 2. 시스템 앱인지 확인 (시스템 앱이면 생명/기능에 직결되므로 허용)
                val isSystemApp = appBlockerRepository.isSystemApp(targetPackageName)
                if (isSystemApp) return@launch

                // 3. 차단 로직 실행: 홈 화면(바탕화면)으로 강제 튕겨냄
                Log.d("AppSupervisor", "Blocking unauthorized user app: $targetPackageName")
                lastBlockedPackage = targetPackageName
                lastBlockTime = currentTime
                
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                // 4. 홈으로 튕긴 직후, 수면 오버레이를 다시 최상단으로 끌어올림
                withContext(Dispatchers.Main) {
                    val bringToFrontIntent = Intent(this@AppSupervisorService, SleepLockService::class.java).apply {
                        // SleepLockService가 켜져 있으면, startService를 다시 부르는 것만으로도 오버레이를 갱신할 수 있음
                    }
                    startService(bringToFrontIntent)
                }
                
            } catch (e: Exception) {
                Log.e("AppSupervisor", "Error in app blocking logic", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AppSupervisor", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
