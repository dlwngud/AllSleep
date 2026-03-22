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

            private val _isAccessibilityEnabled = MutableStateFlow(isAccessibilityServiceEnabledInternal())
            override val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

            private val _isAlarmPermissionGranted = MutableStateFlow(isAlarmPermissionGrantedInternal())
            override val isAlarmPermissionGranted: StateFlow<Boolean> = _isAlarmPermissionGranted.asStateFlow()

            private val _isNotificationPermissionGranted = MutableStateFlow(isNotificationPermissionGrantedInternal())
            override val isNotificationPermissionGranted: StateFlow<Boolean> = _isNotificationPermissionGranted.asStateFlow()

            init {
                // 앱 전체 프로세스의 수명 주기를 관찰하여 ON_RESUME 시점에 상태 갱신
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            }

            override fun onResume(owner: LifecycleOwner) {
                _isBatteryOptimized.value = isIgnoringBatteryOptimizationsInternal()
                _isAccessibilityEnabled.value = isAccessibilityServiceEnabledInternal()
                _isAlarmPermissionGranted.value = isAlarmPermissionGrantedInternal()
                _isNotificationPermissionGranted.value = isNotificationPermissionGrantedInternal()
            }

            private fun isIgnoringBatteryOptimizationsInternal(): Boolean {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                return powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            private fun isAccessibilityServiceEnabledInternal(): Boolean {
                val expectedService = "${context.packageName}/com.wngud.allsleep.service.AppSupervisorService"
                val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                return enabledServices?.contains(expectedService) == true
            }

            private fun isAlarmPermissionGrantedInternal(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            }

            private fun isNotificationPermissionGrantedInternal(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
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

            override fun requestAlarmPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}
