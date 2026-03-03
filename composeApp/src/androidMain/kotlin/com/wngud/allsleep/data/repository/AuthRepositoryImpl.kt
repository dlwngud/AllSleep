package com.wngud.allsleep.data.repository

import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.repository.AuthRepository
import com.wngud.allsleep.platform.auth.KakaoAuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Android 구현 (actual)
 * Firebase Android SDK 사용
 */
class AuthRepositoryImpl(
    private val kakaoAuthService: KakaoAuthService
) : AuthRepository {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    override suspend fun loginWithGoogle(): Result<User> {
        return try {
            throw NotImplementedError("Use GoogleAuthService in MainActivity")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithKakao(): Result<User> {
        return kakaoAuthService.signIn()
    }
    
    /**
     * Google ID Token으로 Firebase 로그인
     * GoogleAuthService에서 호출됨
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            
            val firebaseUser = result.user ?: throw Exception("User is null")
            
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString(),
                provider = AuthProvider.GOOGLE
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            provider = AuthProvider.GOOGLE
        )
    }
    
    override fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
