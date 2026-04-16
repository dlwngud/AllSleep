package com.wngud.allsleep.ui.subscription

import androidx.compose.runtime.Composable

/**
 * RevenueCat Paywall UI를 위한 expect 함수
 * 플랫폼별로 실제 구현(Android: RevenueCat UI SDK, iOS: 미지원 등)이 달라집니다.
 */
@Composable
expect fun SubscriptionPaywall(
    onDismiss: () -> Unit
)
