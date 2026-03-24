package com.wngud.allsleep.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wngud.allsleep.service.AppSupervisorService

@Composable
actual fun rememberAccessibilityPermissionRequester(onResult: (Boolean) -> Unit): AccessibilityPermissionRequester {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onResult(isAccessibilityServiceEnabled(context))
    }

    return remember(context) {
        object : AccessibilityPermissionRequester {
            override fun isGranted(): Boolean {
                return isAccessibilityServiceEnabled(context)
            }

            override fun requestPermission() {
                if (!isGranted()) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    permissionLauncher.launch(intent)
                } else {
                    onResult(true)
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, AppSupervisorService::class.java).flattenToString()
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServicesSetting?.contains(expectedComponentName) == true
}
