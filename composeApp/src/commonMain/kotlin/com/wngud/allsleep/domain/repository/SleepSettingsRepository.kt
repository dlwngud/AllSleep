package com.wngud.allsleep.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수면 설정 (취침/기상 시간) 저장소 인터페이스
 */
interface SleepSettingsRepository {
    // 취침 시간 (HH:mm)
    val bedtime: Flow<String>
    
    // 기상 시간 (HH:mm)
    val wakeTime: Flow<String>

    // 취침/기상 알람 요일 설정 (0=일, 1=월, ..., 6=토)
    val sleepAlarmDays: Flow<Set<Int>>
    val wakeAlarmDays: Flow<Set<Int>>

    // 알람 활성화 여부
    val isSleepAlarmEnabled: Flow<Boolean>
    val isWakeAlarmEnabled: Flow<Boolean>

    // 온보딩 완료 여부
    val isOnboardingCompleted: Flow<Boolean>
    
    // 설정 저장
    suspend fun saveSleepSchedule(bedtime: String, wakeTime: String)

    suspend fun saveSleepAlarmDays(days: Set<Int>)
    suspend fun saveWakeAlarmDays(days: Set<Int>)

    suspend fun saveSleepAlarmEnabled(enabled: Boolean)
    suspend fun saveWakeAlarmEnabled(enabled: Boolean)

    // 온보딩 완료 상태 저장
    suspend fun saveOnboardingCompleted(completed: Boolean)

    // 기기 이름 (로컬 캐시)
    val deviceName: Flow<String?>

    // 기기 이름 저장
    suspend fun saveDeviceName(name: String)
    
    // 프리미엄 구독 여부
    val isPremium: Flow<Boolean>

    // 프리미엄 구독 여부 저장
    suspend fun savePremiumStatus(isPremium: Boolean)

    // 취침 시작 시간 (로컬 세션 보존용)
    val activeSleepStartAt: Flow<Long>

    // 취침 시작 시간 저장
    suspend fun saveActiveSleepStartAt(startTime: Long)

    // 초기화 (로그아웃 시 등)
    suspend fun clear()
}
