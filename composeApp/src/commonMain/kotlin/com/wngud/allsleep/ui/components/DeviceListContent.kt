package com.wngud.allsleep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.ui.theme.IndicatorSynced
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.ic_mobile
import allsleep.composeapp.generated.resources.ic_more
import allsleep.composeapp.generated.resources.ic_tablet
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun DeviceListContent(
    devices: List<DeviceState>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onRenameClick: ((DeviceState) -> Unit)? = null,
    onUnregisterClick: ((DeviceState) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
    ) {
        if (showHeader) {
            Text(
                text = "동기화된 기기",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
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
                
                Column(modifier = Modifier.weight(1f)) {
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

                // [NEW] 이름 변경 버튼
                if (onRenameClick != null) {
                    IconButton(
                        onClick = { onRenameClick.invoke(device) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_more),
                            contentDescription = "Rename",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
