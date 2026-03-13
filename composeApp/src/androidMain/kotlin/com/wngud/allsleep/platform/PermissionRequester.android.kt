package com.wngud.allsleep.platform

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import android.content.Context
import android.os.PowerManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

@Composable
actual fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return remember(context) {
        object : PermissionRequester, DefaultLifecycleObserver {
            private val _isBatteryOptimized = MutableStateFlow(isIgnoringBatteryOptimizationsInternal())
            override val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

            init {
                // 앱 전체 프로세스의 수명 주기를 관찰하여 ON_RESUME 시점에 상태 갱신
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            }

            override fun onResume(owner: LifecycleOwner) {
                _isBatteryOptimized.value = isIgnoringBatteryOptimizationsInternal()
            }

            private fun isIgnoringBatteryOptimizationsInternal(): Boolean {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                return powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            override fun requestBasicPermissions() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        onResult(true)
                    } else {
                        permissionLauncher.launch(permission)
                    }
                } else {
                    onResult(true)
                }
            }

            override fun requestAccessibilityPermission() {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            override fun isIgnoringBatteryOptimizations(): Boolean {
                return _isBatteryOptimized.value
            }

            override fun requestIgnoreBatteryOptimizations() {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}
