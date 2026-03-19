package com.wngud.allsleep.platform

/**
 * 플랫폼별 기기 정보(ID, 이름) 제공자 인터페이스
 */
interface DeviceInfoProvider {
    fun getDeviceId(): String
    fun getDeviceName(): String
    fun getPlatform(): String
    suspend fun getPushToken(): String
}
