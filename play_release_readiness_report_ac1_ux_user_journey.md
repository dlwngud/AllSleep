# Google Play 출시 전 점검 리포트 - Sub-AC 1 UX 사용자 여정

작성일: 2026-05-09

범위: 현재 저장소의 앱 코드, 설정, 문서에서 확인 가능한 화면 흐름과 주요 사용자 여정만 점검했다. Google Play Console 스토어 등록 자료는 직접 점검하지 않았다.

## 점검 요약

- 주요 흐름은 `온보딩 -> 로그인/회원가입 -> 홈 -> 수면 모드 시작 -> 통계/알람/설정/구독`으로 구성되어 있다.
- 첫 실행 분기는 온보딩 완료 여부와 로그인 상태로 결정되며, 메인 화면은 홈/통계/알람/설정 4개 하단 탭으로 이동한다.
- 출시 차단 수준의 UX 결함은 코드만으로 확정하지 못했지만, 권한 온보딩 자동 이동 조건, 비로그인 이메일 인증 진입, 오류 다이얼로그 닫기 동작은 출시 전 수동 QA가 필요하다.
- 광고 및 RevenueCat 테스트 ID 운영 ID 전환은 Sub-AC 1의 UX 범위 밖이지만, 전체 출시 리포트의 필수 확인 항목으로 별도 포함해야 한다.

## 출시 차단

### 1. 비로그인 온보딩에서 이메일 로그인/회원가입으로 진입하면 온보딩 완료 화면으로 복귀하지 못할 가능성

- priority: P0
- category: UX, 안정성
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:160-171`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/AppNavigation.kt:84-109`
- evidence_available: true
- risk: 온보딩 8단계의 `GlobalLoginScreen`에서 이메일 로그인을 선택하면 전역 `auth/email_login` 라우트로 이동한다. 이메일 로그인/회원가입 성공 시 `AppNavigation`은 `Screen.Home.route`로 이동하도록 되어 있어, 온보딩의 설정 요약/시작하기 화면을 거치지 않을 수 있다. 사용자는 방금 설정한 취침/기상 시간이 저장되었는지 확인하지 못하고 홈으로 이동할 수 있다.
- recommendation: 온보딩 중 이메일 인증으로 진입한 경우 성공 후 `onboarding/auth` 또는 완료 화면으로 되돌리는 별도 라우트/인자를 두고, 온보딩 완료 저장과 홈 이동 순서를 명확히 검증한다.

### 2. 설정 화면 오류 다이얼로그의 확인 버튼이 상태를 해제하지 않음

- priority: P0
- category: UX, 품질
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:475-482`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:151-173`
- evidence_available: true
- risk: 프로필 수정 또는 기기 이름 변경 실패 시 `state.error`가 표시되지만 확인 버튼의 `onClick`이 비어 있다. 오류가 발생하면 사용자가 다이얼로그를 닫지 못해 설정 화면 사용이 막힐 수 있다.
- recommendation: 출시 전 `DismissError` 또는 기존 `DismissDialog`에 오류 초기화 동작을 연결하고, 프로필 수정/기기 이름 변경 실패 시 다이얼로그가 닫히는지 수동 QA한다.

## 출시 전 권장

### 3. 온보딩 권한 단계가 시스템 설정 복귀 후 자동 진행되므로 사용자가 진행 상태를 놓칠 수 있음

- priority: P1
- category: UX, 권한
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:53-57`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:100-104`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:123-127`
- evidence_available: true
- risk: 알림/알람, 배터리, 접근성 단계는 권한 상태가 허용으로 바뀌면 `LaunchedEffect`로 다음 화면에 자동 이동한다. 시스템 설정에서 돌아온 직후 화면이 바뀌면 사용자가 어떤 권한이 완료되었는지 인지하기 어렵고, 일부 제조사 설정 화면에서는 상태 반영 지연으로 진행이 끊길 수 있다.
- recommendation: 자동 이동 전에 짧은 완료 상태 또는 명시적 "다음" 액션을 제공하고, 삼성/픽셀 등 주요 Android 환경에서 설정 복귀 후 상태 반영을 확인한다.

### 4. 온보딩 필수 권한 단계에 우회 또는 복구 경로가 제한적임

- priority: P1
- category: UX, 권한
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingPermissionStep.kt:119-151`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:60-75`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:84-93`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/OnboardingGraph.kt:107-116`
- evidence_available: true
- risk: 접근성 단계만 "다음에 하기"가 있고, 알림/알람/오버레이/배터리 단계는 하단 버튼이 허용 액션에 고정되어 있다. 권한 설정 실패, 시스템 설정 취소, 제조사별 메뉴 차이 상황에서 신규 사용자가 온보딩을 이탈할 수 있다.
- recommendation: 각 필수 단계에 실패/거부 후 재시도 안내, 고객지원/설정 재열기, 또는 제한 기능으로 시작하는 명확한 경로를 제공할지 출시 전 결정한다.

### 5. 수면 모드 시작 버튼은 드래그 전용이라 접근성과 발견성이 낮음

- priority: P1
- category: UX, 품질
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:361-377`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:582-652`
- evidence_available: true
- risk: 홈의 핵심 CTA가 `detectHorizontalDragGestures` 기반 "밀어서 수면 모드 켜기"로 구현되어 있다. TalkBack, 한 손 조작, 터치 민감도 이슈가 있는 사용자에게 시작 액션이 어렵거나 실패할 수 있다.
- recommendation: 드래그 CTA 외에 접근성 액션 또는 길게 누르기/확인 버튼 대안을 제공하고, 수면 모드 시작 성공/실패를 명확한 상태 텍스트와 햅틱/스낵바로 피드백한다.

### 6. 홈 권한 실패 메시지가 실제 차단 조건보다 단정적으로 표현됨

- priority: P1
- category: UX, 권한
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:242-254`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:271-283`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:361-375`
- evidence_available: true
- risk: 알림 권한 dismiss 시 "수면 잠금을 사용할 수 없습니다"라고 안내하지만 실제 수면 잠금의 필수 조건은 알림, 배터리, 오버레이, 접근성으로 분리되어 있다. 사용자는 어떤 설정이 반드시 필요한지 혼동할 수 있다.
- recommendation: 권한별로 "알림 리포트", "실시간 잠금 유지", "화면 위 잠금", "앱 이탈 방지"처럼 기능 영향 범위를 구분해 안내한다.

### 7. 통계 탭의 비프리미엄 잠금 화면이 기본 통계 상태와 상호작용 가능성을 가림

- priority: P1
- category: UX, 결제
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsScreen.kt:107-160`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/PremiumOverlay.kt:31-109`
- evidence_available: true
- risk: 비프리미엄 사용자는 통계 화면 전체가 blur 처리되고 프리미엄 오버레이만 보인다. 무료 사용자가 어떤 기록이 쌓였는지, 프리미엄 전환 시 무엇을 얻는지 실제 데이터 맥락을 확인하기 어렵다.
- recommendation: 무료 사용자에게 최소한의 최근 기록/빈 상태/샘플 차트를 노출하고, 프리미엄 기능만 명확히 잠그는 방식으로 전환 유도 메시지를 개선한다.

### 8. 구독 구매 화면 문구와 가격/혜택의 법적·사용자 기대 정보가 충분한지 확인 필요

- priority: P1
- category: UX, 결제
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:136-153`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:403-466`
- evidence_available: true
- risk: 자동 갱신 문구와 이용약관/개인정보/구독 복원 링크는 있으나, 무료 체험 후 결제 시점/기간/취소 경로가 패키지별로 충분히 명확한지는 런타임 상품 데이터에 의존한다. Play 결제 심사와 사용자 신뢰 관점에서 구매 직전 정보 부족 리스크가 있다.
- recommendation: RevenueCat 패키지에서 내려오는 가격/기간/무료 체험 여부를 실제 운영 상품 기준으로 확인하고, 구매 버튼 근처에 기간, 갱신, 취소 가능 안내가 모두 보이는지 QA한다.

## 출시 후 개선

### 9. 홈 화면의 핵심 카피가 일부 영어로 노출됨

- priority: P2
- category: UX
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:495-504`
- evidence_available: true
- risk: 한국어 앱 흐름 안에서 `Sync Hub Control Station`이 영어로 노출되어 브랜드 톤과 이해성이 떨어질 수 있다.
- recommendation: 출시 전 여유가 있으면 한국어 카피로 통일하고, 출시 후 A/B 또는 사용자 피드백으로 홈 핵심 메시지를 다듬는다.

### 10. 알람 탭은 평일/주말 고정 구조라 예외 일정 사용성이 제한됨

- priority: P2
- category: UX
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmScreen.kt:109-167`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmViewModel.kt:82-131`
- evidence_available: true
- risk: 현재 루틴은 평일/주말 탭과 취침/기상 토글로 단순화되어 있다. 교대 근무, 공휴일, 하루만 다른 알람 같은 예외 일정에는 맞지 않아 반복 사용성이 떨어질 수 있다.
- recommendation: 출시 후 사용자 요청을 보고 "오늘만 변경", 요일별 상세 설정, 휴일 예외 기능을 검토한다.

### 11. 하단 탭은 설정을 `ic_more` 아이콘으로 표시해 의미가 약할 수 있음

- priority: P2
- category: UX
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/BottomNavScaffold.kt:25-30`, `composeApp/src/commonMain/composeResources/drawable/ic_more.xml`
- evidence_available: true
- risk: 설정 탭 라벨은 "설정"이지만 아이콘은 more 계열이라 사용자 기대와 다를 수 있다.
- recommendation: 출시 후 아이콘 클릭률/문의가 있으면 설정 전용 아이콘으로 교체한다.

## 저장소만으로 확인 불가

### 12. 실제 Android 기기별 권한 설정 복귀 동작

- priority: P1
- category: UX, 권한
- location: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/PermissionRequester.android.kt`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/OverlayPermissionRequester.android.kt`, `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AccessibilityPermissionRequester.android.kt`
- evidence_available: true
- risk: 권한 요청 코드는 저장소에서 확인 가능하지만, 제조사별 설정 화면 경로와 복귀 후 상태 반영은 저장소만으로 검증할 수 없다.
- recommendation: 출시 전 Pixel, Samsung Galaxy, 저사양 Android 13/14/15 기기에서 온보딩 권한 4단계와 홈 수면 시작 권한 회복 플로우를 수동 QA한다.

### 13. 구독 상품/복원/구매 실패 메시지의 실제 사용자 경험

- priority: P1
- category: UX, 결제
- location: `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:57-72`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:116-153`
- evidence_available: true
- risk: 화면은 패키지 로드/오류/빈 상태/구매 진행을 처리하지만 실제 상품명, 가격, 무료 체험, 복원 결과는 RevenueCat 및 Play Billing 운영 설정에 따라 달라진다.
- recommendation: 전체 출시 점검에서 RevenueCat 테스트 상품 ID를 운영 상품 ID로 전환했는지 확인하고, Play 내부 테스트 트랙에서 구매/복원/취소 후 재진입을 검증한다.

### 14. 광고 노출 위치와 빈도에 따른 화면 흐름 영향

- priority: P1
- category: UX, 광고 SDK
- location: `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/AppOpenAdManagerImpl.kt`, `admob.properties`, `admob.properties.example`
- evidence_available: true
- risk: 저장소에 광고 매니저와 AdMob 설정 파일은 있으나, 실제 운영 광고 단위와 노출 빈도/시점은 런타임 설정 및 운영 ID에 따라 UX 영향이 달라진다.
- recommendation: 전체 출시 점검에서 AdMob 테스트 ID를 운영 광고 ID로 전환했는지 확인하고, 첫 실행/복귀/수면 시작 직전 광고가 핵심 여정을 방해하지 않는지 내부 테스트로 확인한다.

## 사용자 여정별 QA 체크리스트

- 첫 실행: 온보딩 1~8단계, 시스템 권한 설정 복귀, 이메일/소셜 로그인 성공 후 완료 화면 또는 홈 이동 경로 확인
- 재실행: 온보딩 완료+비로그인 상태에서 로그인 화면 진입, 로그인 성공 후 홈 진입 확인
- 수면 시작: 알림/배터리/오버레이/접근성 권한 각각 허용/거부 상태에서 홈 CTA 피드백 확인
- 수면 중 복구: 서비스 중단, 권한 해제, 배터리 최적화 상태에서 복구 카드와 액션 동작 확인
- 알람 설정: 평일/주말 탭, 취침/기상 시간 변경, 토글 후 스케줄 반영 확인
- 통계: 무료 사용자 잠금 화면, 프리미엄 사용자 요약/기록 탭, 기록 없음 상태 확인
- 설정: 프로필 수정, 기기 이름 변경, 기기 등록 해제, 로그아웃, 계정 삭제, 외부 링크 열림 확인
- 구독: 상품 로드 실패/빈 상태/구매/복원/뒤로가기 경로 확인

[AC_COMPLETE: 1]
