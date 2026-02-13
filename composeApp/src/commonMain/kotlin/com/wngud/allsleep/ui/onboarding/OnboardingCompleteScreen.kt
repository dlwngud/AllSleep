package com.wngud.allsleep.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*

/**
 * 온보딩 5번 화면: 준비 완료
 * "준비 완료!"
 */
@Composable
fun OnboardingCompleteScreen(
    onStart: () -> Unit,
    userName: String = "사용자",
    email: String = "로컬 모드",
    bedtime: String = "23:00",
    wakeTime: String = "07:00"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))
        
        // 페이지 인디케이터 (상단)
        PageIndicator(
            currentPage = 4,
            totalPages = 5
        )
        
        Spacer(modifier = Modifier.weight(0.2f))
        
        // 성공 아이콘
        Text(
            text = "✨",
            fontSize = FontSize.iconHuge
        )
        
        Spacer(modifier = Modifier.height(Spacing.large))
        
        // 타이틀
        Text(
            text = "준비 완료!",
            fontSize = FontSize.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = LineHeight.extraLoose
        )
        
        // 서브타이틀
        Text(
            text = "이제 모든 기기에서\n편안한 숙면을 시작하세요",
            fontSize = FontSize.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = LineHeight.relaxed
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))
        
        // 설정 요약 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(CornerRadius.large)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "설정 요약",
                    fontSize = FontSize.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant
                )
                
                InfoRow(
                    icon = "👤",
                    label = "계정",
                    value = email
                )
                
                InfoRow(
                    icon = "🌙",
                    label = "취침 시간",
                    value = bedtime
                )
                
                InfoRow(
                    icon = "☀️",
                    label = "기상 시간",
                    value = wakeTime
                )
                
                // 수면 시간 계산
                val sleepHours = calculateSleepHours(bedtime, wakeTime)
                InfoRow(
                    icon = "💤",
                    label = "수면 시간",
                    value = "${sleepHours}시간"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(0.3f))
        
        // 시작하기 버튼
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonSize.heightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(CornerRadius.medium)
        ) {
            Text(
                text = "시작하기",
                fontSize = FontSize.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun calculateSleepHours(bedtime: String, wakeTime: String): Int {
    val bedHour = bedtime.split(":")[0].toInt()
    val wakeHour = wakeTime.split(":")[0].toInt()
    
    return if (wakeHour > bedHour) {
        wakeHour - bedHour
    } else {
        24 - bedHour + wakeHour
    }
}

@Preview
@Composable
fun OnboardingCompleteScreenPreview() {
    OnboardingCompleteScreen(
        onStart = {},
        email = "user@example.com",
        bedtime = "23:00",
        wakeTime = "07:00"
    )
}
