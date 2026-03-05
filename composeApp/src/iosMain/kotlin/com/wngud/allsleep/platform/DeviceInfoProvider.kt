package com.wngud.allsleep.platform

import platform.UIKit.UIDevice

actual object DeviceInfoProvider {
    actual fun getDeviceId(): String =
        UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"

    actual fun getDeviceName(): String =
        UIDevice.currentDevice.name

    actual fun getPlatform(): String = "iOS"
}
