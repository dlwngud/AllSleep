# Google Play 출시 전 종합 점검 리포트 - Sub-AC 6.2.1 권한 및 Gradle 설정

## 점검 범위

- 기준: 현재 저장소의 AndroidManifest, Gradle 설정, 권한 요청/사용 코드, 관련 앱 내 안내 문구
- 제외: Google Play Console 권한 선언 입력값, 스토어 등록 자료, 실제 심사 승인 상태
- 참고 정책: Google Play Console Help의 민감 권한/API 정책, 접근성 API 정책, Foreground Service 및 Full-screen Intent 요구사항, Exact Alarm Permission 정책

## 출시 차단

| 우선순위 | 범주 | 위치/근거 | 권한/설정 | 사용 목적 판단 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:88`, `composeApp/src/androidMain/AndroidManifest.xml:91`, `composeApp/src/androidMain/AndroidManifest.xml:92`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:3`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:6`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:8`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:27`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:65`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:101` | `BIND_ACCESSIBILITY_SERVICE`, AccessibilityService API | 수면 모드 중 `TYPE_WINDOW_STATE_CHANGED` 이벤트로 다른 앱 실행을 감지하고 메인 앱으로 되돌리는 앱 차단 기능에 사용된다. `canRetrieveWindowContent=false`, `isAccessibilityTool=false`로 선언되어 장애 보조 도구가 아닌 비장애 목적 사용임을 나타낸다. | Google Play는 접근성 API가 더 좁은 권한/API로 대체 가능한 경우 제한적으로 심사하며, 비장애 목적 사용은 Play Console 선언, 명확한 앱 내 고지, 명시적 동의가 필요하다. 현재 서비스는 다른 앱 사용을 감지하고 앱 전환을 유도하므로, 2026년 1월 28일 이후 강화된 접근성 API 정책의 "사용자 인지/동의 없는 자동 실행 또는 우회적 동작" 검토 대상이 될 가능성이 높다. 승인 가능성을 출시 전 확정하지 않으면 심사 반려 리스크가 크다. | Play Console 접근성 선언을 출시 차단 게이트로 둔다. 앱 차단이 사용자가 명시적으로 켠 수면 모드에서만 동작함을 입증하고, 접근성 API로 수집/전송하는 데이터가 없다는 고지와 동의 흐름을 release 빌드에서 확인한다. 정책상 허용 가능한 핵심 기능인지 별도 검토하고, 허용 근거가 부족하면 접근성 API 의존 기능을 출시 범위에서 제외하거나 대체 UX를 준비한다. | true |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:22`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/OverlayPermissionRequester.android.kt:31`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/OverlayPermissionRequester.android.kt:34`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:61`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:72`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:212`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:213` | `SYSTEM_ALERT_WINDOW` | 수면 잠금 화면을 다른 앱 위에 표시하고, 수면 중 앱 사용을 차단하는 핵심 UX에 사용된다. | 다른 앱 위 표시 권한은 사용자가 기기 조작을 방해받는다고 느끼기 쉬운 민감 권한이다. Play Console 권한 선언, 앱 내 안내, 개인정보 처리방침이 "수면 모드 중 잠금 오버레이" 목적과 일치하지 않으면 사용자 기만/방해성 동작으로 해석될 수 있다. | 오버레이가 수면 모드 중에만 표시되고 사용자가 해제할 수 있는 조건을 앱 내 고지와 Play 권한 선언에 동일하게 작성한다. 오버레이가 광고, 피싱, 시스템 UI 모방, 임의 전면 차단에 사용되지 않음을 명시한다. | true |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:8`, `composeApp/src/androidMain/AndroidManifest.xml:9`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/SleepSchedulerImpl.kt:132`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/SleepSchedulerImpl.kt:145`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/SleepSchedulerImpl.kt:147`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/PermissionRequester.android.kt:69`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/PermissionRequester.android.kt:117` | `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | 취침/기상 시각에 `setExactAndAllowWhileIdle` 또는 `setExact`로 수면 모드 시작/종료 알람을 예약한다. | Google Play의 `USE_EXACT_ALARM`은 제한 권한이며 핵심 기능이 정확한 시각 실행을 필요로 하는 알람/타이머 또는 캘린더 성격일 때 심사 통과 가능성이 높다. 현재 앱은 수면 루틴 알람 성격의 근거가 있으나 `SCHEDULE_EXACT_ALARM`과 `USE_EXACT_ALARM`을 모두 선언한다. 불필요한 중복 선언 또는 앱 설명과의 불일치는 심사 반려/게시 불가 리스크가 있다. | 실제 target SDK 36 release 빌드에서 어떤 exact alarm 권한이 필요한지 최소 권한 원칙으로 재검토한다. Play Console 권한 선언에는 수면 모드 자동 시작/종료가 앱 핵심 기능이고 정확한 시각 실행이 필요한 이유를 작성한다. 둘 중 하나로 충분하다면 불필요한 선언 제거를 별도 개발 항목으로 등록한다. | true |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:11`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:75`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:77`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:104`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:110`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:114`, `gradle/libs.versions.toml:5` | `USE_FULL_SCREEN_INTENT`, targetSdk 36 | FCM 원격 수면 제어 알림을 높은 우선순위/알람 카테고리로 띄우고 full-screen intent를 설정한다. | Android 14 이상에서 full-screen intent는 알람 또는 통화 핵심 기능 중심으로 자동 허용된다. 이 앱은 수면 알람/잠금 목적이라는 근거가 있지만, 원격 제어 FCM 알림까지 full-screen으로 띄우는 동작이 "즉각 주의가 필요한 알람"으로 인정되지 않으면 사용자가 별도 허용해야 하며 심사에서 제한될 수 있다. | Play Console full-screen intent 선언에 알람 성격의 핵심 기능만 포함한다. 수면 시작/기상 알람 외 원격 상태 동기화 알림에는 일반 heads-up 알림으로 graceful degradation 가능한지 release 빌드에서 검증한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치/근거 | 권한/설정 | 사용 목적 판단 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:25`, `composeApp/src/androidMain/AndroidManifest.xml:26`, `composeApp/src/androidMain/AndroidManifest.xml:78`, `composeApp/src/androidMain/AndroidManifest.xml:83`, `composeApp/src/androidMain/AndroidManifest.xml:84`, `composeApp/src/androidMain/AndroidManifest.xml:85`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:51`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:53`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:91` | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `foregroundServiceType="specialUse"` | 수면 모드 중 오버레이와 앱 차단 상태를 유지하기 위한 사용자 인지형 포그라운드 서비스에 사용된다. subtype은 `screen_lock_overlay`로 설정되어 있다. | Foreground Service special use는 모든 유형이 검토 대상이며, 사용자가 서비스 실행을 명확히 인지해야 한다. 알림 채널은 존재하지만 정책 선언에서 목적과 subtype 설명이 부정확하면 심사 지연 가능성이 있다. | Play Console FGS 선언에 `screen_lock_overlay` 목적, 사용자 시작 조건, 종료 조건, 지속 알림 문구를 명확히 적는다. release 빌드에서 Android 14+ 기기에서 서비스 시작 실패나 알림 누락이 없는지 확인한다. | true |
| P1 | 권한/UX | `composeApp/src/androidMain/AndroidManifest.xml:7`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/MainActivity.kt:68`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/MainActivity.kt:70`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/PermissionRequester.android.kt:86`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/NotificationPermissionRequester.android.kt:29`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:64`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/NotificationRationaleDialog.kt:26` | `POST_NOTIFICATIONS` | 수면 모드 상태 알림, 수면 리포트, FCM 원격 제어 알림에 사용된다. | 앱 시작 시 `MainActivity`에서 바로 알림 권한을 요청하고, 온보딩에서도 권한 요청 흐름이 있다. 사용자가 맥락을 이해하기 전에 권한 팝업이 나오면 거부율이 높고, 필수 권한처럼 압박하는 문구는 UX/정책 리스크가 될 수 있다. | 첫 실행 즉시 요청과 온보딩 요청이 중복되지 않게 QA한다. 권한 안내 문구는 알림의 구체 목적과 거부 시 제한되는 기능을 설명하되, 앱 전체 사용을 과도하게 막는 표현은 피한다. | true |
| P1 | 권한/성능 | `composeApp/src/androidMain/AndroidManifest.xml:10`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:121`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:127`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:132`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:139` | `WAKE_LOCK` | FCM 원격 신호 수신 시 CPU와 화면을 짧게 깨워 알림/수면 제어를 처리한다. | partial wake lock 10초, 화면 wake lock 5초는 시간 제한이 있어 과도하지는 않지만, 원격 메시지 빈도가 높거나 알림 목적이 불명확하면 배터리/사용자 방해 리스크가 있다. | FCM 명령 수신 시에만 wake lock이 실행되는지 테스트하고, 실패/예외 상황에서도 시간 제한으로 해제되는지 확인한다. 개인정보 처리방침과 앱 설명에는 기기 깨우기 동작이 필요한 이유를 과장 없이 설명한다. | true |
| P1 | 보안/개인정보 | `composeApp/src/androidMain/AndroidManifest.xml:5`, `composeApp/src/androidMain/AndroidManifest.xml:6`, `composeApp/build.gradle.kts:175`, `composeApp/build.gradle.kts:176`, `composeApp/build.gradle.kts:177`, `composeApp/build.gradle.kts:178`, `composeApp/build.gradle.kts:180`, `composeApp/build.gradle.kts:185` | `INTERNET`, `ACCESS_NETWORK_STATE`, Firebase/AdMob SDK | Firebase Auth/Firestore/Functions/FCM, AdMob, RevenueCat/Kakao 로그인 등 네트워크 기반 기능에 필요하다. | 일반 권한이지만 데이터 보안 설문과 개인정보 처리방침 대상이다. 네트워크 권한 자체보다 SDK 데이터 처리 목적, 광고 ID/구독/기기 토큰 처리 고지가 누락될 때 정책 리스크가 생긴다. | Play Console 데이터 보안 설문에는 Firebase Auth, Firestore, FCM 토큰, AdMob, RevenueCat 구독 정보, Kakao 로그인 처리 목적을 저장소 근거와 맞춰 입력한다. | true |
| P1 | 보안 | `composeApp/src/androidMain/AndroidManifest.xml:13`, `composeApp/src/androidMain/AndroidManifest.xml:18` | `<queries>`의 `com.google.android.gms` 및 LAUNCHER intent 조회 | Google Play Services 또는 런처 앱 조회가 필요한 기능 호환성에 사용되는 것으로 보인다. | `QUERY_ALL_PACKAGES`는 없으므로 고위험 패키지 조회 권한은 아니지만, 패키지 가시성 선언 목적이 문서화되어 있지 않다. 심사 리스크는 낮으나 불필요한 조회 선언이면 최소 권한 원칙과 맞지 않을 수 있다. | Google Play Services 조회가 실제 로그인/광고/인증에 필요한지 확인하고, 불필요하면 정리 항목으로 등록한다. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질/정책 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:15`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:36`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:73`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:120`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:136` | 앱 내 접근성 고지 컴포넌트와 온보딩 권한 설명은 존재하지만, 실제 release 빌드에서 권한 요청 전에 항상 노출되는지 자동 검증은 확인되지 않는다. | 접근성/오버레이/알림/정확한 알람 고지가 권한 요청 전에 표시되는지 스모크 테스트 또는 릴리즈 체크리스트를 추가한다. | true |
| P2 | 품질 | `composeApp/build.gradle.kts:90`, `composeApp/build.gradle.kts:97`, `gradle/libs.versions.toml:3`, `gradle/libs.versions.toml:5` | compileSdk/targetSdk 36으로 최신 권한 동작 변경의 영향을 받는다. 저장소에는 권한별 OS 버전 회귀 테스트 문서가 제한적이다. | Android 13, 14, 15, 16 대상 기기에서 알림, full-screen intent, exact alarm, foreground service specialUse, overlay, 접근성 서비스 활성화/비활성화 회귀 테스트 매트릭스를 유지한다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치/근거 | 리스크 | 별도 확인 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 스토어 등록/정책 | 저장소 내 Play Console 권한 선언 자료 부재, `composeApp/src/androidMain/AndroidManifest.xml:5-11`, `composeApp/src/androidMain/AndroidManifest.xml:21-26`, `composeApp/src/androidMain/AndroidManifest.xml:78-98` | Play Console의 접근성, exact alarm, full-screen intent, foreground service, 데이터 보안, 앱 콘텐츠 선언 입력값은 저장소에 없다. 저장소 코드와 콘솔 입력값이 다르면 심사 반려 또는 사후 제재 위험이 있다. | Google Play Console 등록 자료는 본 점검의 직접 대상에서 제외한다. 출시 전에는 본 리포트의 권한별 목적/근거와 콘솔 권한 선언 문구를 대조한다. | false |
| P0 | 정책 승인 | Google Play 심사 결과는 저장소에 없음 | 접근성 API, `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, FGS specialUse는 실제 심사 승인 여부가 콘솔 제출 결과에 좌우된다. | 내부 테스트 트랙 또는 검토 제출 전 권한 선언 폼을 작성하고, 반려 시 접근성 기반 앱 차단/전체 화면 알림/정확한 알람 사용 범위를 축소하는 대안을 준비한다. | false |
| P1 | 개인정보 | 외부 개인정보 처리방침 문서 내용은 저장소에 없음 | 권한 고지와 개인정보 처리방침이 일치하는지 저장소만으로 확인할 수 없다. 특히 접근성 이벤트, 오버레이 표시, FCM 토큰, 광고 SDK, 구독 정보 처리 고지가 누락될 수 있다. | 비로그인 브라우저에서 공개 개인정보 처리방침을 확인하고, 권한 사용 목적과 데이터 미수집/수집 항목을 Play Console 데이터 보안 설문과 일치시킨다. | false |

## 권한별 요약 판단

| 권한/설정 | 출시 판단 | 핵심 사유 |
| --- | --- | --- |
| `BIND_ACCESSIBILITY_SERVICE` | 출시 차단 | 앱 차단 목적의 비장애 접근성 API 사용이며 Play 선언/고지/동의/정책 허용성 확인이 필요하다. |
| `SYSTEM_ALERT_WINDOW` | 출시 차단 | 다른 앱 위 오버레이로 사용자 조작을 직접 제한하므로 목적, 범위, 해제 방법의 명확한 고지가 필요하다. |
| `USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM` | 출시 차단 | targetSdk 36에서 제한 권한 검토 대상이며, 중복 선언과 핵심 알람 기능 근거 확인이 필요하다. |
| `USE_FULL_SCREEN_INTENT` | 출시 차단 | Android 14+에서 알람/통화 핵심 기능 중심으로 자동 허용되므로 원격 제어 알림 사용 범위 검토가 필요하다. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 출시 전 권장 | 수면 잠금 유지 목적은 확인되나 specialUse subtype과 사용자 인지형 알림 설명을 콘솔에 맞춰야 한다. |
| `POST_NOTIFICATIONS` | 출시 전 권장 | 수면/원격 제어 알림 목적은 확인되나 첫 실행 즉시 요청과 온보딩 요청 중복을 QA해야 한다. |
| `WAKE_LOCK` | 출시 전 권장 | FCM 처리 목적의 제한 시간 wake lock으로 보이나 배터리/방해성 리스크 테스트가 필요하다. |
| `INTERNET`/`ACCESS_NETWORK_STATE` | 출시 전 권장 | Firebase, AdMob, RevenueCat, Kakao 연동 목적이며 데이터 보안 설문과 개인정보 처리방침 일치가 필요하다. |

## 참고한 공식 정책 문서

- Google Play Console Help, Permissions and APIs that Access Sensitive Information: https://support.google.com/googleplay/android-developer/answer/9888170
- Google Play Console Help, Use of the AccessibilityService API: https://support.google.com/googleplay/android-developer/answer/10964491
- Google Play Console Help, Understanding foreground service and full-screen intent requirements: https://support.google.com/googleplay/android-developer/answer/13392821
- Google Play Console Help, Developer Program Policy - Exact Alarm Permission and Full-Screen Intent Permission: https://support.google.com/googleplay/android-developer/answer/16313518

## 요약

- AndroidManifest와 Gradle 기준으로 확인된 핵심 권한 리스크는 접근성 서비스, 오버레이, exact alarm, full-screen intent, FGS specialUse다.
- 현재 저장소에는 각 권한의 기능상 사용 근거가 있으나, Google Play Console 권한 선언과 실제 심사 승인 여부는 저장소만으로 확인할 수 없다.
- 출시 전에는 최소 권한 원칙, 앱 내 명확한 고지/동의, 콘솔 선언 문구와 개인정보 처리방침의 일치 여부를 별도 게이트로 관리해야 한다.
