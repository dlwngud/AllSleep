package com.wngud.allsleep.platform

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.*
import androidx.compose.ui.layout.ContentScale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.lifecycle.*
import androidx.savedstate.*
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import allsleep.composeapp.generated.resources.Res
import allsleep.composeapp.generated.resources.ic_mobile
import allsleep.composeapp.generated.resources.ic_tablet
import allsleep.composeapp.generated.resources.charachter_no_phone

class WindowLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class LockOverlayManagerImpl(private val context: Context) : LockOverlayManager {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: WindowLifecycleOwner? = null

    private val _devices = mutableStateOf<List<com.wngud.allsleep.domain.model.DeviceState>>(emptyList())

    override var isShowing: Boolean = false
        private set

    override fun showOverlay(devices: List<com.wngud.allsleep.domain.model.DeviceState>) {
        if (isShowing) return
        _devices.value = devices
        
        lifecycleOwner = WindowLifecycleOwner()
        
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            // 내비게이션 바 및 상태 바 숨기기 (Immersive Mode 설정)
            systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            
            setContent {
                val currentDevices by _devices
                var currentTime by remember { mutableStateOf("") }
                
                LaunchedEffect(Unit) {
                    while(true) {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val hour = now.hour.toString().padStart(2, '0')
                        val min = now.minute.toString().padStart(2, '0')
                        currentTime = "$hour:$min"
                        delay(1000)
                    }
                }

                // HomeScreen과 완전히 동일한 레이아웃 구조 적용
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0C10)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단: 시간 정보
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = currentTime,
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "수면 모드가 켜져있습니다",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // 중앙: HomeScreen의 OrbitalSyncHub와 동일한 구조 (weight(1f)로 영역 확보)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(40.dp)
                            ) {
                                // HomeScreen.kt의 OrbitalSyncHub와 동일: size(340.dp).padding(24.dp)
                                Box(
                                    modifier = Modifier.size(340.dp).padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 0.8f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(2000, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )

                                    // 궤도선 (280dp) - HomeScreen과 동일
                                    Box(
                                        modifier = Modifier
                                            .size(280.dp)
                                            .border(1.dp, Color(0xFF3BA5F5).copy(alpha = 0.15f), CircleShape)
                                    )

                                    // 중앙 허브 (280dp) - HomeScreen과 동일
                                    Box(
                                        modifier = Modifier
                                            .size(280.dp)
                                            .background(Color(0xFF3BA5F5).copy(alpha = 0.05f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 눈 감은 캐릭터 (200dp) - HomeScreen의 character_phone과 동일 크기
                                        androidx.compose.foundation.Image(
                                            painter = org.jetbrains.compose.resources.painterResource(allsleep.composeapp.generated.resources.Res.drawable.charachter_no_phone),
                                            contentDescription = null,
                                            modifier = Modifier.size(200.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }

                                    // 위성 기기들 - orbitRadius 140dp로 HomeScreen과 동일
                                    val deviceCount = currentDevices.size
                                    val angleStep = if (deviceCount > 0) 360f / deviceCount else 0f
                                    val orbitRadius = 140.dp.value

                                    currentDevices.forEachIndexed { index, device ->
                                        val startAngle = index * angleStep
                                        val rotation by infiniteTransition.animateFloat(
                                            initialValue = startAngle,
                                            targetValue = startAngle + 360f,
                                            animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart)
                                        )

                                        val radians = rotation * Math.PI / 180.0
                                        val x = (orbitRadius * Math.cos(radians)).dp
                                        val y = (orbitRadius * Math.sin(radians)).dp

                                        Box(
                                            modifier = Modifier.offset(x = x, y = y).size(56.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .background(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                                            )
                                                        ),
                                                        shape = CircleShape
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    painter = org.jetbrains.compose.resources.painterResource(
                                                        when (device.platform.lowercase()) {
                                                            "android" -> allsleep.composeapp.generated.resources.Res.drawable.ic_mobile
                                                            "ios" -> allsleep.composeapp.generated.resources.Res.drawable.ic_mobile
                                                            "tablet" -> allsleep.composeapp.generated.resources.Res.drawable.ic_tablet
                                                            else -> allsleep.composeapp.generated.resources.Res.drawable.ic_mobile
                                                        }
                                                    ),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 홈 화면의 "Sync Hub Control Station" 텍스트 영역 매칭
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Sleep Mode Protected",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "현재 기기가 안전하게 보호되고 있습니다",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // 하단: HomeScreen의 BottomSwipeArea와 동일한 padding
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .background(Color(0xFF131A26), RoundedCornerShape(32.dp))
                                    .border(1.dp, Color(0xFF1E2633), RoundedCornerShape(32.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val width = this.maxWidth
                                val thumbSize = 56.dp
                                val swipeAreaWidthPx = with(LocalDensity.current) { (width - thumbSize).toPx() }
                                var offsetX by remember { mutableStateOf(0f) }

                                Text(
                                    text = "밀어서 잠금 해제",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                                        .padding(4.dp)
                                        .size(48.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    if (offsetX > swipeAreaWidthPx * 0.8f) {
                                                        com.wngud.allsleep.service.SleepLockService.stop(context)
                                                    } else {
                                                        offsetX = 0f
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                offsetX = (offsetX + dragAmount).coerceIn(0f, swipeAreaWidthPx)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "›",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.offset(y = (-2).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_NOT_TOUCH_MODAL 과 FLAG_NOT_FOCUSABLE을 제거하여 터치와 키 이벤트를 모두 가로챕니다. (앱 블로커 역할)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(composeView, params)
            isShowing = true
            android.util.Log.d("LockOverlay", "Overlay ADDED successfully")
        } catch (e: Exception) {
            android.util.Log.e("LockOverlay", "Failed to add view to WindowManager: ${e.message}", e)
        }
    }

    override fun updateDevices(devices: List<com.wngud.allsleep.domain.model.DeviceState>) {
        _devices.value = devices
    }

    override fun hideOverlay() {
        if (!isShowing) return
        try {
            composeView?.let { windowManager.removeView(it) }
            lifecycleOwner?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            composeView = null
            lifecycleOwner = null
            isShowing = false
        }
    }
}

@Composable
actual fun rememberLockOverlayManager(): LockOverlayManager {
    val context = LocalContext.current.applicationContext
    return remember(context) { LockOverlayManagerImpl(context) }
}
