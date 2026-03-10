package com.wngud.allsleep.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockerSettingsScreen(
    viewModel: AppBlockerViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "차단 앱 관리", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            "‹", 
                            fontSize = 32.sp,
                            color = Color.White,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0C10),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B0C10)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 프리미엄 안내 문구 (옵션)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131821))
                    .padding(16.dp)
            ) {
                Text(
                    "선택한 앱은 수면 모드가 활성화되었을 때 실행이 차단되며, 실행 시 자동으로 홈 화면으로 이동합니다.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // 검색바
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.handleIntent(AppBlockerIntent.UpdateSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("앱 이름 또는 패키지명 검색", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 12.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF131821),
                    unfocusedContainerColor = Color(0xFF131821),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF1C2431)
                ),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // 앱 개수 표시
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val blockedCount = state.apps.count { it.isBlocked }
                Text(
                    text = "차단 중: ${blockedCount}개",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "총 ${state.apps.size}개의 앱",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val filteredApps = state.apps.filter {
                    it.label.contains(state.searchQuery, ignoreCase = true) || 
                    it.packageName.contains(state.searchQuery, ignoreCase = true)
                }

                if (filteredApps.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없습니다.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppItemView(
                                app = app,
                                onToggle = { isBlocked ->
                                    viewModel.handleIntent(AppBlockerIntent.ToggleAppBlock(app.packageName, isBlocked))
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFF1C2431)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemView(
    app: com.wngud.allsleep.domain.model.AppInfo,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 렌더링
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF1C2431), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBytes != null) {
                AppIconImage(app.iconBytes)
            } else {
                Text(
                    app.label.take(1), 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label, 
                color = Color.White, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = app.packageName, 
                color = Color.White.copy(alpha = 0.4f), 
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        Switch(
            checked = app.isBlocked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1C2431)
            )
        )
    }
}

@Composable
fun AppIconImage(bytes: ByteArray) {
    val bitmap = rememberBitmapFromBytes(bytes)
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF252D3A)),
            contentAlignment = Alignment.Center
        ) {
            Text("📱", fontSize = 20.sp)
        }
    }
}

// Modifier 확장 함수 (Switch 크기 조절용)
private fun Modifier.customScale(scale: Float): Modifier = this.then(
    Modifier.scale(scale)
)
