package com.wngud.allsleep.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AllSleepMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("AllSleepFCM", "From: ${remoteMessage.from}")

        // 데이터 메시지가 포함되어 있는지 확인 (실제 수면 제어 명령용)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("AllSleepFCM", "Message data payload: ${remoteMessage.data}")
            
            val command = remoteMessage.data["command"]
            val action = remoteMessage.data["action"]
            Log.d("AllSleepFCM", "Received Command/Action: $command / $action")
            
            // Step 1.4에서 여기에 START_SLEEP / STOP_SLEEP 연동 로직이 들어갑니다.
        }

        // 알림 메시지가 포함되어 있는지 확인 (디버깅용)
        remoteMessage.notification?.let {
            Log.d("AllSleepFCM", "Message Notification Title: ${it.title}")
            Log.d("AllSleepFCM", "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("AllSleepFCM", "New Token: $token")
        // Step 3.1에서 여기에 Firestore 저장 로직이 들어갑니다.
    }
}
