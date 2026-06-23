# AllSleep 아키텍처 개선 구현 계획

> **작업 에이전트 필수 안내:** 이 계획을 실행할 때는 `superpowers:subagent-driven-development` 또는 `superpowers:executing-plans`를 사용해 작업 단위별로 진행한다. 각 단계는 체크박스(`- [ ]`)로 추적한다.

**목표:** AllSleep의 현재 MVVM/MVI + Repository 구조를 유지하면서, Google 권장 Android 아키텍처와 Clean Architecture 원칙에 더 가깝게 개선한다.

**아키텍처 방향:** 당장은 `:composeApp` 단일 모듈을 유지한다. 대신 패키지 경계를 명확히 하고, Composable과 비대한 ViewModel에 섞여 있는 비즈니스 로직을 관심사별 UseCase로 이동한다. `GlobalSleepCoordinator`, `SleepSessionManager`처럼 넓은 전역 객체를 새로 만들지 않는다. ViewModel은 화면 상태 관리와 사용자 Intent 처리에 집중하게 만든다.

**기술 스택:** Kotlin Multiplatform, Jetpack Compose, Koin, StateFlow, Coroutines, Firebase, RevenueCat.

---

## 개선 범위

이번 계획은 이전에 지적한 1~4번 개선안을 대상으로 한다.

1. `GlobalSleepViewModel`의 책임을 분리한다.
2. `HomeScreen`에서 Repository 직접 접근을 제거한다.
3. 구독/결제 로직을 domain use case 뒤로 감춘다.
4. 통계 계산 로직을 `StatsViewModel`에서 분리한다.

이번 작업에서는 Gradle 멀티모듈 분리는 하지 않는다. 멀티모듈화는 구조 안정화 이후 별도 작업으로 진행하는 것이 안전하다.

---

## 리팩토링 불변 조건

이 작업은 기능 추가나 UX 개선이 아니라 리팩토링이다. 따라서 사용자에게 보이는 동작, 화면, 문구, 저장 데이터, 결제 흐름, 수면모드 동작, 통계 계산 결과가 바뀌면 안 된다.

반드시 지켜야 하는 불변 조건은 다음과 같다.

- 홈 화면의 수면 목표 시간 표시가 기존과 동일해야 한다.
- 통계 화면의 계산 결과가 기존과 동일해야 한다.
- 구독 화면의 상품 목록, 기본 선택 상품, 결제 흐름, 구매 복원 흐름이 기존과 동일해야 한다.
- RevenueCat product ID, offering ID, entitlement ID, API key는 변경하지 않는다.
- Google Play 상품 ID와 무료 체험 설정은 변경하지 않는다.
- 수면모드 시작/종료 동작이 기존과 동일해야 한다.
- Overlay, AccessibilityService, Foreground Service 동작이 기존과 동일해야 한다.
- Firebase 저장 구조와 로컬 DataStore 저장 구조를 변경하지 않는다.
- UI 문구, 화면 구조, navigation route를 변경하지 않는다.
- 광고 ID, 앱 버전, 빌드 설정을 변경하지 않는다.
- 같은 입력에 대해 같은 UI 상태, 같은 저장 값, 같은 remote 업데이트, 같은 effect, 같은 에러 메시지가 나와야 한다.

기능 변화가 필요한 경우 이 계획에서 처리하지 않는다. 별도 기능 개선 작업으로 분리한다.

---

## 각 단계별 필수 실행 게이트

각 작업은 반드시 아래 순서를 지킨다. 한 단계를 건너뛰고 다음 작업으로 넘어가지 않는다.

```text
1. 테스트 또는 현재 동작 기준 확인
2. UseCase / ViewModel / DI 수정
3. compileDebugKotlinAndroid 실행
4. 관련 단위 테스트 실행
5. 실제 앱에서 해당 기능 수동 확인
6. 다음 단계로 이동
```

세부 기준은 다음과 같다.

1. 테스트 또는 현재 동작 기준 확인
   - 리팩토링 전에 현재 동작을 테스트, 로그, 화면 확인 중 하나로 고정한다.
   - 통계 계산처럼 값이 중요한 기능은 기존 결과를 expected 값으로 먼저 기록한다.
   - 결제와 수면모드처럼 외부 시스템과 연결된 기능은 현재 정상 흐름을 수동으로 확인한다.

2. UseCase / ViewModel / DI 수정
   - 구조만 변경한다.
   - 상품 ID, 설정 값, 화면 문구, navigation, 저장 schema는 변경하지 않는다.
   - 기존 로직의 계산식과 분기 조건을 임의로 개선하지 않는다.

3. `compileDebugKotlinAndroid` 실행
   - 각 작업 단위마다 반드시 실행한다.
   - 실패하면 다음 단계로 넘어가지 않는다.

4. 관련 단위 테스트 실행
   - 변경한 영역의 테스트를 우선 실행한다.
   - 공통 로직을 옮겼다면 새 UseCase 단위 테스트를 추가한다.
   - 기존 테스트 기대값을 리팩토링 결과에 맞춰 임의 변경하지 않는다.

5. 실제 앱에서 해당 기능 수동 확인
   - Home 변경 후 홈 화면 수면 목표 표시를 확인한다.
   - Stats 변경 후 통계 값과 탭 동작을 확인한다.
   - Subscription 변경 후 상품 로딩, 결제, 복원을 확인한다.
   - GlobalSleep 변경 후 수면모드 시작/종료, Overlay, Foreground Service, 기록 저장을 확인한다.

6. 다음 단계로 이동
   - compile, 테스트, 수동 확인이 모두 끝난 뒤에만 다음 작업으로 넘어간다.

---

## 목표 파일 구조

```text
composeApp/src/commonMain/kotlin/com/wngud/allsleep/
├── domain/
│   ├── model/
│   │   ├── HomeSleepGoal.kt
│   │   ├── SubscriptionModels.kt
│   │   └── stats/
│   │       ├── SleepReport.kt
│   │       ├── SleepPattern.kt
│   │       └── SleepLockEffect.kt
│   ├── repository/
│   │   ├── BillingRepository.kt
│   │   ├── SleepRecordRepository.kt
│   │   └── SleepSettingsRepository.kt
│   └── usecase/
│       ├── home/
│       │   └── ObserveHomeSleepGoalUseCase.kt
│       ├── sleep/
│       │   ├── ObserveSleepModeStateUseCase.kt
│       │   ├── StartSleepModeUseCase.kt
│       │   ├── StopSleepModeUseCase.kt
│       │   ├── RegisterCurrentDeviceUseCase.kt
│       │   └── SyncSleepSettingsUseCase.kt
│       ├── stats/
│       │   ├── CalculateSleepReportUseCase.kt
│       │   ├── CalculateSleepPatternUseCase.kt
│       │   └── CalculateSleepLockEffectUseCase.kt
│       └── subscription/
│           ├── LoadSubscriptionPackagesUseCase.kt
│           ├── PurchaseSubscriptionUseCase.kt
│           ├── RestorePurchasesUseCase.kt
│           └── SyncPremiumStatusUseCase.kt
├── data/
│   └── repository/
│       └── BillingRepositoryImpl.kt
└── ui/
    ├── global/
    │   └── GlobalSleepViewModel.kt
    ├── home/
    │   ├── HomeViewModel.kt
    │   └── HomeScreen.kt
    ├── stats/
    │   └── StatsViewModel.kt
    └── subscription/
        └── SubscriptionViewModel.kt
```

---

## 작업 1: GlobalSleepViewModel 책임 분리

**대상 파일**
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/ObserveSleepModeStateUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/StartSleepModeUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/StopSleepModeUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/RegisterCurrentDeviceUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/SyncSleepSettingsUseCase.kt`
- 수정: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt`
- 수정: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt`
- 테스트: `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModelTest.kt`

### 현재 문제

`GlobalSleepViewModel`은 현재 ViewModel이지만 Koin에서 `singleOf(::GlobalSleepViewModel)`로 등록되어 있다.

이 구조는 다음 문제가 있다.

- 화면 생명주기와 분리된다.
- Activity가 destroy되어도 살아 있을 수 있다.
- `onCleared()` 호출 보장이 약해진다.
- 전역 상태와 화면 상태가 섞인다.
- 수면모드, 기기 등록, 프리미엄 상태, 설정 동기화, 기록 저장 책임이 한 클래스에 몰려 있다.

### 개선 방향

`GlobalSleepCoordinator`나 `SleepSessionManager` 같은 넓은 전역 객체를 만들지 않는다.  
이름만 바뀐 거대 객체가 될 수 있기 때문이다.

대신 `GlobalSleepViewModel` 안에 섞여 있는 변경 이유를 UseCase 단위로 분리한다.

- 수면 상태 관찰: `ObserveSleepModeStateUseCase`
- 수면모드 시작: `StartSleepModeUseCase`
- 수면모드 종료: `StopSleepModeUseCase`
- 현재 기기 등록/동기화: `RegisterCurrentDeviceUseCase`
- 수면 설정 동기화: `SyncSleepSettingsUseCase`
- 프리미엄 상태 동기화: `SyncPremiumStatusUseCase`

`GlobalSleepViewModel`은 lifecycle-aware UI state holder로 축소한다. 전역 상태를 직접 소유하지 않고, UseCase를 호출해 상태를 조립한다.

```kotlin
class GlobalSleepViewModel(
    private val observeSleepModeStateUseCase: ObserveSleepModeStateUseCase,
    private val startSleepModeUseCase: StartSleepModeUseCase,
    private val stopSleepModeUseCase: StopSleepModeUseCase,
    private val registerCurrentDeviceUseCase: RegisterCurrentDeviceUseCase,
    private val syncSleepSettingsUseCase: SyncSleepSettingsUseCase,
    private val syncPremiumStatusUseCase: SyncPremiumStatusUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GlobalSleepContract.State())
    val state = _state.asStateFlow()

    fun handleIntent(intent: GlobalSleepContract.Intent) {
        when (intent) {
            is GlobalSleepContract.Intent.ToggleSleepState -> {
                viewModelScope.launch {
                    if (intent.isSleeping) {
                        startSleepModeUseCase()
                    } else {
                        stopSleepModeUseCase()
                    }
                }
            }
            else -> {
                // 화면 상태 변경과 UI 이벤트 처리만 남긴다.
            }
        }
    }
}
```

수면 시작 UseCase는 시작 책임만 가진다.

```kotlin
class StartSleepModeUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val sleepScheduler: SleepScheduler,
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        val currentStartAt = sleepSettingsRepository.activeSleepStartAt.first()
        if (currentStartAt > 0L) return

        val now = Clock.System.now().toEpochMilliseconds()
        sleepSettingsRepository.saveActiveSleepStartAt(now)
        updateUserSleepStateUseCase(isSleeping = true)
        sleepScheduler.startSleepMode()
    }
}
```

수면 종료 UseCase는 종료와 기록 저장 책임만 가진다.

```kotlin
class StopSleepModeUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val recordSleepSessionUseCase: RecordSleepSessionUseCase,
    private val sleepScheduler: SleepScheduler,
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        val sleepStartAt = sleepSettingsRepository.activeSleepStartAt.first()
        if (sleepStartAt > 0L) {
            recordSleepSessionUseCase(startAt = sleepStartAt)
        }

        sleepSettingsRepository.saveActiveSleepStartAt(0L)
        updateUserSleepStateUseCase(isSleeping = false)
        sleepScheduler.stopSleepMode()
    }
}
```

DI 등록은 다음처럼 바꾼다.

```kotlin
factoryOf(::ObserveSleepModeStateUseCase)
factoryOf(::StartSleepModeUseCase)
factoryOf(::StopSleepModeUseCase)
factoryOf(::RegisterCurrentDeviceUseCase)
factoryOf(::SyncSleepSettingsUseCase)
viewModelOf(::GlobalSleepViewModel)
```

### 세부 단계

- [ ] `GlobalSleepCoordinator`, `SleepSessionManager`, `SleepModeController` 같은 넓은 전역 객체는 만들지 않는다.
- [ ] 기존 `GlobalSleepViewModel`에서 수면 상태 관찰 로직을 찾아 `ObserveSleepModeStateUseCase`로 분리한다.
- [ ] 수면모드 시작 로직을 `StartSleepModeUseCase`로 분리한다.
- [ ] 수면모드 종료와 기록 저장 로직을 `StopSleepModeUseCase`로 분리한다.
- [ ] 현재 기기 등록/갱신 로직을 `RegisterCurrentDeviceUseCase`로 분리한다.
- [ ] 설정 동기화 로직을 `SyncSleepSettingsUseCase`로 분리한다.
- [ ] 프리미엄 상태 동기화는 작업 3의 `SyncPremiumStatusUseCase`를 재사용한다.
- [ ] 중복 start/stop 방지는 Manager가 아니라 각 UseCase 내부의 `Mutex` 또는 저장된 `activeSleepStartAt` 검사로 처리한다.
- [ ] `GlobalSleepViewModel`은 UseCase 호출, UI 로딩 상태, effect 처리만 담당하게 줄인다.
- [ ] `AppModule.kt`에서 `singleOf(::GlobalSleepViewModel)`을 제거한다.
- [ ] `GlobalSleepViewModel`은 `viewModelOf(::GlobalSleepViewModel)`로 등록한다.
- [ ] 새 sleep use case들은 `factoryOf`로 등록한다.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`를 실행한다.
- [ ] 실제 기기에서 수면모드 시작/종료, Overlay, Foreground Service, 기록 저장을 수동 테스트한다.

### 주의점

이 작업은 수면모드 핵심 동작을 건드리므로 가장 위험도가 높다. 한 번에 전부 옮기지 말고, 관찰 로직 → 시작 로직 → 종료 로직 → 기기 등록 → 설정 동기화 순서로 나누어 이동한다. 새로 만든 UseCase가 다시 거대 객체가 되지 않도록 “하나의 UseCase는 하나의 사용자/도메인 동작만 담당한다”는 기준을 지킨다.

---

## 작업 2: HomeScreen에서 Repository 직접 접근 제거

**대상 파일**
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/HomeSleepGoal.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/home/ObserveHomeSleepGoalUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeViewModel.kt`
- 수정: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt`
- 수정: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt`
- 테스트: `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/home/HomeViewModelTest.kt`

### 현재 문제

`HomeScreen`이 `SleepSettingsRepository`를 직접 주입받고 있다.

```kotlin
fun HomeScreen(
    ...
    sleepSettingsRepository: SleepSettingsRepository = koinInject()
)
```

또한 수면 목표 시간 계산도 Composable 파일 내부에 있다.

```kotlin
private fun SleepSettingsRepository.homeSleepGoal() = combine(...)
```

이 구조는 UI 계층이 Data/Domain 계층을 직접 알고 있어 계층 경계가 약하다.

### 개선 방향

수면 목표 계산은 UseCase로 옮기고, `HomeScreen`은 `HomeViewModel`의 상태만 구독한다.

```kotlin
data class HomeSleepGoal(
    val targetMinutes: Int,
    val displayText: String,
)
```

```kotlin
class ObserveHomeSleepGoalUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
) {
    operator fun invoke(): Flow<HomeSleepGoal> {
        return combine(
            sleepSettingsRepository.weekdayBedtime,
            sleepSettingsRepository.weekdayWakeTime,
            sleepSettingsRepository.weekendBedtime,
            sleepSettingsRepository.weekendWakeTime,
        ) { weekdayBedtime, weekdayWakeTime, weekendBedtime, weekendWakeTime ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val isWeekdayTonight = today.dayOfWeek.ordinal < 5
            val targetMinutes = if (isWeekdayTonight) {
                calculateTimeDiffMinutes(weekdayBedtime, weekdayWakeTime)
            } else {
                calculateTimeDiffMinutes(weekendBedtime, weekendWakeTime)
            }

            HomeSleepGoal(
                targetMinutes = targetMinutes,
                displayText = formatGoalDuration(targetMinutes),
            )
        }
    }
}
```

```kotlin
class HomeViewModel(
    observeHomeSleepGoalUseCase: ObserveHomeSleepGoalUseCase,
) : ViewModel() {
    val sleepGoal = observeHomeSleepGoalUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeSleepGoal(targetMinutes = 480, displayText = "8h"),
        )
}
```

`HomeScreen`은 다음처럼 단순화한다.

```kotlin
@Composable
fun HomeScreen(
    ...
    homeViewModel: HomeViewModel = koinViewModel(),
    globalSleepViewModel: GlobalSleepViewModel = koinViewModel(),
) {
    val sleepGoal by homeViewModel.sleepGoal.collectAsState()

    HomeScreenContent(
        sleepGoal = sleepGoal.displayText,
        ...
    )
}
```

### 세부 단계

- [ ] `HomeSleepGoal` 모델을 추가한다.
- [ ] `ObserveHomeSleepGoalUseCase`를 추가한다.
- [ ] 기존 `HomeScreen.kt`의 수면 목표 계산 함수를 UseCase로 옮긴다.
- [ ] `HomeViewModel`을 추가한다.
- [ ] `HomeScreen`에서 `SleepSettingsRepository` 주입을 제거한다.
- [ ] `HomeScreen`에서 `homeSleepGoal()` extension 함수를 제거한다.
- [ ] `AppModule.kt`에 `factoryOf(::ObserveHomeSleepGoalUseCase)`와 `viewModelOf(::HomeViewModel)`을 등록한다.
- [ ] 평일 목표 시간, 주말 목표 시간, 잘못된 시간 문자열 fallback 테스트를 추가한다.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`를 실행한다.

### 주의점

이 작업은 위험도가 낮다. 기존 계산식을 그대로 옮기면 사용자에게 보이는 수면 목표 텍스트는 바뀌지 않아야 한다.

---

## 작업 3: 결제/구독 로직을 UseCase 뒤로 이동

**대상 파일**
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/repository/BillingRepository.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/data/repository/BillingRepositoryImpl.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/subscription/LoadSubscriptionPackagesUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/subscription/PurchaseSubscriptionUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/subscription/RestorePurchasesUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/subscription/SyncPremiumStatusUseCase.kt`
- 수정: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModel.kt`
- 수정: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt`
- 테스트: `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt`

### 현재 문제

`SubscriptionViewModel`이 다음 책임을 모두 갖고 있다.

- RevenueCat 상품 목록 로딩
- 결제 실행
- 구매 복원
- 구독 상태 조회
- Firebase 사용자 premium 값 업데이트
- 로컬 premium 상태 저장

즉 ViewModel이 UI 상태 관리뿐 아니라 결제 도메인 로직과 동기화 로직까지 담당하고 있다.

### 개선 방향

`BillingProvider`는 platform 세부 구현으로 보고, domain에는 `BillingRepository` 인터페이스를 둔다.

```kotlin
interface BillingRepository {
    suspend fun getOfferings(): List<SubscriptionPackage>
    suspend fun purchasePackage(packageId: String, context: PlatformContext): Result<PurchaseResult>
    suspend fun restorePurchases(): Result<Unit>
    suspend fun getSubscriptionStatus(): Result<SubscriptionStatus>
}
```

구현체는 `BillingProvider`를 감싼다.

```kotlin
class BillingRepositoryImpl(
    private val billingProvider: BillingProvider,
) : BillingRepository {
    override suspend fun getOfferings(): List<SubscriptionPackage> {
        return billingProvider.getOfferings()
    }

    override suspend fun purchasePackage(
        packageId: String,
        context: PlatformContext,
    ): Result<PurchaseResult> {
        return billingProvider.purchasePackage(packageId, context)
    }

    override suspend fun restorePurchases(): Result<Unit> {
        return billingProvider.restorePurchases()
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        return billingProvider.getSubscriptionStatus()
    }
}
```

프리미엄 상태 동기화는 별도 UseCase로 이동한다.

```kotlin
class SyncPremiumStatusUseCase(
    private val billingRepository: BillingRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val sleepSettingsRepository: SleepSettingsRepository,
) {
    suspend operator fun invoke(): Boolean? {
        val statusResult = billingRepository.getSubscriptionStatus()
        if (statusResult.isFailure) return null

        val isPremiumNow = statusResult.getOrThrow().isPremiumActive
        val currentUser = getCurrentUserUseCase()

        if (currentUser != null && currentUser.isPremium != isPremiumNow) {
            updateUserProfileUseCase(currentUser.copy(isPremium = isPremiumNow))
        }

        sleepSettingsRepository.savePremiumStatus(isPremiumNow)
        return isPremiumNow
    }
}
```

`SubscriptionViewModel`은 UseCase만 의존하게 만든다.

```kotlin
class SubscriptionViewModel(
    private val loadSubscriptionPackagesUseCase: LoadSubscriptionPackagesUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val syncPremiumStatusUseCase: SyncPremiumStatusUseCase,
) : ViewModel()
```

### 세부 단계

- [ ] `BillingRepository` 인터페이스를 추가한다.
- [ ] `BillingRepositoryImpl`을 추가한다.
- [ ] `LoadSubscriptionPackagesUseCase`를 추가한다.
- [ ] `PurchaseSubscriptionUseCase`를 추가한다.
- [ ] `RestorePurchasesUseCase`를 추가한다.
- [ ] `SyncPremiumStatusUseCase`를 추가한다.
- [ ] `SubscriptionViewModel`에서 `BillingProvider`, `GetCurrentUserUseCase`, `UpdateUserProfileUseCase`, `SleepSettingsRepository` 직접 의존을 제거한다.
- [ ] `SubscriptionViewModel`이 UseCase만 호출하도록 변경한다.
- [ ] `AppModule.kt`에 `BillingRepositoryImpl`과 subscription use case들을 등록한다.
- [ ] 기존 `SubscriptionViewModelTest`를 fake `BillingProvider` 중심에서 fake use case 또는 fake `BillingRepository` 중심으로 수정한다.
- [ ] `./gradlew :composeApp:testDebugUnitTest`를 실행한다.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`를 실행한다.

### 주의점

이 작업은 결제 흐름을 건드린다. 따라서 다음 값은 절대 변경하지 않는다.

- RevenueCat API Key
- Offering ID
- Entitlement ID
- Google Play product ID
- 월간/연간 상품 ID
- 무료 체험 설정

이번 작업은 구조 개선만 해야 한다.

---

## 작업 4: StatsViewModel에서 통계 계산 로직 분리

**대상 파일**
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/stats/SleepReport.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/stats/SleepPattern.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/stats/SleepLockEffect.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/stats/CalculateSleepReportUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/stats/CalculateSleepPatternUseCase.kt`
- 생성: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/stats/CalculateSleepLockEffectUseCase.kt`
- 수정: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt`
- 수정: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsContract.kt`
- 수정: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt`
- 테스트: `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt`
- 테스트: `composeApp/src/commonTest/kotlin/com/wngud/allsleep/domain/usecase/stats/CalculateSleepReportUseCaseTest.kt`

### 현재 문제

`StatsViewModel`이 다음 역할을 동시에 담당하고 있다.

- 사용자 조회
- 월간 기록 로딩
- 기간별 기록 로딩
- 선택 날짜/월 상태 관리
- 수면 점수 계산
- 목표 대비 비율 계산
- 수면 부채 계산
- 패턴 분석
- 잠금 사용 효과 계산

이 구조는 통계 기능이 커질수록 ViewModel 테스트가 무거워지고, 순수 계산 로직을 재사용하기 어렵다.

### 개선 방향

`StatsViewModel`은 데이터 로딩과 UI 상태 조립만 담당한다.  
계산 로직은 순수 UseCase로 분리한다.

```kotlin
class CalculateSleepReportUseCase {
    operator fun invoke(
        latestRecord: SleepRecord?,
        trendRecords: List<SleepRecord>,
        currentTargetMinutes: Int,
    ): SleepReport {
        // 수면 점수, 목표 대비 비율, 수면 부채, 점수 분해 계산
    }
}
```

```kotlin
class CalculateSleepPatternUseCase {
    operator fun invoke(
        records: List<SleepRecord>,
        targetMinutes: Int,
        rangeDays: Int,
    ): SleepPattern {
        // 취침 일관성, 주말 밀림, 기상 안정도, 차트 값 계산
    }
}
```

```kotlin
class CalculateSleepLockEffectUseCase {
    operator fun invoke(records: List<SleepRecord>): SleepLockEffect {
        // 잠금 사용일 평균, 미사용일 평균, 차이, 유지율 계산
    }
}
```

`StatsViewModel`의 `recalculateSummary()`는 계산을 조립하는 역할만 한다.

```kotlin
private fun recalculateSummary() {
    val current = _state.value

    val report = calculateSleepReportUseCase(
        latestRecord = current.latestRecord,
        trendRecords = current.trendRecords,
        currentTargetMinutes = current.currentTargetMinutes,
    )

    val pattern = calculateSleepPatternUseCase(
        records = current.trendRecords,
        targetMinutes = current.currentTargetMinutes,
        rangeDays = current.patternRange.days,
    )

    val lockEffect = calculateSleepLockEffectUseCase(
        records = current.trendRecords,
    )

    _state.update {
        it.copy(
            report = report,
            pattern = pattern,
            lockEffect = lockEffect,
        )
    }
}
```

### 세부 단계

- [ ] 현재 `StatsViewModel`의 계산 결과 필드를 확인한다.
- [ ] `SleepReport`, `SleepPattern`, `SleepLockEffect` domain model을 추가한다.
- [ ] `CalculateSleepReportUseCase`를 추가하고 기존 수면 점수/요약 계산식을 옮긴다.
- [ ] 기록 없음, 목표 미달, 목표 달성, 목표 초과 케이스 테스트를 추가한다.
- [ ] `CalculateSleepPatternUseCase`를 추가하고 기간별 패턴 계산식을 옮긴다.
- [ ] 7일, 14일, 30일 범위 테스트를 추가한다.
- [ ] `CalculateSleepLockEffectUseCase`를 추가하고 잠금 사용/미사용 비교 계산식을 옮긴다.
- [ ] 기록 없음, 잠금 사용 기록만 있음, 잠금 사용/미사용 기록이 섞인 케이스 테스트를 추가한다.
- [ ] `StatsViewModel` 생성자에 통계 계산 UseCase를 주입한다.
- [ ] `StatsViewModel`에서는 계산식을 제거하고 UseCase 호출만 남긴다.
- [ ] `AppModule.kt`에 통계 UseCase들을 등록한다.
- [ ] `./gradlew :composeApp:testDebugUnitTest`를 실행한다.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`를 실행한다.

### 주의점

통계 값은 사용자에게 직접 보이는 프리미엄 기능이다. 계산식을 바꾸는 작업이 아니라, 기존 계산식을 그대로 이동하는 작업으로 제한한다. 리팩터링 전후 값이 달라지면 안 된다.

---

## 권장 실행 순서

1. `HomeScreen`에서 Repository 직접 접근 제거  
   위험도가 낮고, 아키텍처 개선 패턴을 먼저 만들 수 있다.

2. `StatsViewModel` 계산 로직 분리  
   순수 계산 로직 중심이라 테스트로 검증하기 좋다.

3. 결제/구독 로직 UseCase 분리  
   결제 흐름은 민감하므로 상품 ID와 RevenueCat 설정을 건드리지 않고 구조만 이동한다.

4. `GlobalSleepViewModel` 책임 분리  
   수면모드 핵심 동작과 연결되어 가장 위험하므로 마지막에 작은 단계로 진행한다.

출시 안정성이 최우선이라면 1번과 2번만 먼저 진행하고, 3번과 4번은 내부 테스트 빌드에서 충분히 검증한 뒤 반영한다.

---

## 검증 명령어

각 작업 후 실행한다.

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

전체 작업 완료 후 실행한다.

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:bundleRelease
```

---

## 수동 QA 체크리스트

```text
1. 내부 테스트 빌드에서 Google 로그인을 수행한다.
2. 홈 화면에서 수면 목표 시간이 기존과 동일하게 표시되는지 확인한다.
3. 홈 화면에서 수면모드를 시작한다.
4. Foreground Service가 유지되는지 확인한다.
5. Overlay와 AccessibilityService 기반 잠금 동작이 유지되는지 확인한다.
6. 수면모드를 종료하고 수면 기록이 저장되는지 확인한다.
7. 구독 화면에서 월간/연간 상품이 정상 로딩되는지 확인한다.
8. RevenueCat 테스트 결제를 수행한다.
9. 구매 복원을 수행하고 premium 상태가 로컬/Firebase에 반영되는지 확인한다.
10. 통계 화면에서 리포트/패턴/잠금/기록 값이 기존 계산 결과와 일치하는지 확인한다.
11. 비프리미엄 계정에서 PremiumOverlay가 계속 표시되는지 확인한다.
```

---

## 완료 기준

- `HomeScreen`이 `SleepSettingsRepository`를 직접 주입받지 않는다.
- `SubscriptionViewModel`이 `BillingProvider`를 직접 주입받지 않는다.
- `StatsViewModel`이 무거운 통계 계산식을 직접 갖고 있지 않다.
- `GlobalSleepViewModel`이 `singleOf(::GlobalSleepViewModel)`로 등록되어 있지 않다.
- 기존 사용자 동작이 바뀌지 않는다.
- 이동한 순수 로직에 대한 단위 테스트가 존재한다.
- `testDebugUnitTest`, `compileDebugKotlinAndroid`, `bundleRelease`가 통과한다.
