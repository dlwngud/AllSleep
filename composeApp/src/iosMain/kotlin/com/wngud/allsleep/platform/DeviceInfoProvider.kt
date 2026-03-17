package com.wngud.allsleep.platform

import platform.UIKit.UIDevice

actual object DeviceInfoProvider {
    actual fun getDeviceId(): String =
        UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"

    actual fun getDeviceName(): String =
        UIDevice.currentDevice.name

    actual fun getPlatform(): String = "iOS"

    actual suspend fun getPushToken(): String = "" // iOS FCM 및 APNS 연동 시 추후 구현
}
