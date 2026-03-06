package com.wngud.allsleep.data.datasource.auth

import com.google.firebase.auth.FirebaseAuth
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
        return mapFirebaseUser(firebaseUser)
    }

    private fun mapFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            provider = AuthProvider.GOOGLE // TODO: 실제 프로바이더 연동 필요 시 확장
        )
    }

    fun observeUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.let { mapFirebaseUser(it) }
            trySend(user)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null
}
