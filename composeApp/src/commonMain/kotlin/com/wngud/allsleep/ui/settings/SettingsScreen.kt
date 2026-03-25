package com.wngud.allsleep.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.platform.rememberAccessibilityPermissionRequester
import com.wngud.allsleep.platform.rememberNotificationPermissionRequester
import com.wngud.allsleep.ui.components.DeviceListContent
import com.wngud.allsleep.ui.components.TimePickerDialog
import com.wngud.allsleep.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * 설정 탭 화면
 *
 * Stitch 디자인 기준:
 * - 프로필 카드 (아바타, 이름, 이메일, 편집 버튼)
 * - 수면 설정 (취침/기상 시간, 알림, 방해 금지 모드)
 * - 앱 설정 (테마, 언어, 알람음)
 * - 계정 (연결된 기기, 데이터 동기화, 로그아웃, 계정 삭제)
 * - 하단 버전 + 약관
 */
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakeTimePicker by remember { mutableStateOf(false) }
    var deviceToRename by remember { mutableStateOf<DeviceState?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showDeviceSheet by remember { mutableStateOf(false) }

    // 접근성 서비스 상태 실시간 체크
    val accessibilityRequester = rememberAccessibilityPermissionRequester { /* 결과 무시, 재진입 시 isGranted()로 읽음 */ }
    // isGranted()는 Composable 외부(interface)이므로 LaunchedEffect로 주기적으로 동기화
    val isAccessibilityGranted = accessibilityRequester.isGranted()
    LaunchedEffect(isAccessibilityGranted) {
        viewModel.updateAccessibilityStatus(isAccessibilityGranted)
    }

    // 알림 권한 요청자
    val notificationRequester = rememberNotificationPermissionRequester { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    SettingsScreenContent(
        contentPadding = contentPadding,
        state = state,
        onIntent = { intent ->
            when (intent) {
                is SettingsIntent.ToggleNotification -> {
                    if (intent.enabled) {
                        notificationRequester.requestPermission()
                    } else {
                        viewModel.handleIntent(intent)
                    }
                }
                is SettingsIntent.OpenAccessibilitySettings -> {
                    accessibilityRequester.requestPermission()
                }
                is SettingsIntent.NavigateDeviceManagement -> {
                    showDeviceSheet = true
                }
                else -> viewModel.handleIntent(intent)
            }
        },
        onBedtimeClick = { showBedtimePicker = true },
        onWakeTimeClick = { showWakeTimePicker = true },
        onRenameDevice = { device ->
            deviceToRename = device
            renameText = device.deviceName
        }
    )

    if (showDeviceSheet) {
        DeviceManagementBottomSheet(
            devices = state.devices,
            onDismiss = { showDeviceSheet = false },
            onRenameClick = { device ->
                deviceToRename = device
                renameText = device.deviceName
            },
            onUnregisterClick = { device ->
                viewModel.handleIntent(SettingsIntent.UnregisterDevice(device))
            }
        )
    }

    if (showBedtimePicker) {
        val parts = state.bedtime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            title = "취침 시간 설정",
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.handleIntent(SettingsIntent.UpdateBedtime(time))
                showBedtimePicker = false
            },
            onDismiss = { showBedtimePicker = false }
        )
    }

    if (showWakeTimePicker) {
        val parts = state.wakeTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            title = "기상 시간 설정",
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.handleIntent(SettingsIntent.UpdateWakeTime(time))
                showWakeTimePicker = false
            },
            onDismiss = { showWakeTimePicker = false }
        )
    }

    // 기기 이름 변경 다이얼로그
    if (deviceToRename != null) {
        val maxChar = 20
        // [NEW] 중복 이름 체크 로직 추가 (state.devices 사용 및 변수명 명확화)
        val isInputBlank = renameText.isBlank()
        val isInputDuplicate = state.devices.any { it.deviceId != deviceToRename?.deviceId && it.deviceName == renameText.trim() }
        val isInputSameAsOld = renameText.trim() == deviceToRename?.deviceName
        
        AlertDialog(
            onDismissRequest = { deviceToRename = null },
            title = { Text("기기 이름 변경", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { if (it.length <= maxChar) renameText = it },
                        label = { Text("새 기기 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = isInputBlank || isInputDuplicate,
                        supportingText = {
                            if (isInputDuplicate) {
                                Text("이미 사용 중인 기기 이름입니다.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Text(
                        text = "${renameText.length}/$maxChar",
                        fontSize = 12.sp,
                        color = if (renameText.length >= maxChar) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deviceToRename?.let { device ->
                            viewModel.handleIntent(SettingsIntent.RenameDevice(device, renameText.trim()))
                        }
                        deviceToRename = null
                    },
                    enabled = !isInputBlank && !isInputDuplicate && !isInputSameAsOld && renameText.length <= maxChar
                ) { Text("저장", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deviceToRename = null }) { Text("취소") }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsScreenContent(
    contentPadding: PaddingValues,
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBedtimeClick: () -> Unit,
    onWakeTimeClick: () -> Unit,
    onRenameDevice: (DeviceState) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // 헤더
        Text(
            text = "설정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // 프리미엄 상태/업그레이드 카드
        if (state.isPremium) {
            PremiumActiveCard(
                onManageClick = { onIntent(SettingsIntent.ManageSubscription) }
            )
        } else {
            PremiumUpgradeCard(
                onUpgradeClick = { onIntent(SettingsIntent.UpgradePremium) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 보안
        SettingsSection(title = "보안") {
            SettingsRowArrow(
                emoji = "🔑",
                label = "접근성 권한",
                trailing = if (state.isAccessibilityEnabled) "On" else "Off",
                onClick = { onIntent(SettingsIntent.OpenAccessibilitySettings) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 수면 설정
        SettingsSection(title = "수면 설정") {
            SettingsRowArrow(
                emoji = "🌙",
                label = "취침 시간 설정",
                trailing = state.bedtime,
                onClick = onBedtimeClick
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "☀️",
                label = "기상 시간 설정",
                trailing = state.wakeTime,
                onClick = onWakeTimeClick
            )
            SettingsDivider()
            SettingsRowToggle(
                emoji = "🔔",
                label = "알림 설정",
                checked = state.isNotificationEnabled,
                onCheckedChange = { onIntent(SettingsIntent.ToggleNotification(it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 계정
        SettingsSection(title = "계정") {
            SettingsRowArrow(
                emoji = "📱",
                label = "연결된 기기 관리",
                onClick = { onIntent(SettingsIntent.NavigateDeviceManagement) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "📤",
                label = "로그아웃",
                onClick = { onIntent(SettingsIntent.ShowLogoutDialog) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "⚠️",
                label = "계정 삭제",
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { onIntent(SettingsIntent.ShowDeleteAccountDialog) }
            )
        }

        // 하단 버전 정보
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "이용 약관",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "개인정보 처리 방침",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // 로그아웃 확인 다이얼로그
    if (state.showLogoutDialog) {
        ConfirmDialog(
            title = "로그아웃",
            message = "정말 로그아웃하시겠습니까?",
            confirmText = "로그아웃",
            isDestructive = false,
            onConfirm = { onIntent(SettingsIntent.ConfirmLogout) },
            onDismiss = { onIntent(SettingsIntent.DismissDialog) }
        )
    }

    // 계정 삭제 확인 다이얼로그
    if (state.showDeleteAccountDialog) {
        ConfirmDialog(
            title = "계정 삭제",
            message = "계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 정말 삭제하시겠습니까?",
            confirmText = "삭제",
            isDestructive = true,
            onConfirm = { onIntent(SettingsIntent.ConfirmDeleteAccount) },
            onDismiss = { onIntent(SettingsIntent.DismissDialog) }
        )
    }

    // 로딩 인디케이터 (계정 삭제 중)
    if (state.isLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // 오류 발생 시 스낵바 처리 (또는 단순 AlertDialog)
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { /* 오류 메시지 닫기 액션 (viewModel 연동 필요 시 추가) */ },
            title = { Text("오류") },
            text = { Text(state.error) },
            confirmButton = {
                TextButton(onClick = { /* 오류 무시 */ }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
private fun PremiumUpgradeCard(
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("PREMIUM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "AllSleep Premium",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        val features = listOf("고급 수면 분석", "기기 무제한 동기화", "전용 수면 사운드")
        features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Text(feature, color = Color.White, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onUpgradeClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("지금 업그레이드", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PremiumActiveCard(
    onManageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4938FF), Color(0xFFA855F7))
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💎", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PREMIUM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AllSleep Premium 활성",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "구독 갱신: 2025년 4월 1일",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            
            OutlinedButton(
                onClick = onManageClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("구독 관리", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun SettingsRowArrow(
    emoji: String,
    label: String,
    trailing: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun SettingsRowToggle(
    emoji: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SettingsScreenContent(
                contentPadding = PaddingValues(),
                state = SettingsState(isPremium = true),
                onIntent = {},
                onBedtimeClick = {},
                onWakeTimeClick = {}
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceManagementBottomSheet(
    devices: List<DeviceState>,
    onDismiss: () -> Unit,
    onRenameClick: (DeviceState) -> Unit,
    onUnregisterClick: (DeviceState) -> Unit
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
        Box(modifier = Modifier.padding(bottom = 32.dp)) {
            DeviceListContent(
                devices = devices,
                showHeader = true,
                onRenameClick = onRenameClick,
                onUnregisterClick = onUnregisterClick
            )
        }
    }
}
