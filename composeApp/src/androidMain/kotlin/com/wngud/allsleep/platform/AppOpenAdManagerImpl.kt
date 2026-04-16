package com.wngud.allsleep.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Android 전용 앱 오프닝 광고 매니저 구현체
 */
class AppOpenAdManagerImpl(
    private val application: Application,
    private val repository: SleepSettingsRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : AppOpenAdManager, Application.ActivityLifecycleCallbacks {

    // 앱 오프닝 광고 단위 ID (현재 테스트용 ID)
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    private val AD_DISPLAY_GAP_MS = 4 * 60 * 60 * 1000L // 정상 배포 정책: 4시간 (ms)

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun loadAd() {
        if (isAdAvailable()) return

        CoroutineScope(Dispatchers.Main).launch {
            val user = getCurrentUserUseCase()
            if (user?.isPremium == true) return@launch

            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                application,
                AD_UNIT_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        android.util.Log.d("AppOpenAd", "광고 로드 성공")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        android.util.Log.e("AppOpenAd", "광고 로드 실패: ${loadAdError.message}")
                    }
                }
            )
        }
    }

    override fun checkAndShowAdIfAvailable() {
        if (isShowingAd) return

        CoroutineScope(Dispatchers.Main).launch {
            val user = getCurrentUserUseCase()
            if (user?.isPremium == true) return@launch

            val lastShownAt = repository.lastAppOpenAdShownAt.first()
            val currentTime = Clock.System.now().toEpochMilliseconds()

            // 4시간 쿨타임 체크
            if (currentTime - lastShownAt >= AD_DISPLAY_GAP_MS) {
                showAdIfAvailable()
            } else {
                android.util.Log.d("AppOpenAd", "쿨타임 미경과로 광고 노출 스킵")
                // 다음을 위해 미리 로드만 해둠
                loadAd()
            }
        }
    }

    private fun showAdIfAvailable() {
        val ad = appOpenAd
        val activity = currentActivity

        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    
                    // 광고 노출 시점 저장
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.saveLastAppOpenAdShownAt(Clock.System.now().toEpochMilliseconds())
                    }
                    
                    // 다음 노출을 위해 미리 로드
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            ad.show(activity)
        } else {
            // 로드된 광고가 없으면 새로 로드
            loadAd()
        }
    }

    private fun isAdAvailable(): Boolean = appOpenAd != null

    override fun requestColdStartAd(onComplete: () -> Unit) {
        if (isShowingAd) {
            onComplete()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val user = getCurrentUserUseCase()
            if (user?.isPremium == true) {
                android.util.Log.d("AppOpenAd", "프리미엄 구독자 - 콜드 스타트 광고 노출 스킵")
                onComplete()
                return@launch
            }

            val lastShownAt = repository.lastAppOpenAdShownAt.first()
            val currentTime = Clock.System.now().toEpochMilliseconds()

            if (currentTime - lastShownAt < AD_DISPLAY_GAP_MS) {
                android.util.Log.d("AppOpenAd", "콜드 스타트 스킵: 쿨타임 미경과")
                loadAd()
                onComplete()
                return@launch
            }

            if (isAdAvailable()) {
                showColdStartAd(onComplete)
                return@launch
            }

            // Load and wait up to 3 seconds
            var isTimeout = false
            val timeoutJob = launch {
                kotlinx.coroutines.delay(3000)
                isTimeout = true
                android.util.Log.d("AppOpenAd", "콜드 스타트 광고 로드 타임아웃 (3초)")
                onComplete()
            }

            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                application,
                AD_UNIT_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        if (!isTimeout) {
                            timeoutJob.cancel()
                            showColdStartAd(onComplete)
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        android.util.Log.e("AppOpenAd", "콜드 스타트 로드 실패: ${loadAdError.message}")
                        if (!isTimeout) {
                            timeoutJob.cancel()
                            onComplete()
                        }
                    }
                }
            )
        }
    }

    private fun showColdStartAd(onComplete: () -> Unit) {
        val ad = appOpenAd
        val activity = currentActivity

        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.saveLastAppOpenAdShownAt(Clock.System.now().toEpochMilliseconds())
                    }
                    loadAd()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            ad.show(activity)
        } else {
            loadAd()
            onComplete()
        }
    }

    // Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { currentActivity = null }
}
