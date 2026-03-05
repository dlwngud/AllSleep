package com.wngud.allsleep.platform

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.os.Build
import android.provider.Settings

/**
 * Android 전용 기기 정보 제공자
 * - 기기 ID: Settings.Secure.ANDROID_ID
 * - 기기 이름: Build.MODEL
 */
actual object DeviceInfoProvider {

    private var resolver: ContentResolver? = null

    /** Application.onCreate() 혹은 Koin 모듈에서 초기화 필요 */
    fun init(contentResolver: ContentResolver) {
        resolver = contentResolver
    }

    @SuppressLint("HardwareIds")
    actual fun getDeviceId(): String {
        return resolver?.let {
            Settings.Secure.getString(it, Settings.Secure.ANDROID_ID)
        } ?: "unknown-android-device"
    }

    actual fun getDeviceName(): String = Build.MODEL

    actual fun getPlatform(): String = "Android"
}
