package com.wngud.allsleep.platform

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

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

    override var isShowing: Boolean = false
        private set

    override fun showOverlay() {
        if (isShowing) return
        
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0C10)), // HomeScreen 다크 네이비와 동일
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = currentTime,
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "수면 모드가 켜져있습니다",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(120.dp))

                        // 잠금 해제 스와이프 버튼
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
                                    .background(Color(0xFFE53935), CircleShape) // 빨간색(해제) 버튼
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (offsetX > swipeAreaWidthPx * 0.8f) {
                                                    // 끝까지 밀면 서비스 종료!
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
