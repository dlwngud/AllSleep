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
            
            Log.d("AppSupervisor", "Window State Changed: $packageName")
            
            // 시스템 UI이거나 자기 자신 앱(AllSleep)이면 통과
            if (packageName == "com.android.systemui" || packageName == this.packageName) {
                return
            }
            
            // 너무 빈번한 호출 방지 (1초 이내 동일 패키지)
            val currentTime = System.currentTimeMillis()
            if (packageName == lastBlockedPackage && currentTime - lastBlockTime < 1000) {
                return
            }
            
            Log.d("AppSupervisor", "Checking block for: $packageName")
            checkAndBlockApp(packageName, currentTime)
        }
    }

    private fun checkAndBlockApp(targetPackageName: String, currentTime: Long) {
        serviceScope.launch {
            try {
                // 1. 현재 수면 모드인지 확인 (메모리 상주하는 프로세스의 상태 즉시 확인)
                if (!SleepLockService.isServiceRunning) {
                    Log.d("AppSupervisor", "SleepLockService is NOT running, unblocking.")
                    return@launch
                }

                // 2. 시스템 앱인지 확인 (시스템 앱이면 생명/기능에 직결되므로 허용)
                val isSystemApp = appBlockerRepository.isSystemApp(targetPackageName)
                if (isSystemApp) {
                    Log.d("AppSupervisor", "$targetPackageName is an allowed system app, unblocking.")
                    return@launch
                }

                // 3. 차단 로직 실행: MainActivity로 강제 튕겨냄 (앱 소환)
                Log.d("AppSupervisor", "Blocking unauthorized user app: $targetPackageName")
                lastBlockedPackage = targetPackageName
                lastBlockTime = currentTime
                
                withContext(Dispatchers.Main) {
                    try {
                        // 1. 시스템의 홈 전환 처리가 완료될 때까지 대기
                        Log.d("AppSupervisor", "Waiting 500ms for system transition to settle...")
                        delay(500)
                        
                        // 2. 소환 시도 (1회차)
                        fireMainActivityIntent()
                        
                        // 3. 잠시 후 재검사: 여전히 런처면 한 번 더 소환 (끈질기게 소환)
                        delay(200)
                        // Note: 여기서 packageName을 다시 확인할 방법은 AccessibilityEvent를 기다리는 게 정석이지만,
                        // 일단 한 번 더 호출하는 것이 확실한 효과가 있을 수 있음.
                        Log.d("AppSupervisor", "Filing secondary enforcement intent...")
                        fireMainActivityIntent()
                        
                        // 기존 오버레이 유지용 서비스도 갱신
                        val bringToFrontIntent = Intent(this@AppSupervisorService, SleepLockService::class.java)
                        startService(bringToFrontIntent)
                        Log.d("AppSupervisor", "Double forced return sequence executed")
                    } catch (e: Exception) {
                        Log.e("AppSupervisor", "Error during startActivity: ${e.message}", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("AppSupervisor", "CheckAndBlock error: ${e.message}", e)
            }
        }
    }

    private fun fireMainActivityIntent() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                Log.d("AppSupervisor", "Firing Launch Intent with Aggressive Flags")
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e("AppSupervisor", "Failed to fireMainActivityIntent: ${e.message}")
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
