package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppBlockerRepository {
    /**
     * 기기에 설치된 모든 앱 목록을 가져옵니다. (블랙리스트 상태 포함)
     */
    suspend fun getInstalledApps(): List<AppInfo>

    /**
     * 현재 차단된 앱들의 패키지명 목록을 Flow로 관찰합니다.
     */
    fun observeBlockedPackages(): Flow<Set<String>>

    /**
     * 특정 앱의 차단 여부를 설정합니다.
     */
    suspend fun setAppBlocked(packageName: String, isBlocked: Boolean)

    /**
     * 모든 차단 설정을 초기화합니다.
     */
    suspend fun clearAllBlockedApps()
}
