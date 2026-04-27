package com.wngud.allsleep.ui.home

import com.wngud.allsleep.ui.components.DeviceListContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
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
import com.wngud.allsleep.platform.rememberPermissionRequester
import com.wngud.allsleep.platform.rememberOverlayPermissionRequester
import com.wngud.allsleep.platform.rememberAccessibilityPermissionRequester
import com.wngud.allsleep.platform.rememberSleepServiceController
import com.wngud.allsleep.ui.theme.IndicatorSynced
import com.wngud.allsleep.ui.theme.OnSurfaceVariant
import com.wngud.allsleep.ui.components.BatteryOptimizationGuideDialog
import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.platform.rememberNotificationPermissionRequester
import com.wngud.allsleep.ui.components.NotificationRationaleDialog
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import com.wngud.allsleep.ui.global.GlobalSleepContract
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    globalSleepViewModel: GlobalSleepViewModel = org.koin.compose.koinInject(),
    sleepSettingsRepository: SleepSettingsRepository = koinInject()
) {
    val globalState by globalSleepViewModel.state.collectAsState(GlobalSleepContract.State())
    val sleepGoal by sleepSettingsRepository.homeSleepGoal().collectAsState(initial = "8h")

    val devices = globalState.registeredDevices
    val isToggleLoading = globalState.isToggleLoading
    val isSleeping = globalState.sleepState?.isSleeping == true

    HomeScreenContent(
        contentPadding = contentPadding,
        sleepGoal = sleepGoal,
        isSleeping = isSleeping,
        devices = devices,
        snackbarHostState = snackbarHostState,
        onStartSleep = { 
            globalSleepViewModel.handleIntent(
                GlobalSleepContract.Intent.ToggleSleepState(isSleeping = true)
            )
        }
    )

    if (isToggleLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}

private fun SleepSettingsRepository.homeSleepGoal() = combine(
    weekdayBedtime,
    weekdayWakeTime,
    weekendBedtime,
    weekendWakeTime
) { wdB, wdW, weB, weW ->
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val isWeekdayTonight = today.dayOfWeek.ordinal < 5
    val targetMinutes = if (isWeekdayTonight) {
        calculateTimeDiffMinutes(wdB, wdW)
    } else {
        calculateTimeDiffMinutes(weB, weW)
    }
    formatGoalDuration(targetMinutes)
}

private fun calculateTimeDiffMinutes(start: String, end: String): Int {
    return try {
        val startParts = start.split(":").map { it.toInt() }
        val endParts = end.split(":").map { it.toInt() }
        val startTotal = startParts[0] * 60 + startParts[1]
        var endTotal = endParts[0] * 60 + endParts[1]
        if (endTotal <= startTotal) endTotal += 24 * 60
        endTotal - startTotal
    } catch (_: Exception) {
        480
    }
}

private fun formatGoalDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
}

@Composable
fun HomeScreenContent(
    contentPadding: PaddingValues,
    sleepGoal: String,
    isSleeping: Boolean,
    devices: List<DeviceState>,
    snackbarHostState: SnackbarHostState,
    onStartSleep: () -> Unit
) {
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }
    var showForcedBatteryDialog by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val sleepServiceController = rememberSleepServiceController()
    
    val notificationPermissionRequester = rememberNotificationPermissionRequester { _ -> }

    val accessibilityPermissionRequester = rememberAccessibilityPermissionRequester { isGranted ->
        if (isGranted) {
            onStartSleep()
            sleepServiceController.start()
        }
    }
    val permissionRequester = rememberOverlayPermissionRequester { isGranted ->
        if (isGranted) {
            if (accessibilityPermissionRequester.isGranted()) {
                onStartSleep()
                sleepServiceController.start()
            } else {
                showAccessibilityDisclosure = true
            }
        }
    }

    val batteryPermissionRequester = rememberPermissionRequester { }
    val isBatteryOptimized by batteryPermissionRequester.isBatteryOptimized.collectAsState()
    var hasTriedServiceRecovery by remember(isSleeping) { mutableStateOf(false) }

    val needsOverlayRecovery = isSleeping && !permissionRequester.isGranted()
    val needsAccessibilityRecovery = isSleeping && !accessibilityPermissionRequester.isGranted()
    val needsBatteryRecovery = isSleeping && !batteryPermissionRequester.isIgnoringBatteryOptimizations()
    val needsServiceRecovery = isSleeping &&
        permissionRequester.isGranted() &&
        accessibilityPermissionRequester.isGranted() &&
        !sleepServiceController.isRunning()

    LaunchedEffect(isSleeping, needsServiceRecovery) {
        if (!isSleeping) {
            hasTriedServiceRecovery = false
        } else if (needsServiceRecovery && !hasTriedServiceRecovery) {
            hasTriedServiceRecovery = true
            sleepServiceController.start()
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("권한 안내", fontWeight = FontWeight.Bold) },
            text = { Text("수면 중 휴대폰 사용을 효과적으로 차단하기 위해 '다른 앱 위에 표시' 및 '접근성(강제 회귀)' 권한이 필수적입니다.\n\n설정 화면으로 이동하여 권한을 허용해 주세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    if (!permissionRequester.isGranted()) {
                        permissionRequester.requestPermission()
                    } else if (!accessibilityPermissionRequester.isGranted()) {
                        showAccessibilityDisclosure = true
                    }
                }) {
                    Text("설정으로 이동", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false 
                    scope.launch {
                        snackbarHostState.showSnackbar("필수 권한 없이는 수면 잠금을 사용할 수 없습니다.")
                    }
                }) {
                    Text("나중에 가기")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showNotificationRationale) {
        NotificationRationaleDialog(
            onDismissRequest = { 
                showNotificationRationale = false 
                scope.launch {
                    snackbarHostState.showSnackbar("알림 권한 없이는 수면 잠금을 사용할 수 없습니다.")
                }
            },
            onConfirmClick = {
                showNotificationRationale = false
                notificationPermissionRequester.openSettings()
            }
        )
    }

    if (showBatteryGuideDialog || showForcedBatteryDialog) {
        BatteryOptimizationGuideDialog(
            isForced = showForcedBatteryDialog,
            onDismissRequest = {
                showBatteryGuideDialog = false
                showForcedBatteryDialog = false
            },
            onConfirmClick = {
                showBatteryGuideDialog = false
                showForcedBatteryDialog = false
                batteryPermissionRequester.requestIgnoreBatteryOptimizations()
            }
        )
    }
    if (showAccessibilityDisclosure) {
        com.wngud.allsleep.ui.components.AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDisclosure = false
                accessibilityPermissionRequester.requestPermission()
            },
            onDismiss = {
                showAccessibilityDisclosure = false
                scope.launch {
                    snackbarHostState.showSnackbar("접근성 권한 없이는 수면 잠금을 사용할 수 없습니다.")
                }
            }
        )
    }

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
            if (!isBatteryOptimized) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showBatteryGuideDialog = true },
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(
                            text = "실시간 잠금을 위해 배터리 설정이 필요합니다.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "설정하기",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (needsOverlayRecovery || needsAccessibilityRecovery || needsServiceRecovery || needsBatteryRecovery) {
                Spacer(modifier = Modifier.height(12.dp))
                SleepProtectionRecoveryCard(
                    issue = when {
                        needsOverlayRecovery -> RecoveryIssue.OverlayPermission
                        needsAccessibilityRecovery -> RecoveryIssue.AccessibilityPermission
                        needsServiceRecovery -> RecoveryIssue.ServiceStopped
                        else -> RecoveryIssue.BatteryOptimization
                    },
                    onAction = {
                        when {
                            needsOverlayRecovery -> permissionRequester.requestPermission()
                            needsAccessibilityRecovery -> showAccessibilityDisclosure = true
                            needsServiceRecovery -> sleepServiceController.start()
                            needsBatteryRecovery -> showBatteryGuideDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                OrbitalSyncHub(
                    devices = devices,
                    onDeviceClick = { showDeviceSheet = true }
                )
            }

            BottomSwipeArea(
                sleepGoal = sleepGoal,
                onStartSleep = {
                    if (!notificationPermissionRequester.isGranted()) {
                        showNotificationRationale = true
                    } else if (!batteryPermissionRequester.isIgnoringBatteryOptimizations()) {
                        showForcedBatteryDialog = true
                    } else if (permissionRequester.isGranted() && accessibilityPermissionRequester.isGranted()) {
                        onStartSleep()
                        sleepServiceController.start()
                    } else if (!permissionRequester.isGranted()) {
                        permissionRequester.requestPermission()
                    } else {
                        showAccessibilityDisclosure = true
                    }
                }
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

@Composable
private fun getDeviceIcon(platform: String): DrawableResource {
    return when (platform.lowercase()) {
        "android" -> Res.drawable.ic_mobile
        "ios" -> Res.drawable.ic_mobile
        "tablet" -> Res.drawable.ic_tablet
        else -> Res.drawable.ic_mobile
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceBottomSheet(
    devices: List<DeviceState>,
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
        DeviceListContent(
            devices = devices,
            showHeader = true
        )
    }
}

@Composable
private fun OrbitalSyncHub(
    devices: List<DeviceState>,
    onDeviceClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val orbitRadius = 140.dp.value
    val deviceCount = devices.size
    val angleStep = if (deviceCount > 0) 360f / deviceCount else 0f

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
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(1.dp, Color(0xFF3BA5F5).copy(alpha = 0.15f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF3BA5F5).copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.character_phone),
                    contentDescription = "Sleep Sync Center",
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            devices.forEachIndexed { index, device ->
                val startAngle = index * angleStep
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

    val activeColor = MaterialTheme.colorScheme.primary
    val iconColor = if (isSynced) activeColor else Color.Gray

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSynced) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = pulseAlpha),
                                activeColor.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
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
    onStartSleep: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("이번 밤 목표: $sleepGoal", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
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

        LaunchedEffect(isSwiped) {
            if (isSwiped) {
                kotlinx.coroutines.delay(500)
                isSwiped = false
                offsetX = 0f
            }
        }

        Text(
            text = if (isSwiped) "수면 모드 활성화됨" else "밀어서 수면 모드 켜기",
            color = Color.White.copy(alpha = if (isSwiped) 1f else 0.4f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

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
                                offsetX = swipeAreaWidthPx
                                isSwiped = true
                                onSwipeComplete()
                            } else {
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
            sleepGoal = "8h 30m",
            isSleeping = false,
            devices = emptyList(),
            snackbarHostState = SnackbarHostState(),
            onStartSleep = {}
        )
    }
}

private enum class RecoveryIssue(
    val title: String,
    val message: String,
    val actionLabel: String
) {
    OverlayPermission(
        title = "수면 모드 보호가 중단되었어요",
        message = "다른 앱 위에 표시 권한이 꺼져 있어 차단 화면이 나타나지 않을 수 있어요.",
        actionLabel = "권한 다시 허용"
    ),
    AccessibilityPermission(
        title = "앱 복귀 차단 권한이 필요해요",
        message = "접근성 권한이 꺼져 있어 수면 중 다른 앱으로의 이동을 막지 못할 수 있어요.",
        actionLabel = "접근성 설정 열기"
    ),
    ServiceStopped(
        title = "수면 모드를 다시 복구해야 해요",
        message = "수면 모드는 켜져 있지만 보호 서비스가 중단되었어요. 다시 시작해서 차단을 복구하세요.",
        actionLabel = "수면 모드 다시 시작"
    ),
    BatteryOptimization(
        title = "실시간 보호 유지 설정이 필요해요",
        message = "배터리 최적화 예외가 없어 수면 중 보호가 중단될 수 있어요.",
        actionLabel = "배터리 설정 열기"
    )
}

@Composable
private fun SleepProtectionRecoveryCard(
    issue: RecoveryIssue,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = issue.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = issue.message,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(issue.actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
