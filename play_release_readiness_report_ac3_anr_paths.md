# Google Play 출시 전 점검 리포트 - AC3 ANR 가능 코드 경로

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, Manifest, 기존 문서 기준. Google Play Console, Android vitals, Firebase/AdMob/RevenueCat 운영 콘솔, 실제 단말 프로파일링 결과는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

현재 저장소 기준으로 Google Play 출시를 즉시 차단해야 할 명시적 ANR 패턴(`Thread.sleep()`, 실제 사용 중인 `runBlocking()`, 메인 스레드의 직접 파일/네트워크 동기 I/O)은 확인되지 않았다.

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:17`, 저장소 전체 `rg "runBlocking\\(|Thread\\.sleep\\("` 결과 | `runBlocking` import는 있으나 실제 호출은 확인되지 않고, `Thread.sleep()`도 확인되지 않는다. 따라서 저장소 코드만으로는 명시적인 메인 스레드 차단 ANR 출시 차단 항목은 없다. 다만 import가 남아 있어 향후 실수로 메인 스레드 차단 호출이 들어갈 여지는 있다. | 출시 전 정적 점검에서 `runBlocking`, `Thread.sleep`, 메인 스레드 직접 I/O 사용을 금지 규칙으로 유지한다. 미사용 `runBlocking` import는 후속 정리 대상으로 둔다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:29`, `:65-78`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:83-116`, `:357-363` | 포그라운드 서비스의 `serviceScope`가 `Dispatchers.Main`이고, 그 안에서 현재 사용자 조회, Firestore 기기 목록 관찰, 오버레이 생성/갱신이 이어진다. suspend 기반이라 즉시 동기 차단은 아니지만, listener 이벤트가 잦거나 오버레이 Compose 구성이 커지면 메인 스레드 프레임 지연과 “잠금 화면이 멈춘 것처럼 보이는” 사용자 영향이 생길 수 있다. | 사용자/기기 조회와 Flow 수집은 IO/default 컨텍스트에서 수행하고, `showOverlay/updateDevices`처럼 UI 접근이 필요한 최소 구간만 Main으로 전환한다. 오버레이 최초 표시와 기기 목록 업데이트에 Systrace 또는 Android Studio profiler로 프레임 지연을 확인한다. | true |
| P1 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:44-64`, `:70-85`, `:132-187`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/MainActivity.kt:75-118` | 앱 오픈 광고 로드/쿨타임 판단이 `Dispatchers.Main` 코루틴에서 사용자 조회와 DataStore `first()`를 수행하고, 콜드 스타트는 네이티브 스플래시를 최대 3초 유지한다. 코루틴 suspend 자체는 메인 스레드를 막지 않지만, 광고 SDK 콜백 지연/실패 시 앱 시작이 멈춘 것처럼 보일 수 있고 저사양 기기에서 첫 프레임 지연이 커질 수 있다. | 콜드 스타트 광고는 현재처럼 timeout을 유지하되 release 후보에서 첫 실행, 재진입, no-fill, 네트워크 지연 상태를 측정한다. 사용자 조회/DataStore 확인은 가능한 백그라운드에서 끝내고 Main에는 광고 표시 호출만 남기는 구조를 검토한다. 광고 테스트 ID를 운영 ID로 전환한 release 빌드에서 동일 시나리오를 검증한다. | true |
| P1 | 안정성/성능 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:141-167`, `:313-381`, `:451-511`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/data/repository/SleepSettingsRepositoryImpl.kt:47-169` | 전역 ViewModel 초기화, 원격 수면 상태 반영, 수면 토글 경로가 `viewModelScope` 기본 Main 컨텍스트에서 여러 DataStore `first()/edit`, Firestore 업데이트, 알람 스케줄링을 순차 실행한다. DataStore/Firebase 호출은 suspend 기반이지만, 결과 처리와 여러 저장 작업이 Main에서 직렬로 재개되며 수면 시작/중지 같은 핵심 UX에서 입력 지연이 누적될 수 있다. | 수면 토글과 원격 상태 동기화의 저장소/네트워크 작업을 `withContext(Dispatchers.IO)` 또는 repository 내부 dispatcher로 분리하고, Main에서는 상태 업데이트만 수행한다. 수면 토글 연타, 원격 동기화 수신, 알람 스케줄 변경이 동시에 발생하는 케이스를 출시 전 기기에서 ANR/프레임 드롭 기준으로 측정한다. | true |
| P1 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:27-45`, `:49-99` | 접근성 이벤트마다 IO 코루틴을 만들고, 차단 대상이면 Main으로 전환해 `delay(500)`, `startActivity`, 추가 `delay(200)`, `startService`를 수행한다. `delay`는 메인 스레드를 블로킹하지 않지만 접근성 이벤트가 빠르게 반복되면 여러 코루틴이 누적되어 앱 전환/서비스 호출이 몰릴 수 있다. | 패키지별 1초 캐시 외에 실행 중인 차단 작업 Job을 단일화하거나 최신 이벤트만 처리하도록 throttle/debounce를 강화한다. 접근성 서비스 활성 상태에서 앱 전환을 빠르게 반복하는 단말 테스트로 메인 스레드 큐 적체와 ANR 가능성을 확인한다. | true |
| P1 | 품질/성능 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:79-116`, `:140-191`, `:193-255` | 통계 화면은 최신 기록, 월간 기록, 2년 범위 기록을 순차 조회한 뒤 trend/summary 계산을 Main 컨텍스트에서 수행한다. 현재 수면 기록 규모가 작으면 영향은 제한적이나, 장기간 데이터가 누적되면 화면 진입 시 프레임 지연이나 “로딩이 멈춘 듯한” UX가 발생할 수 있다. | Firestore 조회는 병렬화 가능한지 검토하고, 2년 기록 정렬/집계는 Default dispatcher로 분리한다. 내부 테스트 데이터로 2년치 이상 기록을 넣고 통계 화면 진입 시간과 jank를 측정한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질/성능 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmViewModel.kt:82-150`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/SleepSchedulerImpl.kt:14-92`, `:131-168` | 알람 설정 변경 시 DataStore 저장 후 다시 여러 `first()`를 읽고 `AlarmManager` 예약을 수행한다. 작업량은 작지만 Main 컨텍스트에서 직렬 실행되므로 설정을 빠르게 반복 변경할 때 입력 지연이 생길 수 있다. | 저장 직후 이미 알고 있는 상태값으로 스케줄링하거나, 스케줄 계산/예약을 IO로 분리한다. 시간 선택 휠을 빠르게 조작하는 QA에서 UI 반응성을 확인한다. | true |
| P2 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/receiver/SleepAlarmReceiver.kt:25-87` | 브로드캐스트 수신기는 `Dispatchers.IO`에서 사용자 조회, DataStore 읽기, Firestore 업데이트, 알람 재스케줄링을 수행하므로 메인 스레드 직접 차단 위험은 낮다. 다만 `goAsync()` 없이 `onReceive` 후 별도 scope가 계속 동작해 프로세스 생명주기와 작업 완료 보장이 약하고, 실패/지연이 실제 알람 누락으로 이어질 수 있다. | ANR보다는 백그라운드 작업 신뢰성 관점에서 `goAsync()` 또는 WorkManager/foreground-safe 경로를 검토한다. 알람 수신 후 Firestore 지연/네트워크 없음 상태에서 작업 완료와 다음 알람 예약 여부를 검증한다. | true |
| P2 | 안정성/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:20-63`, `:68-141`, `:148-170` | FCM 수신 경로에서 WakeLock 획득, 알림 채널 생성/알림 게시, 수면 서비스 start/stop을 즉시 수행한다. 동기 장시간 I/O는 확인되지 않지만 FCM 콜백 제한 시간 내에 OS 서비스 시작 정책과 알림 작업이 겹치면 지연/실패가 발생할 수 있다. | FCM 수신 경로는 현재처럼 짧게 유지하고, Firestore 토큰 갱신처럼 네트워크가 필요한 작업은 이미 IO scope로 분리된 상태를 유지한다. 여러 원격 명령을 연속 수신하는 테스트로 서비스 start/stop 지연과 알림 게시 실패를 확인한다. | true |
| P2 | 품질/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt:23-39`, `:42-170`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModel.kt:42-115` | RevenueCat 호출은 suspend callback 래퍼로 구현되어 메인 스레드를 직접 블로킹하지 않는다. 다만 ViewModel에서 호출 컨텍스트를 명시하지 않아 결과 매핑이 Main에서 재개될 수 있고, `loginUser/logoutUser` 오류 콜백이 continuation을 종료하지 않아 장시간 로딩으로 보일 수 있다. | 결제/복원/상품 로드는 UI 상태 timeout과 실패 수렴을 명확히 한다. RevenueCat 테스트 API 키와 테스트 상품/entitlement를 운영 ID/운영 상품으로 전환한 release 빌드에서 상품 로딩 지연, 구매 취소, 복원 실패를 측정한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/스토어 등록 | Google Play Console Android vitals, Pre-launch report | 저장소만으로는 실제 기기군의 ANR율, 입력 지연, 콜드 스타트 시간, 제조사별 접근성/오버레이/포그라운드 서비스 정책 영향을 확인할 수 없다. 특히 수면 잠금, 앱 전환 차단, 앱 오픈 광고, FCM 원격 제어는 OS 정책과 단말 성능에 민감하다. | release AAB를 내부 테스트 트랙에 올리고 Pre-launch report와 Android vitals에서 ANR, slow rendering, frozen frames, startup time을 확인한다. 수면 토글, 잠금 오버레이, 앱 전환 차단, 콜드 스타트 광고, FCM START/STOP 경로를 테스트 시나리오로 포함한다. | false |
| P0 | 광고 SDK | AdMob Console, release 빌드 산출물 | 저장소에는 앱 오픈 광고 테스트 ID가 확인되지만 운영 광고 단위의 실제 응답 시간, no-fill, SDK 초기화 지연은 콘솔/운영 빌드 없이는 확인할 수 없다. 테스트 ID로만 측정하면 운영 광고 경로의 시작 지연과 ANR 영향을 놓칠 수 있다. | AdMob 앱 오픈 광고 테스트 ID를 운영 ID로 전환한 release 후보에서 cold start timeout, 광고 로드 실패, 광고 표시 후 복귀 시간을 측정한다. | false |
| P0 | 결제 | RevenueCat 대시보드, Play Console 구독 상품 | 저장소만으로는 RevenueCat 운영 API 키, 운영 상품/entitlement, Play 구독 상품 연결 상태와 실제 응답 지연을 확정할 수 없다. 테스트 ID/샌드박스 설정이 남아 있으면 운영 결제 경로의 로딩/실패/복원 시간을 검증하지 못한다. | RevenueCat 테스트 API 키와 테스트 구독관리 설정을 운영 ID/운영 상품으로 전환했는지 확인하고, 내부 테스트 트랙에서 상품 로딩, 구매, 복원, 로그아웃/재로그인 후 권한 동기화 시간을 측정한다. | false |
| P1 | 품질/성능 | Android Studio profiler, Macrobenchmark/JankStats, 실제 단말 로그 | 저장소에는 ANR/프레임 지연을 자동 측정하는 벤치마크나 JankStats/trace 계측이 확인되지 않는다. 따라서 Main dispatcher에서 재개되는 suspend 작업이 실제로 어느 정도 프레임 지연을 만드는지 정량 근거가 없다. | 출시 후 개선 또는 출시 전 QA로 Macrobenchmark/JankStats를 도입해 콜드 스타트, 홈 진입, 수면 토글, 통계 화면 진입, 오버레이 표시, 앱 전환 차단 시나리오를 계측한다. | false |

## 결론

현재 저장소 기준으로 즉시 출시를 막을 수준의 명시적 동기 차단 호출은 확인되지 않았다. 그러나 수면 보호 앱의 핵심 경로가 Main dispatcher에서 여러 suspend 작업과 UI/서비스 제어를 이어 붙이는 구조라, 실제 단말에서는 ANR보다 먼저 콜드 스타트 지연, 프레임 드롭, 입력 지연으로 나타날 가능성이 있다. Google Play 출시 전에는 앱 오픈 광고 운영 ID 전환, RevenueCat 운영 ID/상품 전환, 접근성/오버레이/FCM/통계 경로의 실기기 성능 측정을 함께 완료해야 한다.
