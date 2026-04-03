package com.wngud.allsleep.domain.model

/**
 * Firestore 단일 진실 공급원 (SSOT)
 * users/{uid} 문서에 매핑되는 도메인 모델
 */
data class UserSleepState(
    val uid: String = "",
    val isSleeping: Boolean = false,
    val targetWakeUpTime: Long? = null,
    
    // 평일 루틴 (월-금 기상용: 일-목 밤 취침)
    val weekdayBedtime: String = "23:00",
    val weekdayWakeTime: String = "07:00",
    val isWeekdaySleepEnabled: Boolean = true,
    val isWeekdayWakeEnabled: Boolean = true,
    
    // 주말 루틴 (토-일 기상용: 금-토 밤 취침)
    val weekendBedtime: String = "00:00",
    val weekendWakeTime: String = "09:00",
    val isWeekendSleepEnabled: Boolean = true,
    val isWeekendWakeEnabled: Boolean = true,
    
    val lastUpdatedAt: Long = 0L,
    val sleepStartAt: Long = 0L
)
