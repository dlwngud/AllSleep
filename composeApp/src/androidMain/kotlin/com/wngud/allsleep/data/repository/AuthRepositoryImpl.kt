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

    override suspend fun getCurrentUser(): User? =
        firebaseAuthDataSource.getCurrentUser()

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
