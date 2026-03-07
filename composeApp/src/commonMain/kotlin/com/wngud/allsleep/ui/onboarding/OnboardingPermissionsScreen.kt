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
import com.wngud.allsleep.platform.rememberPermissionRequester
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*

@Composable
fun OnboardingPermissionsScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    // 8. 거부 시 피드백 다이얼로그(우아한 기능 저하 안내) 표시 상태
    var showDeniedDialog by remember { mutableStateOf(false) }

    val permissionRequester = rememberPermissionRequester { isGranted ->
        if (isGranted) {
            // 승인됨: 정상적으로 다음 화면 진입
            onAllow()
        } else {
            // 거부됨: 기능 성능 제약 안내 다이얼로그 표시
            showDeniedDialog = true
        }
    }

    // 기능 저하를 안내하는 다이얼로그 (Graceful Degradation)
    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeniedDialog = false
                onAllow() // 다이얼로그를 닫아도 다음 단계로 이동
            },
            title = {
                Text(
                    text = "알림 없이 진행할까요?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "알림 권한이 거부되어 수면 리포트 및 기상 알림을 받을 수 없습니다.\n나중에 시스템 설정에서 언제든 켤 수 있습니다.",
                    fontSize = FontSize.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeniedDialog = false
                    onAllow()
                }) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

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
            onClick = { permissionRequester.requestBasicPermissions() },
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
