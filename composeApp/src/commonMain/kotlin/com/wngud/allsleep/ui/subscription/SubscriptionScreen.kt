package com.wngud.allsleep.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.platform.PlatformContext
import com.wngud.allsleep.platform.SubscriptionPackage
import com.wngud.allsleep.ui.theme.Primary
import com.wngud.allsleep.ui.theme.Surface
import com.wngud.allsleep.ui.theme.SurfaceVariant
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.character_phone
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current as? Any as? PlatformContext
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

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
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Surface
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Section
                PremiumHeader()
                
                Spacer(modifier = Modifier.height(32.dp))

                // 2. Benefit List
                BenefitSection()

                Spacer(modifier = Modifier.height(40.dp))

                // 3. Plan Selection Section
                if (state.isLoading) {
                    CircularProgressIndicator(color = Primary)
                } else if (state.packages.isEmpty() && state.error != null) {
                    ErrorState(message = state.error!!, onRetry = {
                        viewModel.handleIntent(SubscriptionContract.Intent.LoadPackages)
                    })
                } else {
                    PlanGrid(
                        packages = state.packages,
                        selectedId = state.selectedPackageId,
                        onSelect = { viewModel.handleIntent(SubscriptionContract.Intent.SelectPackage(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 4. Action Button
                val selectedPkg = state.packages.find { it.id == state.selectedPackageId }
                PrimaryActionButton(
                    isPurchasing = state.isPurchasing,
                    selectedPkg = selectedPkg,
                    onClick = { viewModel.handleIntent(SubscriptionContract.Intent.PurchaseSelected, context) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Footer Section
                SubscriptionFooter(
                    showRenewalText = selectedPkg?.type == com.wngud.allsleep.platform.PackageType.MONTHLY || 
                                     selectedPkg?.type == com.wngud.allsleep.platform.PackageType.ANNUAL,
                    onTermsClick = { uriHandler.openUri("https://www.notion.so/AllSleep-33892d66363680bb8c2de90e9a7cc4e2") }, // 예시 URL
                    onPrivacyClick = { uriHandler.openUri("https://www.notion.so/AllSleep-33892d66363680bb8c2de90e9a7cc4e2") },
                    onRestoreClick = { viewModel.handleIntent(SubscriptionContract.Intent.RestorePurchases) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Error Dialog
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
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = Primary, spotColor = Primary)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.character_phone),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "AllSleep Premium",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "더 깊고 건강한 잠을 위한 올슬립의 모든 기능을 만나보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BenefitSection() {
    val benefits = listOf(
        "정밀 수면 지표 분석" to "AI가 분석하는 고도화된 수면 지표 확인",
        "광고 없는 쾌적함" to "방해 없는 수면 모드와 통계 대시보드",
        "멀티 디바이스 동기화" to "모든 기기에서 끊김 없는 수면 기록 관리",
        "프리미엄 전용 리포트" to "맞춤형 건강 인사이트와 주간/월간 분석"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        benefits.forEach { (title, desc) ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun PlanGrid(
    packages: List<SubscriptionPackage>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    // 3개의 플랜을 상단 연간권을 크게, 나머지를 아래에 배치하거나 균등 배치
    // 이미지에서는 2개가 나란히 있었으므로, 여기서는 3개를 상시 비교 가능하게 Column으로 배치하되 세련되게 디자인
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        packages.forEach { pkg ->
            PlanCard(
                pkg = pkg,
                isSelected = selectedId == pkg.id,
                onClick = { onSelect(pkg.id) }
            )
        }
    }
}

@Composable
fun PlanCard(
    pkg: SubscriptionPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Primary else Color.White.copy(alpha = 0.1f)
    val backgroundColor = if (isSelected) Primary.copy(alpha = 0.08f) else SurfaceVariant.copy(alpha = 0.4f)
    val radioColor = if (isSelected) Primary else Color.White.copy(alpha = 0.3f)

    // 할인율 계산 (연간권 34%, 이미지 참고)
    val discountPercent = when (pkg.type) {
        com.wngud.allsleep.platform.PackageType.ANNUAL -> "34% OFF"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp) // 배지가 튀어나올 공간 확보
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Radio Icon
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = radioColor
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                // 2. Info Section
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (pkg.type) {
                                com.wngud.allsleep.platform.PackageType.MONTHLY -> "월간 이용권"
                                com.wngud.allsleep.platform.PackageType.ANNUAL -> "연간 이용권"
                                com.wngud.allsleep.platform.PackageType.LIFETIME -> "평생 이용권"
                                else -> pkg.title
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        pkg.badge?.let {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Primary,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = it,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = when (pkg.type) {
                            com.wngud.allsleep.platform.PackageType.LIFETIME -> "영구 소장 (단 한 번 결제)"
                            else -> if (pkg.hasFreeTrial) "${pkg.freeTrialDays}일 무료 체험 후 결제" else pkg.subDescription ?: ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // 3. Price Section
                Text(
                    text = pkg.priceString,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // 4. Discount Badge (Top-Right Offset)
        discountPercent?.let {
            Surface(
                color = Primary,
                shape = RoundedCornerShape(20.dp), // 카드와 동일한 20.dp 적용
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-10).dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = it,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    isPurchasing: Boolean,
    selectedPkg: SubscriptionPackage?,
    onClick: () -> Unit
) {
    val enabled = selectedPkg != null
    val buttonText = when (selectedPkg?.type) {
        com.wngud.allsleep.platform.PackageType.MONTHLY -> "7일 무료 후 월 결제"
        com.wngud.allsleep.platform.PackageType.ANNUAL -> "7일 무료 후 연 결제"
        com.wngud.allsleep.platform.PackageType.LIFETIME -> "지금 바로 시작하기"
        else -> "프리미엄 시작하기"
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = Primary.copy(alpha = 0.3f)
        ),
        enabled = !isPurchasing && enabled
    ) {
        if (isPurchasing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubscriptionFooter(
    showRenewalText: Boolean,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showRenewalText) {
            Text(
                text = "구독은 자동 갱신됩니다. 언제든 취소 가능",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterLink("이용약관", onTermsClick)
            FooterDivider()
            FooterLink("개인정보처리방침", onPrivacyClick)
            FooterDivider()
            FooterLink("구독 복원", onRestoreClick)
        }
    }
}

@Composable
fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.4f),
        textDecoration = TextDecoration.Underline
    )
}

@Composable
fun FooterDivider() {
    Box(modifier = Modifier.size(1.dp, 10.dp).background(Color.White.copy(alpha = 0.2f)))
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = Color.Red, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text("다시 시도", color = Primary)
        }
    }
}
