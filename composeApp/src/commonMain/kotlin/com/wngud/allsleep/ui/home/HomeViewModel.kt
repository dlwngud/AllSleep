package com.wngud.allsleep.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 홈 탭 ViewModel
 * 홈 탭 내부의 비즈니스 로직과 상태만 관리합니다.
 * 탭 전환은 NavController(Nav2)가 담당하므로 ViewModel에서 처리하지 않습니다.
 */
class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.StartSleep -> startSleep()
            is HomeIntent.Refresh -> refreshData()
        }
    }

    private fun startSleep() {
        // TODO: 수면 시작 로직 (Repository 호출 등)
    }

    private fun refreshData() {
        // TODO: 데이터 갱신 로직
    }
}
