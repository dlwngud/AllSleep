package com.wngud.allsleep.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import allsleep.composeapp.generated.resources.*
import com.wngud.allsleep.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.sin

/**
 * AllSleep 메인 홈 화면
 * Stitch 디자인: 궤도 애니메이션 + 캐릭터 중심
 */
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                // 홈
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(Res.drawable.ic_home),
                            contentDescription = "홈"
                        )
                    },
                    label = { Text("홈") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // 통계
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(Res.drawable.ic_analytics),
                            contentDescription = "통계"
                        )
                    },
                    label = { Text("통계") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // 알람
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(Res.drawable.ic_alarm),
                            contentDescription = "알람"
                        )
                    },
                    label = { Text("알람") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                // 설정
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(Res.drawable.ic_more),
                            contentDescription = "설정"
                        )
                    },
                    label = { Text("설정") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeContent(Modifier.padding(paddingValues))
            1 -> StatsPlaceholder(Modifier.padding(paddingValues))
            2 -> AlarmPlaceholder(Modifier.padding(paddingValues))
            3 -> SettingsPlaceholder(Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 중앙 궤도 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            OrbitalHub()
        }
        
        // 하단 액션 영역
        BottomActionArea()
    }
}

@Composable
private fun StatsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "통계 화면\n(준비 중)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AlarmPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "알람 화면\n(준비 중)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "설정 화면\n(준비 중)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OrbitalHub() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // 각 기기의 회전 애니메이션
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 480f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 궤도 시스템
        Box(
            modifier = Modifier
                .size(340.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 중앙 캐릭터
            Box(
                modifier = Modifier
                    .size(192.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 빛 효과
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                
                // 캐릭터 이미지
                Image(
                    painter = painterResource(Res.drawable.character_cloud),
                    contentDescription = "Sleep Character",
                    modifier = Modifier
                        .size(160.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            // 궤도 반경
            val orbitRadius = 140.dp.value
            
            // 기기 1: 노트북 (상단)
            DeviceIcon(
                icon = "💻",
                rotation = rotation1,
                orbitRadius = orbitRadius
            )
            
            // 기기 2: 태블릿 (우하단)
            DeviceIcon(
                icon = "📱",
                rotation = rotation2,
                orbitRadius = orbitRadius
            )
            
            // 기기 3: 스마트폰 (좌하단)
            DeviceIcon(
                icon = "📱",
                rotation = rotation3,
                orbitRadius = orbitRadius
            )
        }
        
        // 상태 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "연결된 기기: 3대",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "모든 기기가 수면 모드 준비 완료",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DeviceIcon(
    icon: String,
    rotation: Float,
    orbitRadius: Float
) {
    val radians = Math.toRadians(rotation.toDouble())
    val x = (orbitRadius * cos(radians)).dp
    val y = (orbitRadius * sin(radians)).dp
    
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(48.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                CircleShape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.alpha(0.8f)
        )
    }
}

@Composable
private fun BottomActionArea() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // START SLEEP 버튼
        Button(
            onClick = { /* TODO: 수면 시작 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "START SLEEP",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        // 유리 효과 카드
        GlassCard()
    }
}

@Composable
private fun GlassCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🌙", fontSize = 20.sp)
            }
            
            Column {
                Text(
                    text = "TONIGHT'S GOAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "8h 30m Sleep",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Text(
            text = "›",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}
