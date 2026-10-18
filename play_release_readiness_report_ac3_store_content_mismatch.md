# Google Play 출시 전 점검 리포트 - Sub-AC 3 문서/스토어 등록 콘텐츠 정책 불일치

## 점검 범위

- 기준: 현재 저장소의 앱 코드, 설정, README, 기존 출시 점검 문서
- 제외: Google Play Console 스토어 등록 화면, 앱 콘텐츠 설문 입력값, 데이터 보안 설문, 콘텐츠 등급 설문, AdMob/RevenueCat/Firebase 콘솔 상태의 직접 확인
- 관점: 저장소에서 확인되는 실제 기능/SDK/문서와 Play 스토어 등록 정보 또는 공개 정책 문서가 불일치하거나 누락될 수 있는 리스크

## 출시 차단

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 정책/스토어 등록 | `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:3`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:7`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:8`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:27-45`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:35-74` | 앱은 접근성 서비스로 창 전환 이벤트와 패키지명을 감지해 수면 잠금 흐름을 유지한다. 앱 내 고지는 존재하지만 Play Console 권한 선언, 스토어 설명, 개인정보 처리방침이 이 동작을 동일하게 설명하지 않으면 접근성 API 오용 또는 불충분한 공개로 심사 반려될 수 있다. | Play Console 접근성/민감 권한 선언, 스토어 설명, 앱 내 고지, 개인정보 처리방침에 "수면 모드 중 다른 앱 실행 감지 및 AllSleep 잠금 화면 복귀" 목적을 동일한 표현으로 반영한다. 장애 보조 목적 앱이 아니므로 `isAccessibilityTool=false` 상태에서 비장애 목적 사용의 허용 가능성을 출시 전 별도 정책 검토한다. | true |
| P0 | 권한/스토어 등록 | `composeApp/src/androidMain/AndroidManifest.xml:8-11`, `composeApp/src/androidMain/AndroidManifest.xml:22`, `composeApp/src/androidMain/AndroidManifest.xml:25-26`, `composeApp/src/androidMain/AndroidManifest.xml:78-85`, `README.md:16`, `README.md:249` | 정확한 알람, 전체 화면 인텐트, 다른 앱 위 표시, 특수 용도 포그라운드 서비스가 앱 핵심 기능에 포함된다. Play Console 앱 콘텐츠/권한 선언과 스토어 설명에서 "수면 알람", "수면 잠금 오버레이", "잠금 유지" 목적이 누락되거나 다르게 표현되면 권한 정책 심사 지연 또는 반려 리스크가 있다. | 스토어 등록 설명과 권한 선언에 각 권한의 사용자 가치와 노출 시점을 분리해 적는다. 오버레이가 수면 모드 중에만 사용되고 광고/피싱/시스템 UI 모방 목적이 아님을 공개 문서에도 맞춰 둔다. | true |
| P0 | 개인정보/스토어 등록 | `README.md:82-85`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:108-128`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:40-54`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:21-22`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:380-389` | 앱은 Firebase Auth/Firestore/FCM, Google/Kakao 로그인, AdMob, RevenueCat를 사용하고 이메일, 이름, 수면 루틴/상태, 기기명, FCM 토큰, 광고 SDK 데이터, 구독 정보를 처리할 수 있다. 저장소에는 개인정보 처리방침 링크만 있고 실제 정책 문서 내용과 Play 데이터 보안 설문 일치 여부는 확인되지 않는다. | 개인정보 처리방침과 Play Console 데이터 보안 섹션에 수집 데이터, 처리 목적, 공유 여부, 보관/삭제 기준을 SDK별로 반영한다. Notion 개인정보 처리방침은 비로그인 공개 접근, 최신 앱 버전 반영, 앱 내 링크와 스토어 등록 URL의 일치 여부를 출시 전 확인한다. | true |
| P0 | 광고 SDK | `admob.properties:4-8`, `composeApp/build.gradle.kts:145-163`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:49-52`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:164-168` | release용 AdMob 운영 ID는 설정 파일에 있으나 실제 광고 로딩 코드는 Google 테스트 광고 단위 ID를 하드코딩한다. Play 스토어의 광고 포함 여부, AdMob 앱/광고 단위, 데이터 보안 설문이 운영 광고 구성과 맞지 않거나 테스트 ID가 남으면 광고 정책 및 수익화 검증에 실패할 수 있다. | 출시 전 광고 및 AdMob 테스트 ID를 실제 운영 ID로 전환하는 항목을 필수 체크로 둔다. 광고 로딩 경로가 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`의 release 운영 값을 쓰는지 빌드 산출물 기준으로 확인하고, Play Console의 광고 포함 여부와 데이터 보안 응답을 AdMob 운영 설정과 대조한다. | true |
| P0 | 결제 | `composeApp/build.gradle.kts:103-113`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82`, `README.md:21`, `README.md:230-234`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:448-465` | RevenueCat API 키가 local properties에서 주입되고 debug에서는 test key를 우선한다. Play Console 구독 상품, RevenueCat entitlement/offering, 앱 내 가격/자동 갱신 문구가 서로 불일치하거나 테스트 상품/테스트 ID가 남으면 결제 심사와 실제 구독 복원에 실패할 수 있다. | 출시 전 RevenueCat 구독관리 테스트 ID, 샌드박스 offering, 테스트 entitlement를 실제 운영 ID/운영 상품으로 전환했는지 필수 확인한다. 내부/비공개 트랙에서 운영 release 빌드로 상품 조회, 구매, 복원, 취소 후 상태 반영을 검증한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 스토어 등록/UX | `README.md:24-28`, `design-previews/`, `README.md:253` | README의 스크린샷은 "준비 중"이며 디자인 미리보기 이미지는 별도 폴더에 있다. Play 등록 스크린샷은 직접 점검 대상이 아니지만, 실제 앱 화면과 스토어 스크린샷/설명이 권한, 광고, 구독, 수면 잠금 동작을 다르게 보여주면 사용자 오해 및 심사 커뮤니케이션 리스크가 있다. | 운영 release 빌드의 실제 화면 캡처를 기준으로 스토어 스크린샷과 설명을 작성한다. 특히 접근성 권한 고지, 오버레이 잠금, 구독 화면, 광고 노출 가능성을 스토어 문구와 앱 내 경험이 충돌하지 않게 맞춘다. | true |
| P1 | 품질/스토어 등록 | `composeApp/build.gradle.kts:98-99`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:362-370`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:392-396` | 빌드 설정은 `versionName=1.5`이지만 설정 화면과 문의 메일 템플릿은 `v1.1.0`으로 고정되어 있다. 스토어 등록 버전, 앱 내 버전 정보, 고객 문의 본문이 다르면 QA/고객지원/심사 대응에서 혼선이 생긴다. | 앱 내 버전 표기와 문의 템플릿이 빌드 버전 또는 단일 소스에서 나오도록 정리하고, Play Console 출시명/버전명과 동일한지 출시 전 확인한다. | true |
| P1 | 결제/스토어 등록 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:448-465`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionManageScreen.kt:73-77` | 구독 화면에는 자동 갱신, 약관/개인정보, 복원 링크가 있으나 가격, 청구 주기, 무료 체험/오퍼 조건은 런타임 상품 데이터와 콘솔 설정에 의존한다. 스토어 상품 정보와 앱 내 결제 전 고지가 불일치하면 결제 정책 위반 또는 환불/민원 리스크가 있다. | Play 구독 상품명, 기간, 가격, 무료 체험, 자동 갱신, 취소 경로가 앱 화면과 Play Console 상품 설정에서 동일하게 노출되는지 내부 테스트 트랙에서 확인한다. | true |
| P1 | 개인정보/계정 삭제 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:416-420`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:442-444`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:85-99` | 앱 내 계정 삭제는 사용자 문서와 `devices` 하위 문서, Firebase Auth 계정을 삭제한다. 수면 기록 등 다른 컬렉션이 추가되어 있거나 콘솔/정책 문서에 삭제 범위가 다르게 안내되면 Play 데이터 삭제 정책과 불일치할 수 있다. | Firestore 전체 데이터 모델 기준으로 삭제 대상과 보존 예외를 점검하고, 개인정보 처리방침 및 Play 데이터 삭제 안내의 표현을 앱 내 삭제 동작과 일치시킨다. | true |
| P1 | 정책/콘텐츠 등급 | `README.md:11-18`, `README.md:218-222`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:211-222` | 앱은 수면 습관, 수면 점수, 수면 부채, "건강한 잠" 표현을 사용한다. 저장소 기준 의료 진단/치료 기능은 확인되지 않지만 스토어 설명이 건강 효능을 과장하면 건강/의료 정책 또는 콘텐츠 등급 설문과 어긋날 수 있다. | 스토어 설명과 앱 내 문구를 습관 관리/사용 제한/수면 루틴 보조로 제한하고, 의학적 진단·치료·효능 보장처럼 읽히는 표현은 피한다. 콘텐츠 등급 설문에서 건강/의료 기능 여부를 실제 기능 기준으로 확인한다. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질/정책 | `play_release_readiness_report_ac2_app_content_policy.md:9-45`, `play_release_readiness_report_ac4_repository_unknowns.md` | 기존 리포트는 앱 콘텐츠 정책과 확인 불가 항목을 분리하고 있지만, Play Console 설문/스토어 등록 문구 변경 시 저장소 문서와 자동으로 동기화되는 장치는 확인되지 않는다. 향후 기능 추가 후 정책 문서가 뒤처질 수 있다. | 출시 후 릴리즈 체크리스트에 권한 선언, 데이터 보안, 광고 포함 여부, 구독 상품, 콘텐츠 등급, 개인정보 처리방침 URL 검토 항목을 추가한다. | true |
| P2 | 스토어 등록 | `README.md:236-249` | README는 외부 딥링크가 없다고 설명하지만 Android 시스템 이벤트, 서비스, 알람 수신기, 접근성 서비스는 앱 진입점으로 동작한다. 향후 딥링크나 알림 액션이 추가되면 스토어 등록/개인정보 설명 범위가 바뀔 수 있다. | 외부 진입점이 추가될 때마다 Play Console 앱 콘텐츠 설문과 개인정보 처리방침을 재검토하는 운영 기준을 둔다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 스토어 등록/정책 | Google Play Console 스토어 등록 자료 저장소 부재, `README.md:253` | 앱 이름, 짧은 설명, 전체 설명, 스크린샷, 개인정보 처리방침 URL, 앱 액세스, 광고 포함 여부, 데이터 보안, 콘텐츠 등급, 타겟층, 권한 선언의 실제 입력값은 저장소만으로 확인할 수 없다. 입력값이 실제 코드/SDK와 다르면 심사 반려 또는 출시 후 정책 제재가 발생할 수 있다. | Google Play Console 등록 자료는 본 점검의 직접 대상에서 제외하고, 별도 체크에서 본 리포트의 저장소 근거와 대조한다. 특히 접근성, 오버레이, 알람, FGS, 광고, 구독, 개인정보 항목은 스크린샷과 설명까지 함께 확인한다. | false |
| P0 | 개인정보 | Notion 약관/개인정보 문서 외부 URL, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:21-22`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:380-389` | 약관/개인정보 처리방침은 외부 Notion URL로 연결되며 저장소에 문서 본문이 없다. 문서가 공개 접근 가능한지, 최신 SDK/데이터 항목을 포함하는지, Play Console 개인정보 처리방침 URL과 동일한지는 저장소만으로 검증할 수 없다. | 출시 전 비로그인 브라우저에서 URL 접근성을 확인하고, Firebase/AdMob/RevenueCat/Google/Kakao/FCM/수면 데이터/계정 삭제 내용을 포함하도록 문서를 검토한다. | true |
| P0 | 광고 SDK | AdMob Console 상태 저장소 부재, `admob.properties:5`, `admob.properties:8`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29-30` | 운영 AdMob ID 값은 파일에 있으나 AdMob 앱 승인, 광고 단위 활성화, 정책 센터 이슈, app-ads.txt 필요 여부, Play Console 광고 포함 여부는 저장소만으로 확인할 수 없다. | AdMob 콘솔과 Play Console에서 운영 광고 ID 상태를 확인하고, 테스트 광고 ID가 실제 운영 ID로 전환되었는지 release 빌드에서 검증한다. | true |
| P0 | 결제 | RevenueCat/Play Console 상품 설정 저장소 부재, `composeApp/build.gradle.kts:110-113`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72-82` | RevenueCat 운영 API 키, offering, entitlement, Play 구독 상품/기본 요금제/오퍼, 테스트 상품 제거 여부는 저장소만으로 확인할 수 없다. | RevenueCat 구독관리 테스트 ID와 샌드박스 설정이 실제 운영 ID/운영 상품으로 전환되었는지 콘솔에서 확인하고, Play 내부 테스트로 구매/복원/취소 후 재진입을 검증한다. | true |
| P1 | 스토어 등록/콘텐츠 등급 | Play Console 콘텐츠 등급/타겟층 설문 저장소 부재, `README.md:11-18`, `README.md:236-249` | 저장소 기준 아동 대상, 사용자 생성 콘텐츠, 위치 정보, 뉴스, 금융, 도박 기능은 확인되지 않는다. 그러나 Play Console의 타겟층/콘텐츠 등급 설문 입력값은 저장소에 없어 실제 코드와 일치하는지 확인할 수 없다. | Play Console 콘텐츠 등급과 타겟층 설문을 실제 앱 기능 기준으로 별도 확인한다. 수면/건강 관련 표현은 의료 기능으로 오해되지 않는지 스토어 문구와 함께 검토한다. | false |

## 요약 판단

- 문서/스토어 등록 기준의 주요 출시 차단 리스크는 접근성 서비스 목적 공개, 특수 권한 선언, 개인정보/데이터 보안, AdMob 운영 ID 전환, RevenueCat 운영 구독 설정 전환이다.
- Google Play Console 스토어 등록 자료는 저장소에 없으므로 직접 점검하지 않았고, 저장소 근거가 있는 항목과 콘솔에서만 확인 가능한 항목을 분리했다.
- 광고 및 RevenueCat 구독관리 테스트 ID를 실제 운영 ID로 변경해야 하는 항목은 각각 P0 필수 조치로 포함했다.
