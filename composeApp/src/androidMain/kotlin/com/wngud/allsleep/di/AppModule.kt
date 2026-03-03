package com.wngud.allsleep.di

import android.app.Activity
import com.wngud.allsleep.data.repository.AuthRepositoryImpl
import com.wngud.allsleep.data.repository.SleepSettingsRepositoryImpl
import com.wngud.allsleep.data.source.local.createDataStore
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.LoginWithGoogleUseCase
import com.wngud.allsleep.domain.usecase.LoginWithKakaoUseCase
import com.wngud.allsleep.platform.auth.GoogleAuthService
import com.wngud.allsleep.platform.auth.KakaoAuthService
import com.wngud.allsleep.ui.alarm.AlarmViewModel
import com.wngud.allsleep.ui.auth.login.LoginViewModel
import com.wngud.allsleep.ui.home.HomeViewModel
import com.wngud.allsleep.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin DI 모듈
 * 
 * ViewModel은 각 Screen에서 koinViewModel()로 주입
 */
val appModule = module {
    
    // Data Layer - Android 구현체
    single { (context: android.content.Context) -> KakaoAuthService(context) }
    single<AuthRepositoryImpl> { AuthRepositoryImpl(get { parametersOf(get<android.content.Context>()) }) }
    
    // DataStore 인스턴스 (싱글톤)
    single { createDataStore() }
    
    single<SleepSettingsRepository> {
        SleepSettingsRepositoryImpl(get())
    }

    // AuthRepository를 싱글톤으로 제공 (Params 없이 주입 가능하도록)
    single<AuthRepository> { get<AuthRepositoryImpl>() }
    
    // Platform Layer - GoogleAuthService
    // Activity context는 런타임에 parametersOf로 제공됨
    factory { (activity: Activity) ->
        GoogleAuthService(activity, get())
    }
    
    factory<AuthRepository>(named("google_login")) { (googleAuthService: GoogleAuthService) ->
        object : AuthRepository {
            private val authRepositoryImpl: AuthRepositoryImpl = get()

            override suspend fun loginWithGoogle() = googleAuthService.signIn()
            override suspend fun loginWithKakao() = get<AuthRepositoryImpl>().loginWithKakao()
            override suspend fun logout() = authRepositoryImpl.logout()
            override suspend fun getCurrentUser() = authRepositoryImpl.getCurrentUser()
            override fun isLoggedIn() = authRepositoryImpl.isLoggedIn()
        }
    }
    
    // Domain Layer - Use Cases
    // GoogleAuthService를 파라미터로 받아서 AuthRepository 생성
    factory { (googleAuthService: GoogleAuthService) ->
        val authRepository: AuthRepository = get(named("google_login")) { parametersOf(googleAuthService) }
        LoginWithGoogleUseCase(authRepository)
    }
    
    // Presentation Layer - ViewModels
    // GoogleAuthService를 파라미터로 받아서 UseCase 생성
    viewModel { (googleAuthService: GoogleAuthService) ->
        val loginWithGoogleUseCase: LoginWithGoogleUseCase = get { parametersOf(googleAuthService) }
        val loginWithKakaoUseCase: LoginWithKakaoUseCase = LoginWithKakaoUseCase(get<AuthRepositoryImpl>())
        LoginViewModel(loginWithGoogleUseCase, loginWithKakaoUseCase)
    }
    
    viewModel { 
        OnboardingViewModel(get(), get()) 
    }
    
    viewModel {
        HomeViewModel()
    }

    viewModel {
        AlarmViewModel()
    }

    viewModel {
        com.wngud.allsleep.ui.settings.SettingsViewModel()
    }

    viewModel {
        com.wngud.allsleep.ui.stats.StatsViewModel()
    }
}
