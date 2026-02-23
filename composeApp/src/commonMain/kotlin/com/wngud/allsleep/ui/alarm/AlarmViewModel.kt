package com.wngud.allsleep.ui.alarm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 알람 탭 ViewModel
 * 취침/기상 알람 시간, 요일 설정, 추가 알람 목록을 관리합니다.
 */
class AlarmViewModel : ViewModel() {

    private val _state = MutableStateFlow(AlarmState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: AlarmIntent) {
        when (intent) {
            is AlarmIntent.ToggleSleepAlarm ->
                _state.update { it.copy(sleepAlarm = it.sleepAlarm.copy(isEnabled = intent.enabled)) }

            is AlarmIntent.ToggleWakeAlarm ->
                _state.update { it.copy(wakeAlarm = it.wakeAlarm.copy(isEnabled = intent.enabled)) }

            is AlarmIntent.ToggleSleepDay -> {
                val current = _state.value.sleepAlarm.selectedDays
                val updated = if (intent.dayIndex in current) current - intent.dayIndex else current + intent.dayIndex
                _state.update { it.copy(sleepAlarm = it.sleepAlarm.copy(selectedDays = updated)) }
            }

            is AlarmIntent.ToggleWakeDay -> {
                val current = _state.value.wakeAlarm.selectedDays
                val updated = if (intent.dayIndex in current) current - intent.dayIndex else current + intent.dayIndex
                _state.update { it.copy(wakeAlarm = it.wakeAlarm.copy(selectedDays = updated)) }
            }

            is AlarmIntent.ToggleExtraAlarm -> {
                _state.update {
                    it.copy(
                        extraAlarms = it.extraAlarms.map { alarm ->
                            if (alarm.id == intent.id) alarm.copy(isEnabled = !alarm.isEnabled) else alarm
                        }
                    )
                }
            }

            is AlarmIntent.AddAlarm -> {
                // TODO: 알람 추가 다이얼로그 연결
            }
        }
    }
}
