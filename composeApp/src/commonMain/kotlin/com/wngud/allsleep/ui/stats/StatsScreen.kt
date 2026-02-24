package com.wngud.allsleep.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StatsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: StatsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    StatsScreenContent(
        contentPadding = contentPadding,
        state = state,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun StatsScreenContent(
    contentPadding: PaddingValues,
    state: StatsState,
    onIntent: (StatsIntent) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent // 투명으로 설정하여 배경 일관성 유지
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding) // 외부(네비게이션 바) 패딩 적용
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            PeriodSelector(
                selectedIndex = state.timePeriodIndex,
                onSelect = { onIntent(StatsIntent.SelectTimePeriod(it)) }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            KeyMetricsRow()
            
            Spacer(modifier = Modifier.height(20.dp))
            WeeklySleepChart()
            
            Spacer(modifier = Modifier.height(20.dp))
            QualityTrendChart()
            
            Spacer(modifier = Modifier.height(20.dp))
            SleepStageDonutChart()
            
            Spacer(modifier = Modifier.height(20.dp))
            AIInsightCard()
        }
    }
}


@Composable
private fun PeriodSelector(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val periods = listOf("주", "월", "년", "전체")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEachIndexed { index, period ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    color = if (selectedIndex == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = "💤",
            value = "7.5h",
            label = "평균 수면"
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = "⭐",
            value = "85",
            label = "품질 점수"
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = "🔥",
            value = "12일",
            label = "연속 달성"
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklySleepChart() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "주간 수면 시간",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val sleepData = listOf(6.5f, 7.8f, 8.2f, 7.0f, 6.8f, 9.0f, 8.5f)
            val days = listOf("월", "화", "수", "목", "금", "토", "일")
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = Color(0xFF6B5FFF)
            
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = 32.dp.toPx()
                val spacing = (canvasWidth - (barWidth * 7)) / 6
                val maxSleep = 12f
                
                // 목표선 (8h)
                val targetY = canvasHeight - (8f / maxSleep) * canvasHeight
                drawLine(
                    color = Color(0xFFF59E0B),
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    strokeWidth = 1.dp.toPx()
                )
                
                // 막대 그리기
                sleepData.forEachIndexed { index, hour ->
                    val barHeight = (hour / maxSleep) * canvasHeight
                    val x = index * (barWidth + spacing)
                    val y = canvasHeight - barHeight
                    
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(primaryColor, secondaryColor)),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityTrendChart() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "수면 품질 트렌드",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val qualityData = listOf(70f, 85f, 90f, 75f, 65f, 95f, 88f)
            val primaryColor = MaterialTheme.colorScheme.primary
            
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val spacing = canvasWidth / 6
                
                val path = Path()
                qualityData.forEachIndexed { index, quality ->
                    val x = index * spacing
                    val y = canvasHeight - (quality / 100f) * canvasHeight
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        // 곡선 처리를 위한 단순 로직 (실제로는 cubicTo 권장)
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                // 포인트 그리기
                qualityData.forEachIndexed { index, quality ->
                    val x = index * spacing
                    val y = canvasHeight - (quality / 100f) * canvasHeight
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepStageDonutChart() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "수면 단계 분석",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 24.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        
                        // 데이터 (깊은, 얕은, REM, 깨어있음)
                        val angles = listOf(108f, 162f, 72f, 18f)
                        val colors = listOf(Color(0xFF4938FF), Color(0xFF6B5FFF), Color(0xFF3B82F6), Color(0xFF94A3B8))
                        
                        var startAngle = -90f
                        angles.forEachIndexed { index, angle ->
                            drawArc(
                                color = colors[index],
                                startAngle = startAngle,
                                sweepAngle = angle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(diameter, diameter),
                                topLeft = Offset((size.width - diameter)/2, (size.height - diameter)/2)
                            )
                            startAngle += angle
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("7h 32m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("총 수면", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(Color(0xFF4938FF), "깊은 수면", "30%")
                    LegendItem(Color(0xFF6B5FFF), "얕은 수면", "45%")
                    LegendItem(Color(0xFF3B82F6), "REM 수면", "20%")
                    LegendItem(Color(0xFF94A3B8), "깨어있음", "5%")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, percent: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = percent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AIInsightCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF4938FF), Color(0xFF6B5FFF))))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "이번 주 인사이트",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "수면 품질이 지난주보다 15% 향상됐어요!\n일관된 취침 시간이 큰 도움이 된 것 같아요.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "자세히 보기 →",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview
@Composable
fun StatsScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            StatsScreenContent(
                contentPadding = PaddingValues(),
                state = StatsState(),
                onIntent = {}
            )
        }
    }
}
