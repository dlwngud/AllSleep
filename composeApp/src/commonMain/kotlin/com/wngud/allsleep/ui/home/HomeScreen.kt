package com.wngud.allsleep.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import allsleep.composeapp.generated.resources.*
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 홈 탭 화면
 * 멀티디바이스 동기화(Sync)와 일괄 차단을 시각화한 럭셔리 대시보드 테마입니다.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    HomeScreenContent(
        contentPadding = contentPadding,
        state = state,
        onStartSleep = { viewModel.handleIntent(HomeIntent.StartSleep) }
    )
}

@Composable
fun HomeScreenContent(
    contentPadding: PaddingValues,
    state: HomeState,
    onStartSleep: () -> Unit
) {
    // 럭셔리 다크 네이비 배경 (#0B0C10)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. 최상단: 현재 연결 상태 (기기 맵핑)
            TopConnectionStatus()

            // 2. 중앙 영역: 거대한 동기화 링(Sync Ring)과 상태 메시지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                OrbitalSyncHub()
            }

            // 3. 하단 슬라이더: 무의식적인 탭 방지, 의도적인 수면 시작
            BottomSwipeArea(
                sleepGoal = state.sleepGoal,
                onStartSleep = onStartSleep
            )
        }
    }
}

@Composable
private fun TopConnectionStatus() {
    Box(
        modifier = Modifier
            .background(Color(0xFF131821), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1C2431), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconTextWithStatus("📱", "iPhone")
            Text("·", color = Color.White.copy(alpha = 0.3f))
            IconTextWithStatus("💻", "Mac")
        }
    }
}

@Composable
private fun IconTextWithStatus(icon: String, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape) // 초록색 연결 점
        )
    }
}

@Composable
private fun OrbitalSyncHub() {
    val infiniteTransition = rememberInfiniteTransition()

    // 기기 궤도를 천천히 회전 (평온한 대기 상태)
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart)
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart)
    )
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Box(
            modifier = Modifier.size(340.dp).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 거대한 동기화 링(Sync Ring) 궤도선 표시
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(1.dp, Color(0xFF3BA5F5).copy(alpha = 0.15f), CircleShape)
            )

            // 중앙 허브 애니메이션 (구름/달 캐릭터 유지)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFF3BA5F5).copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .background(Color(0xFF3BA5F5).copy(alpha = 0.15f), CircleShape)
                )
                Image(
                    painter = painterResource(Res.drawable.character_cloud),
                    contentDescription = "Sleep Sync Center",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }

            val orbitRadius = 140.dp.value
            DeviceIcon("📱", rotation1, orbitRadius)
            DeviceIcon("💻", rotation2, orbitRadius)
            DeviceIcon("⌚", rotation3, orbitRadius)
        }

        // 중앙 상태 메시지
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Sync Hub Control Station",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "모든 기기가 수면을 위해 대기 중입니다",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DeviceIcon(icon: String, rotation: Float, orbitRadius: Float) {
    val radians = rotation * PI / 180.0
    val x = (orbitRadius * cos(radians)).dp
    val y = (orbitRadius * sin(radians)).dp

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(44.dp)
            .background(Color(0xFF0B0C10), CircleShape)
            .border(1.dp, Color(0xFF3BA5F5).copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.alpha(0.8f))
    }
}

@Composable
private fun BottomSwipeArea(sleepGoal: String, onStartSleep: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 목표 텍스트
        Text("이번 밤 목표: $sleepGoal", fontSize = 13.sp, color = Color(0xFF3BA5F5))

        // 슬라이더 버튼
        SwipeToSleepButton(onSwipeComplete = onStartSleep)
    }
}

@Composable
private fun SwipeToSleepButton(
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF131A26), RoundedCornerShape(32.dp))
            .border(1.dp, Color(0xFF1E2633), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        val width = this.maxWidth
        val thumbSize = 56.dp
        val swipeAreaWidthPx = with(LocalDensity.current) { (width - thumbSize).toPx() }

        var offsetX by remember { mutableStateOf(0f) }
        var isSwiped by remember { mutableStateOf(false) }

        // 중앙 텍스트
        Text(
            text = if (isSwiped) "수면 모드 활성화됨" else "밀어서 수면 모드 켜기",
            color = Color.White.copy(alpha = if (isSwiped) 1f else 0.4f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

        // 드래그 가능한 손잡이 (Thumb)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(4.dp)
                .size(48.dp)
                .background(Color(0xFF3BA5F5), CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > swipeAreaWidthPx * 0.8f) {
                                // 80% 이상 밀었을 때 활성화
                                offsetX = swipeAreaWidthPx
                                isSwiped = true
                                onSwipeComplete()
                            } else {
                                // 중간에 놓으면 원상복구
                                offsetX = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        if (!isSwiped) {
                            offsetX = (offsetX + dragAmount).coerceIn(0f, swipeAreaWidthPx)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "›",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        HomeScreenContent(
            contentPadding = PaddingValues(),
            state = HomeState(),
            onStartSleep = {}
        )
    }
}