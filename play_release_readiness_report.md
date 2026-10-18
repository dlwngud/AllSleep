# AllSleep Google Play 출시 전 점검 리포트

점검일: 2026-05-09

범위: 현재 저장소의 코드, Gradle 설정, Manifest, README, 앱 내 링크만 기준으로 점검했다. Google Play Console, AdMob Console, RevenueCat 대시보드, 실제 개인정보처리방침 본문은 직접 확인하지 않았다.

참고한 Google 공식 문서:
- Google Play Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Google Play Data safety section: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Permissions and APIs that access sensitive information: https://support.google.com/googleplay/android-developer/answer/13986130
- AccessibilityService API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Foreground service and full-screen intent requirements: https://support.google.com/googleplay/android-developer/answer/13392821

## 출시 차단

| 위치 | 리스크 | 권장 조치 |
| --- | --- | --- |
| `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `composeApp/build.gradle.kts:158-163`, `admob.properties.example` | 앱 오픈 광고 로딩 코드가 Google 테스트 광고 단위 ID를 하드코딩한다. release Gradle에는 운영 ID 주입 구조가 있지만 실제 로딩 경로가 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`를 쓰지 않아 운영 광고가 노출되지 않고 수익화 검증이 실패한다. | 출시 전 필수로 `AD_UNIT_ID` 하드코딩을 제거하고 release `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`를 사용한다. release AAB에서 AdMob 앱 ID와 광고 단위 ID가 모두 운영 ID인지 확인한다. |
| `composeApp/build.gradle.kts:103-113`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/BillingProviderImpl.kt` | RevenueCat 운영 API 키와 테스트 API 키가 모두 BuildConfig에 들어가며, 저장소만으로 운영 offering, entitlement, product ID 연결을 검증할 수 없다. 테스트 구독관리 ID 또는 sandbox 설정이 남으면 상품 조회/구매/복원 실패와 Play Billing 정책 리스크가 생긴다. | RevenueCat 대시보드에서 테스트 API 키, 테스트 offering, 테스트 entitlement, sandbox 상품을 운영 API 키, 운영 offering, `premium` entitlement, Google Play 운영 구독 상품으로 전환한다. 내부/비공개 트랙 release 빌드로 상품 조회, 구매, 복원, 취소/만료 후 권한 반영을 검증한다. |
| `composeApp/src/androidMain/AndroidManifest.xml:8-9`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/SleepSchedulerImpl.kt` | `SCHEDULE_EXACT_ALARM`과 `USE_EXACT_ALARM`이 동시에 선언되어 있다. `USE_EXACT_ALARM`은 제한 권한이며 알람/타이머/캘린더처럼 핵심 기능에 정확한 알람이 필요한 앱만 허용되는 심사 대상이다. | 수면 루틴의 정확한 취침/기상 예약이 핵심 기능이라는 근거를 Play Console 권한 선언에 명확히 작성한다. 둘 중 하나만 충분하면 불필요한 exact alarm 권한 제거를 출시 전 수정 항목으로 처리한다. |
| `composeApp/src/androidMain/AndroidManifest.xml:22`, `composeApp/src/androidMain/AndroidManifest.xml:89-98`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt` | 오버레이와 AccessibilityService가 수면 중 앱 사용 차단에 사용된다. `isAccessibilityTool=false`라서 prominent disclosure와 Play Console Accessibility declaration이 필수다. 설명이 스토어 등록정보와 다르면 심사 반려 가능성이 높다. | 현재 앱 내 고지 문구를 기준으로 Play Console Accessibility declaration을 작성한다. 접근성 서비스가 화면 내용 수집 없이 앱 실행 감지/차단에만 쓰인다는 점, 사용자 동의/해제 경로, 수면 모드 중에만 동작한다는 점을 일치시킨다. |

## 출시 전 권장

| 위치 | 리스크 | 권장 조치 |
| --- | --- | --- |
| `composeApp/src/androidMain/AndroidManifest.xml:11`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AllSleepMessagingService.kt:104-114` | 모든 FCM 원격 제어 알림에 `CATEGORY_ALARM`과 full-screen intent를 적용한다. Android 14+에서는 알람/전화 핵심 기능이 아니면 자동 허용되지 않거나 사용자 허용이 필요하다. | 실제 알람성 이벤트에만 full-screen intent를 제한하고, 단순 원격 상태 동기화는 일반 heads-up 알림으로 degrade하는지 QA한다. Play Console full-screen intent declaration을 작성한다. |
| `composeApp/src/androidMain/AndroidManifest.xml:25-26`, `composeApp/src/androidMain/AndroidManifest.xml:79-85` | `FOREGROUND_SERVICE_SPECIAL_USE`와 `foregroundServiceType="specialUse"`를 사용한다. special use는 Play Console 설명과 사용자 인지 가능한 지속 알림이 중요하다. | `screen_lock_overlay` 목적, 사용자 시작 조건, 종료 조건, 지속 알림 문구를 Play Console FGS 선언에 저장소 동작과 동일하게 작성한다. Android 14+ release 기기에서 서비스 시작/종료와 알림 표시를 검증한다. |
| `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:21-22`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:380-389` | 개인정보처리방침/약관 링크는 앱 안에 있지만 본문은 저장소에 없다. Firebase Auth/Firestore/FCM, Google/Kakao 로그인, AdMob, RevenueCat, 기기 ID/FCM 토큰, 수면 기록 처리 내용과 불일치할 수 있다. | 개인정보처리방침에 수집 데이터, 공유 SDK, 보관/삭제, 문의처, 암호화/전송, 계정 삭제 범위를 포함하고 Play Console Data safety 답변과 일치시킨다. |
| `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:32`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:54-60` | release에서도 RevenueCat DEBUG 로그와 Firebase 프로젝트/Application ID 로그가 남는다. 직접 비밀키 노출은 아니지만 운영 로그 노이즈와 정보 노출 리스크가 있다. | `BuildConfig.DEBUG` 기준으로 디버그 로그를 제한하고 release에서는 RevenueCat 로그 레벨을 낮춘다. |
| `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:90-95` | `isWeekendWakeEnabled`가 `isWakeAlarmEnabled` 필드에 저장되는 주석상 오타가 있다. 주말 기상 알람 설정이 평일/공통 필드를 덮어 출시 후 사용자 루틴 오류를 만들 수 있다. | Firestore 필드명을 `isWeekendWakeEnabled`로 분리하는 수정과 기존 데이터 마이그레이션/호환 처리를 검토한다. |
| `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:361-370`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:393-395`, `composeApp/build.gradle.kts:98-99` | 설정 화면의 문의 메일/버전 표시는 `v1.1.0`이지만 Gradle release 버전은 `1.5`다. 사용자 문의와 스토어 표시 버전이 어긋난다. | 앱 내 표시 버전을 BuildConfig에서 가져오도록 바꾸거나 release 전 수동 동기화 체크를 추가한다. |

## 출시 후 개선

| 위치 | 리스크 | 권장 조치 |
| --- | --- | --- |
| `composeApp/src/commonTest/kotlin` | ViewModel 테스트는 일부 있으나 권한 요청, 알람 예약, FCM 수면 제어, RevenueCat 실제 흐름, AdMob release ID 검증 자동 테스트는 없다. | 출시 후라도 결제/복원, 알람 예약, 접근성/오버레이, 광고 ID 주입을 검증하는 통합 QA 체크리스트와 테스트를 추가한다. |
| `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/*` | 접근성, 알림, 오버레이, 구독 안내가 여러 화면에 분산되어 있어 사용자가 거절/실패 상태에서 다음 행동을 놓칠 수 있다. | 권한별 상태, 필요 이유, 설정 이동, 실패 시 대체 동작을 한 곳에서 확인할 수 있는 출시 후 온보딩/설정 개선을 검토한다. |
| `composeApp/src/commonMain/composeResources/drawable/*.png`, `composeApp/src/androidMain/res/mipmap-*` | 캐릭터 이미지와 앱 아이콘 리소스가 늘어나고 있으나 용량 최적화, 스토어 스크린샷, 접근성 설명 품질은 저장소만으로 충분히 검증되지 않았다. | release AAB 용량, 저사양 기기 메모리, 이미지 압축률, Play 스토어 그래픽 자산 품질을 별도 점검한다. |

## 저장소만으로 확인 불가

| 위치 | 리스크 | 권장 조치 |
| --- | --- | --- |
| AdMob Console, `admob.properties` 로컬 파일 | 운영 AdMob 앱 승인, 광고 단위 활성화, app-ads.txt, release ID 실제 값은 저장소에 없다. | 운영 AdMob Console에서 앱 승인과 광고 단위 상태를 확인하고, release AAB에서 테스트 ID가 남지 않았는지 로그/디컴파일/내부 테스트로 검증한다. |
| RevenueCat Dashboard, Google Play Console 구독 상품 | 운영 offering/package/product/entitlement, 구독 가격/기간/오퍼, Play 상품 연결은 저장소에 없다. | RevenueCat과 Play Console의 상품 ID를 대조하고, 내부/비공개 트랙에서 실제 구매, 복원, 취소, 만료, 환불 후 앱 상태를 검증한다. |
| Google Play Console App content | Data safety, Ads ID, Accessibility API, exact alarm, foreground service, full-screen intent declaration 제출 여부는 저장소에 없다. | 위 권한/SDK 사용 내역을 기준으로 App content 선언을 모두 작성하고, 개인정보처리방침 및 앱 내 고지와 동일하게 맞춘다. |
| 개인정보처리방침 Notion URL | 본문이 저장소에 없어 실제 공개 접근성, 비지오펜스 여부, 수정 가능 문서 여부, Play 정책 요구 항목 포함 여부를 확인할 수 없다. | 비로그인 브라우저에서 공개 접근을 확인하고, 정책 제목/문의처/수집 항목/공유 SDK/보관 및 삭제/계정 삭제를 포함한다. |
| release 서명/업로드 키, Play App Signing | `keystore.properties`와 실제 Play App Signing 설정은 저장소에 없다. | release AAB 서명, 업로드 키, Play App Signing 활성화 상태, versionCode 증가 정책을 Play Console에서 확인한다. |

## 결론

현재 저장소 기준으로 가장 먼저 막아야 할 항목은 두 가지다. 첫째, 앱 오픈 광고 코드의 Google 테스트 광고 단위 ID 하드코딩을 운영 ID 경로로 바꿔야 한다. 둘째, RevenueCat 테스트 구독관리 ID와 sandbox 설정이 운영 상품으로 전환됐는지 콘솔과 내부 트랙 release 빌드에서 검증해야 한다.

그 다음 출시 전에는 exact alarm, AccessibilityService, overlay, foreground service special use, full-screen intent를 Google Play App content 선언과 앱 내 고지에 맞춰 정리해야 한다. 개인정보처리방침과 Data safety 답변은 Firebase, FCM, AdMob, RevenueCat, Google/Kakao 로그인, 기기 정보, 수면 기록 처리 내용을 모두 반영해야 한다.
