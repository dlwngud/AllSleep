package com.wngud.allsleep.ui.subscription

import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.character_phone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
private const val SUPPORT_EMAIL = "official.allsleep@gmail.com"
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
                            tint = Color.White.copy(alpha = 0.6f)
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
            ManageHeader(
                isActive = status?.isPremiumActive == true,
                planName = planName,
                status = status
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Primary)
            } else {
                if (error != null) {
                    ErrorState(
                        message = error!!,
                        onRetry = { scope.launch { refreshState() } }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                SubscriptionStatusCard(
                    status = status,
                    planName = planName,
                    currentPackage = activePackage
                )

                if (status?.billingIssueDetectedAtMillis != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    WarningCard(
                        title = "결제 정보 확인이 필요해요",
                        message = "스토어 결제 수단에 문제가 있으면 자동 갱신이 잠시 보류될 수 있습니다."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ValueReminderSection()

                Spacer(modifier = Modifier.height(20.dp))

                ManageActionSection(
                    onRestoreClick = {
                        scope.launch {
                            billingProvider.restorePurchases()
                                .onSuccess {
                                    snackbarHostState.showSnackbar("구매 내역이 복원되었습니다.")
                                    refreshState()
                                }
                                .onFailure { e ->
                                    snackbarHostState.showSnackbar(e.message ?: "복원에 실패했습니다.")
                                }
                        }
                    },
                    onManageClick = {
                        openUriSafely(
                            uriHandler,
                            status?.managementUrl ?: PLAY_SUBSCRIPTIONS_URL
                        )
                    },
                    onHistoryClick = {
                        openUriSafely(uriHandler, PLAY_ORDER_HISTORY_URL)
                    },
                    onFaqClick = {
                        openUriSafely(uriHandler, FAQ_URL)
                    },
                    onSupportClick = {
                        openUriSafely(
                            uriHandler,
                            "mailto:$SUPPORT_EMAIL?subject=%5BAllSleep%20%EA%B5%AC%EB%8F%85%20%EB%AC%B8%EC%9D%98%5D"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                ManageFooter(
                    onTermsClick = { openUriSafely(uriHandler, TERMS_URL) },
                    onPrivacyClick = { openUriSafely(uriHandler, PRIVACY_URL) },
                    onFaqClick = { openUriSafely(uriHandler, FAQ_URL) },
                    onHistoryClick = { openUriSafely(uriHandler, PLAY_ORDER_HISTORY_URL) }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ManageHeader(
    isActive: Boolean,
    planName: String,
    status: SubscriptionStatus?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Primary,
                    spotColor = Primary
                )
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
            text = if (isActive) "구독 중 (Active)" else "구독 상태 확인",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isActive) {
                "현재 플랜과 다음 청구 정보를 바로 확인하고 관리할 수 있습니다."
            } else {
                "현재 구독 상태를 불러오는 중입니다."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = SurfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(999.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(
                    text = if (isActive) "활성" else "비활성",
                    backgroundColor = if (isActive) Primary else Color(0xFF334155)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = planName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        if (status?.willRenew != null) {
            Spacer(modifier = Modifier.height(10.dp))
            StatusChip(
                text = if (status.willRenew == true) "자동 갱신 켜짐" else "자동 갱신 꺼짐",
                backgroundColor = if (status.willRenew == true) Color(0xFF16A34A) else Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun SubscriptionStatusCard(
    status: SubscriptionStatus?,
    planName: String,
    currentPackage: SubscriptionPackage?
) {
    val renewalLabel = when {
        status?.isPremiumActive == true && status.willRenew == true -> "다음 청구일"
        status?.isPremiumActive == true && status.willRenew == false -> "구독 만료일"
        else -> "만료일"
    }

    val renewalValue = status?.expirationDateMillis?.let(::formatKoreanDate) ?: "-"
    val billingLabel = when {
        status?.isPremiumActive == true && status.willRenew == true -> "자동 갱신으로 유지 중"
        status?.isPremiumActive == true && status.willRenew == false -> "만료일까지 이용 가능"
        else -> "구독 정보가 비활성화되어 있습니다"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Text(
            text = "구독 상태",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (status?.isPremiumActive == true) "구독 중 (Active)" else "구독 대기",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = Primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = planName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = billingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetricGrid(
            firstLabel = "자동 갱신",
            firstValue = if (status?.willRenew == true) "켜짐" else "꺼짐",
            secondLabel = renewalLabel,
            secondValue = renewalValue,
            thirdLabel = "가격 (세금 포함)",
            thirdValue = currentPackage?.priceString ?: "스토어 표시 금액",
            fourthLabel = "결제 스토어",
            fourthValue = status?.store ?: "Google Play"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subscriptionDetailText(status),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun MetricGrid(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    thirdLabel: String,
    thirdValue: String,
    fourthLabel: String,
    fourthValue: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(modifier = Modifier.weight(1f), label = firstLabel, value = firstValue)
            MetricTile(modifier = Modifier.weight(1f), label = secondLabel, value = secondValue)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(modifier = Modifier.weight(1f), label = thirdLabel, value = thirdValue)
            MetricTile(modifier = Modifier.weight(1f), label = fourthLabel, value = fourthValue)
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun WarningCard(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF3A1D20))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ValueReminderSection() {
    val benefits = listOf(
        "무제한 사용 경험" to "광고나 제한 없이 프리미엄 기능을 계속 사용할 수 있어요.",
        "기기 변경 복원" to "새 기기나 재설치 후에도 구매 복원으로 빠르게 이어갈 수 있어요.",
        "자동 갱신의 투명성" to "다음 청구일과 만료일을 미리 확인해서 불안감을 줄일 수 있어요.",
        "스토어 중심 관리" to "해지, 플랜 변경, 결제 수단 관리는 스토어에서 바로 처리합니다."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Text(
            text = "프리미엄 혜택",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        benefits.forEach { (title, desc) ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
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
private fun ManageActionSection(
    onRestoreClick: () -> Unit,
    onManageClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFaqClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariant.copy(alpha = 0.45f))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "구독 관리",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "해지와 플랜 변경은 스토어에서, 복원과 정보 확인은 앱에서 빠르게 처리하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(18.dp))

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
            SecondaryActionButton(
                text = "구매 복원",
                modifier = Modifier.weight(1f),
                onClick = onRestoreClick
            )
            SecondaryActionButton(
                text = "결제 내역",
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                text = "FAQ",
                modifier = Modifier.weight(1f),
                onClick = onFaqClick
            )
            SecondaryActionButton(
                text = "고객센터",
                modifier = Modifier.weight(1f),
                onClick = onSupportClick
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "변경/취소 후에도 다음 청구일까지는 계속 이용할 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
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
private fun ManageFooter(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onFaqClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "구독 정보는 언제든 확인할 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterLink("이용약관", onTermsClick)
            FooterDivider()
            FooterLink("개인정보처리방침", onPrivacyClick)
            FooterDivider()
            FooterLink("FAQ", onFaqClick)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterLink("구독 이력", onHistoryClick)
        }
    }
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

private fun subscriptionDetailText(status: SubscriptionStatus?): String {
    return when {
        status == null -> "현재 구독 정보를 확인할 수 없습니다."
        status.isPremiumActive && status.willRenew == true -> {
            val date = status.expirationDateMillis?.let(::formatKoreanDate) ?: "-"
            "다음 청구일은 ${date}이며 자동 갱신이 켜져 있습니다."
        }
        status.isPremiumActive && status.willRenew == false -> {
            val date = status.expirationDateMillis?.let(::formatKoreanDate) ?: "-"
            "자동 갱신은 꺼져 있지만 ${date}까지는 계속 이용할 수 있습니다."
        }
        else -> "활성 구독이 없으면 구매 화면에서 다시 시작할 수 있습니다."
    }
}

private fun resolveActivePackage(
    status: SubscriptionStatus?,
    packages: List<SubscriptionPackage>
): SubscriptionPackage? {
    if (status?.isPremiumActive != true) {
        return null
    }

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
    if (!planIdentifier.isNullOrBlank()) {
        return planIdentifier
    }

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
