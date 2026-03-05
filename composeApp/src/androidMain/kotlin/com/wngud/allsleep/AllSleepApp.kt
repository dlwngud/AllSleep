package com.wngud.allsleep

import android.app.Application
import com.wngud.allsleep.data.source.local.initDataStore
import com.wngud.allsleep.di.appModule
import com.wngud.allsleep.platform.DeviceInfoProvider
import com.kakao.sdk.common.KakaoSdk
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application 클래스
 * Koin 초기화
 */
class AllSleepApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // DataStore 초기화 (Koin보다 먼저!)
        initDataStore(this)

        // DeviceInfoProvider 초기화 (Android ID 수집용 ContentResolver 주입)
        DeviceInfoProvider.init(contentResolver)

        // 카카오 SDK 초기화 (네이티브 앱 키)
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
        
        // Firebase 설정 확인 로그
        try {
            val options = com.google.firebase.FirebaseApp.getInstance().options
            android.util.Log.d("FirebaseCheck", "Project ID: ${options.projectId}")
            android.util.Log.d("FirebaseCheck", "Application ID: ${options.applicationId}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCheck", "Firebase initialized check failed", e)
        }

        // Koin 시작
        startKoin {
            androidLogger()
            androidContext(this@AllSleepApp)
            modules(appModule)
        }
    }
}

