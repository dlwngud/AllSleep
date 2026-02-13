package com.wngud.allsleep.ui.onboarding

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.wngud.allsleep.ui.auth.login.LoginViewModel

/**
 * 온보딩 메인 화면
 * 5개의 개별 화면을 상태로 관리하여 전환
 * 
 * 플로우: 문제 공감 → 솔루션 → 시간 설정 → 로그인 → 완료 → Home
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(0) }
    var isLocalMode by remember { mutableStateOf(false) }
    var bedtime by remember { mutableStateOf("23:00") }
    var wakeTime by remember { mutableStateOf("07:00") }
    
    when (currentPage) {
        // 1. 문제 공감
        0 -> OnboardingProblemScreen(
            onNext = { currentPage = 1 }
        )
        
        // 2. 솔루션 제시
        1 -> OnboardingSolutionScreen(
            onNext = { currentPage = 2 }
        )
        
        // 3. 시간 설정 (먼저!)
        2 -> OnboardingTimeScreen(
            onNext = { currentPage = 3 }
        )
        
        // 4. 로그인 (나중!)
        3 -> OnboardingLoginScreen(
            onNext = { 
                isLocalMode = false
                currentPage = 4 
            },
            onSkip = {
                isLocalMode = true
                currentPage = 4
            },
            onKakaoLogin = { /* TODO: Kakao login */ },
            onAppleLogin = { /* TODO: Apple login */ },
            onEmailLogin = { /* TODO: Email login */ }
        )
        
        // 5. 준비 완료
        4 -> OnboardingCompleteScreen(
            onStart = onComplete,
            email = if (isLocalMode) "로컬 모드" else "user@example.com",
            bedtime = bedtime,
            wakeTime = wakeTime
        )
    }
}
