package com.wngud.allsleep.data.datasource.auth

import com.google.firebase.auth.FirebaseAuth
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User

/**
 * Firebase Auth 상태 관리 DataSource
 * 로그아웃, 현재 사용자 조회, 로그인 상태 확인
 */
class FirebaseAuthDataSource {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            provider = AuthProvider.GOOGLE
        )
    }

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
