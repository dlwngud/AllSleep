package com.wngud.allsleep.ui.auth.login

import androidx.compose.runtime.Composable

@Composable
expect fun GlobalLoginScreen(
    onLoginSuccess: () -> Unit
)
