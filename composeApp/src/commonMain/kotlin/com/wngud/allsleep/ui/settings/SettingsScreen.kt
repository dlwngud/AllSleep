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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.platform.rememberAccessibilityPermissionRequester
import com.wngud.allsleep.platform.rememberNotificationPermissionRequester
import com.wngud.allsleep.ui.components.DeviceListContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * 설정 탭 화면
 * 개편: 수면 설정 섹션에서 평일/주말 루틴 요약을 보여주고 알람 탭으로 유도함.
 */
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController,
    onNavigateToSubscription: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var deviceToRename by remember { mutableStateOf<DeviceState?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showDeviceSheet by remember { mutableStateOf(false) }

    // 접근성 서비스 상태 실시간 체크
    val accessibilityRequester = rememberAccessibilityPermissionRequester { }
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
                is SettingsIntent.UpgradePremium -> {
                    onNavigateToSubscription()
                }
                is SettingsIntent.ManageSubscription -> {
                    onNavigateToSubscription()
                }
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
        onRoutineClick = {
            // 알람 탭으로 이동 (스케줄 관리 전용 화면)
            navController.navigate(Screen.Alarm.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
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
                viewModel.handleIntent(SettingsIntent.ShowUnregisterDialog(device))
            }
        )
    }

    // 기기 이름 변경 다이얼로그
    if (deviceToRename != null) {
        val maxChar = 20
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

    // 프로필 이름 수정 다이얼로그
    val currentUser = state.user
    if (state.showEditNameDialog && currentUser != null) {
        var newName by remember { mutableStateOf(currentUser.displayName ?: "") }
        val maxChar = 20
        
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(SettingsIntent.DismissDialog) },
            title = { Text("프로필 이름 수정", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= maxChar) newName = it },
                        label = { Text("새 프로필 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = newName.isBlank()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(SettingsIntent.UpdateDisplayName(newName))
                    },
                    enabled = newName.isNotBlank() && (newName != currentUser.displayName) && (newName.length <= maxChar)
                ) { Text("저장", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(SettingsIntent.DismissDialog) }) { Text("취소") }
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
    onRoutineClick: () -> Unit
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

        // 프로필 카드 (최상단)
        state.user?.let { user ->
            ProfileCard(
                user = user,
                onEditClick = { onIntent(SettingsIntent.ShowEditNameDialog) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 프리미엄 상태/업그레이드 카드
        if (state.isPremium) {
            PremiumActiveCard(onManageClick = { onIntent(SettingsIntent.ManageSubscription) })
        } else {
            PremiumUpgradeCard(onUpgradeClick = { onIntent(SettingsIntent.UpgradePremium) })
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

        // 수면 설정 개편: 루틴 요약 행
        SettingsSection(title = "수면 루틴") {
            SettingsRowArrow(
                emoji = "🏃",
                label = "평일 루틴 (월-금)",
                trailing = "${state.weekdayBedtime} ~ ${state.weekdayWakeTime}",
                onClick = onRoutineClick
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "🛋️",
                label = "주말 루틴 (토-일)",
                trailing = "${state.weekendBedtime} ~ ${state.weekendWakeTime}",
                onClick = onRoutineClick
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
                text = "v1.1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // 다이얼로그들 (생략 가능하면 생략하거나 유지)
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

    if (state.showUnregisterDialog && state.deviceToUnregister != null) {
        ConfirmDialog(
            title = "기기 등록 해제",
            message = "'${state.deviceToUnregister.deviceName}' 기기를 등록 해제하시겠습니까?",
            confirmText = "해제",
            isDestructive = true,
            onConfirm = { onIntent(SettingsIntent.ConfirmUnregisterDevice) },
            onDismiss = { onIntent(SettingsIntent.DismissDialog) }
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("오류") },
            text = { Text(state.error) },
            confirmButton = {
                TextButton(onClick = { }) { Text("확인") }
            }
        )
    }
}

@Composable
private fun ProfileCard(user: User, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            val initial = user.displayName?.firstOrNull()?.toString() ?: "?"
            Text(initial, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName ?: "사용자", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(user.email ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        IconButton(onClick = onEditClick) {
            Text("편집", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PremiumUpgradeCard(onUpgradeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
            .padding(20.dp)
    ) {
        Text("AllSleep Premium", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onUpgradeClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("지금 업그레이드", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumActiveCard(onManageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF4938FF), Color(0xFFA855F7))))
            .padding(20.dp)
    ) {
        Text("Premium 활성", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsRowArrow(emoji: String, label: String, trailing: String? = null, labelColor: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(text = label, fontSize = 15.sp, color = labelColor, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(end = 4.dp))
        }
        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

@Composable
private fun SettingsRowToggle(emoji: String, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmText: String, isDestructive: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceManagementBottomSheet(devices: List<DeviceState>, onDismiss: () -> Unit, onRenameClick: (DeviceState) -> Unit, onUnregisterClick: (DeviceState) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131821)
    ) {
        DeviceListContent(devices = devices, showHeader = true, onRenameClick = onRenameClick, onUnregisterClick = onUnregisterClick)
    }
}
