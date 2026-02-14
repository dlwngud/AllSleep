package com.wngud.allsleep.di

import android.app.Activity
import com.wngud.allsleep.data.repository.AuthRepositoryImpl
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.usecase.LoginWithGoogleUseCase
import com.wngud.allsleep.platform.auth.GoogleAuthService
import com.wngud.allsleep.ui.auth.login.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin DI 모듈
 * 
 * ViewModel은 각 Screen에서 koinViewModel()로 주입
 */
val appModule = module {
    
    // Data Layer - Android 구현체
    single<AuthRepositoryImpl> { AuthRepositoryImpl() }
    
    // Platform Layer - GoogleAuthService
    // Activity context는 런타임에 parametersOf로 제공됨
    factory { (activity: Activity) ->
        GoogleAuthService(activity, get())
    }
    
    // Domain Layer - Repository 인터페이스
    // GoogleAuthService를 파라미터로 받아서 AuthRepository 구현
    factory<AuthRepository> { (googleAuthService: GoogleAuthService) ->
        object : AuthRepository {
            private val authRepositoryImpl: AuthRepositoryImpl = get()
            
            override suspend fun loginWithGoogle() = googleAuthService.signIn()
            override suspend fun logout() = authRepositoryImpl.logout()
            override suspend fun getCurrentUser() = authRepositoryImpl.getCurrentUser()
            override fun isLoggedIn() = authRepositoryImpl.isLoggedIn()
        }
    }
    
    // Domain Layer - Use Cases
    // GoogleAuthService를 파라미터로 받아서 AuthRepository 생성
    factory { (googleAuthService: GoogleAuthService) ->
        val authRepository: AuthRepository = get { org.koin.core.parameter.parametersOf(googleAuthService) }
        LoginWithGoogleUseCase(authRepository)
    }
    
    // Presentation Layer - ViewModels
    // GoogleAuthService를 파라미터로 받아서 UseCase 생성
    viewModel { (googleAuthService: GoogleAuthService) ->
        val loginWithGoogleUseCase: LoginWithGoogleUseCase = get { org.koin.core.parameter.parametersOf(googleAuthService) }
        LoginViewModel(loginWithGoogleUseCase)
    }
}
