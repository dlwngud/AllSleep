package com.wngud.allsleep.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun GlobalLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val activity = LocalContext.current as PlatformContext
    val viewModel = koinViewModel<AuthViewModel>()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.user) {
        if (state.user != null) {
            onLoginSuccess()
        }
    }

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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🌙", fontSize = FontSize.iconExtraLarge)
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "다시 만나서 반가워요!",
                    fontSize = FontSize.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "로그인하여 모든 기기의 수면 상태를\n실시간으로 확인하세요",
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
                onAppleLogin = { /* TODO */ },
                onSkip = null // 일반 로그인 시 건너뛰기 불가
            )
        }
    }
}
