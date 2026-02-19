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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import allsleep.composeapp.generated.resources.*
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.ui.stats.StatsScreen
import com.wngud.allsleep.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.sin

/**
 * AllSleep 메인 홈 화면
 * Stitch 디자인: 궤도 애니메이션 + 캐릭터 중심
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    initialTab: Int = 0,
    viewModel: com.wngud.allsleep.ui.home.HomeViewModel = org.koin.compose.viewmodel.koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 초기 탭 설정 (필요시)
    LaunchedEffect(initialTab) {
        if (state.selectedTab != initialTab) {
             viewModel.handleIntent(com.wngud.allsleep.ui.home.HomeIntent.SelectTab(initialTab))
        }
    }

    // 네비게이션 동기화: 현재 Route에 따라 탭 상태 업데이트
    LaunchedEffect(currentRoute) {
        val tabIndex = when (currentRoute) {
            Screen.Home.route -> 0
            Screen.Stats.route -> 1
            Screen.Alarm.route -> 2
            Screen.Settings.route -> 3
            else -> -1
        }
        if (tabIndex != -1 && tabIndex != state.selectedTab) {
            viewModel.handleIntent(com.wngud.allsleep.ui.home.HomeIntent.SelectTab(tabIndex))
        }
    }
    
    // 탭 선택 핸들러
    val onTabSelected: (Int) -> Unit = { index ->
        viewModel.handleIntent(com.wngud.allsleep.ui.home.HomeIntent.SelectTab(index))
        when (index) {
            0 -> if (currentRoute != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
            1 -> if (currentRoute != Screen.Stats.route) {
                navController.navigate(Screen.Stats.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
            2 -> if (currentRoute != Screen.Alarm.route) {
                navController.navigate(Screen.Alarm.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
            3 -> if (currentRoute != Screen.Settings.route) {
                navController.navigate(Screen.Settings.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    
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
                    selected = state.selectedTab == 0,
                    onClick = { onTabSelected(0) },
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
                    selected = state.selectedTab == 1,
                    onClick = { onTabSelected(1) },
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
                    selected = state.selectedTab == 2,
                    onClick = { onTabSelected(2) },
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
                    selected = state.selectedTab == 3,
                    onClick = { onTabSelected(3) },
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
        when (state.selectedTab) {
            0 -> HomeContent(modifier = Modifier.padding(paddingValues))
            1 -> StatsScreen(
                onBack = { navController.popBackStack() },
                modifier = Modifier.padding(paddingValues)
            )
            2 -> AlarmPlaceholder(modifier = Modifier.padding(paddingValues))
            3 -> SettingsPlaceholder(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
fun AlarmPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "알람 화면\n(준비 중)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "설정 화면\n(준비 중)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
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
