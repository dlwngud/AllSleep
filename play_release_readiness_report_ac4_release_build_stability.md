# Google Play 출시 전 종합 점검 리포트 - AC4 릴리즈 빌드 설정 안정성

점검 범위: 현재 저장소의 앱 코드, Gradle 설정, 로컬 설정 파일, 릴리즈 산출물, 검증 스크립트. Google Play Console, AdMob Console, RevenueCat 대시보드의 실제 운영 설정은 직접 점검하지 않았다.

## 출시 차단

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 광고 SDK/안정성 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29`, `:51`, `:167`; `composeApp/build.gradle.kts:158-163`; `admob.properties:5`, `:8` | release 빌드 타입은 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`에 운영 광고 단위 ID를 주입하지만, 실제 앱 오픈 광고 로딩 코드는 Google 샘플 테스트 광고 단위 ID를 하드코딩한다. 운영 출시 시 테스트 광고가 남아 광고 수익 미발생, AdMob 정책/검수 리스크, 실제 운영 광고 로딩 경로 미검증 문제가 발생할 수 있다. | 출시 전 `AppOpenAdManagerImpl`의 광고 단위 ID를 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`로 사용하도록 전환하고, release 빌드에서 `admob.properties`의 운영 앱 ID/광고 단위 ID가 매니페스트와 광고 로딩 코드에 모두 반영되는지 확인한다. 테스트 ID가 운영 빌드에 남지 않아야 한다. | true |
| P0 | 결제/안정성 | `composeApp/build.gradle.kts:103-113`; `local.properties:9-10`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82` | RevenueCat 운영 API 키와 테스트 API 키가 모두 BuildConfig에 주입된다. 런타임 선택 로직은 release에서 `REVENUECAT_API_KEY`를 우선 사용하지만, 저장소만으로 현재 키가 실제 Google Play 운영 프로젝트/상품에 연결된 운영 키인지 확정할 수 없다. 잘못된 테스트 키 또는 미연결 키로 출시하면 구독 조회/구매 복원 실패가 release에서 즉시 발생한다. | 출시 전 RevenueCat Android 운영 API 키가 `revenueCat.apiKey`에 주입되는지 확인하고, `revenueCat.testApiKey`는 debug 전용 검증으로만 사용한다. 내부/비공개 트랙 release 빌드에서 실제 Play 구독 상품, RevenueCat offering, `premium` entitlement가 연결되는지 구매/복원 플로우를 검증한다. | true |
| P0 | 안정성/품질 | `composeApp/build.gradle.kts:152-154`; `composeApp/proguard-rules.pro:1-2`; `composeApp/src/androidMain/AndroidManifest.xml:29`, `:63-98`; `gradle/libs.versions.toml:16-20`, `:56-58` | release 빌드는 R8 최적화가 켜져 있으나 프로젝트별 keep rule 파일이 비어 있다. Application 클래스, Kakao SDK OAuth Activity, 접근성 서비스, Firebase, RevenueCat UI, Compose Multiplatform 조합에서 reflection/manifest 기반 진입점 문제가 release에서만 발생할 수 있다. 현재 저장소에는 release minified 빌드 실행 결과 또는 내부 테스트 스모크 결과가 없다. | `:composeApp:bundleRelease`, `:composeApp:lintRelease`를 통과시킨 뒤 실제 release AAB로 로그인, 광고, 구독, 접근성 서비스, 알람/오버레이 플로우를 스모크 테스트한다. R8 관련 런타임 오류가 발생하면 해당 SDK 또는 앱 진입점에 필요한 keep rule을 `composeApp/proguard-rules.pro`에 최소 범위로 추가한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 안정성/품질 | `verify.sh:9-26` | 검증 스크립트가 `compileDebugKotlinAndroid`, `lintDebug`, `testDebugUnitTest`만 실행한다. release 빌드 타입의 R8, manifest placeholder, signing, BuildConfig 값 주입, resource shrink/packaging 문제는 debug 검증으로 잡히지 않는다. | 출시 후보 확정 전 검증 절차에 `:composeApp:bundleRelease`와 `:composeApp:lintRelease`를 별도 단계로 추가하거나 릴리즈 체크리스트에 수동 실행을 필수화한다. | true |
| P1 | 보안/안정성 | `composeApp/build.gradle.kts:124-156`; `keystore.properties`; `keystore/release.jks` | release signing 설정은 `keystore.properties` 존재 시에만 적용된다. 파일이 없거나 키 경로/비밀번호가 잘못되어도 설정 단계에서 명확히 실패하지 않고, 출시 직전 서명 오류 또는 업로드 불가 산출물이 만들어질 수 있다. 현재 작업트리에는 release keystore와 properties 파일이 존재하지만 git 추적 파일은 아닌 것으로 확인된다. | release 빌드에서는 keystore 설정 누락 시 즉시 실패하도록 운영 빌드 환경 체크를 두고, Play App Signing 등록 키와 현재 `release.jks`의 SHA-1/SHA-256을 별도 보관 문서로 대조한다. 키 파일은 저장소에 커밋하지 않는다. | true |
| P1 | 안정성/운영 | `composeApp/build.gradle.kts:98-99`; `composeApp/release/composeApp-release.aab` | 저장소에는 `versionCode = 6`, `versionName = "1.5"`와 기존 AAB 산출물이 있으나, 이 산출물이 현재 작업트리의 설정/코드와 동일한 커밋에서 생성되었는지 확인할 수 없다. 잘못된 산출물을 Play에 업로드하면 재현성, 롤백, 심사 대응이 어려워진다. | 출시 직전 깨끗한 작업트리 또는 명시된 태그/커밋에서 AAB를 재생성하고, 산출물명에 versionCode/commit hash를 포함해 보관한다. | true |
| P1 | 안정성/성능 | `composeApp/build.gradle.kts:83-87`, `:175-185`; `gradle/libs.versions.toml:2-20` | Guava 버전을 전역 force하고 Firebase BoM, Ads, RevenueCat, Kakao, Compose Multiplatform 등 SDK가 함께 사용된다. dependency 충돌은 debug에서 드러나지 않거나 특정 API 레벨 release 최적화 후에만 노출될 수 있다. | release 빌드 후 dependency insight 또는 Gradle dependency report를 보관하고, 핵심 SDK 초기화 경로를 내부 테스트 트랙에서 API 24, 최신 API, Google Play services 미설치/구버전 기기로 검증한다. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 안정성/품질 | `build.gradle.kts:13-19`; `composeApp/build.gradle.kts:175-180`; crash reporting 설정 부재 | Firebase는 Auth/Firestore/Messaging/Functions만 의존성으로 확인되고 Crashlytics 플러그인/SDK는 보이지 않는다. 출시 후 Android Vitals와 사용자 제보만으로는 release-only 크래시 원인 파악이 늦어진다. | 출시 후라도 Crashlytics 또는 동등한 크래시 수집 체계를 추가하고, release mapping file 업로드까지 CI/릴리즈 절차에 포함한다. | true |
| P2 | 품질/운영 | `gradle.properties:6-8`; `verify.sh:9-26` | Gradle cache/configuration cache는 켜져 있지만 릴리즈 검증 결과를 기록하는 문서나 CI 로그 보관 규칙은 저장소에 없다. 재현 가능한 출시 절차가 약하면 긴급 패치 시 같은 산출물을 재생성하기 어렵다. | release build, lint, unit test, smoke test 결과를 버전별로 남기는 `release-checklist` 문서를 추가하고, 가능하면 CI에서 동일 명령을 실행한다. | true |
| P2 | 보안/개인정보 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:32`, `:54-60`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthViewModel.kt:95-119`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/datasource/auth/KakaoAuthDataSource.kt:41` | release에서도 RevenueCat log level이 DEBUG로 설정되고 Firebase/Kakao/Auth 관련 진단 로그가 남는다. 치명적 출시 차단은 아니지만 운영 로그에 프로젝트 ID, application ID, 사용자 객체, 인증 응답 일부가 노출될 수 있어 문제 분석과 개인정보 최소화 측면에서 불리하다. | 운영 빌드에서는 로그 레벨과 민감 로그를 `BuildConfig.DEBUG` 조건으로 제한하고, 필요한 운영 진단은 익명화된 이벤트/크래시 리포트로 전환한다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 안정성/품질 | 실행 시도: `./gradlew :composeApp:bundleRelease --parallel --build-cache` | 현재 환경에서는 홈 디렉터리 Gradle wrapper lock 파일 접근이 샌드박스에 막혔고, workspace 내부 `GRADLE_USER_HOME` 재시도는 네트워크 제한으로 Gradle 배포판을 받을 수 없어 release 번들 검증을 완료하지 못했다. 따라서 현재 저장소 상태가 실제로 release bundle까지 통과하는지 이 실행 환경만으로 확정할 수 없다. | 개발자 로컬 또는 CI처럼 Gradle wrapper 배포판 접근이 가능한 환경에서 `:composeApp:bundleRelease`와 `:composeApp:lintRelease`를 실행하고 결과 로그를 출시 체크리스트에 첨부한다. | true |
| P0 | 광고 SDK | `admob.properties:5`, `:8`; `composeApp/src/androidMain/AndroidManifest.xml:37-48` | 저장소에는 release AdMob 운영 형식의 앱 ID/광고 단위 ID가 있으나, 해당 ID가 AdMob Console에서 승인된 운영 앱/광고 단위인지, 정책 센터 이슈가 없는지는 확인할 수 없다. | AdMob Console에서 앱 승인, 광고 단위 활성화, app-ads.txt 필요 여부, 정책 센터 이슈를 확인한다. 저장소 코드의 테스트 ID 제거 확인과 콘솔 운영 ID 검증을 둘 다 완료해야 한다. | true |
| P0 | 결제 | `local.properties:9-10`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82` | RevenueCat API 키 문자열은 저장소 로컬 파일에서 확인되지만, 실제 RevenueCat 프로젝트가 Google Play Console의 운영 구독 상품/기본 요금제와 연결되어 있는지는 저장소만으로 확인할 수 없다. | RevenueCat 대시보드에서 Android 앱 설정, Play service credentials, offering/package/product, `premium` entitlement 연결을 확인하고 내부 테스트 트랙 release 빌드로 결제/복원 성공을 검증한다. | true |
| P1 | 보안/서명 | `keystore.properties`; `keystore/release.jks`; `composeApp/build.gradle.kts:124-156` | 현재 keystore 파일이 실제 Play App Signing 업로드 키인지, 키 만료/백업/권한 관리가 적절한지는 저장소만으로 판단할 수 없다. | Play Console App Integrity의 업로드 인증서 지문과 로컬 release keystore 지문을 대조하고, 키 백업/접근 권한/교체 절차를 별도 운영 문서로 관리한다. | true |
