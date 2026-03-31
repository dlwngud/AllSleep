package com.wngud.allsleep.ui.stats

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StatsViewModel : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is StatsIntent.SelectDate -> _state.update { it.copy(selectedDate = intent.date) }
            is StatsIntent.NavigateMonth -> _state.update { it.copy(selectedYearMonth = intent.yearMonth) }
            is StatsIntent.SelectPeriod -> _state.update { it.copy(selectedPeriodIndex = intent.index) }
        }
    }
}
