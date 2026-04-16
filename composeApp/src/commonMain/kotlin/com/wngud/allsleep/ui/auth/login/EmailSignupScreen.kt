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
fun EmailSignupScreen(
    onBack: () -> Unit,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val emailRegex = remember { Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$") }
    val isEmailValid = state.email.isEmpty() || emailRegex.matches(state.email)

    LaunchedEffect(state.user) {
        if (state.user != null) {
            onSignupSuccess()
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
                text = "회원가입",
                fontSize = FontSize.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = "새 계정을 만들어보세요",
                fontSize = FontSize.bodyMedium,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

            // 이름 입력
            AuthInputField(
                label = "이름",
                value = state.name,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdateName(it)) },
                placeholder = "이름을 입력하세요",
                leadingIcon = "👤"
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            // 이메일 입력
            AuthInputField(
                label = "이메일",
                value = state.email,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdateEmail(it)) },
                placeholder = "example@email.com",
                leadingIcon = "✉\uFE0F",
                isError = !isEmailValid
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
            AuthInputField(
                label = "비밀번호",
                value = state.password,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdatePassword(it)) },
                placeholder = "비밀번호를 입력하세요 (6자 이상)",
                leadingIcon = "🔒",
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onVisibilityToggle = { isPasswordVisible = !isPasswordVisible }
            )

            // 비밀번호 확인 입력
            var isConfirmPasswordVisible by remember { mutableStateOf(false) }
            AuthInputField(
                label = "비밀번호 확인",
                value = state.confirmPassword,
                onValueChange = { viewModel.handleIntent(AuthIntent.UpdateConfirmPassword(it)) },
                placeholder = "비밀번호를 다시 입력하세요",
                leadingIcon = "🔒",
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                isError = state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword
            )
            
            if (state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword) {
                Text(
                    text = "비밀번호가 일치하지 않습니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = FontSize.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 이용약관 동의
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이용약관",
                        fontSize = FontSize.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable { 
                            uriHandler.openUri("https://www.notion.so/AllSleep-33892d66363680faadc6e53cd5016e35")
                        }
                    )
                    Text(
                        text = " 및 ",
                        fontSize = FontSize.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "개인정보 처리방침",
                        fontSize = FontSize.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable { 
                            uriHandler.openUri("https://www.notion.so/AllSleep-33892d66363680bb8c2de90e9a7cc4e2")
                        }
                    )
                    Text(
                        text = " 동의",
                        fontSize = FontSize.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.extraExtraLarge))

            // 가입 버튼
            val isLoading = state.loadingProvider == AuthProvider.EMAIL
            Button(
                onClick = { viewModel.handleIntent(AuthIntent.SignUpWithEmail) },
                modifier = Modifier.fillMaxWidth().height(ButtonSize.heightMedium),
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !isLoading && state.name.isNotBlank() && state.email.isNotBlank() && 
                          isEmailValid && state.password.length >= 6 && 
                          state.password == state.confirmPassword && agreedToTerms
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("회원가입", fontSize = FontSize.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // 로그인 유도
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "이미 계정이 있으신가요? ",
                    fontSize = FontSize.bodyMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    text = "로그인",
                    fontSize = FontSize.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToLogin() }
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AuthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: String,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityToggle: (() -> Unit)? = null,
    isError: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = FontSize.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = { Text(leadingIcon, modifier = Modifier.padding(start = 12.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onVisibilityToggle ?: {}) {
                        Text(if (isPasswordVisible) "👁\uFE0F" else "🙈")
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(CornerRadius.medium),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLeadingIconColor = MaterialTheme.colorScheme.error
            )
        )
    }
}
