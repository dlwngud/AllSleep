package com.wngud.allsleep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.AppBlockerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppBlockerViewModel(
    private val appBlockerRepository: AppBlockerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppBlockerState())
    val state: StateFlow<AppBlockerState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AppBlockerEffect>()
    val effect: SharedFlow<AppBlockerEffect> = _effect.asSharedFlow()

    init {
        loadApps()
    }

    fun handleIntent(intent: AppBlockerIntent) {
        when (intent) {
            is AppBlockerIntent.LoadApps -> loadApps()
            is AppBlockerIntent.ToggleAppBlock -> toggleAppBlock(intent.packageName, intent.isBlocked)
            is AppBlockerIntent.UpdateSearchQuery -> _state.update { it.copy(searchQuery = intent.query) }
            is AppBlockerIntent.ToggleSystemApps -> _state.update { it.copy(showSystemApps = intent.show) }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // 1. 초기 설치 앱 목록 로드
                val installedApps = appBlockerRepository.getInstalledApps()
                
                // 2. 블랙리스트 Flow 관찰 시작
                appBlockerRepository.observeBlockedPackages()
                    .collect { blockedSet ->
                        _state.update { currentState ->
                            val updatedApps = installedApps.map { app ->
                                app.copy(isBlocked = blockedSet.contains(app.packageName))
                            }
                            currentState.copy(apps = updatedApps, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _effect.emit(AppBlockerEffect.ShowError("앱 목록을 불러오는데 실패했습니다."))
            }
        }
    }

    private fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        // Optimistic Update: UI 상태를 즉시 변경
        _state.update { currentState ->
            val updatedApps = currentState.apps.map { if (it.packageName == packageName) it.copy(isBlocked = isBlocked) else it }
            currentState.copy(apps = updatedApps)
        }

        viewModelScope.launch {
            try {
                appBlockerRepository.setAppBlocked(packageName, isBlocked)
            } catch (e: Exception) {
                // 실패 시 원래 상태로 복구 (여기서는 단순히 에러만 메시지만 보냄 - 실제로는 이전 상태 저장이 필요할 수 있음)
                _effect.emit(AppBlockerEffect.ShowError("설정 변경에 실패했습니다."))
            }
        }
    }
}
