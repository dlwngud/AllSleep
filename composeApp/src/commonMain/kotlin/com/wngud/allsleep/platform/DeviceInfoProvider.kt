package com.wngud.allsleep.platform

/**
 * 플랫폼별 기기 정보(ID, 이름) 제공자
 * - Android: Settings.Secure.ANDROID_ID + Build.MODEL
 * - iOS: UIDevice identifierForVendor + UIDevice.name
 */
expect object DeviceInfoProvider {
    fun getDeviceId(): String
    fun getDeviceName(): String
    fun getPlatform(): String
}
