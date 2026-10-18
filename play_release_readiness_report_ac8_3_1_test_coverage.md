# Google Play 출시 전 점검 리포트 - AC8.3.1 테스트 커버리지

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, 테스트 코드, README, 검증 스크립트 기준. Google Play Console, Play Pre-launch report, Firebase/AdMob/RevenueCat 운영 콘솔, 실제 단말 테스트 결과는 직접 점검하지 않았다. 코드 변경은 수행하지 않았고, 본 파일은 점검 산출물이다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 광고 SDK/품질 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `:49-52`, `:164-168`; `composeApp/build.gradle.kts:145-163`; `admob.properties` | release 빌드 설정은 AdMob 앱 ID/광고 단위 ID를 프로퍼티에서 주입하지만, 실제 앱 오픈 광고 로딩 코드는 Google 테스트 광고 단위 ID를 하드코딩한다. 이 상태에서는 운영 광고 ID 전환과 운영 광고 로드/실패/쿨타임/프리미엄 제외 동작을 테스트로 검증할 수 없어 출시 후보 검증이 불완전하다. | 출시 전 광고 및 AdMob 테스트 ID를 실제 운영 ID로 전환하고, `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`가 실제 로딩 경로에 사용되는지 단위/계측 테스트 또는 release 후보 수동 테스트로 확인한다. 필수 시나리오: 비프리미엄 사용자 콜드 스타트 광고, 프리미엄 사용자 광고 미노출, 광고 로드 실패/타임아웃, 4시간 쿨타임. | true |
| P0 | 결제/품질 | `composeApp/build.gradle.kts:103-113`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt:23-30`, `:42-119`, `:121-169`; `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt:52-137` | RevenueCat API 키는 로컬 프로퍼티에서 주입되며 현재 테스트는 `SubscriptionViewModel`의 fake `BillingProvider` 기반 복원/선택 일부만 검증한다. RevenueCat 테스트 API 키, 테스트 구독관리 ID, 샌드박스 entitlement가 운영 API 키/상품/entitlement로 전환됐는지 저장소 테스트만으로 보장하지 못한다. | 출시 전 RevenueCat 테스트 API 키와 테스트 구독관리 ID를 운영 ID로 전환한 release 후보에서 상품 조회, 구매, 복원, 취소/만료 후 상태 반영을 테스트한다. 가능하면 `BillingProviderImpl` 콜백 래핑 오류 경로와 `isSandbox=false` 상태를 검증하는 통합 테스트 또는 내부 테스트 트랙 체크리스트를 추가한다. | true |
| P0 | 품질/안정성 | `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ComposeAppCommonTest.kt:8-10`; `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt:109-145`; `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt:52-137`; 저장소 검색 결과 `androidTest` 디렉터리 부재 | 앱의 핵심 출시 리스크인 권한 플로우, 수면 잠금 Foreground Service, AccessibilityService, FCM 수신, 알람 Receiver, 실제 결제/광고 SDK 경로가 자동 테스트로 커버되지 않는다. 현재 테스트는 3개 파일이며 그중 1개는 예제 산술 테스트라, 출시 후보의 핵심 Android 동작 회귀를 막기 어렵다. | 출시 전 최소 P0 시나리오를 수동 QA 또는 자동 계측 테스트로 확정한다. 우선순위는 수면 잠금 시작/종료, 오버레이/접근성 권한 거부와 허용, 알람 자동 시작/종료, 앱 재시작 후 상태 복구, 광고/결제 운영 ID 전환 검증이다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 품질 | `composeApp/build.gradle.kts:76-79`; `gradle/libs.versions.toml:24-29`, `:44`; `verify.sh:24-30` | `commonTest`에는 `kotlin.test`와 `kotlinx-coroutines-test`만 연결되어 있고, `libs.versions.toml`에 JUnit/Espresso 정의는 있지만 `composeApp` 테스트 의존성으로 연결되지 않았다. `verify.sh`는 `testDebugUnitTest`를 실행하지만 UI/계측 테스트, 커버리지 리포트, 커버리지 기준은 없다. | 출시 전 `verify.sh`의 Green 여부뿐 아니라 테스트 범위 기준을 문서화한다. 최소한 ViewModel/UseCase 단위 테스트 대상을 명시하고, Android 계측 테스트가 필요한 권한/서비스/광고/결제는 별도 체크리스트 또는 Gradle 태스크로 분리한다. | true |
| P1 | 품질/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthViewModel.kt:50-123`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingViewModel.kt:20-66`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmViewModel.kt:76-150`; 저장소 검색 결과 해당 테스트 부재 | 로그인, 회원가입, 온보딩 완료, 알람 시간 저장/스케줄링은 첫 사용 경험과 핵심 루틴 설정에 직접 연결되지만 테스트가 없다. 입력 누락, use case 실패, 스케줄러 호출 누락이 출시 직전 회귀로 들어와도 자동 검출이 어렵다. | `AuthViewModel`, `OnboardingViewModel`, `AlarmViewModel`에 fake use case/repository/scheduler 기반 단위 테스트를 추가한다. 필수 케이스: 빈 이메일/비밀번호/이름, 소셜 로그인 실패, 로그인 사용자 온보딩 자동 완료, 평일/주말 시간 변경 후 `scheduleNextEvents` 호출. | true |
| P1 | 품질/안정성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:72-86`, `:141-166`, `:109-138`; 저장소 검색 결과 `GlobalSleepViewModelTest` 부재 | 전역 상태 초기화, 세션 검증 실패 시 로그아웃, 기기 교체, 프리미엄 상태 refresh, 원격 동기화가 하나의 ViewModel에 모여 있지만 자동 테스트가 없다. 이 영역은 앱 진입/로그아웃/다중 기기 제한/구독 상태와 직접 연결된다. | fake repository/use case로 `GlobalSleepViewModel` 단위 테스트를 추가한다. 우선 케이스는 캐시 프리미엄 반영, 세션 검증 실패 시 상태 초기화와 로그아웃, 기기 제한 다이얼로그, 강제 기기 교체 성공/실패, 프리미엄 refresh 실패 시 기존 상태 유지다. | true |
| P1 | 품질/권한 | `composeApp/src/androidMain/AndroidManifest.xml:5-26`, `:78-119`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:40-81`, `:122-166`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/receiver/SleepAlarmReceiver.kt:27-87` | 권한과 Android 컴포넌트 동작은 Manifest와 구현 코드에 존재하지만, 실제 Android 런타임에서의 시작/중지/권한 거부/알림 표시/Receiver 동작 테스트가 없다. 특히 `SYSTEM_ALERT_WINDOW`, 접근성 서비스, exact alarm, foreground service는 Google Play 정책과 Android 버전별 동작 차이가 크다. | 출시 전 실제 단말/에뮬레이터 체크리스트를 테스트 케이스로 고정한다. 자동화 가능 영역은 `androidTest` 또는 Robolectric로 Receiver 계산/스케줄 호출/서비스 인텐트를 검증하고, 권한 승인 화면과 접근성 설정 이동은 수동 QA 증거를 남긴다. | true |
| P1 | 품질/성능 | 저장소 검색 결과 `jacoco`, `kover`, `testCoverage`, `baselineProfile`, `macrobenchmark` 설정 부재 | 테스트 커버리지 리포트와 성능 회귀 테스트가 없어 “어느 영역이 얼마나 테스트되지 않았는지”를 수치로 확인할 수 없다. 출시 후보마다 수면 잠금/통계/결제/광고 변경이 누락 테스트를 늘리는지 추적하기 어렵다. | KMP에 맞는 Kover 또는 Jacoco 설정을 검토하고, 최소 커버리지 기준보다 “핵심 파일 커버 여부”부터 게이트화한다. 성능은 Macrobenchmark/Baseline Profile 도입 전까지 `verify.sh`와 별도 수동 성능 체크리스트를 함께 운영한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질 | `README.md:170-190`; `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt:118-145` | README의 Testing Strategy는 수면 통계, 구독 복원, 비동기 상태 갱신 중심이라고 설명하지만 실제 테스트는 통계 평균/수면부채 일부와 구독 복원 일부에 한정된다. 문서상 전략과 실제 커버리지 간 차이가 커질 수 있다. | 출시 후 README의 테스트 전략을 실제 테스트 목록과 맞춰 갱신하고, 주요 버그가 발생한 영역은 회귀 테스트로 추가한다. 테스트 파일명과 시나리오를 릴리즈 체크리스트에 연결한다. | true |
| P2 | 품질/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt`; 저장소 검색 결과 Compose UI 테스트 부재 | Compose 화면 렌더링, 접근성 라벨, 버튼 활성/비활성, 다이얼로그 표시 여부는 자동 테스트가 없다. UI 회귀는 Play 사전 출시 보고서나 수동 테스트 전까지 발견되지 않을 수 있다. | 핵심 화면부터 스냅샷 또는 Compose UI 테스트를 추가한다. 우선순위는 홈 수면 시작 버튼과 권한 안내, 알람 시간 변경, 구독 상품 목록/복원 버튼, 설정의 접근성/알림 설정 진입이다. | true |
| P2 | 품질/보안 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepRecordRepositoryImpl.kt`; 저장소 검색 결과 Firebase emulator 테스트 부재 | Firebase Auth/Firestore 경로는 fake 기반 단위 테스트도, emulator 기반 통합 테스트도 없다. 권한 오류, 네트워크 오류, 탈퇴 후 데이터 정리, 다중 기기 동시 수정 같은 운영 이슈가 출시 후에야 드러날 수 있다. | 출시 후 Firebase Emulator Suite 기반 통합 테스트를 도입해 사용자 문서 생성/삭제, 기기 등록 제한, 수면 상태 동기화, 수면 기록 저장/조회 권한을 검증한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 품질 | 로컬 Gradle 실행 환경 | 이번 점검에서 `./gradlew :composeApp:testDebugUnitTest --parallel --build-cache`는 기본 Gradle 홈 사용 시 `/Users/wngud/.gradle/...gradle-8.14.3-bin.zip.lck (Operation not permitted)`로 실패했다. `GRADLE_USER_HOME`을 작업공간으로 바꾼 실행은 `services.gradle.org` DNS 실패로 Gradle 배포판 다운로드가 불가했다. 따라서 현재 대화 환경에서는 테스트 통과 여부를 확정할 수 없다. | 네트워크와 Gradle 캐시 접근이 가능한 CI 또는 로컬 개발 환경에서 `./verify.sh`와 `:composeApp:testDebugUnitTest`를 실행해 결과를 릴리즈 증거로 남긴다. 실패 시 이 리포트의 커버리지 판단과 별개로 출시 전 반드시 수정한다. | false |
| P0 | 스토어 등록/품질 | Google Play Console Pre-launch report, 내부 테스트 트랙 | 저장소만으로는 실제 설치, 최초 실행, 권한 승인/거부, 접근성 서비스 활성화, 알람 트리거, 광고/결제 SDK 운영 응답이 Play 환경에서 정상인지 확인할 수 없다. 특히 광고 및 RevenueCat 운영 ID 전환 후 동작은 콘솔/트랙 테스트가 필요하다. | 내부 테스트 트랙에 운영 ID가 반영된 release AAB를 업로드하고 Play Pre-launch report, 실제 단말 수동 QA, 광고/결제 콘솔 로그를 함께 확인한다. | false |
| P1 | 품질/성능 | CI 대시보드, 커버리지 리포트 산출물 | 저장소에는 커버리지 리포트 산출물이나 CI 테스트 이력 파일이 없다. 현재 123개 앱 소스 파일 대비 테스트 파일은 3개로 확인되지만, 실제 라인/브랜치 커버리지 수치는 알 수 없다. | CI에서 테스트 결과 XML, 커버리지 HTML/XML, 실패 스크린샷/로그를 아티팩트로 보관한다. 출시 승인 시 최신 main/release 브랜치의 테스트 실행 일시와 커버리지 요약을 첨부한다. | false |

## 결론

현재 저장소 기준 테스트 커버리지는 `commonTest`의 통계 ViewModel 3개 케이스, 구독 ViewModel 4개 케이스, 예제 산술 테스트 1개에 집중되어 있다. Google Play 출시 전에는 광고 및 AdMob 테스트 ID와 RevenueCat 테스트 구독관리/테스트 API 키를 운영 ID로 전환한 release 후보를 기준으로 테스트해야 하며, 수면 잠금 서비스, 권한 플로우, 알람 Receiver, 인증/온보딩/전역 상태, 실제 결제/광고 SDK 경로는 출시 전 수동 QA 또는 자동 테스트 게이트로 보완해야 한다.
