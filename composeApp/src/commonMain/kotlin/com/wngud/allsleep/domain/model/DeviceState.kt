package com.wngud.allsleep.domain.model

/**
 * users/{uid}/devices/{deviceId} 문서에 매핑되는 도메인 모델
 */
data class DeviceState(
    val deviceId: String = "",
    val deviceName: String = "",
    val fcmToken: String = "",
    val platform: String = "Android",
    val lastActiveForSleepLocking: Long = 0L,
    val isMainAlarmDevice: Boolean = false
)
