package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.UserSleepState
import kotlinx.coroutines.flow.Flow

interface SleepSyncRepository {
    /**
     * 사용자의 수면 설정 및 상태 관찰
     */
    fun observeUserSleepState(uid: String): Flow<UserSleepState?>

    /**
     * 사용자의 수면 설정 및 상태 업데이트
     */
    suspend fun updateUserSleepState(
        uid: String,
        isSleeping: Boolean? = null,
        targetWakeUpTime: Long? = null,
        weekdayBedtime: String? = null,
        weekdayWakeTime: String? = null,
        isWeekdaySleepEnabled: Boolean? = null,
        isWeekdayWakeEnabled: Boolean? = null,
        weekendBedtime: String? = null,
        weekendWakeTime: String? = null,
        isWeekendSleepEnabled: Boolean? = null,
        isWeekendWakeEnabled: Boolean? = null
    ): Result<Unit>

    /**
     * 사용자의 가입 기본 정보 (UID, 프로필 등) 업데이트
     */
    suspend fun updateUserProfile(user: com.wngud.allsleep.domain.model.User): Result<Unit>

    /**
     * 현재 기기 등록 및 정보 갱신
     */
    suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit>

    /**
     * 등록된 모든 기기 목록 조회
     */
    suspend fun getRegisteredDevices(uid: String): Result<List<DeviceState>>

    /**
     * 특정 기기를 메인 알람 기기로 설정
     */
    suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit>

    /**
     * 기기 등록 해제 (로그아웃 시 등)
     */
    suspend fun unregisterDevice(uid: String, deviceId: String): Result<Unit>

    /**
     * 등록된 기기 목록 실시간 관찰
     */
    fun observeRegisteredDevices(uid: String): Flow<List<DeviceState>>
}
