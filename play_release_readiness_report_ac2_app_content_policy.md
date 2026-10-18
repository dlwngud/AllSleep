# Google Play 출시 전 종합 점검 리포트 - AC2 앱 콘텐츠 정책

## 점검 범위

- 기준: 현재 저장소의 앱 코드, 설정, 문서
- 제외: Google Play Console 스토어 등록 자료, Play Console 앱 콘텐츠 설문 입력값, AdMob/RevenueCat/Firebase 콘솔 상태의 직접 확인
- 관점: Google Play 앱 콘텐츠 정책 항목별 적용 가능 리스크, 저장소 내 근거, 출시 전 권장 조치

## 출시 차단

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:88`, `composeApp/src/androidMain/AndroidManifest.xml:91`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:3`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:8`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/AppSupervisorService.kt:27` | 접근성 서비스가 `TYPE_WINDOW_STATE_CHANGED` 이벤트로 다른 앱 패키지 전환을 감지하고, `android:isAccessibilityTool="false"`로 선언되어 있다. 장애 보조 목적이 아닌 앱 차단 목적의 접근성 API 사용은 Google Play 심사에서 민감하게 검토될 수 있으며, 앱 콘텐츠/권한 선언과 공개 고지가 불충분하면 반려될 수 있다. | Play Console 권한 선언에서 접근성 서비스의 목적을 "수면 모드 중 사용자가 설정한 앱 차단/잠금 유지"로 일관되게 설명한다. 앱 내 명시적 고지와 개인정보 처리방침에 수집하지 않는 데이터, 감지하는 이벤트, 사용자가 끌 수 있는 방법을 동일하게 명시한다. 정책상 허용 가능성은 출시 전 별도 검토한다. | true |
| P0 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:22`, `composeApp/src/androidMain/AndroidManifest.xml:78`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/service/SleepLockService.kt:61`, `README.md:16` | `SYSTEM_ALERT_WINDOW` 오버레이로 수면 잠금 화면을 표시한다. 다른 앱 위 표시 권한은 사용자가 이해하지 못하면 기기 사용 방해, 오해 유발, 정책 검토 리스크가 있다. | Play Console 권한 선언과 앱 내 권한 안내에서 오버레이가 수면 모드 중에만 표시되고 종료 방법이 무엇인지 명확히 설명한다. 오버레이가 광고, 피싱, 시스템 UI 모방에 사용되지 않음을 개인정보 처리방침/도움말에도 맞춰 둔다. | true |
| P0 | 광고 SDK | `composeApp/build.gradle.kts:158`, `composeApp/build.gradle.kts:163`, `admob.properties:5`, `admob.properties:8`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:29`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:30` | release용 AdMob 앱 ID/광고 단위 ID는 설정되어 있으나 실제 광고 로딩 코드는 테스트 광고 단위 ID를 하드코딩한다. 운영 빌드에 테스트 ID가 남으면 광고 정책/수익화 검증 실패 및 출시 후 광고 미노출 리스크가 있다. | 출시 전 앱 오픈 광고 로딩 경로가 `BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID`의 release 운영 ID를 사용하도록 확인한다. AdMob 콘솔에서 앱 승인, 광고 단위 활성화, 정책 센터 이슈, app-ads.txt 필요 여부를 별도 확인한다. | true |
| P0 | 결제 | `composeApp/build.gradle.kts:103`, `composeApp/build.gradle.kts:110`, `composeApp/build.gradle.kts:113`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:72`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:77`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:78`, `README.md:21` | RevenueCat API 키가 `local.properties`에서 주입되고 debug에서는 test key를 우선한다. 운영 빌드에 테스트 API 키, 샌드박스 상품, 테스트 entitlement가 남으면 실제 구독 구매/복원 및 Play 결제 정책 검증에 실패할 수 있다. | 출시 후보 release 빌드에서 `REVENUECAT_API_KEY`가 운영 Android API 키인지, `REVENUECAT_TEST_API_KEY`가 사용되지 않는지, RevenueCat entitlement와 Play 구독 상품이 운영 상품으로 연결되어 있는지 확인한다. 내부/비공개 트랙에서 실제 Play Billing 플로우로 구매, 복원, 취소 반영을 검증한다. | true |
| P0 | 개인정보 | `README.md:82`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:108`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:124`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/SleepSyncRepositoryImpl.kt:128`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:21`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthFooter.kt:22` | Firebase Auth, Firestore, FCM, AdMob, RevenueCat를 사용하며 이메일, 이름, 수면 상태/시간, 기기명, FCM 토큰, 광고 관련 SDK 데이터, 구매/구독 정보가 정책상 데이터 안전 설문 대상이 될 수 있다. 공개 개인정보 처리방침 링크는 있으나 저장소만으로 문서 내용과 Play Console 설문 일치 여부를 확정할 수 없다. | Play Console 데이터 보안 섹션에 실제 수집/공유/처리 목적을 SDK별로 반영한다. 개인정보 처리방침에는 인증 정보, 수면 기록, 기기 정보, FCM 토큰, 광고 SDK, 구매/구독 정보, 보관/삭제 기준을 명시한다. Notion 링크는 비로그인 공개 접근 가능 여부를 별도 확인한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 권한/정책 | `composeApp/src/androidMain/AndroidManifest.xml:8`, `composeApp/src/androidMain/AndroidManifest.xml:9`, `composeApp/src/androidMain/AndroidManifest.xml:11`, `composeApp/src/androidMain/AndroidManifest.xml:25`, `composeApp/src/androidMain/AndroidManifest.xml:26`, `composeApp/src/androidMain/AndroidManifest.xml:83`, `composeApp/src/androidMain/AndroidManifest.xml:84` | 정확한 알람, 전체 화면 인텐트, 특수 용도 포그라운드 서비스는 앱 콘텐츠/권한 선언에서 사용 목적을 요구할 가능성이 높다. 수면 알람과 잠금 유지 목적이 콘솔 선언, 앱 설명, 앱 내 안내와 불일치하면 심사 지연 또는 반려 위험이 있다. | Play Console 권한 선언에 수면 루틴 알림/기상 알람/수면 잠금 유지라는 핵심 기능 목적을 정확히 적는다. `specialUse` subtype `screen_lock_overlay`가 실제 동작과 맞는지 내부 테스트에서 확인한다. | true |
| P1 | 광고 SDK | `composeApp/src/androidMain/AndroidManifest.xml:37`, `composeApp/src/androidMain/AndroidManifest.xml:43`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:43`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:126`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt:155` | 앱 오픈 광고가 콜드 스타트에서 최대 3초 대기 후 노출될 수 있다. 광고 표시 타이밍이 앱 사용을 과도하게 방해하거나 수면 잠금/구독 화면과 혼동되면 UX 및 광고 정책 리스크가 있다. | 프리미엄 사용자는 광고가 제외되는지, 광고가 앱 콘텐츠와 구분되는지, 첫 실행/권한 요청/결제 직전 화면에 부적절하게 끼어들지 않는지 release 빌드로 확인한다. | true |
| P1 | 결제 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:448`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:463`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:465`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionManageScreen.kt:76`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionManageScreen.kt:390` | 구독 자동 갱신, 개인정보처리방침, 구독 복원, Play 구독 관리 링크가 앱 내에 존재한다. 다만 상품명, 가격, 청구 주기, 무료 체험/프로모션 여부는 런타임/콘솔 데이터에 의존해 저장소만으로 최종 문구 적합성을 확정할 수 없다. | Play 구독 상품 정보가 앱 화면에 정확히 표시되는지, 결제 전 가격/주기/자동 갱신 고지가 충분한지, 복원 및 구독 관리 링크가 동작하는지 내부 테스트 트랙에서 검증한다. | true |
| P1 | 개인정보 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:85`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:89`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:96`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/data/repository/AuthRepositoryImpl.kt:99`, `README.md:226` | 앱 내 회원 탈퇴 흐름은 Firestore `devices` 하위 문서, 사용자 문서, Firebase Auth 계정을 삭제한다. 수면 기록 등 다른 컬렉션이 있다면 삭제 범위가 불완전할 수 있고, Play 데이터 삭제 정책 설명과 불일치할 수 있다. | Firestore 전체 데이터 모델 기준으로 회원 탈퇴 시 삭제되는 데이터와 유지되는 데이터를 점검한다. 개인정보 처리방침과 Play 데이터 삭제 안내에 동일한 삭제 범위와 예외를 적는다. | true |
| P1 | 보안 | `composeApp/src/androidMain/AndroidManifest.xml:63`, `composeApp/src/androidMain/AndroidManifest.xml:65`, `composeApp/src/androidMain/AndroidManifest.xml:68`, `composeApp/src/androidMain/AndroidManifest.xml:72`, `composeApp/build.gradle.kts:100`, `composeApp/build.gradle.kts:101` | Kakao OAuth 콜백 Activity가 exported로 열려 있고 scheme이 manifest placeholder로 주입된다. OAuth 리다이렉트 URI와 앱 서명 설정이 콘솔과 불일치하면 로그인 실패 또는 계정 정책 리스크가 있다. | Kakao 개발자 콘솔, Firebase Auth, Google OAuth 클라이언트에 release 서명 SHA-1/SHA-256과 redirect scheme이 등록되어 있는지 확인한다. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 품질 | `README.md:171`, `README.md:175`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt` | 테스트 전략과 일부 ViewModel 테스트는 있으나 접근성/오버레이/광고/결제/권한 선언에 대한 자동 검증은 저장소에서 확인되지 않는다. 정책 민감 기능은 회귀가 발생해도 출시 직전 수동 검증에 의존할 수 있다. | 출시 후에는 권한 안내 문구, 접근성 공개 다이얼로그, 광고 ID 선택, 구독 복원 흐름을 대상으로 스모크 테스트 또는 체크리스트를 저장소에 추가한다. | true |
| P2 | UX | `README.md:24`, `README.md:28`, `design-previews/` | README 스크린샷은 준비 중이며 저장소에는 디자인 미리보기 이미지가 별도 폴더에 있다. Play 등록 스크린샷 직접 점검 대상은 아니지만 앱 콘텐츠 설명과 실제 화면이 불일치하면 사용자 오해 및 심사 커뮤니케이션 리스크가 있다. | 운영 빌드 기준 실제 화면 캡처와 앱 설명을 맞추고, 권한/구독/광고가 노출되는 핵심 화면은 내부 문서에 보관한다. | true |
| P2 | 정책 | `README.md:236`, `README.md:238` | README상 외부 딥링크는 제공하지 않는다. 현재 저장소 기준 사용자 생성 콘텐츠, 위치 정보, 건강/의료 주장, 금융, 도박, 뉴스, 아동 대상 기능은 확인되지 않는다. 향후 기능 추가 시 앱 콘텐츠 설문 범위가 바뀔 수 있다. | 기능 추가 시 Play Console 앱 콘텐츠 설문을 다시 검토하는 릴리즈 체크 항목을 유지한다. 특히 건강/수면 점수 표현은 의료 효능처럼 보이지 않게 관리한다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치/근거 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 스토어 등록/정책 | 저장소 내 Google Play Console 앱 콘텐츠 설문 자료 부재, `README.md:253` | Play Console의 앱 콘텐츠 정책 설문, 데이터 보안, 광고 ID, 권한 선언, 타겟층/콘텐츠 등급, 뉴스/건강/위치/아동 대상 여부 입력값은 저장소만으로 확인할 수 없다. 입력값이 실제 앱 코드와 다르면 심사 반려 또는 사후 제재 위험이 있다. | Play Console에서 앱 콘텐츠 각 섹션을 본 리포트의 권한, 개인정보, 광고, 결제, 접근성 근거와 대조한다. Google Play Console 등록 자료는 직접 점검 대상에서 제외하고 별도 확인 결과로 관리한다. | false |
| P0 | 광고 SDK | AdMob Console 상태는 저장소에 없음, `admob.properties:5`, `admob.properties:8` | 운영 AdMob 앱/광고 단위 ID가 설정 파일에는 있으나, AdMob 앱 승인 상태, 광고 단위 활성화, 정책 센터 이슈, app-ads.txt 필요 여부는 저장소만으로 확인할 수 없다. | 출시 전 AdMob 콘솔에서 운영 ID 상태를 확인하고, 테스트 광고 ID가 실제 운영 ID로 전환되었는지 빌드 산출물 기준으로 검증한다. | true |
| P0 | 결제 | RevenueCat/Play Console 상품 설정은 저장소에 없음, `composeApp/build.gradle.kts:110`, `composeApp/build.gradle.kts:111`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/AllSleepApp.kt:77` | RevenueCat 운영 API 키, entitlement, Play 구독 상품/기본 요금제/오퍼 연결, 테스트 상품 제거 여부는 저장소만으로 확인할 수 없다. | RevenueCat 구독관리 테스트 ID와 샌드박스 설정이 운영 ID/운영 상품으로 전환되었는지 콘솔에서 확인하고, release 빌드 내부 테스트로 구매와 복원을 검증한다. | true |
| P1 | 개인정보/보안 | Firebase Console, Firestore Rules, Functions 배포 상태는 저장소만으로 확인 불가 | 앱은 Firebase Auth, Firestore, FCM, Functions를 사용하지만 운영 Rules와 Functions 권한, Auth 제공업체 상태, FCM 토큰 처리 정책은 콘솔 상태에 좌우된다. | Firebase Console에서 운영 프로젝트, Rules, Functions 배포 버전, Auth 제공업체, release SHA 등록 상태를 점검하고 개인정보 처리방침과 데이터 안전 설문에 반영한다. | false |

## 요약 판단

- Google Play 앱 콘텐츠 정책에서 가장 큰 출시 전 리스크는 접근성 서비스, 오버레이, 특수 권한, 개인정보/데이터 안전, 광고 SDK, RevenueCat 구독 설정이다.
- 저장소 근거상 광고는 release ID 설정이 존재하지만 실제 광고 로딩 코드가 테스트 광고 단위 ID를 사용한다. 운영 ID 전환 확인은 필수다.
- RevenueCat은 debug/test key와 release key 분기가 존재한다. 운영 API 키와 운영 구독 상품/entitlement 전환 확인은 필수다.
- Play Console 등록 자료는 직접 점검 범위에서 제외했으며, 저장소 근거가 있는 항목과 콘솔에서만 확인 가능한 항목을 분리했다.
