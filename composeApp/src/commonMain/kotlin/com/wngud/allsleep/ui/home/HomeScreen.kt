package com.wngud.allsleep.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.charachter_no_phone
import allsleep.composeapp.generated.resources.character_phone
import allsleep.composeapp.generated.resources.ic_mobile
import allsleep.composeapp.generated.resources.ic_tablet
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import com.wngud.allsleep.ui.theme.IndicatorSynced


/**
 * 홈 탭 화면
 * 멀티디바이스 동기화(Sync)와 일괄 차단을 시각화한 럭셔리 대시보드 테마입니다.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = koinViewModel(),
    globalSleepViewModel: com.wngud.allsleep.ui.global.GlobalSleepViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sleepState by globalSleepViewModel.sleepState.collectAsState()
    val devices by globalSleepViewModel.registeredDevices.collectAsState()

    HomeScreenContent(
        contentPadding = contentPadding,
        state = state,
        devices = devices,
        isSleepModeActive = sleepState?.isSleeping ?: false,
        onStartSleep = { 
            viewModel.handleIntent(HomeIntent.StartSleep)
            globalSleepViewModel.toggleSleepState(isSleeping = true)
        },
        onWakeUpTest = {
            // [임시 테스트용] 강제 기상 버튼 콜백
            globalSleepViewModel.toggleSleepState(isSleeping = false)
        }
    )
}

@Composable
fun HomeScreenContent(
    contentPadding: PaddingValues,
    state: HomeState,
    devices: List<com.wngud.allsleep.domain.model.DeviceState>,
    isSleepModeActive: Boolean,
    onStartSleep: () -> Unit,
    onWakeUpTest: () -> Unit
) {
    var showDeviceSheet by remember { mutableStateOf(false) }
    
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

            // 1. 중앙 영역: 거대한 동기화 링(Sync Ring)과 상태 메시지
            // (위성 배치 로직으로 통합됨, 기기 탭 시 바텀시트 활성화)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                OrbitalSyncHub(
                    devices = devices,
                    isSleepModeActive = isSleepModeActive,
                    onDeviceClick = { showDeviceSheet = true }
                )
            }

            // 3. 하단 슬라이더: 무의식적인 탭 방지, 의도적인 수면 시작
            BottomSwipeArea(
                sleepGoal = state.sleepGoal,
                isSleepModeActive = isSleepModeActive,
                onStartSleep = onStartSleep,
                onWakeUpTest = onWakeUpTest
            )
        }

        if (showDeviceSheet) {
            DeviceBottomSheet(
                devices = devices,
                onDismiss = { showDeviceSheet = false }
            )
        }
    }
}

// 기기 정보에 따른 아이콘 매핑 유틸리티
@Composable
private fun getDeviceIcon(platform: String): DrawableResource {
    return when (platform.lowercase()) {
        "android" -> Res.drawable.ic_mobile
        "ios" -> Res.drawable.ic_mobile
        "tablet" -> Res.drawable.ic_tablet
        else -> Res.drawable.ic_mobile
    }
}

    // TopConnectionStatus 제거 (OrbitalSyncHub의 위성으로 통합)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceBottomSheet(
    devices: List<com.wngud.allsleep.domain.model.DeviceState>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131821),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFF1C2431), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "동기화된 기기",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            devices.forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSynced = true // Firestore에 존재한다는 것 자체가 현재 동기화 대상임을 의미
                    val icon = getDeviceIcon(device.platform)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0B0C10), CircleShape)
                            .border(1.dp, Color(0xFF1C2431), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = device.deviceName,
                            tint = if (isSynced) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = device.deviceName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // 연결 상태 표시 (Dot + 텍스트)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isSynced) IndicatorSynced else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isSynced) "Ready" else "Offline",
                                color = if (isSynced) IndicatorSynced else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitalSyncHub(
    devices: List<com.wngud.allsleep.domain.model.DeviceState>,
    isSleepModeActive: Boolean, 
    onDeviceClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 궤도 무한 회전 애니메이션용 반지름 관리
    val orbitRadius = 140.dp.value
    // 기기 개수에 따른 궤도 간격
    val deviceCount = devices.size
    val angleStep = if (deviceCount > 0) 360f / deviceCount else 0f

    // 2초 간격으로 빛이 깜빡이는(숨쉬는) 펄스 애니메이션
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.clickable(onClick = onDeviceClick),
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

            // 중앙 허브 애니메이션 (구름/달 캐릭터 등)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF3BA5F5).copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isSleepModeActive) Res.drawable.charachter_no_phone
                        else Res.drawable.character_phone
                    ),
                    contentDescription = "Sleep Sync Center",
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 연결된 기기 위성(Satellite) 애니메이션 리스트
            devices.forEachIndexed { index, device ->
                // 각 기기마다의 고유 시작 각도
                val startAngle = index * angleStep
                // 360도 회전 애니메이션
                val rotation by infiniteTransition.animateFloat(
                    initialValue = startAngle,
                    targetValue = startAngle + 360f,
                    animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart)
                )
                
                DeviceIcon(
                    icon = getDeviceIcon(device.platform),
                    rotation = rotation,
                    orbitRadius = orbitRadius,
                    isSynced = true,
                    pulseAlpha = pulseAlpha
                )
            }
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
private fun DeviceIcon(
    icon: DrawableResource,
    rotation: Float,
    orbitRadius: Float,
    isSynced: Boolean,
    pulseAlpha: Float
) {
    val radians = rotation * PI / 180.0
    val x = (orbitRadius * cos(radians)).dp
    val y = (orbitRadius * sin(radians)).dp

    // 테마 메인 색상(보라색) 롤백 및 하이라이트 투명도 최대로 올려 쨍하게
    val activeColor = MaterialTheme.colorScheme.primary
    val iconColor = if (isSynced) activeColor else Color.Gray

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(56.dp), // 아우라(Glow) 효과가 밖으로 퍼질 수 있는 충분한 공간 확보
        contentAlignment = Alignment.Center
    ) {
        // 활성화된 경우에만 뒤에 흐릿한 네온 글로우 이펙트(Blur) 깔기
        if (isSynced) {
            Box(
                modifier = Modifier
                    .size(68.dp) // 아우라 범위를 넉넉하게
                    // blur(12.dp)의 박스 한계로 사각형 잘림(Clipping)이 발생하는 것을 방지하기 위해 
                    // 중앙부터 바깥으로 부드럽게 완전 투명(Transparent)해지는 진짜 원형(Circle) 파장 브러시 사용
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = pulseAlpha), // 안쪽: 빛의 중심지
                                activeColor.copy(alpha = 0f)          // 바깥쪽: 완전 투명하게 발산
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // 실제 하드웨어 칩 (내부 컨테이너)
        Box(
            modifier = Modifier
                .size(44.dp)
                // 활성화 시 보라색(Primary) 틴트를 아주 살짝 섞어서 럭셔리한 느낌 추가
                .background(if (isSynced) activeColor.copy(alpha = 0.1f) else Color(0xFF131821), CircleShape)
                .border(1.5.dp, if (isSynced) activeColor.copy(alpha = 0.3f) else Color(0xFF1C2431), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomSwipeArea(
    sleepGoal: String, 
    isSleepModeActive: Boolean,
    onStartSleep: () -> Unit,
    onWakeUpTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 목표 텍스트
        Text("이번 밤 목표: $sleepGoal", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

        // 슬라이더 버튼 (수면 상태에 따라 테스트용 해제 버튼으로도 변경 가능)
        if (isSleepModeActive) {
            Button(
                onClick = onWakeUpTest,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("임시: 잠금 해제 (Wake Up Test)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            SwipeToSleepButton(onSwipeComplete = onStartSleep)
        }
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
                .background(MaterialTheme.colorScheme.primary, CircleShape)
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
            devices = emptyList(),
            isSleepModeActive = false,
            onStartSleep = {},
            onWakeUpTest = {}
        )
    }
}