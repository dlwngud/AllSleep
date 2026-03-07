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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*

@Composable
fun OnboardingPermissionsScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
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
        
        PageIndicator(
            currentPage = 3,
            totalPages = 6
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        Text(
            text = "☁️🔔",
            fontSize = FontSize.iconLarge
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "상쾌한 아침을 위해\n권한이 필요해요",
                fontSize = FontSize.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = LineHeight.extraLoose
            )
            
            Text(
                text = "약속한 기상 시간(07:00)에 정확히 깨워드리고,\n수면 통계를 안전하게 전송해 드릴게요.",
                fontSize = FontSize.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = LineHeight.normal
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            PermissionItem(
                iconText = "🔔",
                title = "알림 권한",
                description = "수면 시작 및 리포트 알림"
            )
            PermissionItem(
                iconText = "⏱️",
                title = "알람 및 타이머",
                description = "정확한 기상 미션 실행"
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonSize.heightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(CornerRadius.medium)
        ) {
            Text(
                text = "알림 허용하기",
                fontSize = FontSize.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "나중에 설정하기",
                fontSize = FontSize.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PermissionItem(iconText: String, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = iconText,
                    fontSize = 24.sp
                )
            }
        }
        
        Column {
            Text(
                text = title,
                fontSize = FontSize.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = FontSize.bodyMedium,
                color = OnSurfaceVariant
            )
        }
    }
}
