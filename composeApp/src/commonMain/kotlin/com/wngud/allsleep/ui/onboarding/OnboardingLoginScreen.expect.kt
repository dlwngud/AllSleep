package com.wngud.allsleep.ui.onboarding

import androidx.compose.runtime.Composable

/**
 * 온보딩 로그인 화면 (expect)
 * 플랫폼별 구현 필요
 */
@Composable
expect fun OnboardingLoginScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onKakaoLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    onEmailLogin: () -> Unit
)
