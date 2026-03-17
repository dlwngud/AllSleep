package com.wngud.allsleep.platform

/**
 * 전역 수면 스케줄링(알람) 인터페이스
 */
expect object SleepScheduler {
    /**
     * 다음 취침/기상 시각을 시스템 알람에 등록합니다.
     * @param bedtime "HH:mm" 포맷 (예: "23:00")
     * @param wakeTime "HH:mm" 포맷 (예: "07:00")
     */
    fun scheduleNextEvents(bedtime: String, wakeTime: String)

    /**
     * 모든 예약된 수면 알람을 취소합니다.
     */
    fun cancelAll()
}
