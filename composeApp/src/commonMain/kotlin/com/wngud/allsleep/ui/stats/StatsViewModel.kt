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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    init {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
        _state.update { it.copy(selectedYearMonth = currentMonth) }
        
        loadInitialData(currentMonth)
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
        }
    }

    private fun loadInitialData(yearMonth: String) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            
            // 1. 최신 수면 기록 로드 (헤더용)
            val latestResult = sleepRecordRepository.getLatestRecord(user.uid)
            latestResult.onSuccess { last ->
                _state.update { it.copy(latestRecord = last) }
            }

            // 2. 월간 기록 로드 및 통계 계산
            loadMonthlyRecords(yearMonth)
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
                        _state.update { it.copy(records = recordsMap) }
                        
                        // 통계 계산 실행
                        calculateTrendStats()
                    }.onFailure { e ->
                        _state.update { it.copy(error = "기록 로드 실패: ${e.message}") }
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
        val allRecords = _state.value.records.values.toList().sortedByDescending { it.date }
        if (allRecords.isEmpty()) {
            _state.update { it.copy(
                avgSleepMinutes = 0,
                avgEfficiency = 0f,
                achievementCount = 0,
                streakDays = 0,
                weeklyTrend = emptyList(),
                trendDates = emptyList(),
                heatmapData = emptyList(),
                bestRecord = null,
                worstRecord = null
            ) }
            return
        }

        // 1. 기간별 필터링
        val periodIndex = _state.value.selectedPeriodIndex
        val filteredRecords = when (periodIndex) {
            0 -> getRecentSevenDaysRecords(allRecords) // 이번 주 (최근 7일)
            else -> allRecords // 이번 달 (현재 선택된 월 전체)
        }

        // 2. 핵심 지표 계산
        val totalMinutes = filteredRecords.sumOf { it.durationMinutes }
        val avgMinutes = if (filteredRecords.isNotEmpty()) totalMinutes / filteredRecords.size else 0
        val avgEff = if (filteredRecords.isNotEmpty()) filteredRecords.map { it.sleepEfficiency }.average().toFloat() / 100f else 0f
        val achieved = filteredRecords.count { it.achievementRate >= 95f }

        // 3. 잠금 모드 연속 사용(Streak) 계산
        val streak = calculateStreak(allRecords)

        // 4. 차트용 주간 데이터 생성 (최근 7일 고정 레이아웃 대응)
        val (trendValues, trendLabels) = generateChartData()

        // 5. 요일별 히트맵 데이터 계산 (월~일 평균 수면 시간)
        val heatmap = calculateHeatmapData(allRecords)

            // 6. 베스트/워스트 기록 추출 (달성률 기준)
        val best = filteredRecords.maxByOrNull { it.achievementRate }
        val worst = filteredRecords.minByOrNull { it.achievementRate }

        _state.update { it.copy(
            avgSleepMinutes = avgMinutes,
            avgEfficiency = avgEff,
            achievementCount = achieved,
            streakDays = streak,
            weeklyTrend = trendValues,
            trendDates = trendLabels,
            heatmapData = heatmap,
            bestRecord = best,
            worstRecord = worst
        ) }
    }

    private fun getRecentSevenDaysRecords(allRecords: List<SleepRecord>): List<SleepRecord> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val sevenDaysAgo = today.minus(7, DateTimeUnit.DAY)
        return allRecords.filter { 
            val recordDate = LocalDate.parse(it.date)
            recordDate >= sevenDaysAgo
        }
    }

    private fun calculateStreak(sortedRecords: List<SleepRecord>): Int {
        if (sortedRecords.isEmpty()) return 0
        
        var count = 0
        // 최신 날짜부터 역순으로 확인 (이전 날짜와 차이가 1일 이내면서 isLockUsed인 것들)
        for (i in sortedRecords.indices) {
            val record = sortedRecords[i]
            if (record.isLockUsed) {
                count++
            } else {
                // 오늘 기록이 없거나(아직 잠금 전) 잠금을 안쓴 경우 여기서 바로 끊김
                if (count > 0) break 
            }
        }
        return count
    }

    private fun generateChartData(): Pair<List<Float>, List<String>> {
        val results = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")

        for (i in 6 downTo 0) {
            val date = today.minus(i, DateTimeUnit.DAY)
            val dateStr = "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
            val record = _state.value.records[dateStr]
            
            results.add(if (record != null) record.durationMinutes / 60f else 0f)
            
            // kotlinx-datetime DayOfWeek: MONDAY(1) ~ SUNDAY(7)
            // 요일 이름 매핑을 위해 0-6으로 변환
            val dayOfWeekIndex = (date.dayOfWeek.ordinal) % 7 
            labels.add(dayNames[dayOfWeekIndex])
        }

        return results to labels
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
            if (counts[i] > 0) heatmap[i] /= counts[i]
        }
        return heatmap
    }
}
