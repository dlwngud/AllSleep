package com.wngud.allsleep.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun WheelTimePicker(
    modifier: Modifier = Modifier,
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // ✅ 1. 가장 먼저 그리기 → 텍스트 뒤에 위치 (background layer)
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(60.dp)
                .background(
                    color = Color(0xFF1E1A33).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF4A4476),
                    shape = RoundedCornerShape(14.dp)
                )
        )

        // ✅ 2. 휠 피커 Row → 보라색 박스 위에 텍스트가 렌더링됨
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = (0..23).toList(),
                initialIndex = initialHour,
                itemHeight = 60.dp,
                onItemSelected = { hour ->
                    selectedHour = hour
                    onTimeChange(selectedHour, selectedMinute)
                }
            )

            Text(
                text = ":",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            WheelPicker(
                items = (0..59).toList(),
                initialIndex = initialMinute,
                itemHeight = 60.dp,
                onItemSelected = { minute ->
                    selectedMinute = minute
                    onTimeChange(selectedHour, selectedMinute)
                }
            )
        }

        // ✅ 3. 페이드 오버레이는 마지막에 (텍스트 위로 그라데이션 적용)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF13151A), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF13151A))
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    items: List<Int>,
    initialIndex: Int,
    itemHeight: Dp,
    onItemSelected: (Int) -> Unit
) {
    val count = items.size
    val virtualCount = Int.MAX_VALUE
    val middleOffset = virtualCount / 2
    val startIndex = middleOffset - (middleOffset % count) + initialIndex

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 스크롤 중 여부 감지
    val isScrolling = listState.isScrollInProgress

    // layoutInfo 기반 중앙 아이템 index 계산 (가장 정확한 방법)
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index
        }
    }

    // 스크롤이 멈출 때 콜백 전달
    LaunchedEffect(isScrolling) {
        if (!isScrolling) {
            centerItemIndex?.let { idx ->
                onItemSelected(items[idx % count])
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = Modifier
            .width(80.dp)
            .height(itemHeight * 3),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = itemHeight)
    ) {
        items(
            count = virtualCount,
            key = { it }
        ) { index ->
            val value = items[index % count]

            // ✅ 스크롤 중에는 모두 흐리게, 멈췄을 때만 center 항목 강조
            val isCenter = !isScrolling && centerItemIndex == index

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    fontSize = if (isCenter) 30.sp else 22.sp,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCenter) Color.White else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (isCenter) 1f else 0.4f)
                )
            }
        }
    }
}
