# Google Play 출시 전 점검 리포트 - AC1 크래시 처리 및 크래시 리포팅

점검 범위: 현재 저장소의 Android 앱 코드, Gradle 설정, Manifest, README 기준. Google Play Console, Firebase Console, 실제 운영 대시보드 데이터는 직접 점검하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/품질 | `build.gradle.kts:9`, `composeApp/build.gradle.kts:13-19`, `composeApp/build.gradle.kts:175-180`, `gradle/libs.versions.toml:56-65` | Firebase Auth/Firestore/FCM/Functions는 포함되어 있으나 `com.google.firebase.crashlytics` Gradle 플러그인과 `firebase-crashlytics` 의존성이 없다. 릴리즈에서 실제 크래시가 Google Play 출시 후 자동 수집되지 않아 치명적 장애 원인 파악, 재현, 핫픽스 우선순위 산정이 어렵다. | 출시 전 Firebase Crashlytics 또는 동급 크래시 리포팅 SDK를 운영 Firebase 프로젝트에 연동한다. Android Gradle 플러그인 적용, `firebase-crashlytics` 의존성 추가, 릴리즈 빌드에서 매핑 파일 업로드가 되는지 확인한다. | true |
| P0 | 안정성/품질 | 저장소 전체 검색: `firebase-crashlytics`, `Crashlytics`, `recordException`, `CoroutineExceptionHandler`, `Thread.setDefaultUncaughtExceptionHandler` 검색 결과 없음 | 앱 초기화, 서비스, FCM, 구매/광고, Firestore 동기화처럼 출시 핵심 경로에서 예외가 발생해도 중앙 리포팅으로 남기는 코드가 확인되지 않는다. 사용자는 앱 종료나 기능 실패를 겪지만 개발팀은 릴리즈 현황을 추적하지 못할 수 있다. | 전역 uncaught exception 수집과 주요 recoverable exception의 `recordException` 기록 정책을 정의한다. 인증, 결제, 광고, FCM, 오버레이/접근성 서비스, 알람 수신기 경로에는 사용자 영향이 큰 실패를 별도 키/로그와 함께 남긴다. | true |
| P0 | 안정성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:455-511` | `viewModelScope.launch` 내부 수면 상태 토글 로직이 `try/finally`만 사용하고 `catch`가 없다. `sleepSettingsRepository` 또는 `updateUserSleepStateUseCase` 실패 시 예외가 코루틴 밖으로 전파될 수 있으며, 현재 저장소에는 이를 중앙 리포팅하거나 UI 오류 상태로 복구하는 전역 처리 근거가 없다. | 수면 시작/중지 같은 핵심 경로는 실패를 `Result`/상태 오류로 수렴시키고, 크래시 리포팅 연동 후 예외를 기록한다. 릴리즈 전 네트워크 실패, Firestore 권한 실패, 로컬 저장소 실패 시나리오를 테스트한다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 안정성 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:28-41`, `:43-61` | RevenueCat 초기화 실패와 Firebase 초기화 확인 실패는 `Log.e`로만 남는다. AdMob 초기화는 `CoroutineScope(Dispatchers.IO).launch`에서 수행되지만 예외 처리나 리포팅이 없다. 출시 후 결제/광고/백엔드 초기화 실패의 빈도와 기기 조건을 파악하기 어렵다. | SDK 초기화 실패를 크래시 리포팅 non-fatal 이벤트로 기록한다. AdMob 초기화 코루틴에는 예외 처리와 타임아웃/실패 로깅 정책을 추가한다. | true |
| P1 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:51-59`, `:65-78`, `:128-150` | 포그라운드 서비스 시작 실패는 `Log.e`로 처리되지만, 기기 목록 관찰 코루틴과 Firestore 동기화 코루틴의 예외는 중앙 리포팅으로 연결되지 않는다. 권한/OS 버전별 서비스 실패가 누락될 수 있다. | `SleepLockService`의 서비스 시작, 오버레이 표시, Firestore 업데이트 실패를 non-fatal로 기록하고 OS 버전, 권한 상태, foreground service type 정보를 커스텀 키로 남긴다. | true |
| P1 | 안정성/권한 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:49-98`, `:101-117` | 접근성 기반 앱 차단 경로는 여러 `try/catch`가 있으나 `Log.e`만 수행한다. Android 버전, 접근성 권한 상태, 백그라운드 실행 제한에 따라 실패해도 출시 후 원인 집계가 어렵다. | 접근성 이벤트 처리 실패, `startActivity` 실패, 서비스 재시작 실패를 non-fatal로 기록한다. 단, 패키지명 등 개인정보/앱 사용 정보에 해당할 수 있는 값은 정책 검토 후 최소화하거나 마스킹한다. | true |
| P1 | 안정성/품질 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:20-63`, `:123-141`, `:148-170` | FCM 메시지 처리, WakeLock 획득, 토큰 갱신 경로가 `Log.d/e` 중심이며 `onMessageReceived` 전체 예외 방어가 없다. 원격 수면 제어 실패가 조용히 누락되거나 서비스 프로세스 안정성에 영향을 줄 수 있다. | FCM 명령 처리 전체를 방어적으로 감싸고, 명령 종류, 권한 상태, 실패 타입을 non-fatal로 기록한다. FCM 토큰 자체는 민감 정보이므로 리포팅에 직접 남기지 않는다. | true |
| P1 | 개인정보/품질 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt:68-76` | `FirebaseFirestore.setLoggingEnabled(true)`가 빌드 타입 조건 없이 설정되어 있다. 크래시 리포팅과 별개로 릴리즈 로그 노출 및 성능/개인정보 리스크가 있다. | 릴리즈 빌드에서는 Firestore 디버그 로깅을 비활성화하고, 장애 분석에는 Crashlytics non-fatal 및 안전한 커스텀 키를 사용한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질 | `README.md:171-177`, `composeApp/src/commonTest/kotlin/...` | README에는 ViewModel/비동기 로직 테스트 전략이 있으나 크래시 리포팅 동작 검증, 릴리즈 매핑 파일 업로드, non-fatal 기록 기준에 대한 문서나 테스트 체크리스트는 확인되지 않는다. | 출시 후 운영 체크리스트에 크래시 free users, non-fatal 상위 이슈, 버전별 회귀 확인, 매핑 파일 업로드 여부를 추가한다. | true |
| P2 | 품질/성능 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:357-377` | 오버레이 추가 실패는 로그로 남기고 제거 실패는 `printStackTrace()`를 사용한다. 출시 후 기기 제조사별 WindowManager 예외를 구조적으로 집계하기 어렵다. | WindowManager 예외를 유형별로 non-fatal 집계하고, Android 버전/권한 상태/오버레이 표시 상태를 안전한 키로 기록한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/스토어 등록 | Firebase Console, Google Play Console Android vitals | 저장소에는 Crashlytics 연동 근거가 없으므로 운영 대시보드 상태도 확인할 수 없다. 또한 Play Console Android vitals의 기존 크래시/ANR 수치, 비정상 종료 임계치 초과 여부는 저장소만으로 검증할 수 없다. | Firebase Console에서 Crashlytics 앱 등록 여부, 릴리즈 빌드 심볼/매핑 업로드 여부를 확인한다. Play Console Android vitals에서 크래시율/ANR율/기기별 이슈를 별도 점검한다. | false |
| P1 | 안정성/개인정보 | 운영 Firebase 프로젝트 설정 | Crashlytics를 도입할 경우 수집 데이터와 개인정보 처리방침 고지 필요 여부, 데이터 보존/삭제 정책은 저장소만으로 확정할 수 없다. | 개인정보 처리방침과 Play Console 데이터 보안 설문에 크래시 로그, 기기 정보, 진단 데이터 수집 여부를 반영한다. | false |

## 결론

현재 저장소 기준으로는 Firebase 기반 백엔드/FCM은 사용 중이지만 크래시 리포팅 SDK와 전역 예외 수집 연동은 확인되지 않는다. Google Play 출시 전에는 Crashlytics 또는 동급 도구 연동을 P0 차단 항목으로 처리하고, 핵심 경로의 recoverable failure를 non-fatal로 남기는 기준을 함께 적용해야 한다.
