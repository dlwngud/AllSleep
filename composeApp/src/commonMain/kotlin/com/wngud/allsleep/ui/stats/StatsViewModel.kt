package com.wngud.allsleep.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.math.roundToInt

class StatsViewModel(
    private val sleepRecordRepository: SleepRecordRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    init {
        val currentMonth = currentYearMonth()
        _state.update { it.copy(selectedYearMonth = currentMonth) }
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
        val records = state.trendRecords
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val recentStart = today.minus(6, DateTimeUnit.DAY)
        val previousStart = today.minus(13, DateTimeUnit.DAY)
        val previousEnd = today.minus(7, DateTimeUnit.DAY)
        val recentRecords = records.filter { record ->
            parseDateOrNull(record.date)?.let { it >= recentStart && it <= today } == true
        }
        val previousRecords = records.filter { record ->
            parseDateOrNull(record.date)?.let { it >= previousStart && it <= previousEnd } == true
        }

        val weeklyData = calculateWeeklyTrend(records, today)
        val weeklyAverage = if (recentRecords.isNotEmpty()) {
            recentRecords.map { it.durationMinutes }.average().toInt()
        } else 0
        val previousAverage = if (previousRecords.isNotEmpty()) {
            previousRecords.map { it.durationMinutes }.average().toInt()
        } else 0
        val debt = calculateSleepDebt(recentRecords, state.currentTargetMinutes)
        val score = calculateSleepScore(recentRecords, state.currentTargetMinutes)
        val best = records.maxByOrNull { it.sleepEfficiency }
        val worst = records.minByOrNull { it.sleepEfficiency }

        _state.update {
            it.copy(
                weeklyBars = weeklyData.first,
                weeklyLabels = weeklyData.second,
                weeklyAverageMinutes = weeklyAverage,
                sleepScore = score,
                scoreLabel = scoreLabel(score, recentRecords.isNotEmpty()),
                sleepDebtMinutes = debt,
                sleepDebtLevel = sleepDebtLevel(debt),
                achievementCount = recentRecords.count { record -> record.durationMinutes >= state.currentTargetMinutes },
                streakDays = calculateStreak(records),
                aiMessage = generateInsight(recentRecords, score, debt),
                weeklyDeltaMinutes = weeklyAverage - previousAverage,
                bedtimeConsistencyMinutes = calculateBedtimeConsistency(recentRecords),
                weekendDriftMinutes = calculateWeekendDrift(records),
                bestRecord = best,
                worstRecord = worst,
                premiumSummary = generatePremiumSummary(
                    weeklyDeltaMinutes = weeklyAverage - previousAverage,
                    consistencyMinutes = calculateBedtimeConsistency(recentRecords),
                    weekendDriftMinutes = calculateWeekendDrift(records),
                    recordsCount = records.size
                )
            )
        }
    }

    private fun calculateWeeklyTrend(records: List<SleepRecord>, today: LocalDate): Pair<List<Float>, List<String>> {
        val byDate = records.associateBy { it.date }
        val bars = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        for (offset in 6 downTo 0) {
            val date = today.minus(offset, DateTimeUnit.DAY)
            val record = byDate[date.toString()]
            bars.add((record?.durationMinutes ?: 0) / 60f)
            labels.add(weekdayLabel(date))
        }
        return bars to labels
    }

    private fun calculateSleepDebt(records: List<SleepRecord>, targetMinutes: Int): Int {
        if (records.isEmpty()) return 0
        val targetTotal = targetMinutes * records.size
        val actualTotal = records.sumOf { it.durationMinutes }
        return (targetTotal - actualTotal).coerceAtLeast(0)
    }

    private fun calculateSleepScore(records: List<SleepRecord>, targetMinutes: Int): Int {
        if (records.isEmpty()) return 0
        val durationScore = records.map { record ->
            if (targetMinutes <= 0) 0f else (record.durationMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
        }.average().toFloat()
        val efficiencyScore = records.map { it.sleepEfficiency / 100f }.average().toFloat().coerceIn(0f, 1f)
        val lockScore = records.count { it.isLockUsed }.toFloat() / records.size
        return ((durationScore * 0.45f + efficiencyScore * 0.35f + lockScore * 0.2f) * 100).toInt().coerceIn(0, 100)
    }

    private fun calculateStreak(records: List<SleepRecord>): Int {
        val dates = records.mapNotNull { parseDateOrNull(it.date) }.toSet()
        var day = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var streak = 0
        while (dates.contains(day)) {
            streak += 1
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun calculateBedtimeConsistency(records: List<SleepRecord>): Int {
        val bedtimes = records.mapNotNull { adjustedBedtimeMinute(it.bedtime) }
        if (bedtimes.size < 2) return 0
        val average = bedtimes.average()
        return bedtimes.map { abs(it - average) }.average().roundToInt()
    }

    private fun calculateWeekendDrift(records: List<SleepRecord>): Int {
        val weekdayBedtimes = mutableListOf<Int>()
        val weekendBedtimes = mutableListOf<Int>()
        records.forEach { record ->
            val date = parseDateOrNull(record.date) ?: return@forEach
            val bedtime = adjustedBedtimeMinute(record.bedtime) ?: return@forEach
            if (date.dayOfWeek.ordinal >= 5) {
                weekendBedtimes.add(bedtime)
            } else {
                weekdayBedtimes.add(bedtime)
            }
        }
        if (weekdayBedtimes.isEmpty() || weekendBedtimes.isEmpty()) return 0
        return (weekendBedtimes.average() - weekdayBedtimes.average()).roundToInt()
    }

    private fun adjustedBedtimeMinute(timestamp: Long): Int? {
        if (timestamp <= 0L) return null
        val localTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).time
        val minute = localTime.hour * 60 + localTime.minute
        return if (minute < 12 * 60) minute + 24 * 60 else minute
    }

    private fun generateInsight(records: List<SleepRecord>, score: Int, debtMinutes: Int): String {
        if (records.size < 3) return "3일 이상 기록하면 수면 패턴을 더 정확히 분석할 수 있어요."
        if (debtMinutes >= 180) return "최근 7일 수면이 ${formatMinutes(debtMinutes)} 부족해요. 오늘은 목표 시간보다 30분 일찍 시작해보세요."
        if (score >= 80) return "이번 주 수면 리듬이 안정적이에요. 지금 패턴을 유지하면 좋아요."
        return "수면 시간이 들쭉날쭉해요. 같은 시간에 수면 모드를 시작하는 것부터 맞춰보세요."
    }

    private fun generatePremiumSummary(
        weeklyDeltaMinutes: Int,
        consistencyMinutes: Int,
        weekendDriftMinutes: Int,
        recordsCount: Int
    ): String {
        if (recordsCount < 5) return "5일 이상 기록하면 주간 변화와 취침 패턴을 더 정확히 분석할 수 있어요."
        if (weekendDriftMinutes >= 60) return "주말 취침이 평일보다 ${formatMinutes(weekendDriftMinutes)} 늦어지고 있어요."
        if (consistencyMinutes >= 45) return "취침 시간이 평균 ${formatMinutes(consistencyMinutes)} 흔들리고 있어요."
        if (weeklyDeltaMinutes >= 30) return "지난주보다 평균 수면이 ${formatMinutes(weeklyDeltaMinutes)} 늘었어요."
        return "취침 리듬이 안정적이에요. 지금 패턴을 유지해도 좋아요."
    }

    private fun scoreLabel(score: Int, hasRecords: Boolean): String {
        if (!hasRecords) return "수면 모드를 사용하면 분석이 시작돼요"
        return when {
            score >= 85 -> "아주 안정적인 수면 리듬이에요"
            score >= 70 -> "좋은 흐름이에요"
            score >= 50 -> "조금 더 회복이 필요해요"
            else -> "수면 리듬을 다시 잡아볼 때예요"
        }
    }

    private fun sleepDebtLevel(debtMinutes: Int): SleepDebtLevel = when {
        debtMinutes >= 180 -> SleepDebtLevel.WARNING
        debtMinutes >= 60 -> SleepDebtLevel.CAUTION
        else -> SleepDebtLevel.GOOD
    }

    private fun weekdayLabel(date: LocalDate): String {
        return when (date.dayOfWeek.ordinal + 1) {
            1 -> "월"
            2 -> "화"
            3 -> "수"
            4 -> "목"
            5 -> "금"
            6 -> "토"
            else -> "일"
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

    private fun parseDateOrNull(date: String): LocalDate? = try {
        LocalDate.parse(date)
    } catch (_: Exception) {
        null
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = abs(minutes % 60)
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}
