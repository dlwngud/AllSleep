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

        // 프리미엄 멤버십 카드 (새 개편 디자인: 프로필과 동일 높이)
        PremiumMembershipCard(
            isPremium = state.isPremium,
            onActionClick = {
                if (state.isPremium) onIntent(SettingsIntent.ManageSubscription)
                else onIntent(SettingsIntent.UpgradePremium)
            }
        )

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

        // 고객 지원
        SettingsSection(title = "고객 지원") {
            SettingsRowArrow(
                emoji = "❓",
                label = "자주 묻는 질문",
                onClick = { /* TODO: FAQ 링크 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "⭐",
                label = "앱 리뷰 남기기",
                onClick = { /* TODO: 스토어 이동 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "✉️",
                label = "의견 보내기",
                onClick = { /* TODO: 이메일 전송 */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 서비스 정보
        SettingsSection(title = "서비스 정보") {
            SettingsRowArrow(
                emoji = "📜",
                label = "이용 약관",
                onClick = { /* TODO: 약관 뷰어 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "🛡️",
                label = "개인정보 처리방침",
                onClick = { /* TODO: 방침 뷰어 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "📱",
                label = "버전 정보",
                trailing = "v1.1.0",
                onClick = { }
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

        // 하단 여백
        Spacer(modifier = Modifier.height(32.dp))
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
private fun PremiumMembershipCard(
    isPremium: Boolean,
    onActionClick: () -> Unit
) {
    val title = if (isPremium) "연간 구독 중" else "Premium으로 업그레이드"
    val subtitle = if (isPremium) "2026.04.10 갱신 예정" else "무제한 기기 동기화 및 전용 혜택"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4938FF), Color(0xFF8B5CF6))
                )
            )
            .clickable(onClick = onActionClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측 다이아몬드 아이콘
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💎", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 중앙 텍스트 (한 줄씩 유지)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 우측 화살표 버튼 (박스 형태)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("›", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
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
