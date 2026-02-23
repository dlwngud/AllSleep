package com.wngud.allsleep.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

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

        // 프로필 카드
        ProfileCard(
            userName = state.userName,
            userEmail = state.userEmail,
            onEditClick = { viewModel.handleIntent(SettingsIntent.EditProfile) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 수면 설정
        SettingsSection(title = "수면 설정") {
            SettingsRowArrow(
                emoji = "🌙",
                label = "취침 시간 설정",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateSleepTime) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "☀️",
                label = "기상 시간 설정",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateWakeTime) }
            )
            SettingsDivider()
            SettingsRowToggle(
                emoji = "🔔",
                label = "알림 설정",
                checked = state.isNotificationEnabled,
                onCheckedChange = { viewModel.handleIntent(SettingsIntent.ToggleNotification(it)) }
            )
            SettingsDivider()
            SettingsRowToggle(
                emoji = "🌙",
                label = "방해 금지 모드",
                checked = state.isDndEnabled,
                onCheckedChange = { viewModel.handleIntent(SettingsIntent.ToggleDnd(it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 앱 설정
        SettingsSection(title = "앱 설정") {
            SettingsRowArrow(
                emoji = "🎨",
                label = "테마 설정",
                trailing = when (state.appTheme) {
                    AppTheme.DARK -> "다크"
                    AppTheme.LIGHT -> "라이트"
                    AppTheme.SYSTEM -> "시스템"
                },
                onClick = { /* TODO: 테마 선택 다이얼로그 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "🌐",
                label = "언어 설정",
                trailing = when (state.appLanguage) {
                    AppLanguage.KOREAN -> "한국어"
                    AppLanguage.ENGLISH -> "English"
                },
                onClick = { /* TODO: 언어 선택 다이얼로그 */ }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "🔔",
                label = "알람 변환",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateAlarmSound) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 계정
        SettingsSection(title = "계정") {
            SettingsRowArrow(
                emoji = "📱",
                label = "연결된 기기 관리",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateDeviceManagement) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "💾",
                label = "데이터 동기화",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateDataSync) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "📤",
                label = "로그아웃",
                onClick = { viewModel.handleIntent(SettingsIntent.ShowLogoutDialog) }
            )
            SettingsDivider()
            SettingsRowArrow(
                emoji = "⚠️",
                label = "계정 삭제",
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { viewModel.handleIntent(SettingsIntent.ShowDeleteAccountDialog) }
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
            onConfirm = { viewModel.handleIntent(SettingsIntent.ConfirmLogout) },
            onDismiss = { viewModel.handleIntent(SettingsIntent.DismissDialog) }
        )
    }

    // 계정 삭제 확인 다이얼로그
    if (state.showDeleteAccountDialog) {
        ConfirmDialog(
            title = "계정 삭제",
            message = "계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 정말 삭제하시겠습니까?",
            confirmText = "삭제",
            isDestructive = true,
            onConfirm = { viewModel.handleIntent(SettingsIntent.ConfirmDeleteAccount) },
            onDismiss = { viewModel.handleIntent(SettingsIntent.DismissDialog) }
        )
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    userEmail: String,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 아바타
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "👤", fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = userEmail,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        ) {
            Text(text = "프로필 편집", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingsSection(
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
