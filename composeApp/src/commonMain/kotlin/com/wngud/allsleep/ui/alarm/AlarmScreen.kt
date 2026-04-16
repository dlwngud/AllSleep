package com.wngud.allsleep.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import com.wngud.allsleep.ui.components.TimePickerDialog
import org.koin.compose.viewmodel.koinViewModel
import com.wngud.allsleep.ui.theme.*

/**
 * 알람 탭 화면 (평일/주말 고정 루틴 구조)
 */
@Composable
fun AlarmScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: AlarmViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var showSleepTimePicker by remember { mutableStateOf(false) }
    var showWakeTimePicker by remember { mutableStateOf(false) }

    val currentSleep = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdaySleep else state.weekendSleep
    val currentWake = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdayWake else state.weekendWake

    AlarmScreenContent(
        contentPadding = contentPadding,
        state = state,
        onIntent = viewModel::handleIntent,
        onSleepTimeClick = { showSleepTimePicker = true },
        onWakeTimeClick = { showWakeTimePicker = true }
    )

    if (showSleepTimePicker) {
        TimePickerDialog(
            title = "취침 시간 설정",
            initialHour = currentSleep.hour,
            initialMinute = currentSleep.minute,
            onConfirm = { h, m ->
                val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.handleIntent(AlarmIntent.UpdateSleepTime(time))
                showSleepTimePicker = false
            },
            onDismiss = { showSleepTimePicker = false }
        )
    }

    if (showWakeTimePicker) {
        TimePickerDialog(
            title = "기상 시간 설정",
            initialHour = currentWake.hour,
            initialMinute = currentWake.minute,
            onConfirm = { h, m ->
                val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.handleIntent(AlarmIntent.UpdateWakeTime(time))
                showWakeTimePicker = false
            },
            onDismiss = { showWakeTimePicker = false }
        )
    }
}

@Composable
fun AlarmScreenContent(
    contentPadding: PaddingValues,
    state: AlarmState,
    onIntent: (AlarmIntent) -> Unit,
    onSleepTimeClick: () -> Unit,
    onWakeTimeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. 상단 루틴 요약 및 설명
        RoutineHeader(state)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 평일/주말 선택 탭
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            AlarmTab.values().forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { onIntent(AlarmIntent.SelectTab(tab)) },
                    text = {
                        Text(
                            text = if (tab == AlarmTab.WEEKDAY) "평일 (월-금)" else "주말 (토-일)",
                            fontSize = 15.sp,
                            fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.selectedTab == tab) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val currentSleep = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdaySleep else state.weekendSleep
        val currentWake = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdayWake else state.weekendWake

        // 3. 취침 시간 카드
        AlarmTimeCard(
            icon = "🌙",
            label = "취침 시간",
            routine = currentSleep,
            isAm = false,
            description = if (state.selectedTab == AlarmTab.WEEKDAY) "일 ~ 목 밤 적용" else "금 ~ 토 밤 적용",
            onToggle = { onIntent(AlarmIntent.ToggleSleepAlarm(it)) },
            onTimeClick = onSleepTimeClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 기상 시간 카드
        AlarmTimeCard(
            icon = "☀️",
            label = "기상 시간",
            routine = currentWake,
            isAm = true,
            description = if (state.selectedTab == AlarmTab.WEEKDAY) "월 ~ 금 아침 적용" else "토 ~ 일 아침 적용",
            onToggle = { onIntent(AlarmIntent.ToggleWakeAlarm(it)) },
            onTimeClick = onWakeTimeClick
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun RoutineHeader(state: AlarmState) {
    val currentSleep = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdaySleep else state.weekendSleep
    val currentWake = if (state.selectedTab == AlarmTab.WEEKDAY) state.weekdayWake else state.weekendWake

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = if (state.selectedTab == AlarmTab.WEEKDAY) "🏃‍♂️ 평일 수면 루틴" else "🛋️ 주말 수면 루틴",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "기상일 요일을 기준으로 루틴이 자동 전환됩니다.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "현재 선택된 스케줄",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                val sleepTime = "${currentSleep.hour.toString().padStart(2, '0')}:${currentSleep.minute.toString().padStart(2, '0')}"
                val wakeTime = "${currentWake.hour.toString().padStart(2, '0')}:${currentWake.minute.toString().padStart(2, '0')}"
                Text(
                    text = "$sleepTime ~ 기상 $wakeTime",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(text = if (state.selectedTab == AlarmTab.WEEKDAY) "📅" else "🏖️", fontSize = 28.sp)
        }
    }
}

@Composable
private fun AlarmTimeCard(
    icon: String,
    label: String,
    routine: AlarmRoutine,
    isAm: Boolean,
    description: String,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = routine.isEnabled, onClick = onTimeClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = icon, fontSize = 24.sp)
                Column {
                    Text(
                        text = label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            Switch(
                checked = routine.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ClockCircle(
            hour = routine.hour,
            minute = routine.minute,
            isAm = isAm,
            isEnabled = routine.isEnabled
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "시간을 눌러 수정",
            fontSize = 13.sp,
            color = if (routine.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun ClockCircle(
    hour: Int,
    minute: Int,
    isAm: Boolean,
    isEnabled: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // 배경 원
            drawCircle(
                color = surfaceColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 진행 호
            val activeColor = if (isEnabled) primaryColor else disabledColor
            val hourAngle = ((hour % 12) / 12f + minute / 720f) * 360f
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = hourAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = if (isAm) "AM" else "PM",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview
@Composable
fun AlarmScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            AlarmScreenContent(
                contentPadding = PaddingValues(),
                state = AlarmState(),
                onIntent = {},
                onSleepTimeClick = {},
                onWakeTimeClick = {}
            )
        }
    }
}
