package com.wngud.allsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.ui.theme.IndicatorInactive

/**
 * 온보딩 페이지 인디케이터
 * 
 * @param currentPage 현재 페이지 (0-indexed)
 * @param totalPages 전체 페이지 수
 * @param modifier Modifier
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            if (index == currentPage) {
                // Active indicator (pill shape)
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(8.dp)
                        .background(
                            Color(0xFF4A3AFF),
                            RoundedCornerShape(percent = 50)
                        )
                )
            } else {
                // Inactive indicator (circle)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            IndicatorInactive,
                            CircleShape
                        )
                )
            }
        }
    }
}
