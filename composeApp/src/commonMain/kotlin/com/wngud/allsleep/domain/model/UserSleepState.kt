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
    val lastUpdatedAt: Long = 0L,
    val sleepStartAt: Long = 0L  // ← 신규: 수면 시작 타임스탬프 (ms)
)

