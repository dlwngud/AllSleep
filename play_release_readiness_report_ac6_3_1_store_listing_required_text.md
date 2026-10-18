# Google Play 출시 전 점검 리포트 - Sub-AC 6.3.1 스토어 등록정보 필수 텍스트

## 점검 범위

- 기준: 현재 저장소의 앱 코드, Gradle 설정, Android 리소스, README, 기존 출시 점검 문서
- 제외: Google Play Console 스토어 등록 화면의 실제 입력값, 스토어 스크린샷/그래픽 자산 등록 상태, Play Console 앱 콘텐츠 설문 직접 확인
- 관점: Google Play 스토어 등록정보에 필요한 앱 이름, 짧은 설명, 전체 설명, 개인정보처리방침 URL, 연락처/지원, 광고/구독/권한 고지에 사용할 텍스트 근거의 준비 상태

## 출시 차단

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 스토어 등록/정책 | Google Play Console 등록정보 원문 파일 저장소 부재, `README.md:11-21`, `composeApp/src/androidMain/res/values/strings.xml:3` | 앱 이름과 기능 설명의 원천은 일부 확인되지만, Play Console에 입력할 짧은 설명과 전체 설명의 확정 문안이 저장소에 없다. 접근성 서비스, 오버레이 잠금, 알람, 광고, 구독을 포함하는 실제 기능이 스토어 설명에 누락되면 사용자 오해와 심사 지연/반려가 발생할 수 있다. | 출시 전 Console 입력용 텍스트를 별도 릴리즈 체크리스트나 문서로 고정한다. 짧은 설명은 “수면 루틴과 휴대폰 사용 차단” 수준의 실제 기능으로 제한하고, 전체 설명에는 수면 모드 잠금, 접근성 서비스 기반 앱 전환 감지, 오버레이, 알람, 수면 기록, 다중 기기 동기화, 프리미엄 구독, 앱 오픈 광고 포함 여부를 코드와 일치하게 반영한다. | true |
| P0 | 정책/권한 | `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:3-8`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:35-74`, `composeApp/src/androidMain/AndroidManifest.xml:78-96` | 접근성 서비스 설명과 앱 내 공개 다이얼로그는 확인되지만, 스토어 전체 설명과 권한 선언용 텍스트가 저장소에 없다. 비장애 목적 접근성 API 사용을 스토어 설명에서 명확히 설명하지 않으면 Google Play 정책 검토에서 목적 불명확으로 반려될 수 있다. | 스토어 등록 텍스트와 Play Console 권한 선언에 “수면 모드 중 다른 앱 실행을 감지해 AllSleep 잠금 화면으로 되돌리기 위해 접근성 서비스를 사용하며, 개인 정보나 민감 데이터를 수집/전송하지 않는다”는 취지를 앱 내 고지와 동일하게 작성한다. | true |
| P0 | 개인정보/스토어 등록 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:21-22`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:383-389`, `README.md:82-85` | 개인정보처리방침 URL은 앱 내 Notion 링크로 확인되지만, 정책 문서 본문과 Play Console 등록 URL의 실제 값은 저장소만으로 검증할 수 없다. Firebase Auth/Firestore/FCM, Google/Kakao 로그인, AdMob, RevenueCat, 수면 기록/기기 정보 처리 내용이 정책 문서와 스토어 설명에서 불일치하면 데이터 보안/개인정보 정책 리스크가 있다. | 출시 전 비로그인 브라우저에서 개인정보처리방침 URL 공개 접근을 확인하고, Play Console 개인정보처리방침 URL과 앱 내 링크가 동일한지 대조한다. 정책 문서에는 인증, 수면 루틴/기록, 기기 동기화, FCM 토큰, 광고 SDK, 구독/결제 데이터, 계정 삭제 범위를 포함한다. | true |
| P0 | 광고 SDK/스토어 등록 | `admob.properties:4-8`, `composeApp/build.gradle.kts:145-163`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:26-30`, `play_release_readiness_report_ac3_store_content_mismatch.md:25` | release 프로퍼티에는 운영 AdMob ID가 있지만 앱 오픈 광고 로딩 코드는 Google 테스트 광고 단위 ID를 하드코딩한다. 스토어 등록의 광고 포함 여부와 실제 release 동작이 맞더라도 테스트 ID가 남으면 운영 광고 검증과 수익화가 실패하고, 스토어 설명/데이터 보안 문구와 운영 SDK 동작을 검증할 수 없다. | 출시 전 광고 및 AdMob 테스트 ID를 실제 운영 ID로 전환하는 항목을 필수 게이트로 둔다. 앱 오픈 광고 코드가 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`의 release 운영 값을 사용하도록 확인하고, 스토어 등록정보와 데이터 보안 섹션에 광고 포함 여부를 운영 구성 기준으로 작성한다. | true |
| P0 | 결제/스토어 등록 | `composeApp/build.gradle.kts:103-113`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82`, `README.md:21`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:448-465` | RevenueCat API 키는 로컬 프로퍼티에서 주입되고, 구독 상품/entitlement/offering의 실제 운영 설정은 저장소에 없다. 테스트 구독관리 ID나 샌드박스 offering이 남아 있으면 Play 스토어 구독 설명, 앱 내 가격/자동 갱신 문구, 실제 구매/복원 동작이 불일치할 수 있다. | 출시 전 RevenueCat 구독관리 테스트 ID, 테스트 API 키, 샌드박스 offering/entitlement를 실제 운영 ID/운영 상품으로 전환했는지 필수 확인한다. 스토어 전체 설명과 구독 섹션 문구에는 프리미엄 기능, 자동 갱신, 취소 가능, 복원 가능 범위를 Play 상품 설정과 동일하게 작성한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 스토어 등록 | `composeApp/src/androidMain/res/values/strings.xml:3`, `composeApp/src/androidMain/AndroidManifest.xml:32`, `README.md:1-12` | 앱 표시 이름은 `AllSleep`으로 확인되지만, Google Play 앱 이름 입력값과 현지화별 이름 정책은 저장소에 없다. README의 영문 부제 “Modern Sleep Lock & Habit Tracking App”와 한국어 설명이 혼재해 있어 스토어 기본 언어 문구가 확정되지 않은 상태다. | 기본 등록 언어를 한국어로 둘 경우 앱 이름은 `AllSleep`으로 유지하고, 짧은 설명/전체 설명은 한국어로 확정한다. 영문 등록정보를 제공할 경우 영문 짧은 설명과 전체 설명도 별도 검수한다. | true |
| P1 | 스토어 등록/UX | `README.md:11-21`, `README.md:200-234`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:212-230`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:306-332` | 스토어 전체 설명의 소재가 되는 기능 근거는 충분하지만, 권한이 필요한 기능과 무료/프리미엄 기능의 경계가 문서상 명확히 분리되어 있지 않다. 설명이 과장되면 건강/생산성 앱으로서 기대와 실제 권한 요구가 어긋날 수 있다. | 전체 설명을 기능별로 작성하되 “수면 루틴 관리”, “수면 모드 중 앱 사용 차단”, “수면 기록 확인”, “다중 기기 동기화”, “프리미엄 통계”처럼 실제 화면 단위로 제한한다. 의료적 진단, 치료, 수면 개선 보장 표현은 사용하지 않는다. | true |
| P1 | 스토어 등록/지원 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:345-371`, `README.md:251-253` | FAQ URL과 지원 이메일은 앱 안에서 확인되지만, Play Console 개발자 연락처/지원 URL 입력값은 저장소에 없다. 문의 메일 템플릿은 `v1.1.0`으로 고정되어 빌드 버전 `1.5`와 불일치한다. | 스토어 등록의 개발자 이메일은 앱 내 `official.allsleep@gmail.com`과 일치시키고, FAQ/지원 URL을 등록할지 결정한다. 출시 전 앱 내 문의 템플릿과 버전 표기를 `versionName=1.5` 또는 빌드 값 기반으로 맞춘다. | true |
| P1 | 스토어 등록/품질 | `README.md:24-28`, `design-previews/`, `play-store-icon-candidates/` | README의 스크린샷 표는 “준비 중”이고, 디자인 미리보기/아이콘 후보는 있으나 스토어 등록용 캡션이나 이미지별 설명 문구가 저장소에 없다. 실제 등록자료는 직접 점검 대상 밖이지만, 텍스트와 스크린샷이 다른 기능을 약속할 위험이 있다. | 스토어 스크린샷 캡션은 release 빌드 실제 화면 기준으로 작성한다. 홈 수면 모드, 알람 루틴, 통계, 설정/권한, 구독 화면의 텍스트가 전체 설명과 충돌하지 않는지 확인한다. | true |
| P1 | 정책/권한 | `composeApp/src/androidMain/AndroidManifest.xml:5-26`, `README.md:249`, `play_release_readiness_report_ac6_2_1_permissions.md` | 알림, 정확한 알람, 전체 화면 인텐트, 오버레이, 포그라운드 서비스 권한을 사용한다. 스토어 등록 설명과 앱 콘텐츠/권한 선언의 텍스트가 앱 내 안내와 다르면 권한 심사에서 보완 요청을 받을 수 있다. | 필수 텍스트 검수 시 권한별 사용자 가치와 사용 시점을 한 문장씩 준비한다. 예: “취침/기상 루틴 알림”, “수면 모드 중 잠금 화면 유지”, “앱 전환 감지 후 수면 잠금 복귀”. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질/스토어 등록 | `README.md`, `play_release_readiness_report_ac3_store_content_mismatch.md`, `play_release_readiness_report_ac4_repository_unknowns.md` | 스토어 등록 텍스트가 저장소와 별도로 관리되면 기능 변경 후 설명, 개인정보처리방침, 데이터 보안 응답이 뒤처질 수 있다. | 출시 후 릴리즈마다 `스토어 앱 이름`, `짧은 설명`, `전체 설명`, `개인정보처리방침 URL`, `지원 이메일/URL`, `광고 포함 여부`, `구독 문구`, `권한 설명`을 체크하는 문서 또는 PR 템플릿을 추가한다. | true |
| P2 | 스토어 등록/현지화 | `README.md:1-12`, `composeApp/src/androidMain/res/values/strings.xml:3` | 앱 이름은 영문이고 README는 한국어/영문이 섞여 있다. 향후 국가별 등록정보를 늘리면 기능 설명과 정책 고지의 번역 품질이 불균일해질 수 있다. | 한국어 기본 등록정보를 먼저 확정하고, 영문 등 추가 언어는 동일한 기능 범위와 정책 고지를 유지하도록 번역 리뷰 프로세스를 둔다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 스토어 등록 | Google Play Console 스토어 등록정보 입력 화면 | 앱 이름, 짧은 설명, 전체 설명, 앱 카테고리, 태그, 개인정보처리방침 URL, 개발자 연락처, 스크린샷 캡션, 최근 변경사항의 실제 입력값은 저장소에 없다. 저장소의 앱 기능/권한/SDK와 Console 문구가 일치하는지 직접 확인할 수 없다. | Play Console 자료는 본 저장소 점검 범위에서 제외하고 별도 수동 검수로 확인한다. 검수 시 이 리포트의 P0/P1 항목과 Console 입력값을 한 줄씩 대조한다. | false |
| P0 | 개인정보 | 외부 Notion 개인정보처리방침/이용약관 본문 | 앱 내 URL은 확인되지만 외부 문서의 공개 여부, 최신성, 데이터 보안 설문과의 일치 여부는 저장소만으로 확인할 수 없다. | 비로그인/시크릿 브라우저에서 URL 접근성을 확인하고, Play Console 개인정보처리방침 URL과 앱 내 URL이 동일한지 확인한다. | false |
| P0 | 광고 SDK | AdMob Console, Play Console 광고 포함 여부 | 저장소에는 AdMob 설정과 테스트 ID 하드코딩이 보이지만, 운영 광고 단위 승인 상태와 Play Console 광고 포함 여부 입력값은 확인할 수 없다. | AdMob 테스트 ID를 실제 운영 ID로 전환한 release 후보를 만들고, AdMob 콘솔 및 Play Console 광고 포함 여부를 운영 상태 기준으로 검수한다. | false |
| P0 | 결제 | RevenueCat 대시보드, Play Console 구독 상품 | RevenueCat 운영 API 키, 구독관리 운영 ID, Play 구독 상품명/가격/기간/오퍼의 실제 값은 저장소에 없다. | RevenueCat 테스트 구독관리 ID와 테스트 API 키가 운영 ID/운영 상품으로 전환되었는지 콘솔에서 확인하고, 스토어 설명 및 앱 내 구독 문구와 일치시킨다. | false |
| P1 | 스토어 등록/릴리스 | Google Play Console “새로운 기능” 또는 릴리스 노트 입력값 | 저장소에는 `versionName=1.5`와 README의 기능 설명은 있지만, 이번 출시의 변경사항 텍스트는 없다. 릴리스 노트가 실제 변경 범위와 다르면 사용자 공지와 심사 대응에 혼선이 생긴다. | `1.5` 릴리스 기준으로 사용자 관점의 변경사항 3-5개를 작성하고, 접근성/권한/광고/구독 같은 정책 민감 변경이 있으면 내부 QA 메모와 분리해 관리한다. | false |

## 요약 판단

- 저장소 기준으로 앱 이름(`AllSleep`), 핵심 기능 설명, 개인정보처리방침/이용약관 URL, FAQ/지원 이메일, 광고/구독 SDK 사용 근거는 확인된다.
- 그러나 Play Console에 입력할 짧은 설명, 전체 설명, 최근 변경사항, 개발자 연락처, 개인정보처리방침 URL의 실제 등록값은 저장소에 없어 준비 완료로 판정할 수 없다.
- 출시 전 필수 게이트는 접근성/오버레이/알람/광고/구독 문구를 실제 코드와 일치시키는 것, AdMob 테스트 광고 ID를 운영 ID로 전환하는 것, RevenueCat 테스트 구독관리 ID와 테스트 API 키/상품을 운영 ID/운영 상품으로 전환하는 것이다.
