package com.wngud.allsleep.platform

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Build
import kotlinx.coroutines.tasks.await

class DeviceInfoProviderImpl(private val context: Context) : DeviceInfoProvider {

    @SuppressLint("HardwareIds")
    override fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-android-device"
    }

    override fun getDeviceName(): String = Build.MODEL

    override fun getPlatform(): String = "Android"

    override suspend fun getPushToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            android.util.Log.e("DeviceInfoProvider", "Failed to get FCM token: ${e.message}")
            ""
        }
    }
}
