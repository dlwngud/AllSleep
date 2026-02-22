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
import allsleep.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.sin


/**
 * 홈 탭 화면
 * 홈 탭 고유의 UI만 담당합니다.
 * 탭 전환 로직은 BottomNavScaffold(NavController)가 담당합니다.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = org.koin.compose.viewmodel.koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            OrbitalHub()
        }
        BottomActionArea(
            sleepGoal = state.sleepGoal,
            onStartSleep = { viewModel.handleIntent(HomeIntent.StartSleep) }
        )
    }
}

/** 알람 탭 — 준비 중 */
@Composable
fun AlarmPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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

/** 설정 탭 — 준비 중 */
@Composable
fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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

    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart)
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart)
    )
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Box(
            modifier = Modifier.size(340.dp).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(192.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                )
                Image(
                    painter = painterResource(Res.drawable.character_cloud),
                    contentDescription = "Sleep Character",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
            }
            val orbitRadius = 140.dp.value
            DeviceIcon("💻", rotation1, orbitRadius)
            DeviceIcon("📱", rotation2, orbitRadius)
            DeviceIcon("📱", rotation3, orbitRadius)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("연결된 기기: 3대", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("모든 기기가 수면 모드 준비 완료", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun DeviceIcon(icon: String, rotation: Float, orbitRadius: Float) {
    val radians = Math.toRadians(rotation.toDouble())
    val x = (orbitRadius * cos(radians)).dp
    val y = (orbitRadius * sin(radians)).dp

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 24.sp, modifier = Modifier.alpha(0.8f))
    }
}

@Composable
private fun BottomActionArea(sleepGoal: String, onStartSleep: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Button(
            onClick = onStartSleep,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("START SLEEP", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

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
                    Text("TONIGHT'S GOAL", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp)
                    Text(sleepGoal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
