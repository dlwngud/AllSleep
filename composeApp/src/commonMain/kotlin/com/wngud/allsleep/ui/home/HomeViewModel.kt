package com.wngud.allsleep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 홈 ViewModel (MVI 패턴)
 */
class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectTab -> selectTab(intent.tabIndex)
            is HomeIntent.StartSleep -> startSleep()
            is HomeIntent.Refresh -> refreshData()
        }
    }

    private fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    private fun startSleep() {
        // TODO: 수면 시작 로직 (Repository 호출 등)
        android.util.Log.d("HomeViewModel", "Sleep started!")
    }

    private fun refreshData() {
        // TODO: 데이터 갱신 로직
    }
}
