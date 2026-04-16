package com.wngud.allsleep.platform

/**
 * 앱 오프닝 광고 로드 및 노출을 담당하는 매니저 인터페이스
 */
interface AppOpenAdManager {
    /**
     * 광고 노출 조건(쿨타임 등)을 확인하고 광고를 노출합니다.
     */
    fun checkAndShowAdIfAvailable()
    
    /**
     * 광고 로딩을 미리 준비합니다.
     */
    fun loadAd()

    /**
     * 앱 최초 실행 시 광고 로드를 대기(최대 N초)하고 사용 가능 시 띄웁니다.
     * 로드 실패, 타이머 초과, 광고 노출 완료(닫음) 시 onComplete가 호출됩니다.
     */
    fun requestColdStartAd(onComplete: () -> Unit)
}
