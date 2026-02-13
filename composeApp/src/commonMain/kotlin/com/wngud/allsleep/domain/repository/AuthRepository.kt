package com.wngud.allsleep.domain.repository

import com.wngud.allsleep.domain.model.User

/**
 * 인증 Repository 인터페이스
 * 플랫폼별 구현은 androidMain/iosMain에서 actual로 제공
 */
interface AuthRepository {
    /**
     * Google 로그인
     * @return Result<User> 성공 시 사용자 정보, 실패 시 에러
     */
    suspend fun loginWithGoogle(): Result<User>
    
    /**
     * 로그아웃
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * 현재 로그인된 사용자 가져오기
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean
}
