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

@Composable
actual fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 7. 사용자의 응답(승인/거부) 확인
        onResult(isGranted)
    }

    return remember(context) {
        object : PermissionRequester {
            override fun requestBasicPermissions() {
                // API 33 이상에서만 런타임 알림 권한 필요
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    // 4. 권한이 필요한 작업을 실행할 때마다 권한이 이미 있는지 확인 (플로우 핵심)
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        // 사용자가 이미 권한을 부여했다면 즉시 승인 처리
                        onResult(true)
                    } else {
                        // 5 & 6. 권한 요청 (OnboardingPermissionsScreen 자체가 이미 교육용 Rationale UI 역할을 수행하고 있음)
                        permissionLauncher.launch(permission)
                    }
                } else {
                    // API 32 이하는 알림 권한이 설치 시 자동 부여됨
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
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                return powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            override fun requestIgnoreBatteryOptimizations() {
                // Method 2: 모든 앱의 배터리 최적화 목록으로 이동하는 방식 (Play Store 심사 안전)
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}
