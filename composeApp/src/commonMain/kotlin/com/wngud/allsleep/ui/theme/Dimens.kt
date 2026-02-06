package com.wngud.allsleep.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AllSleep Design System - Dimensions
 * 
 * 모든 spacing, corner radius, size 값을 중앙에서 관리
 * Material Design 3 가이드라인 기반
 */

/**
 * Spacing 시스템
 * 8dp 기반 그리드 시스템 사용
 */
object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp      // 1/2 unit
    val small: Dp = 8.dp           // 1 unit
    val medium: Dp = 16.dp         // 2 units
    val large: Dp = 24.dp          // 3 units
    val extraLarge: Dp = 32.dp     // 4 units
    val extraExtraLarge: Dp = 48.dp // 6 units
    val huge: Dp = 56.dp           // 7 units
}

/**
 * Corner Radius 시스템
 * Material Design 3 Shape Scale
 */
object CornerRadius {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp         // 주요 버튼
    val large: Dp = 16.dp          // 카드
    val medium2: Dp = 20.dp        // 특수 카드 (뱃지 등)
    val extraLarge: Dp = 24.dp     // 큰 카드
    val full: Dp = 9999.dp         // Pill shape (완전 둥근)
}

/**
 * Button Size 시스템
 */
object ButtonSize {
    val heightSmall: Dp = 40.dp
    val heightMedium: Dp = 52.dp   // 소셜 로그인
    val heightLarge: Dp = 56.dp    // 주요 액션
    val heightExtraLarge: Dp = 64.dp
}

/**
 * Icon Size 시스템
 */
object IconSize {
    val extraSmall: Dp = 12.dp
    val small: Dp = 16.dp
    val medium: Dp = 24.dp
    val large: Dp = 32.dp
    val extraLarge: Dp = 48.dp
    val huge: Dp = 96.dp           // 온보딩 이모지
}

/**
 * Card Elevation 시스템
 */
object Elevation {
    val none: Dp = 0.dp
    val small: Dp = 2.dp
    val medium: Dp = 4.dp
    val large: Dp = 8.dp
    val extraLarge: Dp = 16.dp
}

/**
 * Border Width 시스템
 */
object BorderWidth {
    val thin: Dp = 1.dp
    val medium: Dp = 1.5.dp
    val thick: Dp = 2.dp
}

/**
 * Font Size 시스템
 * Material Design 3 Type Scale
 */
object FontSize {
    // Display (가장 큰 텍스트)
    val displayLarge = 57.sp
    val displayMedium = 45.sp
    val displaySmall = 36.sp
    
    // Headline (제목)
    val headlineLarge = 32.sp      // 온보딩 타이틀
    val headlineMedium = 28.sp     // 화면 타이틀
    val headlineSmall = 24.sp      // 섹션 타이틀
    
    // Title (부제목)
    val titleLarge = 22.sp
    val titleMedium = 20.sp        // 카드 타이틀
    val titleSmall = 16.sp
    
    // Body (본문)
    val bodyLarge = 16.sp          // 주요 본문
    val bodyMedium = 14.sp         // 일반 본문
    val bodySmall = 12.sp          // 작은 본문
    
    // Label (라벨, 버튼)
    val labelLarge = 18.sp         // 큰 버튼
    val labelMedium = 16.sp        // 일반 버튼
    val labelSmall = 14.sp         // 작은 버튼
    
    // Icon/Emoji (아이콘, 이모지)
    val iconHuge = 96.sp           // 온보딩 이모지
    val iconExtraLarge = 48.sp
    val iconLarge = 32.sp
    val iconMedium = 20.sp
    val iconSmall = 16.sp
}

/**
 * Line Height 시스템
 */
object LineHeight {
    val tight = 20.sp
    val normal = 24.sp
    val relaxed = 26.sp
    val loose = 32.sp
    val extraLoose = 36.sp
}

