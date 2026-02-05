package com.wngud.allsleep.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * AllSleep 다크 테마 색상 스킴
 * Stitch 디자인 기반
 */
private val DarkColorScheme = darkColorScheme(
    // Main - Primary
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    
    // Surface
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    
    // Outline
    outline = Outline,
    
    // Error
    error = Error,
    onError = OnError,
    
    // Background (Surface와 동일)
    background = Surface,
    onBackground = OnSurface
)

/**
 * AllSleep 라이트 테마 색상 스킴 (현재는 다크 모드만 사용)
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    
    surface = Color(0xFFF6F5F8),
    onSurface = Color(0xFF1A1C2E),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF64748B),
    
    outline = Color(0xFFE2E8F0),
    
    error = Error,
    onError = OnError,
    
    background = Color(0xFFF6F5F8),
    onBackground = Color(0xFF1A1C2E)
)

/**
 * AllSleep 앱 테마
 */
@Composable
fun AllSleepTheme(
    darkTheme: Boolean = true,  // 기본적으로 다크 모드 사용
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
