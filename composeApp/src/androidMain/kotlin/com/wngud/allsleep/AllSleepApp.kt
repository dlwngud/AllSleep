package com.wngud.allsleep

import android.app.Application
import com.wngud.allsleep.data.source.local.initDataStore
import com.wngud.allsleep.di.appModule
import com.wngud.allsleep.platform.DeviceInfoProvider
import com.google.android.gms.ads.MobileAds
import com.kakao.sdk.common.KakaoSdk
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application 클래스
 * Koin 초기화
 */
class AllSleepApp : Application() {
    
    override fun onCreate() {
        super.onCreate()

        // RevenueCat 초기화
        val rcKey = BuildConfig.REVENUECAT_API_KEY
        if (rcKey.isNotBlank()) {
            try {
                Purchases.logLevel = LogLevel.DEBUG
                Purchases.configure(
                    PurchasesConfiguration.Builder(this, rcKey).build()
                )
            } catch (e: Exception) {
                android.util.Log.e("AllSleepApp", "RevenueCat initialization failed", e)
            }
        } else {
            android.util.Log.e("AllSleepApp", "RevenueCat API Key is missing!")
        }
        
        // AdMob 초기화
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@AllSleepApp) {}
        }
        
        // DataStore 초기화 (Koin보다 먼저!)
        initDataStore(this)

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
            // 릴리즈 빌드에서는 로깅 레벨을 높여 잠재적 이슈 방지
            androidLogger(org.koin.core.logger.Level.ERROR)
            androidContext(this@AllSleepApp)
            modules(appModule)
        }
    }
}

