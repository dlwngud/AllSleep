# Google Play 출시 전 점검 리포트 - AC2 주요 예외 처리 흐름과 사용자 영향

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, Manifest, 기존 문서 기준. Google Play Console, Firebase Console, RevenueCat/AdMob 운영 콘솔, 실제 운영 로그는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:451-511` | 수면 시작/중지 핵심 경로가 `try/finally`만 두고 `catch`가 없다. `sleepSettingsRepository.first/save...`, `saveCompletedSleepRecord`, `updateUserSleepStateUseCase` 중 하나가 예외를 던지면 토글 코루틴이 실패하고, 사용자는 수면 상태가 바뀐 것처럼 보이거나 로컬/Firestore/서비스 상태가 어긋날 수 있다. 현재 UI 오류 메시지 또는 재시도 경로도 확인되지 않는다. | 출시 전 수면 토글 경로를 명시적으로 `Result`/UI 오류 상태로 수렴시킨다. 로컬 저장 실패, Firestore 권한/네트워크 실패, 수면 기록 저장 실패를 각각 사용자 메시지와 복구 정책으로 분리하고, 실패 시 낙관적 UI 업데이트를 되돌릴지 확정한다. | true |
| P0 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/receiver/SleepAlarmReceiver.kt:31-87` | 알람 수신 후 실행되는 코루틴에 예외 처리와 `Result.onFailure` 처리가 없다. DataStore 읽기, Firestore 수면 상태 업데이트, 알람 재스케줄링 중 실패가 발생하면 자동 수면 시작/중지가 누락될 수 있고 사용자는 예약 기능이 실행되지 않은 이유를 알 수 없다. | `onReceive` 내부 코루틴 전체에 실패 처리와 실패 로그/리포팅을 추가하고, `updateUserSleepStateUseCase` 결과를 반드시 확인한다. 자동 시작/중지 실패 시 다음 알람 재스케줄링은 가능한 한 독립적으로 수행되도록 분리한다. | true |
| P0 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:65-78`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:357-376` | 포그라운드 서비스는 시작 실패를 로그로만 남기고 계속 진행한다. 이후 기기 목록 관찰/오버레이 표시 코루틴에는 `catch`가 없어 Firestore listener 또는 오버레이 업데이트 실패 시 차단 화면이 표시되지 않거나 중단될 수 있다. WindowManager 추가/제거 실패도 사용자 복구 안내 없이 로그 또는 `printStackTrace()`에 머문다. | 서비스 시작 실패 시 즉시 사용자에게 권한/OS 제한 상태를 안내하고, overlay 표시 실패는 권한 재요청 또는 보호 상태 경고로 연결한다. `observeRegisteredDevicesUseCase` 수집 경로에는 `catch`를 두고 서비스가 계속 살아 있는지와 오버레이 표시 상태를 복구한다. | true |
| P0 | 광고 SDK | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `:44-64`, `:132-187`; `admob.properties:5`, `:8` | 광고 로드/노출 실패는 대체로 로그 또는 `onComplete()`로만 처리된다. 더 큰 출시 차단 리스크는 앱 오픈 광고 단위 ID가 테스트 ID로 하드코딩되어 있어 운영 AdMob ID 전환 경로가 예외/설정 흐름과 분리되어 있다는 점이다. 운영 빌드에서 테스트 ID가 남으면 광고 수익이 발생하지 않거나 정책/품질 점검에서 운영 광고 경로를 검증하지 못한다. | 출시 전 `AppOpenAdManagerImpl`이 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`의 release 운영 광고 단위 ID를 사용하도록 확인한다. 광고 로드 실패, 표시 실패, 저장소 저장 실패는 사용자 진행을 막지 않되 운영 리포팅으로 집계한다. | true |
| P0 | 결제 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt:23-39`, `:89-170`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModel.kt:105-115` | RevenueCat release API 키 누락 시 `error(...)`가 발생할 수 있고, 구독 상태 동기화의 `updateUserProfileUseCase`/`savePremiumStatus` 결과는 확인되지 않는다. 또한 `loginUser`/`logoutUser`의 RevenueCat `onError`가 continuation을 종료하지 않아 호출부가 영구 대기할 가능성이 있다. 테스트 API 키/테스트 상품이 운영 빌드에 남으면 실제 구독 권한이 잘못 판정될 수 있다. | 출시 전 release 빌드에 RevenueCat 운영 API 키와 운영 구독 상품/entitlement(`premium`)가 연결되는지 확인한다. `loginUser`/`logoutUser` 오류도 실패로 반환하고, premium 동기화 실패는 사용자 결제 완료 상태와 서버/로컬 권한 상태가 어긋나지 않도록 재시도/검증 경로를 둔다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 안정성/인증 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthViewModel.kt:59-122`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:32-79` | 이메일/소셜 로그인 실패는 UI 오류 상태로 수렴되어 사용자 영향은 제한적이다. 다만 소셜 로그인 경로는 `println` 디버그 로그가 남아 있고, 일부 Firebase 예외만 사용자 친화 메시지로 매핑된다. | 로그인 실패 메시지 정책을 정리하고, 취소/네트워크/계정 충돌/서버 오류를 분리한다. 릴리즈에서는 `println` 디버그 로그를 제거하거나 안전한 로깅으로 대체한다. | true |
| P1 | 개인정보/인증 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:85-106` | 계정 삭제는 Firestore 기기 문서 삭제, 유저 문서 삭제, Firebase Auth 삭제를 순차 실행한다. 중간 실패 시 부분 삭제 상태가 남을 수 있으며, `onFailure` 내부에서 다시 `throw`해 호출자에게는 오류가 전달되지만 어떤 단계까지 삭제되었는지는 알 수 없다. | 계정 삭제 단계별 실패를 구분하고, 부분 삭제 발생 시 재시도/고객지원 안내를 제공한다. `RecentLoginRequired` 외 권한/네트워크 실패도 사용자 메시지로 매핑한다. | true |
| P1 | 안정성/동기화 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:19-59`, `:172-202`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:313-400` | Firestore listener 권한 실패는 `null` 또는 빈 목록으로 변환되고, `GlobalSleepViewModel`은 flow `catch`에서 곧바로 `logout()`을 호출한다. 일시적 권한/네트워크/문서 부재가 원격 세션 종료로 해석되면 사용자가 예기치 않게 로그아웃되거나 보호 기능이 중단될 수 있다. | 권한 실패, 네트워크 실패, 기기 교체, 실제 세션 종료를 구분하는 상태 모델을 둔다. 자동 로그아웃 전 사용자 안내와 재시도 조건을 명확히 한다. | true |
| P1 | 안정성/알림 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:20-63`, `:123-170` | FCM 수신 경로에 전체 예외 방어가 없다. 알림 생성, WakeLock 획득, `SleepLockService.start/stop`, 토큰 갱신 중 실패 시 원격 수면 제어가 누락되거나 서비스 프로세스 안정성에 영향을 줄 수 있다. 토큰 값도 로그에 직접 출력된다. | `onMessageReceived`, `showNotification`, `acquireWakeLock`, `onNewToken` 코루틴에 실패 처리를 추가한다. FCM 토큰/전체 data payload는 릴리즈 로그에 직접 남기지 않는다. | true |
| P1 | UX/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:79-116`, `:119-137` | 통계 조회 실패는 화면 오류로 표시되어 재시도 가능하다. 다만 최신/월간/범위 조회를 순차 실행하고 하나라도 실패하면 전체 화면 오류가 되므로 일부 데이터만 보여줄 수 있는 상황에서도 사용자는 통계를 전혀 보지 못할 수 있다. | 최신 기록, 월간 캘린더, 추세 그래프를 부분 실패로 분리하고 가능한 데이터는 표시한다. 오류 메시지는 재시도 대상 API 범위를 알 수 있게 구분한다. | true |
| P1 | 설정/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:177-181`, `:201-213` | 기기 등록 해제는 결과를 확인하지 않고, 로그아웃은 `catch`에서 오류를 무시한다. 사용자는 설정 화면에서 조작이 성공한 것으로 이해할 수 있지만 서버 기기 목록 또는 세션 상태가 실제로 정리되지 않을 수 있다. | 등록 해제/로그아웃 결과를 UI 상태와 스낵바로 표시하고 실패 시 재시도 가능하게 한다. 로그아웃 전 기기 등록 해제 실패를 무조건 삼킬지, 백그라운드 재시도로 넘길지 정책을 정한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질/성능 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/RecordSleepSessionUseCase.kt:31-70`, `:77-93`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:317-328` | 시간 파싱 실패 시 기본 480분으로 보정한다. 크래시는 피하지만 잘못된 설정/데이터가 조용히 통계에 반영되어 수면 달성률과 효율이 왜곡될 수 있다. | 잘못된 시간 문자열을 저장 단계에서 막고, 파싱 실패는 운영 리포팅 및 데이터 품질 지표로 집계한다. 사용자에게 기본값 보정 사실을 직접 노출할지 검토한다. | true |
| P2 | 안정성/초기화 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:28-69` | RevenueCat/Firebase 초기화 확인은 일부 `try/catch`가 있으나 AdMob 초기화 코루틴은 예외 방어가 없다. 초기화 실패가 앱 시작 UX, 광고/결제 기능, 이후 예외 흐름에 어떤 영향을 주는지 명확한 정책이 없다. | SDK별 초기화 실패 정책을 문서화한다. 필수 기능인 인증/결제와 선택 기능인 광고를 분리해 앱 시작 차단 여부, fallback, 재시도 기준을 정한다. | true |
| P2 | 품질/테스트 | `composeApp/src/commonTest/kotlin/...`, `README.md:171-177` | 일부 ViewModel 테스트는 있으나 저장소 기준으로 예외 경로별 테스트 체크리스트는 확인되지 않는다. 특히 알람 수신기, FCM, 포그라운드 서비스, 결제 취소/실패, 광고 실패는 기기/콘솔 환경 의존성이 커 출시 후 회귀 가능성이 있다. | 출시 후 회귀 테스트에 네트워크 끊김, Firestore 권한 거부, 알림/오버레이/접근성 권한 거부, RevenueCat 오류, AdMob no-fill/timeout, FCM 잘못된 payload 시나리오를 추가한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/스토어 등록 | Google Play Console Pre-launch report, Android vitals | 저장소만으로는 실제 기기군에서 예외가 발생하는지, 백그라운드 실행 제한/권한 제한/제조사별 오버레이 정책에서 어떤 크래시나 ANR이 나오는지 확인할 수 없다. | release AAB를 내부 테스트 트랙에 업로드하고 Pre-launch report 및 Android vitals에서 수면 토글, 알람, 오버레이, FCM, 결제/광고 경로를 확인한다. | false |
| P0 | 결제 | RevenueCat 대시보드, Play Console 구독 상품 | 저장소에는 운영 RevenueCat API 키 값과 실제 상품 연결 상태가 없다. 테스트 API 키/샌드박스 상품/테스터 계정이 운영 빌드에 남지 않았는지는 저장소만으로 확정할 수 없다. | RevenueCat 운영 프로젝트의 Android API 키, `premium` entitlement, Play Console 구독 상품/기본 요금제 연결, 내부 테스트 구매/복원/취소/결제 실패 예외 흐름을 별도 검증한다. | false |
| P0 | 광고 SDK | AdMob Console | AdMob 앱 승인, 운영 광고 단위 활성화, no-fill 빈도, 정책 센터 이슈는 저장소만으로 확인할 수 없다. 코드상 테스트 광고 ID가 확인되므로 운영 광고 ID 전환 여부는 콘솔과 release 빌드에서 재확인해야 한다. | 운영 광고 단위 ID가 release 빌드에 포함되는지 APK/AAB 산출물과 로그로 검증하고, AdMob 콘솔에서 앱/광고 단위 승인 상태를 확인한다. | false |
| P1 | 개인정보/보안 | Firebase Console, Firestore Rules, 운영 로그 정책 | 예외 처리 과정에서 수집/로그로 남길 수 있는 FCM token, uid, package/payload, 구매 정보의 운영 로그 보존/마스킹 정책은 저장소만으로 확인할 수 없다. | Crashlytics/로그 도입 시 개인정보 처리방침, Play 데이터 보안 설문, 내부 로그 마스킹 정책에 uid/token/payload/구매 식별자 처리 기준을 반영한다. | false |

## 결론

인증/통계/구독 화면 일부는 `Result`와 UI 오류 상태로 수렴되어 사용자에게 실패가 전달된다. 반면 출시 핵심 기능인 수면 토글, 알람 수신, 포그라운드 서비스/오버레이, FCM 원격 제어, RevenueCat 로그인/동기화, AdMob 운영 ID 전환 경로는 예외가 로그에만 남거나 조용히 무시되거나 코루틴 실패로 끝날 수 있다. Google Play 출시 전에는 수면 보호 기능과 결제/광고 운영 설정을 P0로 두고, 실패 시 사용자 영향과 복구 경로를 명확히 검증해야 한다.
