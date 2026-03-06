package com.wngud.allsleep.di

import com.wngud.allsleep.data.datasource.auth.FirebaseAuthDataSource
import com.wngud.allsleep.data.datasource.auth.GoogleAuthDataSource
import com.wngud.allsleep.data.datasource.auth.KakaoAuthDataSource
import com.wngud.allsleep.data.repository.AuthRepositoryImpl
import com.wngud.allsleep.data.repository.SleepSettingsRepositoryImpl
import com.wngud.allsleep.data.source.local.createDataStore
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.auth.ObserveUserUseCase
import com.wngud.allsleep.domain.usecase.auth.LoginWithGoogleUseCase
import com.wngud.allsleep.domain.usecase.auth.LoginWithKakaoUseCase
import com.wngud.allsleep.domain.usecase.auth.SignOutUseCase
import com.wngud.allsleep.domain.usecase.auth.UpdateUserProfileUseCase
import com.wngud.allsleep.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.wngud.allsleep.domain.usecase.onboarding.ObserveOnboardingCompletedUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveRegisteredDevicesUseCase
import com.wngud.allsleep.domain.usecase.sleep.ObserveUserSleepStateUseCase
import com.wngud.allsleep.domain.usecase.sleep.RegisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UnregisterDeviceUseCase
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.ui.alarm.AlarmViewModel
import com.wngud.allsleep.ui.auth.login.AuthViewModel
import com.wngud.allsleep.ui.home.HomeViewModel
import com.wngud.allsleep.ui.global.GlobalSleepViewModel
import com.wngud.allsleep.ui.onboarding.OnboardingViewModel
import com.wngud.allsleep.ui.settings.SettingsViewModel
import com.wngud.allsleep.ui.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.google.firebase.firestore.FirebaseFirestore
import com.wngud.allsleep.data.repository.SleepSyncRepositoryImpl
// SleepSyncRepository 인터페이스 임포트 제거 (위에서 domain으로 통합됨)

val appModule = module {

    // ── DataSource (구체 클래스 직접 등록) ───────────────────────
    single { KakaoAuthDataSource(androidContext()) }
    singleOf(::GoogleAuthDataSource)
    singleOf(::FirebaseAuthDataSource)
    single { 
        FirebaseFirestore.setLoggingEnabled(true)
        // 기본값인 (default) 대신 사용자의 실제 데이터베이스 ID인 "default" 명시
        FirebaseFirestore.getInstance("default").apply {
            val settings = com.google.firebase.firestore.firestoreSettings {
                isPersistenceEnabled = false
            }
            firestoreSettings = settings
        }
    }

    // ── Repository (인터페이스 → 구현체, DIP) ─────────────────────
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::SleepSyncRepositoryImpl) bind SleepSyncRepository::class

    // ── DataStore ─────────────────────────────────────────────────
    single { createDataStore() }
    singleOf(::SleepSettingsRepositoryImpl) bind SleepSettingsRepository::class

    // ── UseCase ───────────────────────────────────────────────────
    factoryOf(::LoginWithKakaoUseCase)
    factoryOf(::LoginWithGoogleUseCase)
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::ObserveUserUseCase)
    factoryOf(::UpdateUserProfileUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveUserSleepStateUseCase)
    factoryOf(::UpdateUserSleepStateUseCase)
    factoryOf(::RegisterDeviceUseCase)
    factoryOf(::UnregisterDeviceUseCase)
    factoryOf(::ObserveRegisteredDevicesUseCase)
    factoryOf(::ObserveOnboardingCompletedUseCase)
    factoryOf(::CompleteOnboardingUseCase)

    // ── ViewModel ─────────────────────────────────────────────────
    viewModelOf(::GlobalSleepViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AlarmViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::StatsViewModel)
}
