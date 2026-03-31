package com.wngud.allsleep.ui.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// ──────────────────────────────────────────
// Mock 데이터
// ──────────────────────────────────────────
private object MockData {
    val sleepBars = listOf(7f, 5f, 8.5f, 6f, 9f, 4.5f, 8f) // 월~일 (시간)
    val days = listOf("월", "화", "수", "목", "금", "토", "일")
    val targetHours = 8f

    // 달력용: "2026-03-DD" -> achievementRate (null = 기록 없음)
    val calendarData: Map<String, Float?> = buildMap {
        val achieved = listOf(1, 3, 5, 8, 10, 12, 14, 15, 16, 18, 20, 22, 24)
        val partial  = listOf(2, 7, 11, 17, 21, 23, 25, 27)
        val poor     = listOf(19, 26, 28)
        for (d in 1..31) {
            val key = "2026-03-%02d".format(d)
            put(key, when (d) {
                in achieved -> 1.05f
                in partial  -> 0.82f
                in poor     -> 0.55f
                else        -> null
            })
        }
    }

    val weekdayAvgHours = listOf(7.1f, 6.8f, 7.5f, 7.2f, 6.5f, 8.8f, 8.5f) // 월~일
}

// ──────────────────────────────────────────
// 색상 상수
// ──────────────────────────────────────────
private val indigo     = Color(0xFF4938FF)
private val indigoFaint = Color(0x1A4938FF)
private val surface    = Color(0xFF1A1C2E)
private val surfaceHigh = Color(0xFF242645)
private val onSurface  = Color(0xFFF6F5F8)
private val onSurfaceVariant = Color(0xFFC7C4DA)
private val green  = Color(0xFF4CAF50)
private val amber  = Color(0xFFF59E0B)
private val red    = Color(0xFFEF4444)

// ──────────────────────────────────────────
// Entry Point
// ──────────────────────────────────────────
@Composable
fun StatsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateToSubscription: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
    globalViewModel: GlobalSleepViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val globalState by globalViewModel.state.collectAsState()
    val isPremium = globalState.isPremium

    StatsScreenContent(
        contentPadding = contentPadding,
        state = state,
        isPremium = isPremium,
        onIntent = viewModel::handleIntent,
        onNavigateToSubscription = onNavigateToSubscription
    )
}

@Composable
fun StatsScreenContent(
    contentPadding: PaddingValues = PaddingValues(),
    state: StatsState,
    isPremium: Boolean,
    onIntent: (StatsIntent) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(innerPadding)
        ) {
            // ── 고정 헤더 ──────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                HeaderTitleRow(isPremium = isPremium, sleepScore = 82)
                Spacer(Modifier.height(12.dp))
                TodaySummaryCard()
                Spacer(Modifier.height(12.dp))
            }

            // ── 탭 바 ───────────────────────
            StatsTabBar(selectedTab = state.selectedTab, onIntent = onIntent)

            // ── 탭 콘텐츠 ──────────────────
            when (state.selectedTab) {
                StatsTab.RECORD -> RecordTab(state = state, onIntent = onIntent)
                StatsTab.TREND  -> TrendTab(state = state, onIntent = onIntent)
                StatsTab.INSIGHT -> InsightTab(
                    isPremium = isPremium,
                    onNavigateToSubscription = onNavigateToSubscription
                )
            }
        }
    }
}

// ──────────────────────────────────────────
// 고정 헤더 컴포저블
// ──────────────────────────────────────────

@Composable
private fun HeaderTitleRow(isPremium: Boolean, sleepScore: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "통계",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            modifier = Modifier.weight(1f)
        )
        // 수면 점수 미니 뱃지 (프리미엄)
        if (isPremium) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(indigo)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                text = "${sleepScore}점 👑",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
    ) {
        // 왼쪽 인디고 강조선
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(indigo)
                .align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 수면 시간
            Column(modifier = Modifier.weight(1f)) {
                Text("어젯밤", fontSize = 12.sp, color = onSurfaceVariant)
                Text(
                    "7h 32m",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 12.sp)
                    Text(
                        " 23:24  →  ☀️ 07:05",
                        fontSize = 12.sp,
                        color = onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "수면 효율 87%  |  목표 8h",
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                )
            }
            // 우측: 달성률
            Column(horizontalAlignment = Alignment.End) {
                Text("✅ 94%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = green)
                Text("달성", fontSize = 11.sp, color = onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────────────────────
// 탭 바
// ──────────────────────────────────────────

@Composable
private fun StatsTabBar(selectedTab: StatsTab, onIntent: (StatsIntent) -> Unit) {
    val tabs = listOf(
        StatsTab.RECORD  to "기록",
        StatsTab.TREND   to "트렌드",
        StatsTab.INSIGHT to "인사이트 👑"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            val textColor by animateColorAsState(
                targetValue = if (isSelected) indigo else onSurfaceVariant,
                animationSpec = tween(200), label = "tabColor"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onIntent(StatsIntent.SelectTab(tab)) }
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) indigo else Color.Transparent)
                )
            }
        }
    }
    HorizontalDivider(color = Color(0x1A4938FF), thickness = 1.dp)
    Spacer(Modifier.height(4.dp))
}

// ══════════════════════════════════════════
// TAB 1 — 기록 (캘린더)
// ══════════════════════════════════════════

@Composable
private fun RecordTab(state: StatsState, onIntent: (StatsIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        MonthlyCalendarCard(state = state, onIntent = onIntent)
        Spacer(Modifier.height(16.dp))
        MonthlySummaryStrip()
    }
}

@Composable
private fun MonthlyCalendarCard(state: StatsState, onIntent: (StatsIntent) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 월 이동 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "‹",
                    fontSize = 22.sp,
                    color = onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onIntent(StatsIntent.NavigateMonth("2026-02"))
                        }
                        .padding(8.dp)
                )
                Text(
                    text = "2026년 3월",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onIntent(StatsIntent.NavigateMonth("2026-04"))
                        }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 요일 헤더
            val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { wd ->
                    Text(
                        text = wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3월 1일 = 일요일(0)
            val startDayOfWeek = 0 // 0=일요일
            val daysInMonth = 31
            val totalCells = ((daysInMonth + startDayOfWeek + 6) / 7) * 7

            val cells = (0 until totalCells).toList()
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        val dayNum = cell - startDayOfWeek + 1
                        val isValidDay = dayNum in 1..daysInMonth
                        val dateStr = if (isValidDay) "2026-03-%02d".format(dayNum) else null
                        val rate = dateStr?.let { MockData.calendarData[it] }
                        val isToday = dayNum == 31
                        val isSelected = dateStr == state.selectedDate

                        CalendarDayCell(
                            dayNum = if (isValidDay) dayNum else null,
                            achievementRate = rate,
                            isToday = isToday,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (dateStr != null) onIntent(StatsIntent.SelectDate(dateStr))
                            }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // 컬러 범례
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarLegendItem(green, "달성")
                Spacer(Modifier.width(12.dp))
                CalendarLegendItem(amber, "부족")
                Spacer(Modifier.width(12.dp))
                CalendarLegendItem(red, "미달")
                Spacer(Modifier.width(12.dp))
                CalendarLegendItem(Color(0xFF4A4A6A), "기록없음")
            }
        }
    }

    // 선택된 날짜 상세 카드
    state.selectedDate?.let { date ->
        Spacer(Modifier.height(12.dp))
        SelectedDayDetailCard(date = date)
    }
}

@Composable
private fun CalendarDayCell(
    dayNum: Int?,
    achievementRate: Float?,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dotColor = when {
        achievementRate == null         -> Color(0xFF4A4A6A)
        achievementRate >= 1.0f         -> green
        achievementRate >= 0.7f         -> amber
        else                            -> red
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (isSelected) Modifier.background(indigo.copy(alpha = 0.25f)) else Modifier)
            .clickable(enabled = dayNum != null) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (dayNum != null) {
            Box(contentAlignment = Alignment.Center) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(2.dp, indigo, CircleShape)
                    )
                }
                if (isSelected && !isToday) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(indigo)
                    )
                }
                Text(
                    text = dayNum.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else onSurface
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        } else {
            Box(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = onSurfaceVariant)
    }
}

@Composable
private fun SelectedDayDetailCard(date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("3월 15일 목요일", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(label = "취침", value = "오후 11:24", modifier = Modifier.weight(1f))
                DetailItem(label = "기상", value = "오전 07:15", modifier = Modifier.weight(1f))
                DetailItem(label = "수면", value = "7h 51m", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            // 달성률 바
            Text("달성률 98%", fontSize = 12.sp, color = onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(surfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.98f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(green)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ChipInfo(label = "잠금 사용 ✅", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                ChipInfo(label = "수면 효율 87%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
    }
}

@Composable
private fun ChipInfo(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceHigh)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = onSurface, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MonthlySummaryStrip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStripItem(value = "7h 12m", label = "평균 수면")
            VerticalDivider()
            SummaryStripItem(value = "18/31일", label = "목표 달성")
            VerticalDivider()
            SummaryStripItem(value = "🔥 5일", label = "잠금 연속")
        }
    }
}

@Composable
private fun SummaryStripItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text(label, fontSize = 11.sp, color = onSurfaceVariant)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color(0x1A4938FF))
    )
}

// ══════════════════════════════════════════
// TAB 2 — 트렌드
// ══════════════════════════════════════════

@Composable
private fun TrendTab(state: StatsState, onIntent: (StatsIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // 기간 선택 Chip
        PeriodChipRow(selectedIndex = state.selectedPeriodIndex, onIntent = onIntent)
        Spacer(Modifier.height(16.dp))

        // 핵심 지표 3열
        KeyMetricsRow()
        Spacer(Modifier.height(16.dp))

        // 수면 추이 차트
        SleepTrendChartCard()
        Spacer(Modifier.height(16.dp))

        // 요일별 히트맵
        WeekdayHeatmapCard()
        Spacer(Modifier.height(16.dp))

        // 베스트/워스트
        BestWorstCard()
    }
}

@Composable
private fun PeriodChipRow(selectedIndex: Int, onIntent: (StatsIntent) -> Unit) {
    val periods = listOf("이번 주", "이번 달", "올해", "전체")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEachIndexed { idx, label ->
            val isSelected = selectedIndex == idx
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) indigo else surface)
                    .border(1.dp, if (isSelected) Color.Transparent else Color(0x334938FF), RoundedCornerShape(20.dp))
                    .clickable { onIntent(StatsIntent.SelectPeriod(idx)) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TrendMetricCard("평균 수면", "7h 12m", Modifier.weight(1f))
        TrendMetricCard("수면 효율", "86%", Modifier.weight(1f))
        TrendMetricCard("목표 달성", "18/31일", Modifier.weight(1f))
    }
}

@Composable
private fun TrendMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceHigh),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = indigo)
        }
    }
}

@Composable
private fun SleepTrendChartCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("수면 시간 추이", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(amber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("목표 8h", fontSize = 11.sp, color = amber, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))

            val maxVal = 10f
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val w = size.width
                val h = size.height
                val barW = w / (MockData.sleepBars.size * 2f)
                val gap   = barW

                // 목표선
                val targetY = h - (MockData.targetHours / maxVal) * h
                drawLine(
                    color = amber,
                    start  = Offset(0f, targetY),
                    end    = Offset(w, targetY),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    strokeWidth = 1.5.dp.toPx()
                )

                MockData.sleepBars.forEachIndexed { i, hrs ->
                    val barH = (hrs / maxVal) * h
                    val x = i * (barW + gap) + gap / 2
                    val y = h - barH
                    val rate = hrs / MockData.targetHours
                    val barColor = when {
                        rate >= 1.0f -> indigo
                        rate >= 0.7f -> amber
                        else         -> red
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MockData.days.forEach { day ->
                    Text(
                        day,
                        fontSize = 11.sp,
                        color = onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeatmapCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("요일별 수면 패턴", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(16.dp))

            val maxAvg = MockData.weekdayAvgHours.max()
            Row(modifier = Modifier.fillMaxWidth()) {
                MockData.days.forEachIndexed { i, day ->
                    val avg = MockData.weekdayAvgHours[i]
                    val intensity = avg / maxAvg
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "%dh".format(avg.toInt()),
                            fontSize = 10.sp,
                            color = onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(indigo.copy(alpha = intensity))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(day, fontSize = 11.sp, color = onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(indigoFaint)
                    .padding(10.dp)
            ) {
                Text(
                    "💡 금요일 밤에 평균 가장 늦게 잡니다",
                    fontSize = 12.sp,
                    color = indigo
                )
            }
        }
    }
}

@Composable
private fun BestWorstCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("이번 달 기록", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BestWorstItem(
                    emoji = "🏅",
                    title = "최고 수면",
                    date = "3월 22일",
                    value = "9h 10m",
                    valueColor = green,
                    modifier = Modifier.weight(1f)
                )
                BestWorstItem(
                    emoji = "😴",
                    title = "최저 수면",
                    date = "3월 19일",
                    value = "4h 55m",
                    valueColor = red,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BestWorstItem(
    emoji: String,
    title: String,
    date: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceHigh)
            .padding(12.dp)
    ) {
        Column {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = onSurfaceVariant)
            Text(date, fontSize = 12.sp, color = onSurface)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

// ══════════════════════════════════════════
// TAB 3 — 인사이트 (프리미엄)
// ══════════════════════════════════════════

@Composable
private fun InsightTab(isPremium: Boolean, onNavigateToSubscription: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        if (!isPremium) {
            // 비프리미엄: 블러 없이 업셀 카드만
            InsightUpsellCard(onNavigateToSubscription = onNavigateToSubscription)
        } else {
            SleepScoreCard()
            Spacer(Modifier.height(16.dp))
            SleepDebtCard()
            Spacer(Modifier.height(16.dp))
            LockStreakCard()
            Spacer(Modifier.height(16.dp))
            AIInsightCard()
        }
    }
}

@Composable
private fun PremiumBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(amber.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("👑 프리미엄", fontSize = 10.sp, color = amber, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SleepScoreCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("수면 점수", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 도넛 게이지
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 16.dp.toPx()
                        val dia = size.minDimension - stroke
                        val topLeft = Offset((size.width - dia) / 2, (size.height - dia) / 2)
                        // 배경 트랙
                        drawArc(
                            color = Color(0xFF2A2C4A),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(stroke),
                            size = Size(dia, dia), topLeft = topLeft
                        )
                        // 점수 아크 (82/100 = 295.2°)
                        drawArc(
                            brush = Brush.sweepGradient(listOf(indigo, Color(0xFF8B7FFF)), center = center),
                            startAngle = -90f, sweepAngle = 82f / 100f * 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            size = Size(dia, dia), topLeft = topLeft
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("82", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onSurface)
                        Text("점", fontSize = 12.sp, color = onSurfaceVariant)
                    }
                }

                Spacer(Modifier.width(20.dp))

                // 서브스코어
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("😊 좋음", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = green)
                    SubScoreBar("시간 충족", 0.88f)
                    SubScoreBar("취침 일관성", 0.79f)
                    SubScoreBar("기상 일관성", 0.82f)
                    SubScoreBar("잠금 준수", 0.90f)
                }
            }
        }
    }
}

@Composable
private fun SubScoreBar(label: String, ratio: Float) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, color = onSurfaceVariant, modifier = Modifier.width(72.dp))
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(surfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(indigo)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("%d%%".format((ratio * 100).toInt()), fontSize = 10.sp, color = indigo)
        }
    }
}

@Composable
private fun SleepDebtCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("이번 주 수면 부채", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("😴", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("2h 30m 부족", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = amber)
                    Text("목표 56h / 실제 53h 30m", fontSize = 12.sp, color = onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(surfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(amber)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("→ 오늘 약 30분 더 주무세요", fontSize = 13.sp, color = amber, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LockStreakCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("잠금 연속 달성", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
            Spacer(Modifier.height(16.dp))
            Text("🔥 5일", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Text("연속 5일 목표 취침 시간에 잠금을 켰어요!", fontSize = 13.sp, color = onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // 최근 7일 인디케이터
            val achievedDays = 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MockData.days.forEachIndexed { i, day ->
                    val isAchieved = i < achievedDays
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isAchieved) indigo else surfaceHigh)
                                .then(
                                    if (!isAchieved) Modifier.border(1.dp, Color(0xFF334), CircleShape)
                                    else Modifier
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(day, fontSize = 10.sp, color = onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AIInsightCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(indigoFaint)
            .border(1.dp, indigo.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        // 인디고 왼쪽 강조선
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                .background(indigo)
                .align(Alignment.CenterStart)
        )
        Column(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨ AI 수면 분석", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text("💤", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "주말에 평균 1시간 32분 더 자는 경향이 있어요. 사회적 시차증(Social Jet Lag)에 주의하세요! 😴",
                        fontSize = 14.sp,
                        color = onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = indigo.copy(alpha = 0.15f))
            Spacer(Modifier.height(10.dp))
            Text(
                "지난 30일 데이터 기반 · 매일 업데이트",
                fontSize = 11.sp,
                color = onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "다음 분석: 취침 일관성 트렌드 →",
                fontSize = 12.sp,
                color = indigo,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InsightUpsellCard(onNavigateToSubscription: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(surface, Color(0xFF1E1B3A)))
            )
            .border(1.dp, indigo.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👑", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "프리미엄에서만 볼 수 있어요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "수면 점수, 부채 분석, 잠금 스트릭,\nAI 인사이트를 지금 바로 확인하세요.",
                fontSize = 14.sp,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onNavigateToSubscription,
                colors = ButtonDefaults.buttonColors(containerColor = indigo),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("프리미엄 구독하기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ──────────────────────────────────────────
// Preview
// ──────────────────────────────────────────

@Preview
@Composable
fun StatsScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = Color(0xFF110F23)) {
            StatsScreenContent(
                contentPadding = PaddingValues(),
                state = StatsState(selectedTab = StatsTab.RECORD),
                isPremium = true,
                onIntent = {},
                onNavigateToSubscription = {}
            )
        }
    }
}

@Preview
@Composable
fun StatsScreenTrendPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = Color(0xFF110F23)) {
            StatsScreenContent(
                contentPadding = PaddingValues(),
                state = StatsState(selectedTab = StatsTab.TREND),
                isPremium = true,
                onIntent = {},
                onNavigateToSubscription = {}
            )
        }
    }
}

@Preview
@Composable
fun StatsScreenInsightPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = Color(0xFF110F23)) {
            StatsScreenContent(
                contentPadding = PaddingValues(),
                state = StatsState(selectedTab = StatsTab.INSIGHT),
                isPremium = true,
                onIntent = {},
                onNavigateToSubscription = {}
            )
        }
    }
}
