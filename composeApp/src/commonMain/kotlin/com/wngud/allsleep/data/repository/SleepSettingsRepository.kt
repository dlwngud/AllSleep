package com.wngud.allsleep.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수면 설정 (취침/기상 시간) 저장소 인터페이스
 */
interface SleepSettingsRepository {
    // 취침 시간 (HH:mm)
    val bedtime: Flow<String>
    
    // 기상 시간 (HH:mm)
    val wakeTime: Flow<String>
    
    // 설정 저장
    suspend fun saveSleepSchedule(bedtime: String, wakeTime: String)
    
    // 초기화 (로그아웃 시 등)
    suspend fun clear()
}
