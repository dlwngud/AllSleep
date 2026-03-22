package com.wngud.allsleep.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*

/**
 * 온보딩 권한 안내 공통 컴포넌트
 * Stitch 'Luminous Nocturne' 디자인 시스템 반영
 */
@Composable
fun OnboardingPermissionStep(
    stepNumber: Int,
    icon: String, // Emoji
    title: String,
    whyContent: String,
    whenContent: String,
    howContent: String,
    onAllow: () -> Unit,
    onSkip: (() -> Unit)? = null,
    isAllowed: Boolean = false
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

        // 상단 페이지 인디케이터 (8단계 중 X단계)
        PageIndicator(
            currentPage = stepNumber - 1,
            totalPages = 8
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 중앙 아이콘 (Glow 효과 - 임시로 Shadow/Tint 활용)
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Text(
                    text = icon,
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 메인 타이틀
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 안내 섹션들 (Why, When, How)
            PermissionInfoCard(
                label = "왜 필요한가요?",
                content = whyContent
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            PermissionInfoCard(
                label = "언제 사용되나요?",
                content = whenContent
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionInfoCard(
                label = "허용 방법",
                content = howContent
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // 하단 버튼 섹션
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp), // parent Column already has padding(bottom = 48.dp)
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonSize.heightLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isAllowed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(CornerRadius.medium)
            ) {
                Text(
                    text = if (isAllowed) "허용 완료" else "허용하기",
                    fontSize = FontSize.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (onSkip != null) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "다음에 하기",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = FontSize.labelMedium,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(ButtonSize.heightSmall + 8.dp))
            }
        }
    }
}

@Composable
private fun PermissionInfoCard(
    label: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Glassmorphism 느낌
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}
