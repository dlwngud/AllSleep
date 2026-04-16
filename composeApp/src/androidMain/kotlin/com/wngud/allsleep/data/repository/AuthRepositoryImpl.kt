package com.wngud.allsleep.data.repository

import com.wngud.allsleep.data.datasource.auth.FirebaseAuthDataSource
import com.wngud.allsleep.data.datasource.auth.GoogleAuthDataSource
import com.wngud.allsleep.data.datasource.auth.KakaoAuthDataSource
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.platform.PlatformContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking

class AuthRepositoryImpl(
    private val kakaoAuthDataSource: KakaoAuthDataSource,
    private val googleAuthDataSource: GoogleAuthDataSource,
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : AuthRepository {

    override suspend fun loginWithKakao(): Result<User> =
        kakaoAuthDataSource.signIn()

    override suspend fun loginWithGoogle(context: PlatformContext): Result<User> =
        googleAuthDataSource.signIn(context)

    override suspend fun loginWithEmail(email: String, password: String): Result<User> = runCatching {
        val user = firebaseAuthDataSource.signInWithEmail(email, password)
        // Firestore에서 프리미엄 정보 등 최신화하여 반환
        getCurrentUser() ?: user
    }.recoverCatching { e ->
        throw mapAuthException(e)
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> = runCatching {
        // 1. Auth 계정 생성
        val user = firebaseAuthDataSource.createUserWithEmail(email, password, name)
        
        // 2. Firestore 유저 문서 초기화
        try {
            val userMap = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to name,
                "provider" to "EMAIL",
                "isPremium" to false,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(user.uid).set(userMap).await()
            
            // 프리미엄 정보 등이 포함된 최신 유저 정보 반환
            getCurrentUser() ?: user
        } catch (e: Exception) {
            // 3. Firestore 실패 시 생성된 Auth 계정 삭제 (Rollback)
            firebaseAuthDataSource.deleteCurrentUser()
            throw Exception("회원 정보 저장 중 오류가 발생했습니다. 다시 시도해 주세요.")
        }
    }.recoverCatching { e ->
        throw if (e.message?.contains("회원 정보 저장") == true) e else mapAuthException(e)
    }

    private fun mapAuthException(e: Throwable): Throwable {
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                Exception("이미 가입된 이메일이거나 소셜 로그인으로 사용 중인 계정입니다.")
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> 
                Exception("비밀번호가 너무 취약합니다. (최소 6자리 이상)")
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> 
                Exception("유효하지 않은 이메일 형식이거나 비밀번호가 틀렸습니다.")
            is com.google.firebase.FirebaseNetworkException -> 
                Exception("네트워크 연결을 확인해 주세요.")
            else -> e
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseAuthDataSource.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val uid = firebaseAuthDataSource.getCurrentUser()?.uid ?: throw Exception("User not logged in")
        val userDocRef = firestore.collection("users").document(uid)
        
        // 1. Delete all documents in 'devices' subcollection
        val devicesSnapshot = userDocRef.collection("devices").get().await()
        for (document in devicesSnapshot.documents) {
            document.reference.delete().await()
        }
        
        // 2. Delete the user document
        userDocRef.delete().await()

        // 3. Delete Firebase Auth account
        firebaseAuthDataSource.deleteAccount()
    }.onFailure { e ->
        android.util.Log.e("AuthRepository", "Account deletion failed", e)
        if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            throw Exception("보안을 위해 재로그인이 필요합니다. 로그아웃 후 다시 로그인하여 시도해 주세요.")
        }
        throw e
    }

    override suspend fun validateSession(): Result<Unit> = runCatching {
        firebaseAuthDataSource.reloadUser()
    }

    override suspend fun getCurrentUser(): User? {
        val user = firebaseAuthDataSource.getCurrentUser() ?: return null
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            val isPremium = snapshot.getBoolean("isPremium") ?: false
            user.copy(isPremium = isPremium)
        } catch (e: Exception) {
            user // Firestore 실패 시 기본 유저 정보 반환
        }
    }

    override fun isLoggedIn(): Boolean =
        firebaseAuthDataSource.isLoggedIn()

    override fun observeUser(): Flow<User?> =
        firebaseAuthDataSource.observeUser().flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                firestore.collection("users").document(user.uid)
                    .snapshots()
                    .map { snapshot ->
                        val isPremium = snapshot.getBoolean("isPremium") ?: false
                        user.copy(isPremium = isPremium)
                    }
            }
        }
}
