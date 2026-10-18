# Google Play 출시 전 점검 리포트 - AC8 앱 시작 성능

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, Manifest, 기존 문서 기준. Google Play Console, Android vitals, Firebase/AdMob/RevenueCat 운영 콘솔, 실제 단말 프로파일링 결과는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 광고 SDK/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `:164-168`; `composeApp/build.gradle.kts:158-163`; `admob.properties:5`, `:8` | release 설정에는 운영 AdMob 앱 ID와 앱 오픈 광고 단위 ID가 있지만, 실제 앱 오픈 광고 로딩 코드는 Google 테스트 광고 단위 ID를 하드코딩한다. 운영 빌드에서 테스트 ID가 남으면 실제 운영 광고의 로드 시간, no-fill, 실패율, 콜드 스타트 지연을 검증하지 못하고 수익화도 정상 동작하지 않을 수 있다. | 출시 전 앱 오픈 광고 로딩 경로가 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`의 release 운영 값을 사용하도록 확인한다. 광고 및 AdMob 테스트 ID를 실제 운영 ID로 전환한 release 후보에서 콜드 스타트, 네트워크 지연, 광고 실패, 광고 닫기 후 첫 화면 표시 시간을 측정한다. | true |
| P0 | 결제/성능 | `composeApp/build.gradle.kts:103-113`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:28-35`, `:72-82`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:292-310` | RevenueCat API 키는 `local.properties`에서 주입되고 앱 시작 시 `Application.onCreate`에서 즉시 configure된다. 운영 API 키/운영 구독 상품 전환이 확정되지 않으면 실제 운영 결제 SDK 초기화와 구독 상태 조회 지연을 출시 후보에서 검증할 수 없다. | RevenueCat 테스트 API 키와 테스트 구독관리 설정을 운영 API 키, 운영 상품, 운영 entitlement로 전환했는지 출시 게이트에 포함한다. 운영 설정이 들어간 release 후보에서 최초 실행, 로그인 직후, 앱 복귀 시 구독 상태 조회 시간이 첫 화면 표시를 지연시키는지 측정한다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 성능/UX | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/MainActivity.kt:48-76`, `:115-123`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:126-187` | `installSplashScreen()` 후 `setKeepOnScreenCondition { !isColdStartFinished }`로 네이티브 스플래시를 유지하고, 콜드 스타트 광고 로드가 최대 3초까지 첫 화면 표시를 지연시킨다. 타임아웃은 있지만 네트워크 지연이나 광고 SDK 응답 실패 시 사용자는 앱이 멈춘 것처럼 느낄 수 있다. | 앱 오픈 광고는 첫 화면 렌더 이후 프리로드/노출하는 방식 또는 첫 실행 제외 정책을 검토한다. 현재 정책을 유지한다면 P50/P95 cold start time, time to first draw, splash 유지 시간을 release 후보에서 측정하고 3초 timeout이 실제 UX 기준을 넘지 않는지 확인한다. | true |
| P1 | 성능/안정성 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:28-69`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt:62-89` | `Application.onCreate`에서 RevenueCat configure, AdMob initialize, DataStore context 초기화, Kakao SDK 초기화, Firebase 옵션 조회, Koin 시작을 한 번에 수행한다. 일부는 백그라운드 코루틴이지만 SDK 클래스 로딩과 초기화가 프로세스 시작 비용을 키울 수 있다. | 첫 화면에 필수인 DataStore/Koin 최소 초기화와 지연 가능 SDK 초기화를 구분한다. RevenueCat, AdMob, Firebase 진단 로그처럼 첫 프레임 이전 필수성이 낮은 작업은 첫 화면 렌더 후 또는 로그인/구독/광고 진입 시점으로 늦출 수 있는지 검토한다. | true |
| P1 | 성능/개인정보 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:32`, `:54-61`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt:68-76` | RevenueCat 로그 레벨이 빌드 타입 조건 없이 `DEBUG`로 설정되고, Firestore 로깅도 항상 활성화된다. 릴리즈 시작 경로에서 불필요한 로그 생성과 SDK 진단 비용이 생길 수 있고, 개인정보/운영 로그 노출 리스크도 있다. | 릴리즈 빌드에서는 RevenueCat/Firestore 디버그 로깅을 비활성화하고, 출시 전 성능 측정은 별도 QA 빌드 또는 안전한 계측으로 수행한다. 시작 성능 기준에서는 로그 비용을 제거한 release 빌드를 기준으로 판단한다. | true |
| P1 | 성능/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/App.kt:36-80`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:72-74`, `:141-167` | Compose `App()`은 전역 ViewModel 초기화가 끝날 때까지 빈 화면을 보여준다. 초기화에는 프리미엄 캐시, 온보딩 여부, 현재 사용자, 세션 검증, 사용자 세션 처리, 구독 상태 refresh가 포함된다. suspend 기반이라 ANR 패턴은 아니지만 실제 네트워크/스토어 지연이 첫 의미 있는 화면 표시를 늦출 수 있다. | 첫 화면 라우팅에 필요한 최소 데이터와 시작 후 동기화 가능한 데이터를 분리한다. 세션 검증, 구독 상태 refresh, 기기 등록/동기화는 첫 화면 렌더 후 상태 업데이트로 전환 가능한지 검토하고, 로그인 상태/비로그인 상태 각각의 time to interactive를 측정한다. | true |
| P1 | 성능/안정성 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/MainActivity.kt:78-113`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:313-400`, `:408-428` | `MainActivity.onCreate` 직후 전역 수면 상태를 관찰하고 초기 상태에 따라 `SleepLockService.start/stop`을 호출하며 FCM 토큰도 조회한다. 로그인 사용자의 경우 ViewModel은 Firestore 관찰, 기기 등록, FCM 토큰 조회까지 이어질 수 있어 첫 화면 진입과 백그라운드 초기 동기화가 동시에 몰린다. | 시작 직후 반드시 필요한 잠금 복구 작업과 지연 가능한 기기 등록/토큰 갱신/원격 관찰 작업을 분리한다. 수면 중 복구는 우선 유지하되, 일반 상태에서는 첫 화면 표시 이후 단계적으로 실행하고 실제 기기에서 startup trace로 병목을 확인한다. | true |
| P1 | 성능/품질 | `composeApp/build.gradle.kts:152-154`; `composeApp/proguard-rules.pro:1-2`; `gradle/libs.versions.toml:60-65`; 저장소 검색 결과 baseline profile/macrobenchmark 모듈 부재 | release 빌드는 minify가 켜져 있지만 리소스 축소, Baseline Profile, Macrobenchmark/JankStats 계측 근거는 확인되지 않는다. Compose/KMP 앱은 첫 실행 JIT와 Compose 초기 렌더 비용이 커질 수 있어, 프로파일 기반 최적화가 없으면 저사양 기기에서 시작 시간이 길어질 수 있다. | Google Play 출시 전 Baseline Profile 또는 Macrobenchmark를 도입해 cold start/warm start/hot start를 계측한다. 최소한 내부 테스트 트랙 release AAB 기준으로 Android Studio Startup Profiler와 Play Pre-launch report의 startup time을 확인한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 성능/품질 | `composeApp/src/androidMain/AndroidManifest.xml:42-48`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:43-46` | AdMob 최적화 플래그와 백그라운드 초기화는 적용되어 있으나, 실제 광고 SDK 초기화 비용과 첫 광고 프리로드 타이밍에 대한 저장소 내 측정 근거는 없다. | 출시 후 Android vitals와 자체 로그/계측으로 AdMob 초기화 시간, 첫 광고 로드 시간, 스플래시 유지 시간을 버전별로 추적한다. no-fill 또는 네트워크 실패가 잦으면 콜드 스타트 광고 정책을 완화한다. | true |
| P2 | 성능/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/App.kt:73-80` | 초기화 중 빈 화면만 보여주는 방식은 깜빡임은 줄이지만, 지연이 길어질 때 진행 상태나 복구 행동을 제공하지 않는다. 실제 지연이 1초 이상이면 “앱이 비어 있음”으로 인식될 수 있다. | 시작 성능 측정 결과 지연이 남는다면 브랜드 스플래시 이후 가벼운 로딩 상태 또는 즉시 표시 가능한 온보딩/로그인 뼈대를 제공한다. 단, UI 추가는 실제 측정 결과에 기반해 진행한다. | true |
| P2 | 성능/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:189-237`, `:313-400` | 전역 설정 동기화와 Firestore 관찰은 앱 수명 동안 유지된다. 시작 직후 초기 이벤트가 몰릴 경우 직접적인 출시 차단은 아니지만 첫 화면 이후 프레임 드롭이나 배터리 사용 증가로 이어질 수 있다. | 출시 후 JankStats, Firebase Performance Monitoring 또는 자체 trace로 앱 시작 후 10초 구간의 프레임 지연, Firestore listener 이벤트 수, DataStore read/write 빈도를 추적한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 성능/스토어 등록 | Google Play Console Pre-launch report, Android vitals | 저장소만으로는 실제 cold start/warm start/hot start 시간, 기기별 slow start 비율, Android vitals의 excessive slow sessions 여부를 확인할 수 없다. 앱 오픈 광고, RevenueCat, Firebase, Kakao SDK 초기화는 네트워크와 단말 상태에 따라 편차가 크다. | release AAB를 내부 테스트 트랙에 업로드하고 Pre-launch report 및 Android vitals에서 startup time, ANR, slow rendering을 확인한다. 최초 설치 후 첫 실행, 로그인 사용자 재실행, 수면 중 잠금 복구 실행, 네트워크 끊김 상태를 시나리오에 포함한다. | false |
| P0 | 광고 SDK | AdMob Console, 운영 release 빌드 런타임 | 저장소에는 release 운영 ID 값과 테스트 ID 하드코딩이 함께 존재한다. 운영 AdMob 앱/광고 단위 승인 상태, 실제 fill rate, 로드 지연, 정책 센터 이슈는 저장소만으로 확인할 수 없다. | AdMob 테스트 ID를 실제 운영 ID로 전환한 release 후보에서 앱 오픈 광고 로드 시간과 실패율을 측정하고, AdMob 콘솔에서 앱 승인/광고 단위 활성화/app-ads.txt 필요 여부를 별도 확인한다. | false |
| P0 | 결제 | RevenueCat 대시보드, Play Console 구독 상품 | 저장소만으로는 RevenueCat 운영 API 키, 운영 상품/entitlement, Play Console 구독 상품 연결 상태와 실제 SDK 응답 시간을 확정할 수 없다. 테스트 API 키나 샌드박스 구독관리 설정이 남아 있으면 운영 시작 성능과 구매 상태 동기화 지연을 검증하지 못한다. | RevenueCat 테스트 API 키와 테스트 구독관리 ID를 운영 ID/운영 상품으로 전환했는지 확인한다. 내부 테스트 트랙에서 상품 조회, 구매, 복원, 앱 재시작 후 프리미엄 상태 반영 시간을 측정한다. | false |
| P1 | 성능/품질 | Android Studio Startup Profiler, Macrobenchmark, 실제 단말 로그 | 저장소에는 시작 성능 자동 회귀 테스트나 기준치 문서가 확인되지 않는다. 코드상 리스크는 식별 가능하지만 실제 첫 프레임/상호작용 가능 시간의 합격 여부는 계측 없이는 판단할 수 없다. | 출시 전 최소 기준을 정한다. 예: cold start P95, warm start P95, 스플래시 최대 유지 시간, 로그인 사용자 첫 화면 표시 시간. 이후 Macrobenchmark 또는 CI 성능 테스트로 회귀를 추적한다. | false |

## 결론

현재 저장소 기준으로 앱 시작 성능의 가장 큰 출시 전 리스크는 앱 오픈 광고가 스플래시를 최대 3초 유지하는 구조, 앱 시작 시 여러 SDK와 전역 동기화가 한꺼번에 초기화되는 구조, 그리고 광고/RevenueCat 운영 ID 전환 검증이 저장소만으로 완결되지 않는 점이다. 출시 전에는 AdMob 테스트 ID와 RevenueCat 테스트 구독관리 설정을 운영 ID로 전환한 release 후보를 기준으로 실제 콜드 스타트와 첫 화면 표시 시간을 계측해야 한다.
