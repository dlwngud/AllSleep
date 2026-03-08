package com.wngud.allsleep.platform

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberOverlayPermissionRequester(onResult: (Boolean) -> Unit): OverlayPermissionRequester {
    val context = LocalContext.current

    // ACTION_MANAGE_OVERLAY_PERMISSION 설정 창에서 돌아왔을 때 결과를 수신하는 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // 설정 화면에서 돌아왔을 때 실제 권한 승인 여부 재점검
        onResult(Settings.canDrawOverlays(context))
    }

    return remember(context) {
        object : OverlayPermissionRequester {
            override fun isGranted(): Boolean {
                return Settings.canDrawOverlays(context)
            }

            override fun requestPermission() {
                if (!isGranted()) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    permissionLauncher.launch(intent)
                } else {
                    onResult(true)
                }
            }
        }
    }
}
