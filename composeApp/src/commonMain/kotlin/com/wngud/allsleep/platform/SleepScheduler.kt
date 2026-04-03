package com.wngud.allsleep.platform

/**
 * 전역 수면 스케줄링(알람) 인터페이스
 */
interface SleepScheduler {
    /**
     * 다음 취침/기상 시각을 시스템 알람에 등록합니다.
     * 평일과 주말 스케줄을 모두 받아, 내부적으로 가장 가까운 다음 이벤트를 계산하여 예약합니다.
     * 
     * @param weekdayBedtime "HH:mm" (월-금 기상용)
     * @param weekdayWakeTime "HH:mm" (월-금 기상용)
     * @param isWeekdayEnabled 평일 루틴 활성화 여부
     * @param weekendBedtime "HH:mm" (토-일 기상용)
     * @param weekendWakeTime "HH:mm" (토-일 기상용)
     * @param isWeekendEnabled 주말 루틴 활성화 여부
     */
    fun scheduleNextEvents(
        weekdayBedtime: String,
        weekdayWakeTime: String,
        isWeekdayEnabled: Boolean,
        weekendBedtime: String,
        weekendWakeTime: String,
        isWeekendEnabled: Boolean
    )

    /**
     * 모든 예약된 수면 알람을 취소합니다.
     */
    fun cancelAll()
}
