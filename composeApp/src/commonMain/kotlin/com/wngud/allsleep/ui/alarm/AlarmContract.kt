package com.wngud.allsleep.ui.alarm

/**
 * 알람 화면의 State와 Intent 정의 (MVI 패턴)
 * 평일(월-금)과 주말(토-일) 고정 루틴 구조로 개편
 */

enum class AlarmTab { WEEKDAY, WEEKEND }

data class AlarmRoutine(
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean
)

data class AlarmState(
    val selectedTab: AlarmTab = AlarmTab.WEEKDAY,
    
    // 평일 루틴 (월-금 기상용)
    val weekdaySleep: AlarmRoutine = AlarmRoutine(23, 0, true),
    val weekdayWake: AlarmRoutine = AlarmRoutine(7, 0, true),
    
    // 주말 루틴 (토-일 기상용)
    val weekendSleep: AlarmRoutine = AlarmRoutine(0, 0, true),
    val weekendWake: AlarmRoutine = AlarmRoutine(9, 0, true)
)

sealed interface AlarmIntent {
    /** 탭 전환 (평일 <-> 주말) */
    data class SelectTab(val tab: AlarmTab) : AlarmIntent
    
    /** 현재 선택된 탭의 취침 시간 업데이트 */
    data class UpdateSleepTime(val time: String) : AlarmIntent
    
    /** 현재 선택된 탭의 기상 시간 업데이트 */
    data class UpdateWakeTime(val time: String) : AlarmIntent
    
    /** 현재 선택된 탭의 취침 알람(잠금) 활성화 토글 */
    data class ToggleSleepAlarm(val enabled: Boolean) : AlarmIntent
    
    /** 현재 선택된 탭의 기상 알람 활성화 토글 */
    data class ToggleWakeAlarm(val enabled: Boolean) : AlarmIntent
}
