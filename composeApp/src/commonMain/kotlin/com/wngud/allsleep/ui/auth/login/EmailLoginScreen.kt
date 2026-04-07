package com.wngud.allsleep.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.ui.theme.*
import com.wngud.allsleep.domain.model.AuthProvider
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$") }
    val isEmailValid = state.email.isEmpty() || emailRegex.matches(state.email)

    LaunchedEffect(state.user) {
        if (state.user != null) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.handleIntent(AuthIntent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = FontSize.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(Spacing.large))

            // 헤더
            Text(
                text = "로그인",
                fontSize = FontSize.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = "이메일과 비밀번호를 입력하세요",
                fontSize = FontSize.bodyMedium,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

            // 이메일 입력
            Text(
                text = "이메일",
                fontSize = FontSize.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdateEmail(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("example@email.com", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                leadingIcon = { Text("✉\uFE0F", modifier = Modifier.padding(start = 8.dp)) },
                isError = !isEmailValid,
                shape = RoundedCornerShape(CornerRadius.medium),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            
            if (!isEmailValid) {
                Text(
                    text = "올바른 이메일 형식을 입력해 주세요.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = FontSize.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 비밀번호 입력
            Text(
                text = "비밀번호",
                fontSize = FontSize.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdatePassword(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("비밀번호를 입력하세요", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                leadingIcon = { Text("🔒", modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "👁\uFE0F" else "🙈")
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(CornerRadius.medium),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

            // 로그인 버튼
            val isLoading = state.loadingProvider == AuthProvider.EMAIL
            Button(
                onClick = { viewModel.handleIntent(AuthIntent.LoginWithEmail) },
                modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !isLoading && state.email.isNotBlank() && state.password.isNotBlank() && isEmailValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("로그인", fontSize = FontSize.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 회원가입 유도
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "계정이 없으신가요? ",
                    fontSize = FontSize.bodyMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    text = "회원가입",
                    fontSize = FontSize.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToSignup() }
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
