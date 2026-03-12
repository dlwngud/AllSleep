package com.wngud.allsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wngud.allsleep.ui.theme.*

/**
 * Stitch 디자인을 100% 반영한 프리미엄 배터리 최적화 가이드 다이얼로그
 */
@Composable
fun BatteryOptimizationGuideDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    isForced: Boolean = false
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceVariant, // #1A1C2E
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 아이콘 영역 (글로우 효과)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Primary.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Primary.copy(alpha = 0.1f), CircleShape)
                            .border(1.5.dp, Primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 타이틀
                Text(
                    text = if (isForced) "잠금 기능 시작 불가" else "배터리 최적화 예외 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 설명 문구
                Text(
                    text = if (isForced) 
                        "배터리 최적화가 켜져 있으면 자는 동안 잠금이 풀릴 수 있습니다. 계속하려면 아래 설정을 완료해주세요."
                        else "AllSleep이 밤새 끊김 없이 작동하려면 배터리 사용량을 '제한 없음'으로 설정해야 합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 가이드 박스
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF131424),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GuideStepItem(number = 1, text = "[모든 앱] 필터 선택")
                        GuideStepItem(number = 2, text = "[AllSleep] 앱 찾기")
                        GuideStepItem(number = 3, text = "[제한 없음] 선택")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 메인 액션 버튼 (필형 보라색)
                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = "설정으로 이동",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 보조 버튼
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = if (isForced) "돌아가기" else "나중에 하기",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStepItem(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
