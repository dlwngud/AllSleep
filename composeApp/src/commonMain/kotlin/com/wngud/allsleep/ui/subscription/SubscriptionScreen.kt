package com.wngud.allsleep.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.SubscriptionPackage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current as? Any as? PlatformContext
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
        viewModel.effect.collect { effect ->
            when (effect) {
                is SubscriptionContract.Effect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SubscriptionContract.Effect.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AllSleep 프리미엄") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1115)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumHeader()
                Spacer(modifier = Modifier.height(32.dp))
                BenefitList()
                Spacer(modifier = Modifier.height(40.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                } else {
                    state.packages.forEach { pkg ->
                        PackageCard(
                            pkg = pkg,
                            isSelected = state.selectedPackageId == pkg.id,
                            onClick = { viewModel.handleIntent(SubscriptionContract.Intent.SelectPackage(pkg.id)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.handleIntent(SubscriptionContract.Intent.PurchaseSelected, context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    enabled = !state.isPurchasing && state.selectedPackageId != null
                ) {
                    if (state.isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "프리미엄 시작하기",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "구매 복원",
                        modifier = Modifier.clickable { viewModel.handleIntent(SubscriptionContract.Intent.RestorePurchases) },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textDecoration = TextDecoration.Underline
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }

            state.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.handleIntent(SubscriptionContract.Intent.DismissError) },
                    title = { Text("알림") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.handleIntent(SubscriptionContract.Intent.DismissError) }) {
                            Text("확인")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PremiumHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFFD700)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AllSleep Premium",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "최상의 수면 환경을 위해 지금 시작하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
fun BenefitList() {
    val benefits = listOf(
        "제한 기능 무제한 사용",
        "광고 없는 쾌적한 환경",
        "기기 등록 대수 제한 없음",
        "프리미엄 통계 분석 제공"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E2128))
            .padding(24.dp)
    ) {
        benefits.forEach { benefit ->
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = benefit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PackageCard(
    pkg: SubscriptionPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2E323D)
    val backgroundColor = if (isSelected) Color(0xFF252932) else Color(0xFF1E2128)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = pkg.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (pkg.hasFreeTrial) {
                    Text(
                        text = "${pkg.freeTrialDays}일 무료 체험",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            Text(
                text = pkg.priceString,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}
