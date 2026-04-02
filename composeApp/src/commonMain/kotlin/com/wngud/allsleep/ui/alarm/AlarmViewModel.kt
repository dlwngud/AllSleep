package com.wngud.allsleep.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.platform.SleepScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 알람 탭 ViewModel
 * 취침/기상 알람 시간, 요일 설정, 추가 알람 목록을 관리합니다.
 */
class AlarmViewModel(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val sleepScheduler: SleepScheduler
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
        viewModelScope.launch {
            sleepSettingsRepository.sleepAlarmDays.collectLatest { days ->
                _state.update { it.copy(sleepAlarm = it.sleepAlarm.copy(selectedDays = days)) }
            }
        }
        viewModelScope.launch {
            sleepSettingsRepository.wakeAlarmDays.collectLatest { days ->
                _state.update { it.copy(wakeAlarm = it.wakeAlarm.copy(selectedDays = days)) }
            }
        }
        viewModelScope.launch {
            sleepSettingsRepository.isSleepAlarmEnabled.collectLatest { enabled ->
                _state.update { it.copy(sleepAlarm = it.sleepAlarm.copy(isEnabled = enabled)) }
            }
        }
        viewModelScope.launch {
            sleepSettingsRepository.isWakeAlarmEnabled.collectLatest { enabled ->
                _state.update { it.copy(wakeAlarm = it.wakeAlarm.copy(isEnabled = enabled)) }
            }
        }
    }

    fun handleIntent(intent: AlarmIntent) {
        when (intent) {
            is AlarmIntent.ToggleSleepAlarm -> {
                viewModelScope.launch {
                    sleepSettingsRepository.saveSleepAlarmEnabled(intent.enabled)
                    triggerScheduling()
                }
            }

            is AlarmIntent.ToggleWakeAlarm -> {
                viewModelScope.launch {
                    sleepSettingsRepository.saveWakeAlarmEnabled(intent.enabled)
                    triggerScheduling()
                }
            }

            is AlarmIntent.ToggleSleepDay -> {
                val current = _state.value.sleepAlarm.selectedDays
                val updated = if (intent.dayIndex in current) current - intent.dayIndex else current + intent.dayIndex
                viewModelScope.launch {
                    sleepSettingsRepository.saveSleepAlarmDays(updated)
                    triggerScheduling()
                }
            }

            is AlarmIntent.ToggleWakeDay -> {
                val current = _state.value.wakeAlarm.selectedDays
                val updated = if (intent.dayIndex in current) current - intent.dayIndex else current + intent.dayIndex
                viewModelScope.launch {
                    sleepSettingsRepository.saveWakeAlarmDays(updated)
                    triggerScheduling()
                }
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
            triggerScheduling()
        }
    }

    private fun updateWakeTime(time: String) {
        viewModelScope.launch {
            val sleepTime = "${_state.value.sleepAlarm.hour.toString().padStart(2, '0')}:${_state.value.sleepAlarm.minute.toString().padStart(2, '0')}"
            sleepSettingsRepository.saveSleepSchedule(sleepTime, time)
            triggerScheduling()
        }
    }

    private fun triggerScheduling() {
        viewModelScope.launch {
            val bedtime = sleepSettingsRepository.bedtime.first()
            val wakeTime = sleepSettingsRepository.wakeTime.first()
            val sleepDays = if (_state.value.sleepAlarm.isEnabled) {
                sleepSettingsRepository.sleepAlarmDays.first()
            } else {
                emptySet()
            }
            val wakeDays = if (_state.value.wakeAlarm.isEnabled) {
                sleepSettingsRepository.wakeAlarmDays.first()
            } else {
                emptySet()
            }
            
            sleepScheduler.scheduleNextEvents(bedtime, wakeTime, sleepDays, wakeDays)
        }
    }
}
