package com.wngud.allsleep.platform

import androidx.compose.runtime.Composable

interface PermissionRequester {
    fun requestBasicPermissions()
}

@Composable
expect fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester
