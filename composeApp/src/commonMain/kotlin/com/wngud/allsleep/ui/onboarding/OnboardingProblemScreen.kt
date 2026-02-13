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
 * 온보딩 1번 화면: 문제 공감
 * "폰을 내려놓아도 태블릿을 집어드나요?"
 */
@Composable
fun OnboardingProblemScreen(
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
            currentPage = 0,
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
        
        // 타이틀
        Text(
            text = "폰을 내려놓아도\n태블릿을 집어드나요?",
            fontSize = FontSize.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = LineHeight.extraLoose
        )
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // 서브타이틀 (변경!)
        Text(
            text = "기기 간 도파민 뺑뺑이로\n수면을 놓치고 있습니다",
            fontSize = FontSize.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = LineHeight.relaxed,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
fun OnboardingProblemScreenPreview() {
    OnboardingProblemScreen({})
}
