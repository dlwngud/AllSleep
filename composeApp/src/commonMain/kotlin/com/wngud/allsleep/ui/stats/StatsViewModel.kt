package com.wngud.allsleep.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.stats.CalculateSleepReportUseCase
import com.wngud.allsleep.domain.usecase.subscription.ObservePremiumStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

class StatsViewModel(
    private val sleepRecordRepository: SleepRecordRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val calculateSleepReportUseCase: CalculateSleepReportUseCase,
    private val observePremiumStatusUseCase: ObservePremiumStatusUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    init {
        val currentMonth = currentYearMonth()
        _state.update { it.copy(selectedYearMonth = currentMonth) }
        observePremiumStatus()
        observeTargetSettings()
        loadData(currentMonth)
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is StatsIntent.SelectDate -> {
                _state.update {
                    it.copy(
                        selectedDate = intent.date,
                        selectedRecord = it.records[intent.date]
                    )
                }
            }
            is StatsIntent.NavigateMonth -> {
                _state.update { it.copy(selectedYearMonth = intent.yearMonth, selectedDate = null, selectedRecord = null) }
                loadMonthlyRecords(intent.yearMonth)
            }
            StatsIntent.Retry -> loadData(_state.value.selectedYearMonth.ifBlank { currentYearMonth() })
        }
    }

    private fun observeTargetSettings() {
        viewModelScope.launch {
            combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime,
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime
            ) { weekdayBedtime, weekdayWakeTime, weekendBedtime, weekendWakeTime ->
                val weekdayTarget = calculateTimeDiffMinutes(weekdayBedtime, weekdayWakeTime)
                val weekendTarget = calculateTimeDiffMinutes(weekendBedtime, weekendWakeTime)
                (weekdayTarget * 5 + weekendTarget * 2) / 7
            }.collect { targetMinutes ->
                _state.update { it.copy(currentTargetMinutes = targetMinutes) }
                recalculateSummary()
            }
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            observePremiumStatusUseCase().collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    private fun loadData(yearMonth: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val user = getCurrentUserUseCase()
            if (user == null) {
                _state.update { it.copy(isLoading = false, error = "로그인이 필요합니다.") }
                return@launch
            }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val twoYearsAgo = today.minus(730, DateTimeUnit.DAY)

            val latestResult = sleepRecordRepository.getLatestRecord(user.uid)
            val monthResult = sleepRecordRepository.getRecordsByMonth(user.uid, yearMonth)
            val trendResult = sleepRecordRepository.getRecordsByRange(user.uid, twoYearsAgo.toString(), today.toString())

            val error = latestResult.exceptionOrNull()?.message
                ?: monthResult.exceptionOrNull()?.message
                ?: trendResult.exceptionOrNull()?.message

            if (error != null) {
                _state.update { it.copy(isLoading = false, error = "수면 기록을 불러오지 못했어요: $error") }
                return@launch
            }

            val monthlyRecords = monthResult.getOrDefault(emptyList()).associateBy { it.date }
            _state.update {
                it.copy(
                    isLoading = false,
                    latestRecord = latestResult.getOrNull(),
                    records = monthlyRecords,
                    trendRecords = trendResult.getOrDefault(emptyList()).sortedBy { record -> record.date },
                    selectedRecord = it.selectedDate?.let { date -> monthlyRecords[date] },
                    error = null
                )
            }
            recalculateSummary()
        }
    }

    private fun loadMonthlyRecords(yearMonth: String) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _state.update { it.copy(isLoading = true, error = null) }
            sleepRecordRepository.getRecordsByMonth(user.uid, yearMonth)
                .onSuccess { recordList ->
                    val records = recordList.associateBy { it.date }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            records = records,
                            selectedRecord = it.selectedDate?.let { date -> records[date] }
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = "월간 기록을 불러오지 못했어요: ${e.message}") }
                }
        }
    }

    private fun recalculateSummary() {
        val state = _state.value
        val report = calculateSleepReportUseCase(
            records = state.trendRecords,
            currentTargetMinutes = state.currentTargetMinutes,
        )

        _state.update {
            it.copy(
                weeklyBars = report.weeklyBars,
                weeklyLabels = report.weeklyLabels,
                weeklyAverageMinutes = report.weeklyAverageMinutes,
                sleepScore = report.sleepScore,
                scoreLabel = report.scoreLabel,
                sleepDebtMinutes = report.sleepDebtMinutes,
                sleepDebtLevel = report.sleepDebtLevel,
                achievementCount = report.achievementCount,
                streakDays = report.streakDays,
                aiMessage = report.aiMessage,
                weeklyDeltaMinutes = report.weeklyDeltaMinutes,
                bedtimeConsistencyMinutes = report.bedtimeConsistencyMinutes,
                weekendDriftMinutes = report.weekendDriftMinutes,
                bestRecord = report.bestRecord,
                worstRecord = report.worstRecord,
                premiumSummary = report.premiumSummary
            )
        }
    }

    private fun currentYearMonth(): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
    }

    private fun calculateTimeDiffMinutes(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }
            val startTotal = startParts[0] * 60 + startParts[1]
            var endTotal = endParts[0] * 60 + endParts[1]
            if (endTotal <= startTotal) endTotal += 24 * 60
            endTotal - startTotal
        } catch (e: Exception) {
            println("StatsViewModel Error: 시간 파싱 실패(start=$start, end=$end): ${e.message}")
            480
        }
    }

}
