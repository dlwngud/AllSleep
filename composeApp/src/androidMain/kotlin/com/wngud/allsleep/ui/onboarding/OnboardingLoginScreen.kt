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
import com.wngud.allsleep.ui.auth.login.LoginButtonGroup
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

            PageIndicator(currentPage = 4, totalPages = 6)

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

            LoginButtonGroup(
                state = state,
                onKakaoLogin = { viewModel.handleIntent(AuthIntent.LoginWithKakao) },
                onGoogleLogin = { viewModel.handleIntent(AuthIntent.LoginWithGoogle(activity)) },
                onAppleLogin = { onNext() }, // TODO: 실제 애플 로그인 연동
                onSkip = onSkip
            )
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
