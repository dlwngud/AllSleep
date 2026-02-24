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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import com.wngud.allsleep.ui.theme.*

/**
 * 알람 탭 화면
 *
 * Stitch 디자인 기준:
 * - 상단: 수면 예약 시간 요약 (취침 ~ 기상)
 * - 취침 시간 카드: 원형 시계 + 요일 선택 + ON/OFF
 * - 기상 시간 카드: 원형 시계 + 요일 선택 + ON/OFF
 * - 추가 알람 목록
 * - 새로운 알람 추가 버튼
 */
@Composable
fun AlarmScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: AlarmViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    AlarmScreenContent(
        contentPadding = contentPadding,
        state = state,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun AlarmScreenContent(
    contentPadding: PaddingValues,
    state: AlarmState,
    onIntent: (AlarmIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // 헤더
        AlarmHeader(state)

        // 취침 시간 카드
        AlarmTimeCard(
            icon = "🌙",
            label = "취침 시간",
            alarm = state.sleepAlarm,
            isAm = false,
            onToggle = { onIntent(AlarmIntent.ToggleSleepAlarm(it)) },
            onDayToggle = { onIntent(AlarmIntent.ToggleSleepDay(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 기상 시간 카드
        AlarmTimeCard(
            icon = "☀️",
            label = "기상 시간",
            alarm = state.wakeAlarm,
            isAm = true,
            onToggle = { onIntent(AlarmIntent.ToggleWakeAlarm(it)) },
            onDayToggle = { onIntent(AlarmIntent.ToggleWakeDay(it)) }
        )

        // 추가 알람 목록
        if (state.extraAlarms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            state.extraAlarms.forEach { alarm ->
                ExtraAlarmItem(
                    alarm = alarm,
                    onToggle = { onIntent(AlarmIntent.ToggleExtraAlarm(alarm.id)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 새로운 알람 추가 버튼
        Button(
            onClick = { onIntent(AlarmIntent.AddAlarm) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "+ 새로운 알람 추가",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AlarmHeader(state: AlarmState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "수면 예약 시간",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            val sleepTime = "%02d:%02d".format(state.sleepAlarm.hour, state.sleepAlarm.minute)
            val wakeTime = "%02d:%02d".format(state.wakeAlarm.hour, state.wakeAlarm.minute)
            Text(
                text = "$sleepTime ~ 기상 $wakeTime",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(text = "🌙", fontSize = 24.sp)
    }
}

@Composable
private fun AlarmTimeCard(
    icon: String,
    label: String,
    alarm: AlarmItem,
    isAm: Boolean,
    onToggle: (Boolean) -> Unit,
    onDayToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 헤더 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 원형 시계
        ClockCircle(
            hour = alarm.hour,
            minute = alarm.minute,
            isAm = isAm,
            isEnabled = alarm.isEnabled
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 요일 선택
        DaySelector(
            selectedDays = alarm.selectedDays,
            isEnabled = alarm.isEnabled,
            onDayToggle = onDayToggle
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
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // 배경 원
            drawCircle(
                color = surfaceColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 진행 호 (시침 위치 기준)
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
                text = "%02d:%02d".format(hour, minute),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = if (isAm) "AM" else "PM",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<Int>,
    isEnabled: Boolean,
    onDayToggle: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DAYS.forEachIndexed { index, day ->
            val isSelected = index in selectedDays
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isEnabled -> MaterialTheme.colorScheme.surface
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .clickable(enabled = isEnabled) { onDayToggle(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ExtraAlarmItem(
    alarm: AlarmItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "%02d:%02d".format(alarm.hour, alarm.minute),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = alarm.label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
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
                onIntent = {}
            )
        }
    }
}
