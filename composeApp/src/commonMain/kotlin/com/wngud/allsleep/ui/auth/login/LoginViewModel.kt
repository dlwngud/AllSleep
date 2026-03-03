package com.wngud.allsleep.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.usecase.LoginWithGoogleUseCase
import com.wngud.allsleep.domain.usecase.LoginWithKakaoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 로그인 ViewModel (MVVM + MVI)
 * 플랫폼 공통 로직
 */
class LoginViewModel(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.LoginWithGoogle -> loginWithGoogle()
            is LoginIntent.LoginWithKakao -> loginWithKakao()
            is LoginIntent.DismissError -> dismissError()
        }
    }
    
    private fun loginWithKakao() {
        viewModelScope.launch {
            android.util.Log.d("LoginViewModel", "🚀 카카오 로그인 시작")
            _state.update { it.copy(loadingProvider = AuthProvider.KAKAO, error = null) }

            loginWithKakaoUseCase()
                .onSuccess { user ->
                    android.util.Log.d("LoginViewModel", "✅ 카카오 로그인 성공! uid=${user.uid}")
                    _state.update { it.copy(loadingProvider = null, user = user, error = null) }
                }
                .onFailure { error ->
                    android.util.Log.e("LoginViewModel", "❌ 카카오 로그인 실패: ${error.message}", error)
                    _state.update { it.copy(loadingProvider = null, error = error.message ?: "카카오 로그인 실패") }
                }
        }
    }

    private fun loginWithGoogle() {
        viewModelScope.launch {
            android.util.Log.d("LoginViewModel", "🚀 Google 로그인 시작")
            _state.update { it.copy(loadingProvider = AuthProvider.GOOGLE, error = null) }
            
            loginWithGoogleUseCase()
                .onSuccess { user ->
                    android.util.Log.d("LoginViewModel", "✅ 로그인 성공! User: ${user.email}")
                    android.util.Log.d("LoginViewModel", "   - UID: ${user.uid}")
                    android.util.Log.d("LoginViewModel", "   - Name: ${user.displayName}")
                    android.util.Log.d("LoginViewModel", "   - Provider: ${user.provider}")
                    
                    _state.update { 
                        it.copy(
                            loadingProvider = null, 
                            user = user,
                            error = null
                        ) 
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("LoginViewModel", "❌ 로그인 실패: ${error.message}", error)
                    
                    _state.update { 
                        it.copy(
                            loadingProvider = null, 
                            error = error.message ?: "로그인 실패"
                        ) 
                    }
                }
        }
    }
    
    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
