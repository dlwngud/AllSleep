package com.wngud.allsleep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.HomeSleepGoal
import com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase
import com.wngud.allsleep.domain.usecase.home.ObserveHomeSleepGoalUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveRegisteredDevicesUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.sleep.ToggleSleepModeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    observeHomeSleepGoalUseCase: ObserveHomeSleepGoalUseCase,
    private val observeUserUseCase: ObserveUserUseCase,
    private val observeUserSleepStateUseCase: ObserveUserSleepStateUseCase,
    private val observeRegisteredDevicesUseCase: ObserveRegisteredDevicesUseCase,
    private val toggleSleepModeUseCase: ToggleSleepModeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private var currentUid: String? = null
    private var sleepStateJob: Job? = null
    private var devicesJob: Job? = null

    val sleepGoal = observeHomeSleepGoalUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeSleepGoal(targetMinutes = 480, displayText = "8h"),
        )

    init {
        observeUserSession()
    }

    fun startSleepMode() {
        val uid = currentUid ?: return
        if (_state.value.isToggleLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isToggleLoading = true, error = null) }
            toggleSleepModeUseCase(
                uid = uid,
                currentState = _state.value.sleepState,
                isSleeping = true,
            ).onSuccess { nextState ->
                _state.update { it.copy(sleepState = nextState) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isToggleLoading = false) }
        }
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            observeUserUseCase().collect { user ->
                val uid = user?.uid
                if (uid == currentUid) return@collect

                currentUid = uid
                sleepStateJob?.cancel()
                devicesJob?.cancel()

                if (uid == null) {
                    _state.update { HomeState() }
                } else {
                    observeSleepState(uid)
                    observeDevices(uid)
                }
            }
        }
    }

    private fun observeSleepState(uid: String) {
        sleepStateJob = viewModelScope.launch {
            observeUserSleepStateUseCase(uid)
                .catch { e -> _state.update { it.copy(error = e.message) } }
                .collect { sleepState ->
                    _state.update { it.copy(sleepState = sleepState) }
                }
        }
    }

    private fun observeDevices(uid: String) {
        devicesJob = viewModelScope.launch {
            observeRegisteredDevicesUseCase(uid)
                .catch { e -> _state.update { it.copy(error = e.message) } }
                .collect { devices ->
                    _state.update { it.copy(registeredDevices = devices) }
                }
        }
    }
}
