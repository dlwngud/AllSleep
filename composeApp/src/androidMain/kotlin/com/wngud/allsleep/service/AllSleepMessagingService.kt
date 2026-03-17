package com.wngud.allsleep.service

import com.wngud.allsleep.MainActivity

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.platform.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AllSleepMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("AllSleepFCM", "--- onMessageReceived START ---")
        Log.d("AllSleepFCM", "From: ${remoteMessage.from}")

        // 1.3 하이라이트: 기기 깨우기 및 카톡 스타일 알림 띄우기
        acquireWakeLock()
        showNotification(remoteMessage)

        // 데이터 메시지가 포함되어 있는지 확인 (실제 수면 제어 명령용)
        if (remoteMessage.data.isNotEmpty()) {
            val command = remoteMessage.data["command"]
            Log.d("AllSleepFCM", "Command Received: $command, Full Data: ${remoteMessage.data}")
            
            when (command) {
                "START_SLEEP" -> {
                    Log.d("AllSleepFCM", "Remote START_SLEEP triggered")
                    // 수면 모드 시작 (서비스 실행 및 오버레이 점등)
                    com.wngud.allsleep.service.SleepLockService.start(this)
                }
                "STOP_SLEEP" -> {
                    Log.d("AllSleepFCM", "Remote STOP_SLEEP triggered")
                    // 수면 모드 중단
                    com.wngud.allsleep.service.SleepLockService.stop(this)
                }
            }
        }
        
        Log.d("AllSleepFCM", "--- onMessageReceived END ---")
    }

    /**
     * 카톡처럼 앱이 켜져 있어도 상단 팝업(Heads-up) 알림을 강제로 띄우는 로직
     */
    private fun showNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "AllSleep 원격 신호"
        val body = remoteMessage.notification?.body ?: "수면 모드 명령이 수신되었습니다."

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "AllSleep_Remote_Channel"

        // 전체 화면 인텐트 권한 상태 로그 확인 (Android 14 대응용)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val canUseFullScreen = notificationManager.canUseFullScreenIntent()
            Log.d("AllSleepFCM", "canUseFullScreenIntent: $canUseFullScreen")
        }

        // 오레오 이상 채널 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "원격 제어 알림",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "원격 잠금/해제 신호를 수신했을 때 알림을 표시합니다."
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 클릭 시 앱 실행을 위한 인텐트
        val intent = android.content.Intent(applicationContext, com.wngud.allsleep.MainActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d("AllSleepFCM", "Notification posted with fullScreenIntent")
    }

    /**
     * 기기를 강제로 깨우고 CPU를 유지시키는 WakeLock 로직
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        
        // 1. 우선 CPU를 깨운 상태로 유지 (Partial)
        val cpuWakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "AllSleep:CPUWake")
        cpuWakeLock.acquire(10000L) // 10초간 CPU 생존 보장
        
        // 2. 화면을 강제로 밝힘
        @Suppress("DEPRECATION")
        val screenWakeLock = powerManager.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
            "AllSleep:ScreenWake"
        )
        
        screenWakeLock.acquire(5000L) // 5초간 화면 밝힘
        Log.d("AllSleepFCM", "CPU & Screen WakeLocks acquired")
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val sleepSyncRepository: SleepSyncRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AllSleepFCM", "New Token Generated: $token")
        
        serviceScope.launch {
            val user = getCurrentUserUseCase()
            if (user != null) {
                Log.d("AllSleepFCM", "User logged in, updating token in Firestore...")
                val deviceState = DeviceState(
                    deviceId = DeviceInfoProvider.getDeviceId(),
                    deviceName = DeviceInfoProvider.getDeviceName(),
                    fcmToken = token,
                    platform = DeviceInfoProvider.getPlatform(),
                    lastActiveForSleepLocking = System.currentTimeMillis(),
                    isMainAlarmDevice = false // 기존 상태 보존 로직이 필요할 수 있으나, 여기서는 갱신에 집중
                )
                sleepSyncRepository.registerDevice(user.uid, deviceState)
                    .onSuccess { Log.d("AllSleepFCM", "Token updated successfully in Firestore") }
                    .onFailure { e -> Log.e("AllSleepFCM", "Failed to update token: ${e.message}") }
            } else {
                Log.d("AllSleepFCM", "No user logged in, skipping token update")
            }
        }
    }
}
