package com.wngud.allsleep.domain.model

/**
 * 완료된 단일 수면 세션의 데이터를 담는 모델
 * Firestore: users/{uid}/sleep_records/{date}
 */
data class SleepRecord(
    val id: String = "",              // 문서 ID (예: "2026-03-31")
    val uid: String = "",
    val date: String = "",            // "yyyy-MM-dd"
    val bedtime: Long = 0L,           // 실제 취침 시각 (ms)
    val wakeTime: Long = 0L,          // 실제 기상 시각 (ms)
    val targetBedtime: String = "",   // 설정된 목표 취침 (예: "23:00")
    val targetWakeTime: String = "",  // 설정된 목표 기상 (예: "07:00")
    val targetMinutes: Int = 0,       // 목표 수면 시간 (분)
    val durationMinutes: Int = 0,     // 실제 수면 시간 (분)
    val sleepEfficiency: Float = 0f,  // 수면 효율 (%)
    val achievementRate: Float = 0f,  // 목표 달성률 (%)
    val isLockUsed: Boolean = false   // 앱 잠금 기능 사용 여부
)
