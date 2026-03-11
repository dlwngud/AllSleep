package com.wngud.allsleep.domain.repository

interface AppBlockerRepository {
    /**
     * 특정 패키지명이 시스템 앱(기본 탑재 앱)인지 확인합니다.
     */
    fun isSystemApp(packageName: String): Boolean
}
