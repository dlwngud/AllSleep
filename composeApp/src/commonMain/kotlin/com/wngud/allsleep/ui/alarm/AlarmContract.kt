package com.wngud.allsleep.ui.alarm

/**
 * 알람 화면의 State와 Intent 정의 (MVI 패턴)
 *
 * Stitch 디자인 기준:
 * - 취침 시간(SleepAlarm): 시간 + 요일 선택 + ON/OFF
 * - 기상 시간(WakeAlarm): 시간 + 요일 선택 + ON/OFF
 * - 추가 알람 목록
 */

val DAYS = listOf("일", "월", "화", "수", "목", "금", "토")

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val selectedDays: Set<Int>,  // 0=일, 1=월, ..., 6=토
    val isEnabled: Boolean
)

data class AlarmState(
    val sleepAlarm: AlarmItem = AlarmItem(
        id = 0,
        hour = 23,
        minute = 0,
        label = "취침 시간",
        selectedDays = setOf(1, 2, 3, 4, 5),
        isEnabled = true
    ),
    val wakeAlarm: AlarmItem = AlarmItem(
        id = 1,
        hour = 7,
        minute = 0,
        label = "기상 시간",
        selectedDays = setOf(1, 2, 3, 4, 5),
        isEnabled = true
    ),
    val extraAlarms: List<AlarmItem> = emptyList()
)

sealed interface AlarmIntent {
    data class ToggleSleepAlarm(val enabled: Boolean) : AlarmIntent
    data class ToggleWakeAlarm(val enabled: Boolean) : AlarmIntent
    data class ToggleSleepDay(val dayIndex: Int) : AlarmIntent
    data class ToggleWakeDay(val dayIndex: Int) : AlarmIntent
    data class ToggleExtraAlarm(val id: Int) : AlarmIntent
    data object AddAlarm : AlarmIntent
}
