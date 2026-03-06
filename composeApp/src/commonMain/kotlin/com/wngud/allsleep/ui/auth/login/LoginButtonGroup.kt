package com.wngud.allsleep.ui.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.ic_kakao_logo
import allsleep.composeapp.generated.resources.ic_google_logo
import allsleep.composeapp.generated.resources.ic_apple_logo
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.ui.theme.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginButtonGroup(
    state: LoginState,
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAnyLoading = state.loadingProvider != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 카카오 로그인
        val isKakaoLoading = state.loadingProvider == AuthProvider.KAKAO
        Button(
            onClick = onKakaoLogin,
            modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFEE500),
                contentColor = Color(0xFF000000),
                disabledContainerColor = Color(0xFFFEE500),
                disabledContentColor = Color(0xFF000000)
            ),
            shape = RoundedCornerShape(CornerRadius.medium),
            enabled = !isAnyLoading
        ) {
            if (isKakaoLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF000000),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_kakao_logo),
                        contentDescription = "Kakao Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = "카카오로 계속하기",
                        fontSize = FontSize.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Google 로그인
        val isGoogleLoading = state.loadingProvider == AuthProvider.GOOGLE
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                BorderWidth.thin,
                MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(CornerRadius.medium),
            enabled = !isAnyLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text(
                        text = "Google로 계속하기",
                        fontSize = FontSize.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. Apple 로그인
        OutlinedButton(
            onClick = onAppleLogin,
            modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                BorderWidth.thin,
                MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(CornerRadius.medium),
            enabled = !isAnyLoading
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_apple_logo),
                    contentDescription = "Apple Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Apple로 계속하기",
                    fontSize = FontSize.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 건너뛰기 버튼 (온보딩에서만 노출 가능)
        if (onSkip != null) {
            // 구분선
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "또는",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = FontSize.bodySmall,
                    color = OnSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    BorderWidth.medium,
                    MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(CornerRadius.medium),
                enabled = !isAnyLoading
            ) {
                Text(
                    text = "건너뛰기 (현재 기기만 사용)",
                    fontSize = FontSize.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "로컬 모드에서는 현재 기기만 잠글 수 있어요",
                fontSize = FontSize.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
