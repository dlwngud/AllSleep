# Google Play 출시 전 점검 리포트 - AC8.3.2 예외 처리 및 장애 대응 흐름

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, Manifest, README 및 기존 리포트 문서 기준. Google Play Console, Firebase Console, RevenueCat/AdMob 운영 콘솔, 실제 운영 로그와 서버 런타임 상태는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:451-511`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:519-546` | 수면 시작/중지 핵심 경로가 `try/finally`로 로딩만 해제하고 `catch`가 없다. DataStore 조회/저장, 수면 기록 저장, Firestore 상태 업데이트 중 예외가 발생하면 코루틴 실패로 끝나며 사용자 오류 메시지와 재시도 경로가 없다. 수면 상태 UI, 로컬 활성 수면 시작 시각, Firestore 상태가 서로 어긋날 수 있다. | 출시 전 수면 토글을 `Result` 또는 명시적 UI 오류 상태로 수렴시킨다. 로컬 저장 실패, 수면 기록 저장 실패, Firestore 동기화 실패를 분리하고 실패 시 낙관적 UI 업데이트를 되돌릴지, 보호 기능을 제한 상태로 유지할지 정책을 확정한다. | true |
| P0 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/receiver/SleepAlarmReceiver.kt:31-87`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/receiver/SleepAlarmReceiver.kt:103-107` | 예약 알람 수신 후 실행되는 코루틴에 전체 예외 처리가 없고 `updateUserSleepStateUseCase` 결과도 확인하지 않는다. 잘못된 시간 문자열은 `toInt()`에서 예외가 날 수 있으며, 자동 수면 시작/중지 또는 다음 알람 재스케줄링이 누락되어도 사용자가 원인을 알 수 없다. | `onReceive` 코루틴 전체에 실패 처리와 운영 리포팅을 추가한다. 시간 파싱은 안전 파서로 교체하고, Firestore 업데이트 실패와 알람 재스케줄 실패를 독립적으로 처리해 다음 예약이 끊기지 않게 한다. | true |
| P0 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:51-78`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:357-376` | 포그라운드 서비스 시작 실패는 로그만 남기고 계속 진행한다. 이후 기기 목록 관찰과 오버레이 표시 경로에는 `catch`가 없어 Firestore listener, WindowManager, 오버레이 권한 상태 예외가 발생하면 보호 화면이 표시되지 않거나 서비스가 조용히 중단될 수 있다. | 서비스 시작 실패, listener 실패, WindowManager 추가/제거 실패를 별도 상태로 다룬다. 오버레이 표시 실패 시 권한 재확인/설정 이동/수면 보호 비활성 경고를 사용자에게 제공하고, 서비스가 계속 살아 있는지 복구 루틴을 둔다. | true |
| P0 | 결제 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt:23-39`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt:89-170`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModel.kt:105-115` | RevenueCat `loginUser`/`logoutUser`의 `onError`가 continuation을 완료하지 않아 호출부가 영구 대기할 수 있다. 구독 상태 동기화에서 `updateUserProfileUseCase`와 `savePremiumStatus` 결과를 확인하지 않아 결제 성공 후 앱/서버 premium 상태가 불일치할 수 있다. 테스트 API 키, 테스트 상품, 테스트 entitlement가 운영 빌드에 남으면 실제 구독 권한 판정도 실패할 수 있다. | RevenueCat 오류 콜백을 실패로 반환하고, 구독 상태 동기화 실패 시 재시도/사용자 안내/서버 검증 경로를 둔다. 출시 전 RevenueCat 테스트 API 키, 테스트 offering, 테스트 entitlement, 샌드박스 상품 ID가 실제 운영 ID/상품으로 전환되었는지 필수 확인한다. | true |
| P0 | 광고 SDK | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:44-64`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:132-187`, `admob.properties:4-8` | 앱 오픈 광고 단위 ID가 테스트 ID로 하드코딩되어 있고, 광고 로드/노출 실패는 로그 또는 `onComplete()`로만 처리된다. 운영 빌드에서 테스트 광고 ID가 남으면 수익화 검증이 불가능하고, Play/AdMob 정책 및 운영 광고 장애 대응 흐름도 검증되지 않는다. | 출시 전 광고 및 AdMob 테스트 ID를 실제 운영 ID로 전환하는 항목을 필수 체크로 둔다. 광고 실패는 사용자 진행을 막지 않되 운영 리포팅으로 집계하고, release 빌드가 `BuildConfig` 또는 운영 설정의 광고 단위 ID를 사용하는지 산출물 기준으로 확인한다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 안정성/동기화 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:313-400`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:403-406` | Firestore 수면 상태/기기 목록 flow의 `catch`가 곧바로 `handleRemoteSessionClosed()`와 `logout()`으로 이어진다. 일시적 네트워크 오류, 권한 오류, 문서 부재, 실제 원격 세션 종료가 구분되지 않으면 사용자가 예기치 않게 로그아웃되거나 수면 보호가 중단될 수 있다. | 원격 세션 종료, 네트워크 장애, 권한 오류, 기기 교체를 구분하는 상태 모델을 둔다. 자동 로그아웃 전 사용자 안내와 재시도 조건을 명확히 한다. | true |
| P1 | 안정성/인증 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthViewModel.kt:59-122` | 로그인/회원가입 실패는 UI 오류로 전달되지만 Firebase/Kakao/Google 오류가 사용자 친화 코드로 충분히 분류되지 않고, 소셜 로그인 실패가 `println` 디버그 로그로 남는다. 취소, 네트워크, 계정 충돌, 서버 오류의 사용자 대응이 같아질 수 있다. | 인증 오류 매핑 표를 만들고 취소/재시도/계정 충돌/서버 장애 메시지를 분리한다. 릴리즈에서는 `println` 로그를 제거하거나 개인정보가 마스킹되는 로깅/리포팅으로 대체한다. | true |
| P1 | 안정성/알림 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:20-63`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:68-141`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:148-170` | FCM 수신, 알림 생성, WakeLock 획득, 서비스 시작/중지, 토큰 갱신 경로에 전체 예외 방어가 없다. FCM 토큰과 전체 data payload도 로그에 직접 출력되어 장애 분석 과정에서 개인정보/식별자 로그 노출 가능성이 있다. | `onMessageReceived`, `showNotification`, `acquireWakeLock`, `onNewToken`에 실패 처리를 추가하고 실패 원인을 운영 리포팅한다. FCM 토큰, uid, 전체 payload는 릴리즈 로그에서 마스킹하거나 제거한다. | true |
| P1 | 설정/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:144-181`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:201-213`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:475-482` | 프로필/기기명 오류는 상태에 저장되지만 설정 화면 오류 다이얼로그의 확인 버튼이 상태를 해제하지 않는다. 기기 등록 해제 결과는 확인하지 않고, 로그아웃 예외는 무시되어 사용자는 실패를 성공으로 오인할 수 있다. | 오류 다이얼로그 확인/닫기 동작에 오류 초기화를 연결한다. 기기 등록 해제와 로그아웃 실패는 스낵바/다이얼로그와 재시도 경로로 노출한다. | true |
| P1 | UX/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:79-116`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:119-137` | 최신 기록, 월간 기록, 추세 기록을 순차 조회하고 하나라도 실패하면 전체 통계 화면 오류로 처리한다. 일부 데이터만 실패한 상황에서도 사용자가 전체 통계를 볼 수 없고, 어느 데이터 조회가 실패했는지 구분하기 어렵다. | 통계 조회를 부분 실패로 분리해 가능한 데이터는 표시한다. 오류 메시지는 최신/월간/추세 중 실패한 영역과 재시도 대상을 구분한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 안정성/초기화 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:28-82` | Firebase/RevenueCat/AdMob 초기화 실패 정책이 SDK별로 일관되게 문서화되어 있지 않다. 광고는 선택 기능, 인증/결제는 핵심 기능인데 시작 실패 시 앱 시작 차단 여부와 fallback 기준이 불명확하다. | SDK별 초기화 실패 정책을 릴리즈 체크리스트에 추가한다. 필수 기능과 선택 기능을 분리하고, 재시도/사용자 메시지/운영 리포팅 기준을 정한다. | true |
| P2 | 품질/데이터 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/usecase/sleep/RecordSleepSessionUseCase.kt:31-93` | 시간 계산/수면 기록 저장 경로는 일부 잘못된 값을 기본값으로 보정할 수 있다. 크래시는 줄지만 데이터 품질 문제가 조용히 통계에 반영될 수 있다. | 잘못된 시간 문자열은 저장 단계에서 막고, 보정 발생은 운영 리포팅 또는 데이터 품질 지표로 집계한다. | true |
| P2 | 품질/테스트 | `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt`, `README.md:171-177` | 일부 ViewModel 테스트는 있으나 저장소 기준으로 알람 수신기, FCM, 포그라운드 서비스, 오버레이, RevenueCat 오류, AdMob no-fill/timeout 등 장애 시나리오 테스트 체크리스트는 확인되지 않는다. | 출시 후 회귀 테스트에 네트워크 끊김, Firestore 권한 거부, 권한 회수, 잘못된 FCM payload, RevenueCat 오류, AdMob no-fill/timeout, 오버레이 표시 실패를 추가한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/스토어 등록 | Google Play Console Pre-launch report, Android vitals | 저장소만으로는 실제 기기군에서 수면 토글, 알람, FCM, 오버레이, 백그라운드 제한, 제조사별 권한 정책이 어떤 크래시/ANR/서비스 중단을 일으키는지 확인할 수 없다. | release AAB를 내부 테스트 트랙에 업로드하고 Pre-launch report와 Android vitals에서 주요 장애 경로를 확인한다. | false |
| P0 | 결제 | RevenueCat 대시보드, Google Play Console 구독 상품 | 저장소에는 운영 RevenueCat 프로젝트의 실제 API 키 값, offering, entitlement, Play 구독 상품 연결 상태가 없다. 테스트 ID/샌드박스 상품이 운영 경로에 남지 않았는지 확정할 수 없다. | RevenueCat 운영 Android API 키, `premium` entitlement, offering, Play 구독 상품/기본 요금제 연결을 콘솔에서 대조한다. 내부/비공개 트랙에서 구매/복원/취소/결제 실패 장애 흐름을 검증한다. | false |
| P0 | 광고 SDK | AdMob Console, release AAB 산출물 | AdMob 앱 승인, 운영 광고 단위 활성화, no-fill 빈도, 정책 센터 이슈는 저장소만으로 확인할 수 없다. 코드상 테스트 광고 ID가 확인되므로 운영 광고 ID 전환 여부는 콘솔과 산출물에서 별도 확인해야 한다. | 운영 광고 단위 ID가 release 빌드에 포함되는지 APK/AAB 산출물과 런타임 로그로 검증하고, AdMob 콘솔 승인/정책 상태를 확인한다. | false |
| P1 | 개인정보/보안 | Firebase Console, Firestore Rules, 운영 로그/Crashlytics 정책 | 장애 대응 과정에서 남길 수 있는 uid, FCM token, remoteMessage payload, 구매 식별자, 광고 오류 정보의 로그 마스킹/보관 정책은 저장소만으로 확인할 수 없다. | 운영 로그와 Crashlytics 도입/설정 시 개인정보 처리방침 및 Play 데이터 보안 설문에 진단 로그와 식별자 처리 기준을 반영한다. | false |

## 결론

인증, 구독 상품 로드, 일부 통계 조회는 사용자 오류 상태로 수렴되는 흐름이 있다. 반면 출시 핵심 기능인 수면 토글, 예약 알람, 포그라운드 서비스/오버레이, FCM 원격 제어, RevenueCat 로그인/권한 동기화, AdMob 운영 광고 전환은 예외가 로그에만 남거나 무시되거나 코루틴 실패로 끝날 수 있다. 출시 전에는 수면 보호 기능의 실패 복구와 광고/RevenueCat 테스트 ID의 운영 ID 전환을 P0로 두고, 내부 테스트 트랙에서 실제 장애 경로를 검증해야 한다.
