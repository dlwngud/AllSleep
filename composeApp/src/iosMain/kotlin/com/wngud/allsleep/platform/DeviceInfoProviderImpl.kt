package com.wngud.allsleep.platform

import platform.UIKit.UIDevice

class DeviceInfoProviderImpl : DeviceInfoProvider {
    override fun getDeviceId(): String =
        UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"

    override fun getDeviceName(): String =
        UIDevice.currentDevice.name

    override fun getPlatform(): String = "iOS"

    override fun getPushToken(): suspend () -> String = {
        "" // iOS FCM 및 APNS 연동 시 추후 구현
    }

    override suspend fun getPushToken(): String = ""
}
