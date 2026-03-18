package com.wngud.allsleep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wngud.allsleep.platform.LockOverlayManagerImpl

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepLockService : Service(), KoinComponent {
    private var overlayManager: LockOverlayManagerImpl? = null
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase by inject()
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? {
        return null // 바인딩을 사용하지 않는 Started Service
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AllSleep 수면 모드 작동 중")
            .setContentText("수면을 방해하는 앱 사용을 차단하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
            
        try {
            if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("SleepLockService", "Foreground Service Start Failed: ${e.message}", e)
        }

        // 오버레이 UI 표시
        if (overlayManager == null) {
            overlayManager = LockOverlayManagerImpl(this)
            overlayManager?.showOverlay()
        }

        // 시스템에 의해 서비스가 종료되더라도 자동으로 다시 시작하도록 설정(불사조 옵션)
        return START_STICKY 
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.hideOverlay()
        overlayManager = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "수면 모드 및 오버레이 상태",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "앱 차단 오버레이가 실행 중일 때 표시되는 알림입니다."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object : KoinComponent {
        private val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val CHANNEL_ID = "SleepLockServiceChannel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SleepLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            // Firestore 상태 동기화 (KoinComponent를 통해 필요한 UseCase 가져오기)
            val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase by inject()
            val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
            
            externalScope.launch {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    android.util.Log.d("SleepLockService", "Stopping service: Updating Firestore isSleeping to false for user ${user.uid}")
                    updateUserSleepStateUseCase(
                        uid = user.uid,
                        isSleeping = false,
                        targetWakeUpTime = null,
                        bedtime = null,
                        wakeTime = null
                    ).onFailure { e ->
                        android.util.Log.e("SleepLockService", "Failed to update Firestore on stop: ${e.message}")
                    }
                }
            }

            // 강제로 메인 액티비티를 포그라운드로 끌어올리기 (Intent Bouncing)
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
            }

            // 서비스 종료
            val intent = Intent(context, SleepLockService::class.java)
            context.stopService(intent)
        }
    }
}
