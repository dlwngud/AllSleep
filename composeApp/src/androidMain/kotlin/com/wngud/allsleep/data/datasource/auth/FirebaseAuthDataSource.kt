package com.wngud.allsleep.data.datasource.auth

import com.google.firebase.auth.FirebaseAuth
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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

    suspend fun createUserWithEmail(email: String, password: String, name: String): User {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("계정 생성 실패")

        // 1. 프로필 이름 업데이트
        try {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
            // 이름 업데이트 실패는 로깅만 하고 진행 (선택 사항)
            android.util.Log.e("FirebaseAuthDS", "프로필 업데이트 실패: ${e.message}")
        }

        // 2. 이메일 인증 메일 발송 (Soft-fail: 실패해도 회원가입은 유지)
        try {
            firebaseUser.sendEmailVerification().await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthDS", "인증 메일 발송 실패: ${e.message}")
        }

        return mapFirebaseUser(firebaseUser)
    }

    suspend fun signInWithEmail(email: String, password: String): User {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("로그인 실패")
        return mapFirebaseUser(firebaseUser)
    }

    suspend fun deleteAccount() {
        firebaseAuth.currentUser?.delete()?.await()
    }

    suspend fun deleteCurrentUser() {
        firebaseAuth.currentUser?.delete()?.await()
    }

    suspend fun reloadUser() {
        firebaseAuth.currentUser?.reload()?.await()
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
