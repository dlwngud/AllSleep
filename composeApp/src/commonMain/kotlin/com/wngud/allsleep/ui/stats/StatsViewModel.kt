package com.wngud.allsleep.ui.stats

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 통계 탭 ViewModel
 * 통계 화면의 상태(선택된 기간 등)를 관리합니다.
 */
class StatsViewModel : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.SelectTimePeriod -> {
                _state.update { it.copy(timePeriodIndex = intent.index) }
            }
        }
    }
}
