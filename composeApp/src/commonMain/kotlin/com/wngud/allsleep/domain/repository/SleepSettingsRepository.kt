package com.wngud.allsleep.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수면 설정 (취침/기상 시간) 저장소 인터페이스
 */
interface SleepSettingsRepository {
    // --- 평일 루틴 (월-금 기상용) ---
    val weekdayBedtime: Flow<String>
    val weekdayWakeTime: Flow<String>
    val isWeekdaySleepEnabled: Flow<Boolean>
    val isWeekdayWakeEnabled: Flow<Boolean>
    
    // --- 주말 루틴 (토-일 기상용) ---
    val weekendBedtime: Flow<String>
    val weekendWakeTime: Flow<String>
    val isWeekendSleepEnabled: Flow<Boolean>
    val isWeekendWakeEnabled: Flow<Boolean>

    // --- 공통 설정 ---
    val isOnboardingCompleted: Flow<Boolean>
    val deviceName: Flow<String?>
    val isPremium: Flow<Boolean>
    val activeSleepStartAt: Flow<Long>

    // --- 저장 메서드 ---
    suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String)
    suspend fun saveWeekdaySleepEnabled(enabled: Boolean)
    suspend fun saveWeekdayWakeEnabled(enabled: Boolean)

    suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String)
    suspend fun saveWeekendSleepEnabled(enabled: Boolean)
    suspend fun saveWeekendWakeEnabled(enabled: Boolean)

    suspend fun saveOnboardingCompleted(completed: Boolean)
    suspend fun saveDeviceName(name: String)
    suspend fun savePremiumStatus(isPremium: Boolean)
    suspend fun saveActiveSleepStartAt(startTime: Long)

    // 초기화 (로그아웃 시 등)
    suspend fun clear()
}
