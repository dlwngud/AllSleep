package com.wngud.allsleep

import android.app.Application
import com.wngud.allsleep.data.source.local.initDataStore
import com.wngud.allsleep.di.appModule
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
        
        // Koin 시작
        startKoin {
            androidLogger()
            androidContext(this@AllSleepApp)
            modules(appModule)
        }
    }
}
