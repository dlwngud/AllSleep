package com.wngud.allsleep.ui.auth.login

import androidx.compose.runtime.Composable

@Composable
expect fun GlobalLoginScreen(
    title: String? = null,
    subtitle: String? = null,
    onLoginSuccess: () -> Unit,
    onEmailLogin: () -> Unit
)
