package com.wngud.allsleep.di

import android.app.Activity
import com.wngud.allsleep.data.repository.AuthRepositoryImpl
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.usecase.LoginWithGoogleUseCase
import com.wngud.allsleep.platform.auth.GoogleAuthService
import com.wngud.allsleep.ui.auth.login.LoginViewModel
import com.wngud.allsleep.ui.onboarding.OnboardingViewModel
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
    single<com.wngud.allsleep.domain.repository.SleepSettingsRepository> { 
        com.wngud.allsleep.data.repository.SleepSettingsRepositoryImpl() 
    }

    // AuthRepository를 싱글톤으로 제공 (Params 없이 주입 가능하도록)
    single<AuthRepository> { get<AuthRepositoryImpl>() }
    
    // Platform Layer - GoogleAuthService
    // Activity context는 런타임에 parametersOf로 제공됨
    factory { (activity: Activity) ->
        GoogleAuthService(activity, get())
    }
    
    // Domain Layer - Repository 인터페이스
    // GoogleAuthService를 파라미터로 받아서 AuthRepository 구현
    factory<AuthRepository>(org.koin.core.qualifier.named("google_login")) { (googleAuthService: GoogleAuthService) ->
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
        val authRepository: AuthRepository = get(org.koin.core.qualifier.named("google_login")) { org.koin.core.parameter.parametersOf(googleAuthService) }
        LoginWithGoogleUseCase(authRepository)
    }
    
    // Presentation Layer - ViewModels
    // GoogleAuthService를 파라미터로 받아서 UseCase 생성
    viewModel { (googleAuthService: GoogleAuthService) ->
        val loginWithGoogleUseCase: LoginWithGoogleUseCase = get { org.koin.core.parameter.parametersOf(googleAuthService) }
        LoginViewModel(loginWithGoogleUseCase)
    }
    
    viewModel { 
        OnboardingViewModel(get(), get()) 
    }
    
    viewModel {
        com.wngud.allsleep.ui.home.HomeViewModel()
    }
}
