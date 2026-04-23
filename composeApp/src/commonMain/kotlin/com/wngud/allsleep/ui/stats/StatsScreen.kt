package com.wngud.allsleep.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.formatTimestampToTime
import com.wngud.allsleep.ui.components.PremiumOverlay
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max

private val Background = Color(0xFF0B0C10)
private val Surface = Color(0xFF151A24)
private val SurfaceHigh = Color(0xFF1E2633)
private val SurfaceSoft = Color(0xFF222B3A)
private val Primary = Color(0xFF4938FF)
private val SleepBlue = Color(0xFF3BA5F5)
private val OnSurface = Color(0xFFF6F5F8)
private val OnSurfaceMuted = Color(0xFFC7C4DA)
private val Green = Color(0xFF35D07F)
private val Amber = Color(0xFFF5A524)
private val Red = Color(0xFFEF4444)

@Composable
fun StatsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateToSubscription: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
    globalViewModel: GlobalSleepViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val globalState by globalViewModel.state.collectAsState()

    StatsScreenContent(
        contentPadding = contentPadding,
        state = state,
        isPremium = globalState.isPremium,
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
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Background,
            modifier = Modifier
                .padding(contentPadding)
                .then(if (!isPremium) Modifier.blur(16.dp) else Modifier)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatsHeader()
                    StatsTabs(selectedTab = state.selectedTab, onIntent = onIntent)

                    when (state.selectedTab) {
                        StatsTab.SUMMARY -> SummaryTab(state = state)
                        StatsTab.RECORD -> RecordTab(state = state, onIntent = onIntent)
                    }

                    Spacer(Modifier.height(20.dp))
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Background.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SleepBlue)
                    }
                }
            }
        }

        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                PremiumOverlay(onSubscribeClick = onNavigateToSubscription)
            }
        }
    }
}

@Composable
private fun StatsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("수면 분석", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        Text("내 수면 리듬을 빠르게 확인하고 다음 행동을 정해요", fontSize = 13.sp, color = OnSurfaceMuted)
    }
}

@Composable
private fun StatsTabs(selectedTab: StatsTab, onIntent: (StatsIntent) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Surface,
        contentColor = OnSurface,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)),
        indicator = {},
        divider = {}
    ) {
        StatsTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Tab(
                selected = selected,
                onClick = { onIntent(StatsIntent.SelectTab(tab)) },
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Primary else Color.Transparent),
                text = {
                    Text(
                        text = if (tab == StatsTab.SUMMARY) "요약" else "기록",
                        color = if (selected) Color.White else OnSurfaceMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun SummaryTab(
    state: StatsState
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (state.trendRecords.isEmpty()) {
            EmptyStatsCard()
        }

        StatsScoreHero(state = state)
        WeeklyTrendCard(state = state)
        SleepDebtCard(state = state)
        StatsMetricGrid(state = state)
        PremiumAnalysisSection(state = state)
        AiInsightCard(message = state.aiMessage)
    }
}

@Composable
private fun StatsScoreHero(state: StatsState) {
    val latest = state.latestRecord
    StatsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ScoreRing(score = state.sleepScore)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.scoreLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Text(
                    if (latest != null) "어젯밤 ${formatMinutes(latest.durationMinutes)} · 효율 ${latest.sleepEfficiency.toInt()}%"
                    else "수면 기록이 쌓이면 점수와 리듬을 보여드려요",
                    fontSize = 13.sp,
                    color = OnSurfaceMuted,
                    lineHeight = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip("목표", formatMinutes(state.currentTargetMinutes), SleepBlue)
                    SummaryChip("부채", formatMinutes(state.sleepDebtMinutes), sleepDebtColor(state.sleepDebtLevel))
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            drawCircle(color = SurfaceSoft, style = Stroke(width = stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(SleepBlue, Primary, Green, SleepBlue)),
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Text("점", fontSize = 12.sp, color = OnSurfaceMuted)
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeeklyTrendCard(state: StatsState) {
    StatsCard {
        SectionTitle("최근 7일 흐름", "평균 ${formatMinutes(state.weeklyAverageMinutes)}")
        Spacer(Modifier.height(18.dp))
        WeeklyBarChart(
            bars = state.weeklyBars,
            labels = state.weeklyLabels,
            targetHours = state.currentTargetMinutes / 60f
        )
    }
}

@Composable
private fun WeeklyBarChart(bars: List<Float>, labels: List<String>, targetHours: Float) {
    val maxValue = max((bars.maxOrNull() ?: 0f), targetHours).coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val spacing = 10.dp.toPx()
            val barWidth = (size.width - spacing * (bars.size - 1)) / bars.size.coerceAtLeast(1)
            val chartHeight = size.height - 8.dp.toPx()
            val targetY = chartHeight - (targetHours / maxValue) * chartHeight

            drawLine(
                color = Amber.copy(alpha = 0.65f),
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            bars.forEachIndexed { index, value ->
                val left = index * (barWidth + spacing)
                val barHeight = (value / maxValue) * chartHeight
                val top = chartHeight - barHeight
                val color = when {
                    value == 0f -> SurfaceSoft
                    value >= targetHours -> Green
                    value >= targetHours * 0.8f -> SleepBlue
                    else -> Amber
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(8.dp.toPx())),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(label, fontSize = 11.sp, color = OnSurfaceMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SleepDebtCard(state: StatsState) {
    val color = sleepDebtColor(state.sleepDebtLevel)
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("수면 부채", fontSize = 13.sp, color = OnSurfaceMuted)
                Text(
                    if (state.sleepDebtMinutes == 0) "부족 없음" else "${formatMinutes(state.sleepDebtMinutes)} 부족",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    when (state.sleepDebtLevel) {
                        SleepDebtLevel.GOOD -> "최근 기록은 목표 수면과 잘 맞고 있어요."
                        SleepDebtLevel.CAUTION -> "이번 주는 조금 부족해요. 하루 20분만 보충해도 좋아요."
                        SleepDebtLevel.WARNING -> "회복 시간이 필요해요. 오늘은 평소보다 일찍 시작해보세요."
                    },
                    fontSize = 13.sp,
                    color = OnSurfaceMuted,
                    lineHeight = 18.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.sleepDebtLevel == SleepDebtLevel.GOOD) "OK" else "!", color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatsMetricGrid(state: StatsState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("평균 수면", formatMinutes(state.weeklyAverageMinutes), modifier = Modifier.weight(1f))
        MetricCard("목표 달성", "${state.achievementCount}일", modifier = Modifier.weight(1f))
        MetricCard("잠금 스트릭", "${state.streakDays}일", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontSize = 11.sp, color = OnSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1)
    }
}

@Composable
private fun PremiumAnalysisSection(
    state: StatsState
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("패턴 분석", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface, modifier = Modifier.weight(1f))
        }

        StatsCard {
            Text(state.premiumSummary, fontSize = 14.sp, color = OnSurface, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            PremiumInsightRow(
                title = "지난주 대비",
                value = formatSignedMinutes(state.weeklyDeltaMinutes),
                description = "최근 7일 평균 수면 변화"
            )
            PremiumInsightRow(
                title = "취침 일관성",
                value = "±${formatMinutes(state.bedtimeConsistencyMinutes)}",
                description = "취침 시간이 평균에서 벗어나는 정도"
            )
            PremiumInsightRow(
                title = "주말 밀림",
                value = formatSignedMinutes(state.weekendDriftMinutes),
                description = "평일 대비 주말 취침 시간 변화"
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            RecordHighlightCard(
                label = "베스트",
                record = state.bestRecord,
                color = Green,
                modifier = Modifier.weight(1f)
            )
            RecordHighlightCard(
                label = "주의",
                record = state.worstRecord,
                color = Amber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PremiumInsightRow(title: String, value: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.Bold)
            Text(description, fontSize = 12.sp, color = OnSurfaceMuted, lineHeight = 16.sp)
        }
        Text(value, fontSize = 17.sp, color = SleepBlue, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun RecordHighlightCard(
    label: String,
    record: SleepRecord?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        Text(record?.date ?: "기록 없음", fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            record?.let { "${formatMinutes(it.durationMinutes)} · 효율 ${it.sleepEfficiency.toInt()}%" } ?: "데이터가 필요해요",
            fontSize = 12.sp,
            color = OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AiInsightCard(
    message: String
) {
    StatsCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI 인사이트", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Text(
                message,
                fontSize = 13.sp,
                color = OnSurfaceMuted,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun RecordTab(state: StatsState, onIntent: (StatsIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MonthHeader(
            yearMonth = state.selectedYearMonth,
            onPrevious = { onIntent(StatsIntent.NavigateMonth(shiftMonth(state.selectedYearMonth, -1))) },
            onNext = { onIntent(StatsIntent.NavigateMonth(shiftMonth(state.selectedYearMonth, 1))) }
        )
        CalendarCard(
            yearMonth = state.selectedYearMonth,
            records = state.records,
            selectedDate = state.selectedDate,
            onDateClick = { onIntent(StatsIntent.SelectDate(it)) }
        )
        SelectedRecordDetailCard(record = state.selectedRecord)
    }
}

@Composable
private fun MonthHeader(yearMonth: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPrevious, border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)) {
            Text("이전", color = OnSurface)
        }
        Text(
            text = yearMonth,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        OutlinedButton(onClick = onNext, border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)) {
            Text("다음", color = OnSurface)
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: String,
    records: Map<String, SleepRecord>,
    selectedDate: String?,
    onDateClick: (String) -> Unit
) {
    StatsCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            listOf("월", "화", "수", "목", "금", "토", "일").forEach {
                Text(it, fontSize = 12.sp, color = OnSurfaceMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        val cells = calendarCells(yearMonth)
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        yearMonth = yearMonth,
                        record = day?.let { records["$yearMonth-${it.toString().padStart(2, '0')}"] },
                        selectedDate = selectedDate,
                        onClick = onDateClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int?,
    yearMonth: String,
    record: SleepRecord?,
    selectedDate: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val date = day?.let { "$yearMonth-${it.toString().padStart(2, '0')}" }
    val isSelected = date != null && date == selectedDate
    val recordColor = record?.let {
        when {
            it.achievementRate >= 95f -> Green
            it.achievementRate >= 75f -> SleepBlue
            else -> Amber
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Primary else SurfaceHigh)
            .clickable(enabled = date != null) { date?.let(onClick) },
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(day.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else OnSurface)
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(recordColor ?: Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun SelectedRecordDetailCard(record: SleepRecord?) {
    StatsCard {
        if (record == null) {
            Text("날짜를 선택하면 상세 기록을 볼 수 있어요.", fontSize = 14.sp, color = OnSurfaceMuted)
            return@StatsCard
        }
        SectionTitle(record.date, formatMinutes(record.durationMinutes))
        Spacer(Modifier.height(14.dp))
        DetailRow("취침", formatTimestampToTime(record.bedtime))
        DetailRow("기상", formatTimestampToTime(record.wakeTime))
        DetailRow("수면 효율", "${record.sleepEfficiency.toInt()}%")
        DetailRow("목표 대비", "${record.achievementRate.toInt()}%")
        DetailRow("잠금 모드", if (record.isLockUsed) "사용" else "미사용")
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = OnSurfaceMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = OnSurface, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EmptyStatsCard() {
    StatsCard {
        Text("아직 분석할 기록이 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        Spacer(Modifier.height(8.dp))
        Text("수면 모드를 한 번 사용하면 어젯밤 요약, 최근 7일 흐름, 수면 부채를 보여드릴게요.", fontSize = 13.sp, color = OnSurfaceMuted, lineHeight = 19.sp)
    }
}

@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface, modifier = Modifier.weight(1f))
        Text(trailing, fontSize = 13.sp, color = OnSurfaceMuted, fontWeight = FontWeight.SemiBold)
    }
}

private fun sleepDebtColor(level: SleepDebtLevel): Color = when (level) {
    SleepDebtLevel.GOOD -> Green
    SleepDebtLevel.CAUTION -> Amber
    SleepDebtLevel.WARNING -> Red
}

private fun formatMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun formatSignedMinutes(minutes: Int): String {
    return when {
        minutes > 0 -> "+${formatMinutes(minutes)}"
        minutes < 0 -> "-${formatMinutes(-minutes)}"
        else -> "변화 없음"
    }
}

private fun shiftMonth(yearMonth: String, delta: Int): String {
    val parts = yearMonth.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).year
    val month = parts.getOrNull(1)?.toIntOrNull() ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).monthNumber
    val total = year * 12 + (month - 1) + delta
    val nextYear = total / 12
    val nextMonth = total % 12 + 1
    return "$nextYear-${nextMonth.toString().padStart(2, '0')}"
}

private fun calendarCells(yearMonth: String): List<Int?> {
    val parts = yearMonth.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return emptyList()
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return emptyList()
    val firstDay = LocalDate(year, month, 1)
    val leadingEmpty = firstDay.dayOfWeek.ordinal
    val days = daysInMonth(year, month)
    val cells = MutableList<Int?>(leadingEmpty) { null }
    cells.addAll((1..days).toList())
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

@Preview
@Composable
fun StatsScreenPreview() {
    MaterialTheme {
        Surface(color = Background) {
            StatsScreenContent(
                state = StatsState(
                    selectedYearMonth = "2026-04",
                    sleepScore = 82,
                    scoreLabel = "이번 주 리듬이 안정적이에요",
                    weeklyBars = listOf(7.2f, 6.1f, 7.8f, 5.9f, 6.8f, 8.2f, 7.1f),
                    weeklyLabels = listOf("월", "화", "수", "목", "금", "토", "일"),
                    weeklyAverageMinutes = 402,
                    sleepDebtMinutes = 130,
                    sleepDebtLevel = SleepDebtLevel.CAUTION,
                    achievementCount = 4,
                    streakDays = 3,
                    aiMessage = "주말 취침 시간이 평일보다 늦어지고 있어요. 이번 주말은 30분만 앞당겨 보세요.",
                    currentTargetMinutes = 480
                ),
                isPremium = true,
                onIntent = {},
                onNavigateToSubscription = {}
            )
        }
    }
}
