package com.wngud.allsleep.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class StatsViewModel(
    private val sleepRecordRepository: SleepRecordRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sleepSettingsRepository: com.wngud.allsleep.domain.repository.SleepSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    init {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
        _state.update { it.copy(selectedYearMonth = currentMonth) }
        
        loadInitialData(currentMonth)
        observeTargetSettings()
    }

    private fun observeTargetSettings() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                sleepSettingsRepository.weekdayBedtime,
                sleepSettingsRepository.weekdayWakeTime,
                sleepSettingsRepository.weekendBedtime,
                sleepSettingsRepository.weekendWakeTime
            ) { wdB, wdW, weB, weW ->
                val wdTarget = calculateTimeDiffMinutes(wdB, wdW)
                val weTarget = calculateTimeDiffMinutes(weB, weW)
                // 주간 총 목표 시간의 평균 (평일 5일 + 주말 2일)
                (wdTarget * 5 + weTarget * 2) / 7
            }.collect { avgTargetMinutes ->
                _state.update { it.copy(currentTargetMinutes = avgTargetMinutes) }
                // 목표가 바뀌면 부채 계산 등이 달라지므로 통계 재계산
                calculateTrendStats()
            }
        }
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
            480
        }
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is StatsIntent.SelectDate -> _state.update { it.copy(selectedDate = intent.date) }
            is StatsIntent.NavigateMonth -> {
                _state.update { it.copy(selectedYearMonth = intent.yearMonth) }
                loadMonthlyRecords(intent.yearMonth)
            }
            is StatsIntent.SelectPeriod -> {
                _state.update { it.copy(selectedPeriodIndex = intent.index) }
                calculateTrendStats()
            }
            StatsIntent.Refresh -> {
                loadInitialData(_state.value.selectedYearMonth, isRefresh = true)
            }
        }
    }

    private fun loadInitialData(yearMonth: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            }
            val user = getCurrentUserUseCase() ?: run {
                if (isRefresh) _state.update { it.copy(isRefreshing = false) }
                return@launch
            }
            
            // 1. 최신 수면 기록 로드 (헤더용)
            val latestResult = sleepRecordRepository.getLatestRecord(user.uid)
            latestResult.onSuccess { last ->
                _state.update { it.copy(latestRecord = last) }
            }

            // 2. 월간 기록 로드 (캘린더용)
            loadMonthlyRecords(yearMonth)

            // 3. 전체 기록 로드 (통계 분석용)
            loadTrendRecords()
            
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun loadTrendRecords() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val twoYearsAgo = today.minus(730, DateTimeUnit.DAY)
            
            val result = sleepRecordRepository.getRecordsByRange(user.uid, twoYearsAgo.toString(), today.toString())
            result.onSuccess { recordList ->
                _state.update { it.copy(trendRecords = recordList, isLoading = false) }
                calculateTrendStats()
            }.onFailure { e ->
                _state.update { it.copy(error = "기본 데이터 로드 실패: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun loadMonthlyRecords(yearMonth: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val user = getCurrentUserUseCase()
                if (user != null) {
                    val result = sleepRecordRepository.getRecordsByMonth(user.uid, yearMonth)
                    result.onSuccess { recordList ->
                        val recordsMap = recordList.associateBy { it.date }
                        _state.update { it.copy(records = recordsMap, isLoading = false) }
                        
                        // 통계 계산 실행
                        calculateTrendStats()
                    }.onFailure { e ->
                        _state.update { it.copy(error = "기록 로드 실패: ${e.message}", isLoading = false) }
                    }
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 현재 로드된 records와 선택된 기간(selectedPeriodIndex)을 기반으로 모든 통계 지표 계산
     */
    private fun calculateTrendStats() {
        val allRecords = _state.value.trendRecords.sortedByDescending { it.date }
        if (allRecords.isEmpty()) {
            _state.update { it.copy(
                avgSleepMinutes = 0,
                avgEfficiency = 0f,
                achievementCount = 0,
                streakDays = 0,
                heatmapData = emptyList(),
                bestRecord = null,
                worstRecord = null
            ) }
            return
        }

        // 1. 기간별 필터링 및 그룹화
        val (filteredRecords, periodBars, periodLabels) = aggregatePeriodData(allRecords)

        // 2. 핵심 지표 계산
        val totalMinutes = filteredRecords.sumOf { it.durationMinutes }
        val avgMinutes = if (filteredRecords.isNotEmpty()) totalMinutes / filteredRecords.size else 0
        val avgEff = if (filteredRecords.isNotEmpty()) filteredRecords.map { it.sleepEfficiency * 0.01f }.average().toFloat().coerceIn(0f, 1f) else 0f
        val achieved = filteredRecords.count { it.achievementRate >= 95f }

        // 3. 수면 분석 점수 산출
        val (score, durationScore, consistencyScore, lockScore) = calculateAnalysisScores(filteredRecords)

        // 4. 잠금 모드 연속 사용(Streak) 계산
        val streak = calculateStreak(allRecords)

        // 5. 요일별 히트맵 데이터 계산
        val heatmap = calculateHeatmapData(allRecords)

        // 6. 베스트/워스트 기록 추출
        val best = filteredRecords.maxByOrNull { it.achievementRate }
        val worst = filteredRecords.minByOrNull { it.achievementRate }

        // 7. 동적 지표 및 AI 메시지 생성
        val (mLabels, mValues) = calculateDynamicMetrics(filteredRecords)
        val (aiMsg, aiSym) = generateAIInsightMessage(filteredRecords, score, streak)

        val (recentAvg, recentDebt) = calculateRecentMetrics(allRecords)

        _state.update { it.copy(
            avgSleepMinutes = avgMinutes,
            avgEfficiency = avgEff,
            achievementCount = achieved,
            streakDays = streak,
            sleepScore = score,
            durationScore = durationScore,
            consistencyScore = consistencyScore,
            lockComplianceScore = lockScore,
            periodBars = periodBars,
            periodLabels = periodLabels,
            heatmapData = heatmap,
            bestRecord = best,
            worstRecord = worst,
            metricLabel1 = mLabels.getOrElse(0) { "" },
            metricValue1 = mValues.getOrElse(0) { "" },
            metricLabel2 = mLabels.getOrElse(1) { "" },
            metricValue2 = mValues.getOrElse(1) { "" },
            metricLabel3 = mLabels.getOrElse(2) { "" },
            metricValue3 = mValues.getOrElse(2) { "" },
            aiMessage = aiMsg,
            aiSymbol = aiSym,
            recentAvgMinutes = recentAvg,
            recentSevenDaysDebt = recentDebt
        ) }
    }

    private fun calculateRecentMetrics(allRecords: List<SleepRecord>): Pair<Int, Int> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val sevenDaysAgo = today.minus(7, DateTimeUnit.DAY)
        val recentRecords = allRecords.filter { LocalDate.parse(it.date) >= sevenDaysAgo }
        
        val avg = if (recentRecords.isNotEmpty()) recentRecords.map { it.durationMinutes }.average().toInt() else 0
        
        val target = _state.value.currentTargetMinutes
        val actualTotal = recentRecords.sumOf { it.durationMinutes }
        val targetTotal = target * 7 
        val debt = (targetTotal - actualTotal).coerceAtLeast(0)
        
        return avg to debt
    }

    private fun calculateDynamicMetrics(records: List<SleepRecord>): Pair<List<String>, List<String>> {
        val periodIndex = _state.value.selectedPeriodIndex
        val labels = mutableListOf<String>()
        val values = mutableListOf<String>()

        val avgMin = if (records.isNotEmpty()) records.map { it.durationMinutes }.average().toInt() else 0
        val avgEff = if (records.isNotEmpty()) records.map { it.sleepEfficiency }.average().toInt() else 0
        val achieved = records.count { it.achievementRate >= 95f }

        when (periodIndex) {
            0 -> { // 주간
                labels.addAll(listOf("평균 수면", "수면 효율", "목표 달성"))
                values.addAll(listOf(formatMinutes(avgMin), "$avgEff%", "${achieved}일"))
            }
            1 -> { // 월간
                labels.addAll(listOf("평균 수면", "총 숙면 시간", "달성일"))
                val totalMin = records.sumOf { it.durationMinutes }
                values.addAll(listOf(formatMinutes(avgMin), formatMinutes(totalMin), "${achieved}일"))
            }
            2 -> { // 올해
                labels.addAll(listOf("연간 평균", "최고 수면 월", "평균 효율"))
                val monthlyAvgs = records.groupBy { LocalDate.parse(it.date).monthNumber }
                    .mapValues { it.value.map { r -> r.durationMinutes }.average() }
                val bestMonth = monthlyAvgs.maxByOrNull { it.value }?.key ?: "-"
                values.addAll(listOf(formatMinutes(avgMin), if (bestMonth == "-") "-" else "${bestMonth}월", "$avgEff%"))
            }
            else -> { // 전체
                labels.addAll(listOf("평생 평균", "누계 수면", "최대 스트릭"))
                val totalMin = records.sumOf { it.durationMinutes }
                values.addAll(listOf(formatMinutes(avgMin), formatMinutes(totalMin), "${_state.value.streakDays}일"))
            }
        }
        return labels to values
    }

    private fun generateAIInsightMessage(records: List<SleepRecord>, score: Int, streak: Int): Pair<String, String> {
        if (records.isEmpty()) {
            return "분석할 수면 데이터가 아직 부족해요. 오늘 밤부터 잠금을 켜고 숙면을 기록해 보세요!" to "✨"
        }
        
        val avgEff = records.map { it.sleepEfficiency }.average()
        val avgDuration = records.map { it.durationMinutes }.average()
        
        return when {
            score >= 95 -> "완벽한 수면 패턴을 유지하고 계시네요! 지금처럼만 하시면 최고의 컨디션을 유지할 수 있습니다. ✨" to "🥇"
            streak >= 7 -> "일주일 연속 잠금 모드 성공! 스마트폰 없는 깊은 잠의 가치를 증명하고 계시네요. 🔥" to "🔥"
            avgEff < 75 -> "최근 수면의 질이 조금 낮아졌어요. 자기 전 블루라이트를 멀리하고 암막 환경을 만들어보세요. 😴" to "💤"
            score < 70 -> "최근 수면이 부족합니다. 오늘 하루는 30분만 더 일찍 잠자리에 들어보는 건 어떨까요? 🌙" to "⚠️"
            else -> "규칙적인 수면 습관은 뇌 건강에 큰 도움이 됩니다. 일관성 있는 취침 시간을 잘 유지하고 계시네요! 😊" to "✨"
        }
    }

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h}h ${m}m"
    }

    /**
     * 수면 분석 점수 산출 (시간/일관성/준수율 기반)
     */
    private fun calculateAnalysisScores(records: List<SleepRecord>): ScoreResult {
        if (records.isEmpty()) return ScoreResult(0, 0f, 0f, 0f)

        // 1. 시간 충족도 (40%)
        val target = _state.value.currentTargetMinutes.toFloat()
        val avgDuration = records.map { it.durationMinutes }.average().toFloat()
        val durationScore = (avgDuration / target).coerceIn(0f, 1.25f) // 과수면 시 1.25까지 허용 후 감점 로직 가능
        val normDuration = if (durationScore > 1f) (2f - durationScore).coerceIn(0.5f, 1f) else durationScore

        // 2. 잠금 준수율 (30%)
        val lockScore = records.map { it.sleepEfficiency / 100f }.average().toFloat().coerceIn(0f, 1f)

        // 3. 일관성 (30%) - 취침 시간의 표준편차 활용
        val bedtimes = records.map { 
            val instant = Instant.fromEpochMilliseconds(it.bedtime)
            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            dt.hour * 60 + dt.minute
        }
        val avgBedtime = bedtimes.average()
        val variance = bedtimes.map { (it - avgBedtime) * (it - avgBedtime) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        // 표준편차가 30분 이내면 만점, 2시간(120분) 이상이면 0점
        val consistencyScore = (1f - (stdDev.toFloat() / 120f)).coerceIn(0f, 1f)

        val totalScore = (normDuration * 40 + consistencyScore * 30 + lockScore * 30).toInt()
        
        return ScoreResult(totalScore, normDuration, consistencyScore, lockScore)
    }

    private data class ScoreResult(val total: Int, val duration: Float, val consistency: Float, val lock: Float)

    /**
     * 기간 필터링 및 차트용 그룹화 데이터 생성
     */
    private fun aggregatePeriodData(allRecords: List<SleepRecord>): Triple<List<SleepRecord>, List<Float>, List<String>> {
        val periodIndex = _state.value.selectedPeriodIndex
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return when (periodIndex) {
            0 -> { // 이번 주 (7일간의 일별 데이터)
                val sevenDaysAgo = today.minus(7, DateTimeUnit.DAY)
                val filtered = allRecords.filter { LocalDate.parse(it.date) >= sevenDaysAgo }
                val (vals, labs) = generateWeeklyChartData(today, allRecords)
                Triple(filtered, vals, labs)
            }
            1 -> { // 이번 달 (4주간의 주별 평균 데이터)
                val firstDayOfMonth = LocalDate(today.year, today.monthNumber, 1)
                val filtered = allRecords.filter { LocalDate.parse(it.date) >= firstDayOfMonth }
                val (vals, labs) = generateMonthlyChartData(today.year, today.monthNumber, allRecords)
                Triple(filtered, vals, labs)
            }
            2 -> { // 올해 (12개월간의 월별 평균 데이터)
                val firstDayOfYear = LocalDate(today.year, 1, 1)
                val filtered = allRecords.filter { LocalDate.parse(it.date).year == today.year }
                val (vals, labs) = generateYearlyChartData(today.year, allRecords)
                Triple(filtered, vals, labs)
            }
            else -> { // 전체 (연도별 평균 데이터)
                val (vals, labs) = generateAllTimeChartData(allRecords)
                Triple(allRecords, vals, labs)
            }
        }
    }

    private fun generateWeeklyChartData(today: LocalDate, allRecords: List<SleepRecord>): Pair<List<Float>, List<String>> {
        val results = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")
        val recordsMap = allRecords.associateBy { it.date }

        for (i in 6 downTo 0) {
            val date = today.minus(i, DateTimeUnit.DAY)
            val dateStr = date.toString()
            val record = recordsMap[dateStr]
            results.add(if (record != null) record.durationMinutes / 60f else 0f)
            labels.add(dayNames[(date.dayOfWeek.ordinal) % 7])
        }
        return results to labels
    }

    private fun generateMonthlyChartData(year: Int, month: Int, allRecords: List<SleepRecord>): Pair<List<Float>, List<String>> {
        val results = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        for (i in 1..4) {
            val weekAvg = allRecords
                .filter { r -> 
                    val d = LocalDate.parse(r.date)
                    d.year == year && d.monthNumber == month && ((d.dayOfMonth - 1) / 7 == i - 1)
                }
                .map { it.durationMinutes / 60f }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            results.add(weekAvg)
            labels.add("${i}주")
        }
        return results to labels
    }

    private fun generateAllTimeChartData(allRecords: List<SleepRecord>): Pair<List<Float>, List<String>> {
        val results = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val yearlyData = allRecords.groupBy { LocalDate.parse(it.date).year }.toSortedMap()
        
        yearlyData.forEach { (year, records) ->
            val avg = records.map { it.durationMinutes / 60f }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            results.add(avg)
            labels.add("${year}년")
        }
        
        return results to labels
    }

    private fun generateYearlyChartData(year: Int, allRecords: List<SleepRecord>): Pair<List<Float>, List<String>> {
        val results = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val months = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")
        for (i in 1..12) {
            val monthAvg = allRecords
                .filter { r -> 
                    val d = LocalDate.parse(r.date)
                    d.year == year && d.monthNumber == i 
                }
                .map { it.durationMinutes / 60f }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            results.add(monthAvg)
            labels.add(months[i - 1])
        }
        return results to labels
    }

    private fun calculateStreak(sortedRecords: List<SleepRecord>): Int {
        if (sortedRecords.isEmpty()) return 0
        
        var count = 0
        // 최신 날짜부터 역순으로 확인
        for (i in sortedRecords.indices) {
            val record = sortedRecords[i]
            if (record.isLockUsed) {
                count++
            } else {
                if (count > 0) break 
            }
        }
        return count
    }

    private fun calculateHeatmapData(allRecords: List<SleepRecord>): List<Float> {
        val heatmap = MutableList(7) { 0f }
        val counts = MutableList(7) { 0 }
        
        allRecords.forEach { record ->
            val date = LocalDate.parse(record.date)
            val dayIndex = (date.dayOfWeek.ordinal) % 7 
            heatmap[dayIndex] += (record.durationMinutes / 60f)
            counts[dayIndex]++
        }
        
        for (i in 0..6) {
            if (counts[i] > 0) heatmap[i] /= counts[i].toFloat()
        }
        return heatmap
    }
}
