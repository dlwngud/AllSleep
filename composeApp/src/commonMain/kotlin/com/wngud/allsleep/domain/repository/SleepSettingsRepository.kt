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

    // 온보딩 완료 여부
    val isOnboardingCompleted: Flow<Boolean>
    
    // 설정 저장
    suspend fun saveSleepSchedule(bedtime: String, wakeTime: String)

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
