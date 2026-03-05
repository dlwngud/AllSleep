package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.UserSleepState
import kotlinx.coroutines.flow.Flow

/**
 * 수면 상태 동기화 및 기기 관리를 위한 Repository
 * 공통(commonMain) 인터페이스이며 구현체는 각 플랫폼 모듈에 위치함.
 */
interface SleepSyncRepository {
    
    /**
     * 1. 전역 수면 상태 실시간 구독 (SnapshotListener 활용)
     * users/{uid} 문서의 변경을 감지하여 퍼블리싱 (Flow)
     */
    fun observeUserSleepState(uid: String): Flow<UserSleepState?>

    /**
     * 2. 수면 상태 전면 변경 (수면 시작/기상 버튼 클릭 시)
     * 낙관적 업데이트를 위해 호출 즉시 처리를 시도함.
     */
    suspend fun updateUserSleepState(
        uid: String, 
        isSleeping: Boolean, 
        targetWakeUpTime: Long? = null
    ): Result<Unit>

    /**
     * 3. 현재 기기 정보 등록/갱신 (FCM 토큰 갱신 시 혹은 앱 구동 시)
     * users/{uid}/devices/{deviceId} 하위에 덮어쓰기
     */
    suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit>

    /**
     * 4. 등록된 기기 목록 가져오기 (기기 관리 뷰)
     */
    suspend fun getRegisteredDevices(uid: String): Result<List<DeviceState>>

    /**
     * 5. 메인 기기 설정 변경 (알람이 울릴 Hub 기기)
     */
    suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit>
}
