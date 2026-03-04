package com.wngud.allsleep.data.datasource.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 카카오 로그인 DataSource
 *
 * 흐름:
 * 1. 카카오 SDK로 Access Token 획득
 * 2. Firebase Cloud Function(verifyKakaoToken)으로 Custom Token 발급
 * 3. Firebase Auth Custom Token 로그인
 */
class KakaoAuthDataSource(private val context: Context) {

    private val functions = FirebaseFunctions.getInstance("asia-northeast3")
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signIn(): Result<User> = runCatching {
        val accessToken = getKakaoAccessToken()

        val data = hashMapOf("accessToken" to accessToken)
        val result = functions
            .getHttpsCallable("verifyKakaoToken")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val resultMap = result.data as Map<String, Any?>
        val customToken = resultMap["customToken"] as String
        val uid = resultMap["uid"] as String
        val displayName = resultMap["displayName"] as? String
        val email = resultMap["email"] as? String
        val photoUrl = resultMap["photoUrl"] as? String

        firebaseAuth.signInWithCustomToken(customToken).await()

        User(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            provider = AuthProvider.KAKAO
        )
    }

    private suspend fun getKakaoAccessToken(): String =
        suspendCancellableCoroutine { continuation ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else if (token != null) {
                    continuation.resume(token.accessToken)
                } else {
                    continuation.resumeWithException(IllegalStateException("카카오 토큰이 null입니다."))
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            continuation.resumeWithException(error)
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } else if (token != null) {
                        continuation.resume(token.accessToken)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
}
