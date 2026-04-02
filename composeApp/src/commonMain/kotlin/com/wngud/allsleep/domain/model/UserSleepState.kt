package com.wngud.allsleep.domain.model

/**
 * Firestore 단일 진실 공급원 (SSOT)
 * users/{uid} 문서에 매핑되는 도메인 모델
 */
data class UserSleepState(
    val uid: String = "",
    val isSleeping: Boolean = false,
    val targetWakeUpTime: Long? = null,
    val bedtime: String = "23:00",
    val wakeTime: String = "07:00",
    val sleepAlarmDays: Set<Int> = setOf(1, 2, 3, 4, 5), // 월-금 기본값
    val wakeAlarmDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val isSleepAlarmEnabled: Boolean = true,
    val isWakeAlarmEnabled: Boolean = true,
    val lastUpdatedAt: Long = 0L,
    val sleepStartAt: Long = 0L
)

