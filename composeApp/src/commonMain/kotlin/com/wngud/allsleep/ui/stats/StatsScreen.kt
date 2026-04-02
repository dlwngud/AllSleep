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
import androidx.compose.ui.draw.blur
import com.wngud.allsleep.ui.components.PremiumOverlay
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.*
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.formatTimestampToDate
import com.wngud.allsleep.domain.model.formatTimestampToTime

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
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .padding(contentPadding)
                .then(if (!isPremium) Modifier.blur(16.dp) else Modifier)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── 고정 헤더 ──────────────────
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(12.dp))
                    HeaderTitleRow(isPremium = isPremium, sleepScore = (state.avgEfficiency * 100).toInt())
                    Spacer(Modifier.height(12.dp))
                    TodaySummaryCard(record = state.latestRecord)
                    Spacer(Modifier.height(12.dp))
                }

                // ── 탭 바 ───────────────────────
                StatsTabBar(selectedTab = state.selectedTab, onIntent = onIntent)

                // ── 탭 콘텐츠 ──────────────────
                when (state.selectedTab) {
                    StatsTab.RECORD -> RecordTab(state = state, onIntent = onIntent)
                    StatsTab.TREND  -> TrendTab(state = state, onIntent = onIntent)
                    StatsTab.INSIGHT -> InsightTab(
                        state = state,
                        isPremium = isPremium,
                        onNavigateToSubscription = onNavigateToSubscription
                    )
                }
            }
        }

        // 구독 안했으면 오버레이 표시
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { }, // 뒷배경 터치 방지
                contentAlignment = Alignment.Center
            ) {
                PremiumOverlay(onSubscribeClick = onNavigateToSubscription)
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
private fun TodaySummaryCard(record: SleepRecord?) {
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
                .height(110.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(if (record != null) indigo else surfaceHigh)
                .align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (record != null) {
                // 좌측: 수면 시간
                Column(modifier = Modifier.weight(1f)) {
                    Text("어젯밤", fontSize = 12.sp, color = onSurfaceVariant)
                    Text(
                        formatMinutesToDuration(record.durationMinutes),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌙", fontSize = 12.sp)
                        Text(
                            " ${formatMillisToTime(record.bedtime)}  →  ☀️ ${formatMillisToTime(record.wakeTime)}",
                            fontSize = 12.sp,
                            color = onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "수면 효율 ${(record.sleepEfficiency).toInt()}%  |  목표 ${formatMinutesToDuration(record.targetMinutes)}",
                        fontSize = 11.sp,
                        color = onSurfaceVariant
                    )
                }
                // 우측: 달성률
                Column(horizontalAlignment = Alignment.End) {
                    val rate = record.achievementRate.toInt()
                    Text("✅ $rate%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (rate >= 95) green else amber)
                    Text("달성", fontSize = 11.sp, color = onSurfaceVariant)
                }
            } else {
                Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    Text("최근 수면 데이터가 없습니다", color = onSurfaceVariant, fontSize = 14.sp)
                }
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
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = indigo)
            }
        } else {
            Spacer(Modifier.height(16.dp))
            MonthlyCalendarCard(state = state, onIntent = onIntent)
            Spacer(Modifier.height(16.dp))
            MonthlySummaryStrip(state = state)
        }
    }
}

@Composable
private fun MonthlyCalendarCard(state: StatsState, onIntent: (StatsIntent) -> Unit) {
    // 현재 표시 중인 년-월 파싱
    val currentYearMonth = try {
        val parts = state.selectedYearMonth.split("-")
        parts[0].toInt() to parts[1].toInt()
    } catch (e: Exception) {
        2026 to 4 // 기본값
    }
    
    val year = currentYearMonth.first
    val month = currentYearMonth.second

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
                            val prevDate = LocalDate(year, month, 1).minus(1, DateTimeUnit.MONTH)
                            onIntent(StatsIntent.NavigateMonth("${prevDate.year}-${prevDate.monthNumber.toString().padStart(2, '0')}"))
                        }
                        .padding(8.dp)
                )
                Text(
                    text = "${year}년 ${month}월",
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
                            val nextDate = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH)
                            onIntent(StatsIntent.NavigateMonth("${nextDate.year}-${nextDate.monthNumber.toString().padStart(2, '0')}"))
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

            // 해당 월의 시작 요일 및 총 일수 계산
            val firstDayOfMonth = LocalDate(year, month, 1)
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal % 7 // ISO ordinal(Mon=1~Sun=7) -> (Sun=0~Sat=6)
            // 주의: dayOfWeek.ordinal은 월=0일 수 있으므로 조정 필요. 
            // kotlinx-datetime DayOfWeek enum: MONDAY(0), TUESDAY(1), ..., SUNDAY(6)
            // 일요일=0으로 맞추기 위해: (dayOfWeek.ordinal + 1) % 7
            val startPadding = (firstDayOfMonth.dayOfWeek.ordinal + 1) % 7
            
            val daysInMonth = try {
                firstDayOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
            } catch (e: Exception) { 30 }

            val totalCells = ((daysInMonth + startPadding + 6) / 7) * 7

            val cells = (0 until totalCells).toList()
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        val dayNum = cell - startPadding + 1
                        val isValidDay = dayNum in 1..daysInMonth
                        val dateStr = if (isValidDay) "${year}-${month.toString().padStart(2, '0')}-${dayNum.toString().padStart(2, '0')}" else null
                        val record = dateStr?.let { state.records[it] }
                        
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        val isToday = isValidDay && today.year == year && today.monthNumber == month && today.dayOfMonth == dayNum
                        val isSelected = dateStr == state.selectedDate

                        CalendarDayCell(
                            dayNum = if (isValidDay) dayNum else null,
                            achievementRate = record?.achievementRate,
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
        state.records[date]?.let { record ->
            Spacer(Modifier.height(12.dp))
            SelectedDayDetailCard(record = record)
        } ?: run {
            Spacer(Modifier.height(12.dp))
            EmptyDayDetailCard(date = date)
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
private fun CalendarDayCell(
    dayNum: Int?,
    achievementRate: Float?,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dotColor = when {
        achievementRate == null || achievementRate <= 0f -> Color(0xFF4A4A6A)
        achievementRate >= 0.95f         -> green
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
private fun SelectedDayDetailCard(record: SleepRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateParts = record.date.split("-")
            val formattedDate = "${dateParts[1].toInt()}월 ${dateParts[2].toInt()}일 수면 기록"
            
            Text(formattedDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(label = "취침", value = formatMillisToTime(record.bedtime), modifier = Modifier.weight(1f))
                DetailItem(label = "기상", value = formatMillisToTime(record.wakeTime), modifier = Modifier.weight(1f))
                DetailItem(label = "수면", value = formatMinutesToDuration(record.durationMinutes), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            
            // 달성률 바
            val ratePct = (record.achievementRate * 100).toInt()
            Text("목표 달성률 $ratePct%", fontSize = 12.sp, color = onSurfaceVariant)
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
                        .fillMaxWidth(record.achievementRate.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (record.achievementRate >= 0.95f) green else amber)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ChipInfo(label = if (record.isLockUsed) "잠금 사용 ✅" else "잠금 미사용", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                ChipInfo(label = "수면 효율 ${(record.sleepEfficiency * 100).toInt()}%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyDayDetailCard(date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("기록된 수면 데이터가 없습니다.", color = onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

private fun formatMillisToTime(millis: Long): String {
    if (millis <= 0) return "--:--"
    // 간단한 시간 포맷팅 (KMP 유틸 사용 권장)
    return com.wngud.allsleep.domain.model.formatTimestampToTime(millis)
}

private fun formatMinutesToDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m}m"
}

@Composable
private fun MonthlySummaryStrip(state: StatsState) {
    val totalRecords = state.records.size
    val avgDuration = state.avgSleepMinutes
    val successCount = state.achievementCount

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
            SummaryStripItem(value = formatMinutesToDuration(avgDuration), label = "평균 수면")
            VerticalDivider(color = surfaceHigh, modifier = Modifier.height(30.dp).width(1.dp))
            SummaryStripItem(value = "$successCount/${totalRecords}일", label = "목표 달성")
            VerticalDivider(color = surfaceHigh, modifier = Modifier.height(30.dp).width(1.dp))
            SummaryStripItem(value = "🔥 ${state.streakDays}일", label = "잠금 연속")
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
private fun VerticalDivider(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color)
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
        KeyMetricsRow(state)
        Spacer(Modifier.height(16.dp))

        // 수면 추이 차트
        SleepTrendChartCard(state)
        Spacer(Modifier.height(16.dp))

        // 요일별 히트맵
        WeekdayHeatmapCard(state)
        Spacer(Modifier.height(16.dp))

        // 베스트/워스트
        BestWorstCard(state)
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
private fun KeyMetricsRow(state: StatsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TrendMetricCard("평균 수면", formatMinutesToDuration(state.avgSleepMinutes), Modifier.weight(1f))
        TrendMetricCard("수면 효율", "${(state.avgEfficiency * 100).toInt()}%", Modifier.weight(1f))
        TrendMetricCard("목표 달성", "${state.achievementCount}일", Modifier.weight(1f))
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
private fun SleepTrendChartCard(state: StatsState) {
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

            val maxVal = maxOf(state.weeklyTrend.maxOrNull() ?: 10f, 10f)
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val w = size.width
                val h = size.height
                val barCount = state.weeklyTrend.size.coerceAtLeast(1)
                val barW = w / (barCount * 2f)
                val gap = barW

                // 목표선 (8시간 고정 표시 - 실제 앱에서는 유저 설정값 연동)
                val targetHours = 8f
                val targetY = h - (targetHours / maxVal) * h
                drawLine(
                    color = amber,
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    strokeWidth = 1.5.dp.toPx()
                )

                state.weeklyTrend.forEachIndexed { i, hrs ->
                    val barH = (hrs / maxVal) * h
                    val x = i * (barW + gap) + gap / 2
                    val y = h - barH
                    val rate = hrs / targetHours
                    val barColor = when {
                        rate >= 0.95f -> indigo
                        rate >= 0.7f -> amber
                        hrs > 0 -> red
                        else -> surfaceHigh
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH.coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                state.trendDates.forEach { day ->
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
private fun WeekdayHeatmapCard(state: StatsState) {
    val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("요일별 수면 패턴", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(16.dp))

            val heatmapData = state.heatmapData.ifEmpty { List(7) { 0f } }
            val maxAvg = maxOf(heatmapData.maxOrNull() ?: 1f, 1f)
            
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEachIndexed { i, day ->
                    val avg = heatmapData.getOrElse(i) { 0f }
                    val intensity = (avg / maxAvg).coerceIn(0.05f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (avg > 0) "${avg.toInt()}h" else "-",
                            fontSize = 10.sp,
                            color = onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (avg > 0) indigo.copy(alpha = intensity) else surfaceHigh)
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
                val bestDayIdx = heatmapData.indexOfMax()
                val bestDayName = if (bestDayIdx != -1 && heatmapData[bestDayIdx] > 0) dayNames[bestDayIdx] else "요일"
                Text(
                    "💡 ${bestDayName}요일에 평균 가장 많이 주무시네요",
                    fontSize = 12.sp,
                    color = indigo
                )
            }
        }
    }
}

private fun List<Float>.indexOfMax(): Int {
    if (isEmpty()) return -1
    var maxIndex = 0
    for (i in indices) {
        if (this[i] > this[maxIndex]) maxIndex = i
    }
    return maxIndex
}

@Composable
private fun BestWorstCard(state: StatsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("선택 기간 성과", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.bestRecord != null) {
                    BestWorstItem(
                        emoji = "🏅",
                        title = "최고 성과",
                        date = formatDayLabel(state.bestRecord.date),
                        value = formatMinutesToDuration(state.bestRecord.durationMinutes),
                        valueColor = green,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.worstRecord != null) {
                    BestWorstItem(
                        emoji = "😴",
                        title = "최저 성과",
                        date = formatDayLabel(state.worstRecord.date),
                        value = formatMinutesToDuration(state.worstRecord.durationMinutes),
                        valueColor = red,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.bestRecord == null && state.worstRecord == null) {
                    Text("성과를 분석할 기록이 부족합니다.", color = onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatDayLabel(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        "${date.monthNumber}월 ${date.dayOfMonth}일"
    } catch (e: Exception) { dateStr }
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
private fun InsightTab(state: StatsState, isPremium: Boolean, onNavigateToSubscription: () -> Unit) {
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
            SleepScoreCard(score = (state.avgEfficiency * 100).toInt())
            Spacer(Modifier.height(16.dp))
            SleepDebtCard(state = state)
            Spacer(Modifier.height(16.dp))
            LockStreakCard(streak = state.streakDays)
            Spacer(Modifier.height(16.dp))
            AIInsightCard(state = state)
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
private fun SleepScoreCard(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("수면 종합 점수", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
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
                        // 점수 아크
                        drawArc(
                            brush = Brush.sweepGradient(listOf(indigo, Color(0xFF8B7FFF)), center = center),
                            startAngle = -90f, sweepAngle = score / 100f * 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            size = Size(dia, dia), topLeft = topLeft
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onSurface)
                        Text("점", fontSize = 12.sp, color = onSurfaceVariant)
                    }
                }

                Spacer(Modifier.width(20.dp))

                // 서브스코어 (평균적 수식 기반 가상 스코어링)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusText = when {
                        score >= 90 -> "😊 매우 좋음"
                        score >= 80 -> "🙂 좋음"
                        else        -> "🤔 관리 필요"
                    }
                    Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (score >= 80) green else amber)
                    SubScoreBar("수면 시간", (score / 100f).coerceIn(0f, 1f))
                    SubScoreBar("일관성", 0.85f)
                    SubScoreBar("잠금 준수", if (score > 50) 0.9f else 0.4f)
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
private fun SleepDebtCard(state: StatsState) {
    val targetTotal = 56 * 60f // 주간 목표 56시간(8h * 7)
    val actualTotal = state.avgSleepMinutes * 7f
    val debt = (targetTotal - actualTotal).coerceAtLeast(0f)
    val debtH = (debt / 60).toInt()
    val debtM = (debt % 60).toInt()

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
                Text(if (debt > 0) "😴" else "✅", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    if (debt > 0) {
                        Text("${debtH}h ${debtM}m 부족", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = amber)
                        Text("주간 목표 56h / 실제 ${formatMinutesToDuration(actualTotal.toInt())}", fontSize = 12.sp, color = onSurfaceVariant)
                    } else {
                        Text("부채 없음", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = green)
                        Text("수집된 데이터를 기반으로 목표를 충족했습니다.", fontSize = 12.sp, color = onSurfaceVariant)
                    }
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
                val ratio = if (targetTotal > 0) (actualTotal / targetTotal).coerceIn(0f, 1f) else 1f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (ratio >= 1f) green else amber)
                )
            }
            if (debt > 0) {
                Spacer(Modifier.height(10.dp))
                Text("→ 오늘 약 ${debtM + 10}분 더 주무세요", fontSize = 13.sp, color = amber, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LockStreakCard(streak: Int) {
    val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")
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
            Text("🔥 ${streak}일", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (streak > 0) amber else onSurface)
            Text(if (streak > 0) "연속 ${streak}일째 잠금을 켜고 숙면을 취했어요!" else "기록을 쌓아 연속 잠금에 도전하세요!", fontSize = 13.sp, color = onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // 최근 7일 시각화 (간략화)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayNames.forEachIndexed { i, day ->
                    val isAchieved = i < (streak % 8)
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
                                    if (!isAchieved) Modifier.border(1.dp, Color(0xFF333344), CircleShape)
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
private fun AIInsightCard(state: StatsState) {
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
                val message = when {
                    state.records.isEmpty() -> "아직 분석할 데이터가 부족해요. 3일 이상 기록해 주세요!"
                    state.avgEfficiency < 0.8f -> "수면 효율이 조금 낮아요. 취침 전 스마트폰 사용을 줄여보시는 건 어떨까요? 😴"
                    state.streakDays >= 3 -> "잠금 모드를 아주 잘 사용하고 계시네요! 숙면의 비결입니다. 🔥"
                    else -> "일정한 시간에 잠드시는 편이네요. 규칙적인 생활은 건강의 기본입니다. ✨"
                }
                Column {
                    Text(
                        message,
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
                "선택 기간 ${state.records.size}개 데이터 기반 · 매일 업데이트",
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
