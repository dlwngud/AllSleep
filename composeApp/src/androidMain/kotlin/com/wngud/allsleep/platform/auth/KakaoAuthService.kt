package com.wngud.allsleep.platform.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.wngud.allsleep.domain.model.AuthProvider
import com.wngud.allsleep.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 카카오 로그인 서비스
 *
 * 흐름:
 * 1. 카카오 SDK로 Access Token 획득
 * 2. Firebase Cloud Function(verifyKakaoToken)으로 Access Token 검증
 * 3. Cloud Function이 Firebase Custom Token 발급
 * 4. Firebase Auth로 Custom Token 로그인
 */
class KakaoAuthService(private val context: Context) {

    private val functions = FirebaseFunctions.getInstance("asia-northeast3")
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signIn(): Result<User> = runCatching {
        // 1. 카카오 Access Token 획득
        val accessToken = getKakaoAccessToken()

        // 2. Firebase Cloud Function 호출 → Custom Token 발급
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

        // 3. Firebase Custom Token으로 로그인
        firebaseAuth.signInWithCustomToken(customToken).await()

        User(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            provider = AuthProvider.KAKAO
        )
    }

    /**
     * 카카오 Access Token을 Coroutine으로 획득
     * - 카카오톡 앱이 있으면 앱으로, 없으면 웹 브라우저로 로그인
     */
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

            // 카카오톡 설치 여부에 따라 분기
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        // 사용자가 카카오 로그인 취소 시
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            continuation.resumeWithException(error)
                            return@loginWithKakaoTalk
                        }
                        // 카카오톡 로그인 실패 → 웹으로 fallback
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } else if (token != null) {
                        continuation.resume(token.accessToken)
                    }
                }
            } else {
                // 카카오톡 미설치 → 웹 브라우저 로그인
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
}
