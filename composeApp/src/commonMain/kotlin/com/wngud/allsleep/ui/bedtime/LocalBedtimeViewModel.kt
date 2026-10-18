package com.wngud.allsleep.ui.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.bedtime.*
import com.wngud.allsleep.domain.repository.LocalBedtimeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class LocalBedtimeUiState(
    val isLoading: Boolean = true,
    val routines: List<BedtimeRoutine> = emptyList(),
    val owner: LocalOwner? = null,
    val sessions: List<BedtimeSession> = emptyList(),
    val todayKind: RoutineKind = RoutineKind.WEEKDAY,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null,
)

class LocalBedtimeViewModel(private val repository: LocalBedtimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(LocalBedtimeUiState())
    val state = _state.asStateFlow()
    private var observeJob: Job? = null
    init { reload() }

    fun reload() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.initialize().onFailure { error ->
                _state.update { it.copy(isLoading = false, error = "로컬 설정을 읽지 못했어요. 저장된 원본은 유지했어요.") }
                return@launch
            }
            combine(repository.routines, repository.journal) { routines, journal -> routines to journal }
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(isLoading = false, error = "로컬 기록을 읽지 못했어요. 다시 확인해 주세요.") }
                }
                .collect { (routines, journal) ->
                    val day = Clock.System.todayIn(TimeZone.currentSystemDefault()).dayOfWeek
                    val todayKind = if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) RoutineKind.WEEKEND else RoutineKind.WEEKDAY
                    _state.update { it.copy(isLoading = false, routines = routines, todayKind = todayKind, owner = journal.owner, sessions = journal.owner?.let(journal::sessionsFor) ?: emptyList()) }
                }
        }
    }

    fun saveRoutine(kind: RoutineKind, bedtime: String, release: String, enabled: Boolean, onSaved: () -> Unit) {
        if (_state.value.isSaving) return
        val routine = try { BedtimeRoutine(kind, bedtime.trim(), release.trim(), enabled).validated() } catch (error: IllegalArgumentException) {
            _state.update { it.copy(error = "시각은 23:30처럼 입력하고 시작과 해제 시각을 다르게 선택해 주세요.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null, savedMessage = null) }
        viewModelScope.launch {
            repository.saveRoutine(routine).onSuccess {
                _state.update { it.copy(isSaving = false, savedMessage = "이 기기에 약속을 저장했어요.") }
                onSaved()
            }.onFailure {
                _state.update { it.copy(isSaving = false, error = "저장하지 못했어요. 입력한 내용을 유지했어요.") }
            }
        }
    }
    fun dismissMessage() = _state.update { it.copy(error = null, savedMessage = null) }
}
