package com.wngud.allsleep.ui.subscription

import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.character_phone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wngud.allsleep.platform.BillingProvider
import com.wngud.allsleep.platform.PackageType
import com.wngud.allsleep.platform.SubscriptionPackage
import com.wngud.allsleep.platform.SubscriptionStatus
import com.wngud.allsleep.ui.theme.Primary
import com.wngud.allsleep.ui.theme.Surface as AppSurface
import com.wngud.allsleep.ui.theme.SurfaceVariant
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private const val TERMS_URL = "https://www.notion.so/AllSleep-33892d66363680faadc6e53cd5016e35"
private const val PRIVACY_URL = "https://www.notion.so/AllSleep-33892d66363680bb8c2de90e9a7cc4e2"
private const val FAQ_URL = "https://www.notion.so/AllSleep-FAQ-33892d663636805481d6ec75e097676c"
private const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
private const val PLAY_ORDER_HISTORY_URL = "https://play.google.com/store/account/orderhistory"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManageScreen(
    onBack: () -> Unit,
    billingProvider: BillingProvider = koinInject()
) {
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<SubscriptionStatus?>(null) }
    var packages by remember { mutableStateOf<List<SubscriptionPackage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refreshState() {
        isLoading = true
        error = null

        val statusResult = billingProvider.getSubscriptionStatus()
        val packagesResult = runCatching { billingProvider.getOfferings() }

        status = statusResult.getOrNull()
        packages = packagesResult.getOrElse { emptyList() }

        error = when {
            statusResult.isFailure && packagesResult.isFailure -> {
                statusResult.exceptionOrNull()?.message
                    ?: packagesResult.exceptionOrNull()?.message
                    ?: "구독 상태를 불러오지 못했습니다."
            }
            statusResult.isFailure -> statusResult.exceptionOrNull()?.message ?: "구독 상태를 불러오지 못했습니다."
            packagesResult.isFailure -> packagesResult.exceptionOrNull()?.message ?: "요금 정보를 불러오지 못했습니다."
            else -> null
        }

        isLoading = false
    }

    LaunchedEffect(Unit) {
        refreshState()
    }

    val activePackage = remember(status, packages) {
        resolveActivePackage(status, packages)
    }

    val planName = remember(status, activePackage) {
        resolvePlanName(status, activePackage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("구독 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color.White.copy(alpha = 0.65f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppSurface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ManageHero(
                isActive = status?.isPremiumActive == true
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Primary)
            } else {
                error?.let {
                    ManageErrorState(
                        message = it,
                        onRetry = { scope.launch { refreshState() } }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SubscriptionSummaryCard(
                    status = status,
                    currentPackage = activePackage
                )

                Spacer(modifier = Modifier.height(16.dp))

                PremiumBenefitSection()

                Spacer(modifier = Modifier.height(16.dp))

                ManageActionCard(
                    onManageClick = {
                        openUriSafely(uriHandler, status?.managementUrl ?: PLAY_SUBSCRIPTIONS_URL)
                    },
                    onRestoreClick = {
                        scope.launch {
                            billingProvider.restorePurchases()
                                .onSuccess {
                                    snackbarHostState.showSnackbar("구매 내역을 복원했어요.")
                                    refreshState()
                                }
                                .onFailure { e ->
                                    snackbarHostState.showSnackbar(e.message ?: "복원에 실패했어요.")
                                }
                        }
                    },
                    onHistoryClick = { openUriSafely(uriHandler, PLAY_ORDER_HISTORY_URL) },
                    onFaqClick = { openUriSafely(uriHandler, FAQ_URL) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                FooterLinks(
                    onTermsClick = { openUriSafely(uriHandler, TERMS_URL) },
                    onPrivacyClick = { openUriSafely(uriHandler, PRIVACY_URL) }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ManageHero(
    isActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Primary,
                    spotColor = Primary
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.34f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.character_phone),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
        }
    }
}

@Composable
private fun SubscriptionSummaryCard(
    status: SubscriptionStatus?,
    currentPackage: SubscriptionPackage?
) {
    val dateLabel = when {
        status?.isPremiumActive == true && status.willRenew == true -> "다음 청구일"
        status?.isPremiumActive == true && status.willRenew == false -> "만료일"
        else -> "구독 정보"
    }

    val dateValue = status?.expirationDateMillis?.let(::formatKoreanDate)
        ?: if (status?.isPremiumActive == true) "정보 없음" else "구독 없음"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "구독 상태",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            StatusChip(
                text = if (status?.isPremiumActive == true) "활성" else "비활성",
                backgroundColor = if (status?.isPremiumActive == true) Primary else Color(0xFF475569)
            )
            if (status?.isPremiumActive == true) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(
                    text = if (status.willRenew == true) "자동갱신 켜짐" else "자동갱신 꺼짐",
                    backgroundColor = if (status.willRenew == true) Color(0xFF16A34A) else Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SummaryLine(label = dateLabel, value = dateValue)
        SummaryLine(label = "가격 (세금 포함)", value = currentPackage?.priceString ?: "스토어 표시 금액")
        SummaryLine(label = "결제 스토어", value = status?.store ?: "Google Play")
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun PremiumBenefitSection() {
    val benefits = listOf(
        "무제한 기기 동기화" to "기기 등록 제한 없이 여러 기기를 연결할 수 있어요.",
        "주간/월간 패턴 추적" to "수면과 기상 패턴을 더 정밀하게 볼 수 있어요.",
        "AI 개인 맞춤 인사이트" to "수면 습관을 바탕으로 개인화된 분석을 확인할 수 있어요.",
        "통계 탭 전체 해제" to "잠겨 있던 고급 분석 데이터를 모두 볼 수 있어요."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = "프리미엄 혜택",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        benefits.forEach { (title, desc) ->
            Row(
                modifier = Modifier.padding(vertical = 7.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.48f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageActionCard(
    onManageClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFaqClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.45f))
            .padding(20.dp)
    ) {
        Text(
            text = "구독 관리",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onManageClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
            )
        ) {
            Text("스토어에서 관리하기", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = "구매 복원",
                onClick = onRestoreClick
            )
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = "결제 내역",
                onClick = onHistoryClick
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "FAQ 보기",
            onClick = onFaqClick
        )
    }
}

@Composable
private fun SecondaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.06f),
            contentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FooterLinks(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ManageFooterLink(text = "이용약관", onClick = onTermsClick)
        ManageFooterDivider()
        ManageFooterLink(text = "개인정보처리방침", onClick = onPrivacyClick)
    }
}

@Composable
private fun ManageFooterLink(
    text: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ManageFooterDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 10.dp)
            .background(Color.White.copy(alpha = 0.18f))
    )
}

@Composable
private fun StatusChip(
    text: String,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ManageErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text("다시 시도", color = Primary)
        }
    }
}

private fun resolveActivePackage(
    status: SubscriptionStatus?,
    packages: List<SubscriptionPackage>
): SubscriptionPackage? {
    if (status?.isPremiumActive != true) return null

    val productId = status.productIdentifier
    val planIdentifier = status.productPlanIdentifier?.lowercase()

    packages.firstOrNull { pkg ->
        pkg.productId.isNotBlank() && pkg.productId == productId
    }?.let { return it }

    packages.firstOrNull { pkg ->
        pkg.id == productId
    }?.let { return it }

    if (planIdentifier != null) {
        when {
            planIdentifier.contains("annual") -> packages.firstOrNull { it.type == PackageType.ANNUAL }
            planIdentifier.contains("month") -> packages.firstOrNull { it.type == PackageType.MONTHLY }
            planIdentifier.contains("life") -> packages.firstOrNull { it.type == PackageType.LIFETIME }
            else -> null
        }?.let { return it }
    }

    return packages.firstOrNull()
}

private fun resolvePlanName(
    status: SubscriptionStatus?,
    currentPackage: SubscriptionPackage?
): String {
    currentPackage?.let { pkg ->
        return when (pkg.type) {
            PackageType.MONTHLY -> "Premium Monthly"
            PackageType.ANNUAL -> "Premium Annual"
            PackageType.LIFETIME -> "Premium Lifetime"
            PackageType.UNKNOWN -> pkg.title.ifBlank { "프리미엄" }
        }
    }

    val planIdentifier = status?.productPlanIdentifier
    if (!planIdentifier.isNullOrBlank()) return planIdentifier

    val productIdentifier = status?.productIdentifier
    return when {
        productIdentifier?.contains("annual", ignoreCase = true) == true -> "Premium Annual"
        productIdentifier?.contains("monthly", ignoreCase = true) == true -> "Premium Monthly"
        productIdentifier?.contains("lifetime", ignoreCase = true) == true -> "Premium Lifetime"
        status?.isPremiumActive == true -> "프리미엄"
        else -> "구독 없음"
    }
}

private fun formatKoreanDate(millis: Long): String {
    val local = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}년 ${local.monthNumber}월 ${local.dayOfMonth}일"
}

private fun openUriSafely(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    uri: String
) {
    runCatching { uriHandler.openUri(uri) }
}
