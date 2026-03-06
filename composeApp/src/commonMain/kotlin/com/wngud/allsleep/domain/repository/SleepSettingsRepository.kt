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
    
    // 초기화 (로그아웃 시 등)
    suspend fun clear()
}
