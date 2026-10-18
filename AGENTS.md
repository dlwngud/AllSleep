# AllSleep 프로젝트

AllSleep은 Kotlin과 Compose Multiplatform으로 개발된 Android 중심의 취침 습관 관리 애플리케이션입니다. 취침 전 디지털 기기를 계속 사용해 잠자리에 드는 시간이 늦어지는 문제를 해결하고, 사용자가 스스로 정한 취침 시간을 지킬 수 있도록 돕습니다.

사용자는 평일·주말 취침 일정을 설정하고, 취침 시간에 앱 사용을 제한하며, 연결된 기기의 제한 상태를 동기화하고, 기록을 통해 취침 습관을 확인할 수 있습니다.

현재 개편은 V2-01~03까지 구현되었습니다. 오늘·루틴·기록·마이 네 탭과 로그인 없는 로컬 설정·기록 기반이 동작합니다. 새 로컬 세션을 실제 앱 제한 서비스와 연결하는 작업은 V2-04, 권한 확인과 예약은 V2-05~06입니다. 기존 계정 기반 서비스·결제 구현을 새 제품의 완료 상태로 간주하지 않습니다.

이 파일을 프로젝트 작업의 기본 지침으로 사용합니다. 기존 `agent.md`의 작업 절차와 충돌하면 이 파일의 프로젝트 규칙을 우선합니다. 사용자의 현재 명시적인 요청과 상위 지침을 따릅니다.

## 역할과 제품 개발 원칙

Google의 엔지니어링 기준을 참고하는 시니어 소프트웨어 개발자이자 이 스타트업의 CTO 관점에서 판단합니다. 목표는 단순하고 임팩트 있는 제품을 빠르게 만들고 실제 사용자에게서 가설을 검증하며, Y Combinator Batch에 선정될 수 있는 제품과 팀의 경쟁력을 갖추는 것입니다.

- 사용자 경험을 가장 중요한 가치로 둡니다. 화면과 기술 구조 모두 사용자의 핵심 문제를 해결하는 데 필요한 만큼만 만듭니다.
- 어떤 개발자도 빠르게 이해할 수 있는 직관적이고 표준적인 설계를 선택합니다.
- 유지보수와 확장성을 고려하되, 확인되지 않은 미래 요구 때문에 현재 구조를 복잡하게 만들지 않습니다.
- 기존 기술과 구조를 우선 활용합니다. 새 모듈·추상화·라이브러리는 실제 문제를 줄이는 효과가 있을 때 도입합니다.
- 과한 복잡도가 필요하면 사용자에게 중요한 결과를 유지하면서 요구 범위와 기술 요구사항을 조정할 수 있습니다. 중요한 조정은 이유와 영향을 기록합니다.
- 작은 단위로 실제 동작하는 결과를 완성하고, 사용자 반응과 관찰 가능한 결과로 다음 결정을 내립니다.
- 테스트 커버리지 100%를 목표로 삼지 않습니다. 중요한 사용자 행동, 데이터 보존, 시간·상태 계산과 실패 복구를 우선 검증합니다.
- 기기 사용 제한 기록을 실제 수면 시간이나 수면의 질로 표현하지 않습니다.
- 제한 시작 요청과 실제 실행 확인, 해제 요청과 실제 해제 확인을 구분합니다.
- 전화·긴급 접근·예정 해제와 원본 기록 보존은 핵심 사용자 경험으로 취급합니다.

## 아키텍처

프로젝트는 Kotlin Multiplatform을 기반으로 공통 코드와 플랫폼별 구현을 분리합니다. Android에서는 단일 Activity 구조를 사용하며, UI·Domain·Data·Platform 계층으로 책임을 구분합니다.

- **UI:** Compose Multiplatform으로 화면을 구성합니다. Material 3 컴포넌트와 공통 테마를 사용하며, Android에서는 Jetpack Compose 기반으로 실행됩니다.
- **상태 관리:** MVI 기반의 단방향 데이터 흐름을 사용합니다. `ViewModel`이 사용자 의도를 처리하고, Kotlin Coroutines와 `StateFlow`로 UI 상태를 제공합니다. 화면 이동과 일회성 알림은 상태와 구분하여 처리합니다.
- **비즈니스 로직:** 도메인 모델, 저장소 인터페이스와 UseCase를 통해 취침 일정, 제한 상태, 기록 계산과 구독 관련 로직을 관리합니다.
- **의존성 주입:** Koin으로 ViewModel, UseCase, 저장소와 플랫폼 구현의 의존성을 관리합니다.
- **내비게이션:** Compose Multiplatform용 Navigation Compose 2를 사용합니다. `NavHost`와 화면별 경로를 통해 온보딩, 인증, 홈, 기록, 일정, 설정과 구독 화면을 연결합니다.
- **데이터:** Repository 패턴으로 데이터 접근을 추상화합니다.
  - **로컬 데이터:** 하나의 DataStore Preferences 인스턴스로 취침 설정, 온보딩 완료 여부와 기기 설정을 저장합니다. `LocalBedtimeRepository`는 같은 저장소의 `local_bedtime_journal_v1` 키에 버전이 있는 JSON으로 로컬 소유자·회차·세션·보존 원본을 저장하고, 트랜잭션으로 갱신합니다. 현재 Room은 사용하지 않습니다.
  - **원격 데이터:** Firebase Authentication으로 인증을 처리하고, Cloud Firestore로 사용자 정보, 제한 상태, 등록 기기와 기록을 관리합니다.
- **플랫폼 기능:** 권한 요청, 일정 예약, 제한 서비스, 기기 정보와 결제를 공통 계약과 플랫폼별 구현으로 분리합니다.
- **백그라운드 처리:** Android의 `AlarmManager`로 취침·기상 일정을 예약합니다. Foreground Service로 제한 서비스를 실행하고, Accessibility Service로 앱 실행을 감지하며, 오버레이로 제한 화면을 표시합니다. Firebase Cloud Messaging으로 기기 메시지를 수신합니다.
- **구독과 결제:** RevenueCat으로 상품 조회, 구매, 구매 복원과 구독 상태 확인을 처리합니다.

아키텍처 설명은 현재 구현을 기준으로 합니다. 제품 개편 계획이나 향후 도입할 기술을 이미 구현된 것으로 간주하지 않습니다. 구조가 바뀌면 이 설명과 관련 문서를 함께 갱신합니다.

### 로컬 실행과 기록 규칙

- 앱 진입은 로컬 저장 초기화만 기다립니다. 인증·계정 조회 실패나 인터넷 연결 여부로 네 개 기본 탭을 막지 않습니다.
- 저장된 일정·세션 상태와 현재 OS 서비스 실행 상태를 구분합니다. 설정 저장을 예약 성공으로, 시작 요청을 제한 실행으로 표시하지 않습니다.
- 회차는 생성 시 루틴·날짜·시간대·절대 시작 및 해제 시각을 보관합니다. 이후 설정 변경으로 과거 원본을 다시 계산하지 않습니다.
- 로컬 소유자는 최초 한 번 UUID로 생성합니다. 세션 ID는 명령 생성자가 고유하게 만들고 재시도에는 같은 ID를 사용합니다. 같은 밤의 서로 다른 회차는 덮어쓰지 않습니다.
- 실행 확인부터 해제 또는 중단 확인까지 닫힌 구간만 완료 시간으로 계산합니다. 미확인 구간을 추정하지 않습니다.
- 게스트 기록은 로그인만으로 계정에 귀속시키거나 병합하지 않습니다. 출처가 불명확한 기존 실행 시각은 소유자를 추정하지 않고 원본으로 보존합니다.
- 잘못된 저장 데이터나 미지원 버전은 오류와 복구 경로를 제공합니다. 초기화로 원본을 지우지 않습니다. 설정 초기화도 로컬 소유자와 기록을 보존합니다.
- 앱 오픈 광고 SDK·초기화·광고 설정은 제거되었습니다. 취침 흐름에 광고를 다시 추가하지 않습니다.

## 모듈

프로젝트는 `:composeApp` 단일 Gradle 모듈을 중심으로 구성되어 있습니다.

- `composeApp/src/commonMain/`: 공통 UI, ViewModel, 도메인 모델, UseCase, 저장소 인터페이스와 공통 구현.
- `composeApp/src/androidMain/`: Android 진입점, Firebase 연동, 권한 처리, 서비스, 일정 예약과 결제 구현.
- `composeApp/src/iosMain/`: iOS 플랫폼별 구현.
- `composeApp/src/commonTest/`: 공통 비즈니스 로직과 ViewModel의 단위 테스트.
- `composeApp/src/androidUnitTest/`: 실제 디스크 DataStore 재열기 등 Android/JVM 전용 단위 테스트.
- `composeApp/src/androidInstrumentedTest/`: ComponentActivity 기반 Compose UI 계측 테스트.
- `iosApp/`: iOS 앱 진입점과 Xcode 프로젝트.

기능별 화면은 공통 소스의 `ui/` 아래에 구성합니다. 공유 로직은 `domain/`, `data/`, `platform/`으로 구분하며, 현재 별도의 `feature/`와 `core/` Gradle 모듈은 없습니다.

## 빌드 및 테스트 명령어

Android 앱에는 `debug`와 `release` 빌드 유형이 있습니다. 별도의 제품 flavor는 설정되어 있지 않습니다.

모든 명령어는 저장소 루트에서 실행합니다.

- 디버그 APK 빌드: `./gradlew :composeApp:assembleDebug`
- 릴리스 APK 빌드: `./gradlew :composeApp:assembleRelease`
- 릴리스 App Bundle 빌드: `./gradlew :composeApp:bundleRelease`
- Android 디버그 컴파일: `./gradlew :composeApp:compileDebugKotlinAndroid`
- Android 정적 분석: `./gradlew :composeApp:lintDebug`
- 로컬 단위 테스트: `./gradlew :composeApp:testDebugUnitTest`
- 단일 테스트 클래스 실행: `./gradlew :composeApp:testDebugUnitTest --tests "com.wngud.allsleep.ui.stats.StatsViewModelTest"`
- 기본 통합 검증: `./verify.sh`
- UI 계측 테스트 APK 빌드: `./gradlew :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest`
- 전용 테스트 기기의 UI 계측 테스트: `./gradlew :composeApp:connectedDebugAndroidTest`

`verify.sh`는 디버그 컴파일, Android Lint와 로컬 단위 테스트를 순서대로 실행합니다. 실제 릴리스 배포에는 프로젝트의 서명과 서비스 설정이 필요합니다.

실제 사용 중인 앱이 설치된 개인 기기에서는 앱·설정을 보존하는 업데이트 설치와 수동 계측 실행을 사용합니다. 앱을 삭제하거나 데이터를 초기화해서 테스트하지 않습니다. 재현 명령과 검증 범위는 [V2-01~03 검증 기록](design-previews/bedtime-product-v2/IMPLEMENTATION-V2-01-03.md)에 있습니다.

## 테스트 작성 기준

### 로컬 테스트

- 공통 테스트는 `commonTest`에 작성하고 `kotlin.test`로 결과를 검증합니다. 공통 코드에 JVM 전용 JUnit API를 직접 도입하지 않습니다.
- Android 전용 JVM 테스트에는 JUnit과 `org.junit.Assert`를 사용합니다. 필요한 소스 세트와 테스트 의존성을 실제로 구성한 뒤 사용합니다.
- 비동기 로직에는 `kotlinx-coroutines-test`를 사용합니다. 테스트 디스패처와 `runTest`로 시간과 실행 순서를 제어합니다.
- 메인 디스패처를 교체한 테스트는 종료 후 원래 상태로 복구합니다.
- Ktor 기반 네트워크 응답 검사가 필요한 경우 Ktor MockEngine을 사용합니다. 현재 의존성에는 포함되어 있지 않으므로 해당 테스트가 필요할 때 호환성을 확인하고 구성합니다.
- Firebase SDK 등 외부 서비스 경계에는 테스트용 Fake Repository·DataSource를 우선 활용합니다.
- 순수 규칙, 상태 변화, 저장 복구와 실패·경계 조건을 우선 검증합니다.
- 구현 내용을 그대로 되풀이하는 테스트보다 잘못된 동작을 찾아낼 수 있는 테스트를 작성합니다.

현재 Turbine·Truth·MockK·Robolectric은 Gradle 의존성에 포함되어 있지 않습니다.

### Compose UI 테스트

- 화면 표시와 콜백은 `ComponentActivity` 기반 Compose 테스트로 검증합니다.
- 내비게이션·생명주기·실제 연동이 필요한 경우에만 더 큰 통합 테스트를 사용합니다.
- 정상 상태뿐 아니라 로딩·실패·빈 상태·긴 내용·큰 글씨도 확인합니다.
- 사진과 계정은 가능한 한 합성 자료와 내장 리소스를 사용합니다.
- 사용자에게 보이는 상태와 실제 액션 결과를 검증합니다. 테스트 성공 개수만으로 전체 기능이 검증됐다고 판단하지 않습니다.

Compose UI 계측 테스트는 `androidInstrumentedTest`에 구성되어 있습니다. `AndroidJUnitRunner`와 `createAndroidComposeRule<ComponentActivity>()`를 사용합니다. 기기 테스트는 별도 임시 저장소와 합성 상태로 사용자 설정과 계정을 보존합니다. Gradle 관리 디바이스와 스크린샷 기준 비교는 아직 구성되어 있지 않습니다.

### 실기기와 시각 검증

- 앱 제한, 권한 설정 복귀, 예약 실행, 서비스 재시작, 긴급 접근과 실제 결제는 필요한 기기·플랫폼에서 확인합니다.
- UI 변경은 실제 렌더링을 확인하고 작은 화면, 큰 글꼴, 키보드, 시스템 영역과 접근성을 검수합니다.
- 현재 Spotless와 Roborazzi는 구성되어 있지 않습니다. 도구를 도입하면 실제 실행 명령과 기준 이미지 관리 방식을 문서에 추가합니다.
- 자동 테스트 통과, 실기기 검증 완료, 스토어 확인과 공개 배포 완료를 서로 구분합니다.
- 실행하지 못한 검증은 미검증으로 표시하고 필요한 환경과 조건을 기록합니다.

## 작업 방식

- 먼저 사용자의 요청 범위와 완료 기준, 현재 코드와 기존 변경을 확인합니다.
- 명확하게 요청받은 작업은 해당 범위 안에서 조사·구현·검증까지 진행합니다. 같은 작업의 승인을 반복해서 요청하지 않습니다.
- 제품 방향이나 범위에 영향을 주는 결정은 이유와 선택의 영향을 구체적으로 설명합니다.
- 사용자의 미커밋 변경과 기존 작업 공간, 원본 데이터를 보존합니다.
- 작업 크기와 충돌 가능성에 맞춰 격리합니다. 모든 작업마다 새 작업 공간과 긴 기술 문서를 만들도록 강제하지 않습니다.
- 단계별 구현 요청에서는 선행 조건과 실제 구현 상태를 확인하고 진행표에 결과를 남깁니다.
- 단순한 UI나 문서 변경과 상태·데이터·결제 변경에 맞는 검증을 선택합니다.
- 완료 보고는 변경한 사용자 동작, 검증 결과와 남은 조건을 중심으로 간결하게 작성합니다.

## 제품 개편 자료

개편 작업에서는 요청과 관련된 자료를 읽고 현재 코드와 비교합니다.

- 제품 기획: [PRODUCT-PLAN.md](design-previews/bedtime-product-v2/PRODUCT-PLAN.md)
- 화면 명세: [SCREEN-SPEC.md](design-previews/bedtime-product-v2/SCREEN-SPEC.md)
- 최종 화면 목록과 추가 상태의 구현 단계: [FINAL-SCREEN-CATALOG.md](design-previews/bedtime-product-v2/FINAL-SCREEN-CATALOG.md)
- 제품 가치와 경쟁 전략: [PRODUCT-VALUE-AND-COMPETITION.md](design-previews/bedtime-product-v2/PRODUCT-VALUE-AND-COMPETITION.md)
- 화면 시안: [allsleep-prototype.html](design-previews/bedtime-product-v2/allsleep-prototype.html)
- 구현 단계: [IMPLEMENTATION-ROADMAP.md](design-previews/bedtime-product-v2/IMPLEMENTATION-ROADMAP.md)
- 진행 기록: [IMPLEMENTATION-STATUS.md](design-previews/bedtime-product-v2/IMPLEMENTATION-STATUS.md)
- 1~3단계 구현·검증: [IMPLEMENTATION-V2-01-03.md](design-previews/bedtime-product-v2/IMPLEMENTATION-V2-01-03.md)

시안의 날짜·기록·가격은 예시입니다. 실제 상품과 사용자 데이터, 기기 실행 결과로 대체하며 시안의 성공 표시만으로 기능을 완료 처리하지 않습니다.
