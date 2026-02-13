package com.wngud.allsleep.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.character_cloud
import androidx.compose.ui.tooling.preview.Preview
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * 온보딩 2번 화면: 솔루션 제시
 * "하나를 잠그면 모두 잠깁니다"
 */
@Composable
fun OnboardingSolutionScreen(
    onNext: () -> Unit
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
            currentPage = 1,
            totalPages = 5
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 캐릭터 이미지
        Image(
            painter = painterResource(Res.drawable.character_cloud),
            contentDescription = "Sleep Character",
            modifier = Modifier.size(240.dp)
        )
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        // 타이틀 (변경!)
        Text(
            text = "하나를 잠그면\n모두 잠깁니다",
            fontSize = FontSize.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = LineHeight.extraLoose
        )
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // 서브타이틀 (변경!)
        Text(
            text = "AllSleep은 모든 기기를\n실시간으로 동기화합니다",
            fontSize = FontSize.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = LineHeight.relaxed
        )
        
        Spacer(modifier = Modifier.weight(0.3f))
        
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

@Preview
@Composable
fun OnboardingSolutionScreenPreview() {
    OnboardingSolutionScreen({})
}
