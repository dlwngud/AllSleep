package com.wngud.allsleep.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * 알림 권한이 필요한 이유를 설명하고 설정을 유도하는 다이얼로그
 */
@Composable
fun NotificationRationaleDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { 
            Text(
                text = "알림 권한 안내", 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = { 
            Text(
                text = "수면 모드 진입 시 시스템에 의해 앱이 종료되는 것을 방지하고, 다중 기기 간 동기화를 실시간으로 유지하기 위해 알림 권한이 반드시 필요합니다.\n\n설정 화면으로 이동하여 알림을 허용해 주세요.",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text("설정으로 이동", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("나중에 하기")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
