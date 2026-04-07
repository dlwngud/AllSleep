package com.wngud.allsleep.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.usecase.auth.LoginWithGoogleUseCase
import com.wngud.allsleep.domain.usecase.auth.LoginWithKakaoUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.platform.PlatformContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 인증 ViewModel (MVI, commonMain)
 * PlatformContext = Activity — Google 로그인에 전달
 */
class AuthViewModel(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.LoginWithGoogle -> loginWithGoogle(intent.context)
            is AuthIntent.LoginWithKakao -> loginWithKakao()
            is AuthIntent.DismissError -> _state.update { it.copy(error = null) }
            
            // 이메일 로그인 관련
            is AuthIntent.UpdateEmail -> _state.update { it.copy(email = intent.email) }
            is AuthIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is AuthIntent.UpdateName -> _state.update { it.copy(name = intent.name) }
            is AuthIntent.ToggleAuthMode -> _state.update { it.copy(isSignUpMode = !it.isSignUpMode) }
            is AuthIntent.LoginWithEmail -> loginWithEmail()
            is AuthIntent.SignUpWithEmail -> signUpWithEmail()
        }
    }

    private fun loginWithEmail() {
        // TODO: 향후 실제 UseCase 연동
        println("AuthViewModel [DEBUG] 이메일 로그인 시도: ${_state.value.email}")
    }

    private fun signUpWithEmail() {
        // TODO: 향후 실제 UseCase 연동
        println("AuthViewModel [DEBUG] 이메일 회원가입 시도: ${_state.value.email}, 이름: ${_state.value.name}")
    }

    private fun loginWithKakao() {
        viewModelScope.launch {
            println("AuthViewModel [DEBUG] 카카오 로그인 시작")
            _state.update { it.copy(loadingProvider = AuthProvider.KAKAO, error = null) }
            loginWithKakaoUseCase()
                .onSuccess { user ->
                    println("AuthViewModel [DEBUG] 카카오 로그인 성공 uid=${user.uid}")
                    _state.update { it.copy(loadingProvider = null, user = user) }
                }
                .onFailure { error ->
                    println("AuthViewModel [DEBUG] 카카오 로그인 실패: ${error.message}")
                    _state.update { it.copy(loadingProvider = null, error = error.message ?: "카카오 로그인 실패") }
                }
        }
    }

    private fun loginWithGoogle(context: PlatformContext) {
        viewModelScope.launch {
            println("AuthViewModel [DEBUG] Google 로그인 시작")
            _state.update { it.copy(loadingProvider = AuthProvider.GOOGLE, error = null) }
            loginWithGoogleUseCase(context)
                .onSuccess { user ->
                    println("AuthViewModel [DEBUG] Google 로그인 성공 uid=${user.uid}")
                    _state.update { it.copy(loadingProvider = null, user = user) }
                }
                .onFailure { error ->
                    println("AuthViewModel [DEBUG] Google 로그인 실패: ${error.message}")
                    _state.update { it.copy(loadingProvider = null, error = error.message ?: "Google 로그인 실패") }
                }
        }
    }
}
