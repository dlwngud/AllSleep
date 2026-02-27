package com.wngud.allsleep.platform.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.wngud.allsleep.R
import com.wngud.allsleep.data.repository.AuthRepositoryImpl
import com.wngud.allsleep.domain.model.User
import android.util.Log

/**
 * Android Google Sign-In Service
 * Credential Manager API 사용 (최신 방식)
 */
class GoogleAuthService(
    private val context: Context,
    private val authRepository: AuthRepositoryImpl
) {
    
    private val credentialManager = CredentialManager.create(context)
    
    /**
     * Google 로그인 시작
     * Activity context가 필요함
     */
    suspend fun signIn(): Result<User> {
        return try {
            Log.d("GoogleAuthService", "Starting Google Sign-In")
            
            // Activity context 확인
            val activityContext = context as? Activity
            if (activityContext == null) {
                Log.e("GoogleAuthService", "Context is not an Activity")
                return Result.failure(Exception("Activity context가 필요합니다"))
            }
            
            // Google ID 옵션 설정
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()
            
            Log.d("GoogleAuthService", "Google ID Option created")
            
            // Credential 요청
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            Log.d("GoogleAuthService", "Requesting credentials")
            
            // Credential 가져오기
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            Log.d("GoogleAuthService", "Credentials received")
            
            // Google ID Token 추출
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            
            Log.d("GoogleAuthService", "ID Token extracted, signing in with Firebase")
            
            // Firebase 로그인
            authRepository.signInWithGoogleToken(idToken)
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // 사용자가 로그인을 취소한 경우 - 정상적인 동작
            Log.d("GoogleAuthService", "User cancelled Google Sign-In")
            Result.failure(Exception("로그인이 취소되었습니다"))
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthService", "GetCredentialException: ${e.message}", e)
            Result.failure(Exception("Google 로그인 실패: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GoogleAuthService", "Exception: ${e.message}", e)
            Result.failure(Exception("로그인 중 오류 발생: ${e.message}"))
        }
    }
}
