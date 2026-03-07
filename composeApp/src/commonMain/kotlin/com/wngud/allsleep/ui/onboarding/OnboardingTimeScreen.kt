package com.wngud.allsleep.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.components.WheelTimePicker
import com.wngud.allsleep.ui.theme.*

/**
 * 온보딩 3번 화면: 시간 설정
 * "수면 시간을 설정해주세요"
 */
@Composable
fun OnboardingTimeScreen(
    onNext: () -> Unit,
    bedtime: String = "23:00",
    wakeTime: String = "07:00",
    onBedtimeChange: (String) -> Unit = {},
    onWakeTimeChange: (String) -> Unit = {}
) {
    // String "HH:mm" -> Int parsed
    val bedtimeHour = remember(bedtime) { bedtime.split(":")[0].toInt() }
    val bedtimeMinute = remember(bedtime) { bedtime.split(":")[1].toInt() }
    val wakeHour = remember(wakeTime) { wakeTime.split(":")[0].toInt() }
    val wakeMinute = remember(wakeTime) { wakeTime.split(":")[1].toInt() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))
        
        // 페이지 인디케이터 (상단)
        PageIndicator(
            currentPage = 2,
            totalPages = 6
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        // 작은 앱 로고
        Text(
            text = "💤",
            fontSize = FontSize.iconLarge
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        // 타이틀 & 서브타이틀
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "수면 시간을 설정해주세요",
                fontSize = FontSize.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = LineHeight.extraLoose
            )
            
            Text(
                text = "모든 기기가 이 시간에\n자동으로 잠깁니다",
                fontSize = FontSize.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = LineHeight.normal
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))
        
        // 시간 설정 카드들
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 취침 시간
            TimePickerCard(
                label = "취침 시간",
                hour = bedtimeHour,
                minute = bedtimeMinute,
                onTimeChange = { h, m -> onBedtimeChange("${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}") }
            )
            
            // 기상 시간
            TimePickerCard(
                label = "기상 시간",
                hour = wakeHour,
                minute = wakeMinute,
                onTimeChange = { h, m -> onWakeTimeChange("${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}") }
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.large))
        
        // 수면 시간 뱃지
        SleepDurationBadge(
            bedtimeHour = bedtimeHour,
            bedtimeMinute = bedtimeMinute,
            wakeHour = wakeHour,
            wakeMinute = wakeMinute
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 다음 버튼
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonSize.heightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(CornerRadius.medium)
        ) {
            Text(
                text = "다음",
                fontSize = FontSize.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun TimePickerCard(
    label: String,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(CornerRadius.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = FontSize.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            WheelTimePicker(
                initialHour = hour,
                initialMinute = minute,
                onTimeChange = { h, m -> onTimeChange(h, m) }
            )
        }
    }
}

@Composable
private fun SleepDurationBadge(
    bedtimeHour: Int,
    bedtimeMinute: Int,
    wakeHour: Int,
    wakeMinute: Int
) {
    val bedtimeTotal = bedtimeHour * 60 + bedtimeMinute
    val wakeTotal = wakeHour * 60 + wakeMinute
    val durationMinutes = if (wakeTotal > bedtimeTotal) {
        wakeTotal - bedtimeTotal
    } else {
        1440 - bedtimeTotal + wakeTotal
    }
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    val durationText = if (minutes > 0) "${hours}시간 ${minutes}분" else "${hours}시간"

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(CornerRadius.medium2)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💤",
                fontSize = FontSize.bodyLarge
            )
            Text(
                text = "수면 시간: $durationText",
                fontSize = FontSize.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
fun OnboardingTimeScreenPreview() {
    OnboardingTimeScreen({})
}
