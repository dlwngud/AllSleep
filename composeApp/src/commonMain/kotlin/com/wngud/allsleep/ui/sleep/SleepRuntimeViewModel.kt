package com.wngud.allsleep.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.formatTimestampToDate
import com.wngud.allsleep.domain.model.platformTimeMillis
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.sleep.RecordSleepSessionUseCase
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.platform.SleepScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SleepRuntimeViewModel(
    private val observeUserUseCase: ObserveUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val observeUserSleepStateUseCase: ObserveUserSleepStateUseCase,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val recordSleepSessionUseCase: RecordSleepSessionUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val sleepScheduler: SleepScheduler,
) : ViewModel() {

    private data class RoutineData(
        val bedtime: String,
        val wakeTime: String,
        val sleepEnabled: Boolean,
        val wakeEnabled: Boolean,
    )

    private val _state = MutableStateFlow(SleepRuntimeState())
    val state = _state.asStateFlow()

    private var currentUid: String? = null
    private var observeJob: Job? = null
    private var isLoggingOut = false

    init {
        observeUserSession()
        setupOnboardingSync()
        setupSettingsSync()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            observeUserUseCase().collect { user ->
                val uid = user?.uid
                if (uid == currentUid) return@collect

                currentUid = uid
                observeJob?.cancel()

                if (uid == null) {
                    _state.update { SleepRuntimeState() }
                } else {
                    startObserving(uid)
                }
            }
        }
    }

    private fun setupOnboardingSync() {
        viewModelScope.launch {
            sleepSettingsRepository.isOnboardingCompleted
                .distinctUntilChanged()
                .collect { completed ->
                    if (completed && currentUid != null) {
                        syncScheduleToFirestore()
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSettingsSync() {
        viewModelScope.launch {
            val weekdayFlow = combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime,
                sleepSettingsRepository.isWeekdaySleepEnabled,
                sleepSettingsRepository.isWeekdayWakeEnabled,
            ) { bedtime, wakeTime, sleepEnabled, wakeEnabled ->
                RoutineData(bedtime, wakeTime, sleepEnabled, wakeEnabled)
            }

            val weekendFlow = combine(
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime,
                sleepSettingsRepository.isWeekendSleepEnabled,
                sleepSettingsRepository.isWeekendWakeEnabled,
            ) { bedtime, wakeTime, sleepEnabled, wakeEnabled ->
                RoutineData(bedtime, wakeTime, sleepEnabled, wakeEnabled)
            }

            combine(weekdayFlow, weekendFlow) { weekday, weekend -> weekday to weekend }
                .distinctUntilChanged()
                .debounce(500L)
                .collect { (weekday, weekend) ->
                    val uid = currentUid ?: return@collect
                    val remoteState = _state.value.sleepState ?: return@collect

                    val isChanged = remoteState.weekdayBedtime != weekday.bedtime ||
                        remoteState.weekdayWakeTime != weekday.wakeTime ||
                        remoteState.isWeekdaySleepEnabled != weekday.sleepEnabled ||
                        remoteState.isWeekdayWakeEnabled != weekday.wakeEnabled ||
                        remoteState.weekendBedtime != weekend.bedtime ||
                        remoteState.weekendWakeTime != weekend.wakeTime ||
                        remoteState.isWeekendSleepEnabled != weekend.sleepEnabled ||
                        remoteState.isWeekendWakeEnabled != weekend.wakeEnabled

                    if (isChanged) {
                        updateUserSleepStateUseCase(
                            uid = uid,
                            weekdayBedtime = weekday.bedtime,
                            weekdayWakeTime = weekday.wakeTime,
                            isWeekdaySleepEnabled = weekday.sleepEnabled,
                            isWeekdayWakeEnabled = weekday.wakeEnabled,
                            weekendBedtime = weekend.bedtime,
                            weekendWakeTime = weekend.wakeTime,
                            isWeekendSleepEnabled = weekend.sleepEnabled,
                            isWeekendWakeEnabled = weekend.wakeEnabled,
                        )
                    }
                }
        }
    }

    private suspend fun syncScheduleToFirestore() {
        val uid = currentUid ?: return
        updateUserSleepStateUseCase(
            uid = uid,
            weekdayBedtime = sleepSettingsRepository.weekdayBedtime.first(),
            weekdayWakeTime = sleepSettingsRepository.weekdayWakeTime.first(),
            isWeekdaySleepEnabled = sleepSettingsRepository.isWeekdaySleepEnabled.first(),
            isWeekdayWakeEnabled = sleepSettingsRepository.isWeekdayWakeEnabled.first(),
            weekendBedtime = sleepSettingsRepository.weekendBedtime.first(),
            weekendWakeTime = sleepSettingsRepository.weekendWakeTime.first(),
            isWeekendSleepEnabled = sleepSettingsRepository.isWeekendSleepEnabled.first(),
            isWeekendWakeEnabled = sleepSettingsRepository.isWeekendWakeEnabled.first(),
        )
    }

    private fun startObserving(uid: String) {
        observeJob = viewModelScope.launch {
            observeUserSleepStateUseCase(uid)
                .catch { handleRemoteSessionClosed() }
                .collect { sleepState ->
                    val oldState = _state.value.sleepState
                    if (sleepState == null && currentUid != null) {
                        handleRemoteSessionClosed()
                        return@collect
                    }

                    if (oldState?.isSleeping == true && sleepState?.isSleeping == false) {
                        launch { saveSleepRecordForRemoteStop(uid) }
                    }

                    _state.update { it.copy(sleepState = sleepState) }

                    if (sleepState != null) {
                        launch {
                            syncRemoteScheduleToLocalIfNeeded(sleepState)
                            sleepScheduler.scheduleNextEvents(
                                sleepState.weekdayBedtime,
                                sleepState.weekdayWakeTime,
                                sleepState.isWeekdaySleepEnabled,
                                sleepState.weekendBedtime,
                                sleepState.weekendWakeTime,
                                sleepState.isWeekendSleepEnabled,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun saveSleepRecordForRemoteStop(uid: String) {
        val sleepStartAt = sleepSettingsRepository.activeSleepStartAt.first()
        if (sleepStartAt <= 0L) return

        val currentIsWeekday = isCurrentWeekday()
        val targetBedtime = if (currentIsWeekday) {
            sleepSettingsRepository.weekdayBedtime.first()
        } else {
            sleepSettingsRepository.weekendBedtime.first()
        }
        val targetWakeTime = if (currentIsWeekday) {
            sleepSettingsRepository.weekdayWakeTime.first()
        } else {
            sleepSettingsRepository.weekendWakeTime.first()
        }
        val wakeTimeMs = platformTimeMillis()

        recordSleepSessionUseCase(
            uid = uid,
            date = formatTimestampToDate(wakeTimeMs),
            sleepStartAt = sleepStartAt,
            wakeTimeMs = wakeTimeMs,
            targetBedtime = targetBedtime,
            targetWakeTime = targetWakeTime,
            isLockUsed = true,
        )
        sleepSettingsRepository.saveActiveSleepStartAt(0L)
    }

    private suspend fun syncRemoteScheduleToLocalIfNeeded(sleepState: com.wngud.allsleep.domain.model.UserSleepState) {
        val isDefaultServerData = sleepState.lastUpdatedAt == 0L
        val isDifferent = sleepState.weekdayBedtime != sleepSettingsRepository.weekdayBedtime.first() ||
            sleepState.weekdayWakeTime != sleepSettingsRepository.weekdayWakeTime.first() ||
            sleepState.isWeekdaySleepEnabled != sleepSettingsRepository.isWeekdaySleepEnabled.first() ||
            sleepState.isWeekdayWakeEnabled != sleepSettingsRepository.isWeekdayWakeEnabled.first() ||
            sleepState.weekendBedtime != sleepSettingsRepository.weekendBedtime.first() ||
            sleepState.weekendWakeTime != sleepSettingsRepository.weekendWakeTime.first() ||
            sleepState.isWeekendSleepEnabled != sleepSettingsRepository.isWeekendSleepEnabled.first() ||
            sleepState.isWeekendWakeEnabled != sleepSettingsRepository.isWeekendWakeEnabled.first()

        val isOnboardingDone = sleepSettingsRepository.isOnboardingCompleted.first()
        if (isDifferent && !isDefaultServerData && isOnboardingDone) {
            sleepSettingsRepository.saveWeekdaySchedule(sleepState.weekdayBedtime, sleepState.weekdayWakeTime)
            sleepSettingsRepository.saveWeekdaySleepEnabled(sleepState.isWeekdaySleepEnabled)
            sleepSettingsRepository.saveWeekdayWakeEnabled(sleepState.isWeekdayWakeEnabled)
            sleepSettingsRepository.saveWeekendSchedule(sleepState.weekendBedtime, sleepState.weekendWakeTime)
            sleepSettingsRepository.saveWeekendSleepEnabled(sleepState.isWeekendSleepEnabled)
            sleepSettingsRepository.saveWeekendWakeEnabled(sleepState.isWeekendWakeEnabled)
        }
    }

    private fun handleRemoteSessionClosed() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            signOutUseCase()
            currentUid = null
            _state.update { SleepRuntimeState() }
            isLoggingOut = false
        }
    }

    private fun isCurrentWeekday(): Boolean = com.wngud.allsleep.domain.model.isWeekday(platformTimeMillis())
}
