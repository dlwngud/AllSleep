package com.wngud.allsleep.data.datasource.auth

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.wngud.allsleep.R
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.platform.PlatformContext
import kotlinx.coroutines.tasks.await

/**
 * Google 로그인 DataSource
 *
 * PlatformContext = Activity (Android)
 */
class GoogleAuthDataSource {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signIn(context: PlatformContext): Result<User> {
        return try {
            Log.d("GoogleAuthDataSource", "Starting Google Sign-In")

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            signInWithGoogleToken(idToken)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("로그인이 취소되었습니다"))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google 로그인 실패: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("로그인 중 오류 발생: ${e.message}"))
        }
    }

    private suspend fun signInWithGoogleToken(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("User is null")
            Result.success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    provider = AuthProvider.GOOGLE
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
