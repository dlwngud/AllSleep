package com.wngud.allsleep.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AllSleep 앱의 Material Design 3 색상 시스템
 * Stitch 디자인 기반: Dark Mode, Custom Color #4938ff
 */

// Main - Primary Colors (보라색 그라데이션 기반)
val Primary = Color(0xFF4938FF)  // 메인 브랜드 컬러
val OnPrimary = Color(0xFFFFFFFF)  // Primary 위의 텍스트/아이콘
val PrimaryContainer = Color(0xFF6B5AFF)  // Primary의 컨테이너
val OnPrimaryContainer = Color(0xFFFFFFFF)  // PrimaryContainer 위의 텍스트

// Surface - 배경 및 표면 색상
val Surface = Color(0xFF0B0E14)  // 메인 배경색 (다크 네이비)
val OnSurface = Color(0xFFFFFFFF)  // Surface 위의 텍스트/아이콘
val SurfaceVariant = Color(0xFF1A1C2E)  // 카드, 다이얼로그 등의 표면
val OnSurfaceVariant = Color(0xFF94A3B8)  // SurfaceVariant 위의 보조 텍스트

// Outline - 구분선
val Outline = Color(0xFF2D3142)  // 구분선, 테두리

// Error - 에러 상태
val Error = Color(0xFFFF5252)  // 에러 색상
val OnError = Color(0xFFFFFFFF)  // Error 위의 텍스트

// Additional Brand Colors (그라데이션용)
val GradientStart = Color(0xFFA855F7)  // 그라데이션 시작 (밝은 보라)
val GradientEnd = Color(0xFF4938FF)  // 그라데이션 끝 (메인 보라)

// Semantic Colors
val TextPrimary = Color(0xFFFFFFFF)  // 주요 텍스트
val TextSecondary = Color(0xFF94A3B8)  // 보조 텍스트
val TextTertiary = Color(0xFF64748B)  // 3차 텍스트

// Indicator Colors
val IndicatorActive = Color(0xFF4A3AFF)  // 활성 인디케이터
val IndicatorInactive = Color(0x4D94A3B8)  // 비활성 인디케이터 (30% opacity)
