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
 * 평일(월-금)과 주말(토-일) 고정 루틴 구조로 개편
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
            val weekdayFlow = combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime,
                sleepSettingsRepository.isWeekdaySleepEnabled,
                sleepSettingsRepository.isWeekdayWakeEnabled
            ) { b, w, se, we -> 
                RoutineData(b, w, se, we)
            }
            
            val weekendFlow = combine(
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime,
                sleepSettingsRepository.isWeekendSleepEnabled,
                sleepSettingsRepository.isWeekendWakeEnabled
            ) { b, w, se, we -> 
                RoutineData(b, w, se, we)
            }

            combine(weekdayFlow, weekendFlow) { wd, we -> wd to we }
                .collectLatest { (wd, we) ->
                    _state.update { 
                        it.copy(
                            weekdaySleep = parseToRoutine(wd.bedtime, wd.sleepEnabled),
                            weekdayWake = parseToRoutine(wd.wakeTime, wd.wakeEnabled),
                            weekendSleep = parseToRoutine(we.bedtime, we.sleepEnabled),
                            weekendWake = parseToRoutine(we.wakeTime, we.wakeEnabled)
                        )
                    }
                }
        }
    }

    private data class RoutineData(
        val bedtime: String,
        val wakeTime: String,
        val sleepEnabled: Boolean,
        val wakeEnabled: Boolean
    )

    private fun parseToRoutine(time: String, isEnabled: Boolean): AlarmRoutine {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return AlarmRoutine(h, m, isEnabled)
    }

    fun handleIntent(intent: AlarmIntent) {
        when (intent) {
            is AlarmIntent.SelectTab -> {
                _state.update { it.copy(selectedTab = intent.tab) }
            }

            is AlarmIntent.UpdateSleepTime -> {
                val tab = _state.value.selectedTab
                viewModelScope.launch {
                    if (tab == AlarmTab.WEEKDAY) {
                        val currentW = "${_state.value.weekdayWake.hour.toString().padStart(2, '0')}:${_state.value.weekdayWake.minute.toString().padStart(2, '0')}"
                        sleepSettingsRepository.saveWeekdaySchedule(intent.time, currentW)
                    } else {
                        val currentW = "${_state.value.weekendWake.hour.toString().padStart(2, '0')}:${_state.value.weekendWake.minute.toString().padStart(2, '0')}"
                        sleepSettingsRepository.saveWeekendSchedule(intent.time, currentW)
                    }
                    triggerScheduling()
                }
            }

            is AlarmIntent.UpdateWakeTime -> {
                val tab = _state.value.selectedTab
                viewModelScope.launch {
                    if (tab == AlarmTab.WEEKDAY) {
                        val currentS = "${_state.value.weekdaySleep.hour.toString().padStart(2, '0')}:${_state.value.weekdaySleep.minute.toString().padStart(2, '0')}"
                        sleepSettingsRepository.saveWeekdaySchedule(currentS, intent.time)
                    } else {
                        val currentS = "${_state.value.weekendSleep.hour.toString().padStart(2, '0')}:${_state.value.weekendSleep.minute.toString().padStart(2, '0')}"
                        sleepSettingsRepository.saveWeekendSchedule(currentS, intent.time)
                    }
                    triggerScheduling()
                }
            }

            is AlarmIntent.ToggleSleepAlarm -> {
                val tab = _state.value.selectedTab
                viewModelScope.launch {
                    if (tab == AlarmTab.WEEKDAY) {
                        sleepSettingsRepository.saveWeekdaySleepEnabled(intent.enabled)
                    } else {
                        sleepSettingsRepository.saveWeekendSleepEnabled(intent.enabled)
                    }
                    triggerScheduling()
                }
            }

            is AlarmIntent.ToggleWakeAlarm -> {
                val tab = _state.value.selectedTab
                viewModelScope.launch {
                    if (tab == AlarmTab.WEEKDAY) {
                        sleepSettingsRepository.saveWeekdayWakeEnabled(intent.enabled)
                    } else {
                        sleepSettingsRepository.saveWeekendWakeEnabled(intent.enabled)
                    }
                    triggerScheduling()
                }
            }
        }
    }

    private fun triggerScheduling() {
        viewModelScope.launch {
            val wdB = sleepSettingsRepository.weekdayBedtime.first()
            val wdW = sleepSettingsRepository.weekdayWakeTime.first()
            val wdSE = sleepSettingsRepository.isWeekdaySleepEnabled.first()
            
            val weB = sleepSettingsRepository.weekendBedtime.first()
            val weW = sleepSettingsRepository.weekendWakeTime.first()
            val weSE = sleepSettingsRepository.isWeekendSleepEnabled.first()
            
            sleepScheduler.scheduleNextEvents(
                wdB, wdW, wdSE, weB, weW, weSE
            )
        }
    }
}
