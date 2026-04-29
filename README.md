# AllSleep

> Modern Sleep Lock & Habit Tracking App
> Compose Multiplatform · Firebase · Koin · MVI

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-24+-3DDC84?style=flat-square&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20FCM-FFCA28?style=flat-square&logo=firebase&logoColor=black)

AllSleep은 잠들기 전 스마트폰 사용을 줄이고, 수면 루틴을 꾸준히 지킬 수 있도록 돕는 수면 잠금 애플리케이션입니다.
사용자는 평일·주말 수면 루틴을 설정하고, 수면 모드 중 다른 앱 사용을 차단하며, 누적된 수면 기록을 기반으로 수면 패턴을 확인할 수 있습니다.

## ✨ Key Features

- 수면 모드 잠금 - 오버레이와 접근성 서비스를 활용한 앱 사용 차단
- 평일·주말 루틴 관리 - 취침/기상 시간과 알람 활성화 상태를 분리 관리
- 수면 기록 분석 - 주간 평균, 수면 부채, 수면 점수, 연속 달성일 계산
- 다중 기기 동기화 - Firebase Firestore 기반 수면 상태 및 등록 기기 동기화
- 소셜/이메일 로그인 - Google, Kakao, Email 인증 지원
- 프리미엄 구독 - RevenueCat 기반 상품 조회, 구매, 복원, 구독 상태 반영
- Material 3 Design - Compose Multiplatform 기반 공통 UI와 반응형 상태 관리

## Screenshots

| 홈 | 알람 | 통계 | 설정 |
| --- | --- | --- | --- |
| 준비 중 | 준비 중 | 준비 중 | 준비 중 |

## Architecture

이 프로젝트는 Compose Multiplatform 기반의 단일 앱 모듈 안에서 Domain, Data, UI, Platform 계층을 분리하고 MVI 패턴을 적용해 예측 가능한 상태 관리를 지향합니다.

```text
composeApp/
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/wngud/allsleep/
│   │       ├── domain/         # Model, Repository interface, UseCase
│   │       ├── data/           # Repository 구현 및 로컬 설정 저장
│   │       ├── navigation/     # 화면 라우트와 NavHost
│   │       ├── platform/       # 플랫폼 기능 expect/actual 계약
│   │       └── ui/             # Compose 화면, 컴포넌트, 테마
│   ├── androidMain/
│   │   └── kotlin/com/wngud/allsleep/
│   │       ├── data/           # Firebase, Kakao, Android 데이터 소스
│   │       ├── platform/       # Android 권한, 결제, 스케줄러 구현
│   │       └── service/        # 수면 잠금 Foreground/Accessibility 서비스
│   └── iosMain/
│       └── kotlin/com/wngud/allsleep/
│           └── platform/       # iOS 플랫폼 구현
└── build.gradle.kts
```

### Architecture Principles

- MVI (Model-View-Intent) - 화면별 State/Intent를 분리해 단방향 데이터 흐름 구성
- Clean Layering - Domain interface와 Data 구현을 분리해 테스트 가능한 구조 유지
- Reactive Streams - Flow 기반 설정, 인증, 수면 상태, 기기 목록 관찰
- Platform Abstraction - 권한, 스케줄러, 결제, 디바이스 정보 등 플랫폼 의존 기능 격리

## Tech Stack

### Core Technologies

- Language: Kotlin
- UI Framework: Compose Multiplatform, Material 3
- Architecture: Clean Architecture + MVI
- Dependency Injection: Koin
- Asynchronous: Coroutines + Flow
- Local Storage: DataStore Preferences

### Libraries & Dependencies

| Category | Library | Purpose |
| --- | --- | --- |
| UI | Compose Multiplatform | Android/iOS 공통 선언형 UI |
| Design | Material 3 | 앱 테마 및 컴포넌트 |
| Navigation | Navigation Compose | 화면 간 라우팅 |
| DI | Koin | ViewModel, Repository, Platform 구현 주입 |
| Local Storage | DataStore Preferences | 루틴, 온보딩, 프리미엄 상태 저장 |
| Backend | Firebase Auth, Firestore, FCM | 인증, 수면 상태 동기화, 기기 토큰 관리 |
| Social Login | Google Identity, Kakao SDK | Google/Kakao 로그인 |
| Billing | RevenueCat | 구독 상품, 구매, 복원 관리 |
| Ads | Google Mobile Ads | 앱 오픈 광고 |
| Testing | kotlin.test, Coroutines Test | ViewModel 및 비동기 로직 테스트 |

## Key Technical Highlights

### 1. MVI Pattern

```kotlin
data class StatsState(
    val isLoading: Boolean = false,
    val selectedTab: StatsTab = StatsTab.SUMMARY,
    val records: Map<String, SleepRecord> = emptyMap(),
    val weeklyAverageMinutes: Int = 0,
    val sleepScore: Int = 0,
    val sleepDebtMinutes: Int = 0,
    val error: String? = null
)

sealed interface StatsIntent {
    data class SelectTab(val tab: StatsTab) : StatsIntent
    data class SelectDate(val date: String) : StatsIntent
    data class NavigateMonth(val yearMonth: String) : StatsIntent
    data object Retry : StatsIntent
}
```

화면의 상태와 사용자 의도를 명시적으로 분리해 UI 이벤트 처리와 상태 갱신 흐름을 단순하게 유지합니다.

### 2. Reactive Sleep Routine

```kotlin
combine(
    sleepSettingsRepository.weekdayBedtime,
    sleepSettingsRepository.weekdayWakeTime,
    sleepSettingsRepository.weekendBedtime,
    sleepSettingsRepository.weekendWakeTime
) { weekdayBedtime, weekdayWakeTime, weekendBedtime, weekendWakeTime ->
    val weekdayTarget = calculateTimeDiffMinutes(weekdayBedtime, weekdayWakeTime)
    val weekendTarget = calculateTimeDiffMinutes(weekendBedtime, weekendWakeTime)
    (weekdayTarget * 5 + weekendTarget * 2) / 7
}.collect { targetMinutes ->
    _state.update { it.copy(currentTargetMinutes = targetMinutes) }
    recalculateSummary()
}
```

DataStore의 루틴 변경을 Flow로 관찰하고, 변경 즉시 수면 목표와 통계 요약을 다시 계산합니다.

### 3. Android Sleep Lock Enforcement

```kotlin
if (!SleepLockService.isServiceRunning) {
    return@launch
}

val isSystemApp = appBlockerRepository.isSystemApp(targetPackageName)
if (isSystemApp) {
    return@launch
}

fireMainActivityIntent()
startService(Intent(this@AppSupervisorService, SleepLockService::class.java))
```

수면 모드 중 접근성 이벤트로 실행 앱을 감지하고, 시스템 앱이 아닌 경우 AllSleep 화면으로 되돌려 수면 잠금 흐름을 유지합니다.

### 4. Firebase Device Sync

```kotlin
usersCollection.document(uid).collection("devices")
    .addSnapshotListener { snapshot, error ->
        val devices = snapshot?.documents?.map { doc ->
            DeviceState(
                deviceId = doc.getString("deviceId") ?: "",
                deviceName = doc.getString("deviceName") ?: "",
                platform = doc.getString("platform") ?: "Android",
                isMainAlarmDevice = doc.getBoolean("isMainAlarmDevice") ?: false
            )
        } ?: emptyList()

        trySend(devices)
    }
```

Firestore snapshot listener를 Flow로 감싸 등록 기기 목록과 수면 상태를 실시간으로 UI와 잠금 오버레이에 반영합니다.

## Testing Strategy

수면 통계, 구독 복원, 비동기 상태 갱신처럼 사용자 경험에 직접 영향을 주는 로직을 중심으로 테스트합니다.

- Unit Tests: ViewModel 상태 변화와 도메인 계산 로직 검증
- Fake Repository: Firebase, Billing 등 외부 의존성 분리
- Async Testing: Coroutines Test로 Flow 수집과 비동기 로딩 상태 검증

```kotlin
@Test
fun weekly_summary_should_reflect_recent_records() = runTest {
    val records = recentRecords(durationMinutes = 480, days = 7)
    val viewModel = createViewModel(records)

    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals(480, state.weeklyAverageMinutes)
    assertEquals(0, state.sleepDebtMinutes)
    assertEquals(7, state.achievementCount)
    assertTrue(state.sleepScore >= 90)
}
```

## Module Structure

`:composeApp` - Kotlin Multiplatform Application Module

- `commonMain/` - Android/iOS 공통 UI, Domain, UseCase, Repository interface
- `androidMain/` - Android 권한, Firebase/Kakao 인증, Foreground Service, Accessibility Service
- `iosMain/` - iOS 엔트리포인트와 플랫폼별 구현
- `commonTest/` - 공통 ViewModel 및 도메인 테스트

### Main Screens

`ui/home`

- 수면 모드 시작
- 연결 기기 상태 표시
- 잠금 권한 및 배터리 최적화 안내

`ui/alarm`

- 평일/주말 취침 및 기상 루틴 설정
- 수면 잠금 알람 활성화 관리
- 플랫폼 스케줄러와 연동

`ui/stats`

- 월간 수면 기록 조회
- 주간 평균, 수면 부채, 수면 점수 계산
- 프리미엄 장기 패턴 요약

`ui/settings`

- 프로필, 로그아웃, 회원 탈퇴
- 기기 관리 및 이름 변경
- 알림/접근성 권한 상태 관리

`ui/subscription`

- RevenueCat 상품 조회
- 구매 및 복원
- 프리미엄 상태 로컬 캐시 및 Firebase 프로필 동기화

## Deep Links

현재 AllSleep은 외부 앱에서 특정 화면으로 진입하는 딥링크를 제공하지 않습니다.
대신 수면 잠금 기능을 위해 Android 시스템 이벤트와 다음 플랫폼 진입점을 사용합니다.

```text
SleepLockService          # 수면 모드 Foreground Service
AppSupervisorService      # 접근성 이벤트 기반 앱 사용 감지
SleepAlarmReceiver        # 예약된 수면/기상 이벤트 수신
LockOverlayManager        # 수면 모드 오버레이 표시
SleepScheduler            # 다음 수면 이벤트 예약
```

필수 권한과 플랫폼 제약은 화면 안에서 안내하며, 권한 상태에 따라 수면 잠금 시작 가능 여부를 제어합니다.

## Download

Google Play 출시 준비 중
