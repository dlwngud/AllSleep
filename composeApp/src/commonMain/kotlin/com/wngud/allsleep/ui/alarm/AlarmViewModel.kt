package com.wngud.allsleep.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 알람 탭 ViewModel
 * 취침/기상 알람 시간, 요일 설정, 추가 알람 목록을 관리합니다.
 */
class AlarmViewModel(
    private val sleepSettingsRepository: SleepSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmState())
    val state = _state.asStateFlow()

    init {
        // 로컬 저장소(DataStore)의 수면 스케줄을 관찰하여 UI 상태와 동기화
        viewModelScope.launch {
            sleepSettingsRepository.bedtime.collectLatest { bedtime ->
                val parts = bedtime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 23
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                _state.update { it.copy(sleepAlarm = it.sleepAlarm.copy(hour = h, minute = m)) }
            }
        }
        viewModelScope.launch {
            sleepSettingsRepository.wakeTime.collectLatest { wakeTime ->
                val parts = wakeTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                _state.update { it.copy(wakeAlarm = it.wakeAlarm.copy(hour = h, minute = m)) }
            }
        }
    }

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

            is AlarmIntent.UpdateSleepTime -> updateSleepTime(intent.time)
            is AlarmIntent.UpdateWakeTime -> updateWakeTime(intent.time)

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

    private fun updateSleepTime(time: String) {
        viewModelScope.launch {
            val wakeTime = "${_state.value.wakeAlarm.hour.toString().padStart(2, '0')}:${_state.value.wakeAlarm.minute.toString().padStart(2, '0')}"
            sleepSettingsRepository.saveSleepSchedule(time, wakeTime)
        }
    }

    private fun updateWakeTime(time: String) {
        viewModelScope.launch {
            val sleepTime = "${_state.value.sleepAlarm.hour.toString().padStart(2, '0')}:${_state.value.sleepAlarm.minute.toString().padStart(2, '0')}"
            sleepSettingsRepository.saveSleepSchedule(sleepTime, time)
        }
    }
}
