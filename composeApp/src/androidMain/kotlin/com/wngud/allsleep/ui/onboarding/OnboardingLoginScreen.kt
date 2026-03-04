package com.wngud.allsleep.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.ic_kakao_logo
import allsleep.composeapp.generated.resources.ic_google_logo
import allsleep.composeapp.generated.resources.ic_apple_logo
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.ui.auth.login.AuthIntent
import com.wngud.allsleep.ui.auth.login.AuthViewModel
import com.wngud.allsleep.ui.components.PageIndicator
import com.wngud.allsleep.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import com.wngud.allsleep.platform.PlatformContext

/**
 * 온보딩 4번 화면: 로그인
 *
 * ✅ ViewModel은 Koin이 자동 주입 (parametersOf 불필요)
 * ✅ Activity는 버튼 클릭 시점에 LoginIntent로 전달 (생성자 주입 X)
 */
@Composable
actual fun OnboardingLoginScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onKakaoLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    onEmailLogin: () -> Unit
) {
    val activity = LocalContext.current as PlatformContext
    val viewModel = koinViewModel<AuthViewModel>()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 로그인 성공 시 다음 화면으로
    LaunchedEffect(state.user) {
        if (state.user != null) {
            android.util.Log.d("OnboardingLoginScreen", "✅ 로그인 성공 감지! 다음 화면으로 이동")
            android.util.Log.d("OnboardingLoginScreen", "   - User: ${state.user}")
            onNext()
        }
    }

    // 에러 발생 시 Snackbar 표시
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            if (!error.contains("취소")) {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.handleIntent(AuthIntent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

            PageIndicator(currentPage = 3, totalPages = 5)

            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "💤", fontSize = FontSize.iconExtraLarge)
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "설정을 저장하고\n모든 기기를 연결하세요",
                    fontSize = FontSize.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = LineHeight.loose
                )
                Text(
                    text = "로그인하면 방금 설정한 시간이\n모든 기기에 자동으로 적용됩니다",
                    fontSize = FontSize.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = LineHeight.tight
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isAnyLoading = state.loadingProvider != null

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 카카오 로그인
                val isKakaoLoading = state.loadingProvider == AuthProvider.KAKAO
                Button(
                    onClick = { viewModel.handleIntent(AuthIntent.LoginWithKakao) },
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
                    onClick = { viewModel.handleIntent(AuthIntent.LoginWithGoogle(activity)) },
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
                    onClick = { onNext() },
                    modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        BorderWidth.thin,
                        MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(CornerRadius.medium)
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

                // 건너뛰기 버튼
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
                    shape = RoundedCornerShape(CornerRadius.medium)
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
}

@Preview
@Composable
fun OnboardingLoginScreenPreview() {
    OnboardingLoginScreen(
        onNext = {},
        onSkip = {},
        onKakaoLogin = {},
        onAppleLogin = {},
        onEmailLogin = {}
    )
}
