# Google Play 출시 전 점검 리포트 - AC3 입력, 검증, 오류/빈/로딩 상태

점검 범위: 현재 저장소의 Android/KMP 앱 코드와 기존 문서 기준. Google Play Console, Firebase Console, RevenueCat/AdMob 운영 콘솔, 실제 단말 실행 결과는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | UX/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:475-483`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:151-160`, `:164-173`, `play_release_readiness_report_ac1_ux_user_journey.md:31` | 설정 화면의 오류 다이얼로그가 `state.error != null`일 때 표시되지만 `onDismissRequest`와 확인 버튼 `onClick`이 비어 있다. 프로필 수정 또는 기기 이름 변경 실패 후 사용자가 오류를 닫을 수 없어 설정 화면 이용이 막힐 수 있다. | 출시 전 `DismissError` 또는 기존 `DismissDialog`와 별도 오류 초기화 Intent를 추가하고, 확인/외부 닫기 모두 `error = null`로 수렴시킨다. 실패 후 입력 다이얼로그 재진입, 재시도, 취소가 가능한지 실제 플로우로 확인한다. | true |
| P0 | 안정성/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepContract.kt:13-24`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:65-105`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/global/GlobalSleepViewModel.kt:451-511` | 전역 수면 상태에는 `error`와 `isToggleLoading`이 있으나 홈 화면은 `isToggleLoading` 오버레이만 표시하고 `globalState.error`를 사용자에게 노출하지 않는다. 수면 시작/중지 실패가 발생하면 사용자는 로딩 종료 이후 실패 이유나 복구 조치를 알 수 없다. | 수면 토글 실패를 `GlobalSleepContract.Effect.ShowSnackbar` 또는 화면 오류 배너로 연결한다. 로딩 오버레이는 장시간 실패/타임아웃에도 빠져나오도록 하고, 권한/네트워크/저장 실패별 사용자 메시지를 분리한다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | UX/인증 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/EmailLoginScreen.kt:34-47`, `:98-121`, `:155-176`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/EmailSignupScreen.kt:34-48`, `:102-156`, `:160-234`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/AuthViewModel.kt:50-90` | 이메일 로그인/회원가입은 이메일 형식, 비밀번호 길이, 비밀번호 확인, 약관 동의를 버튼 활성 조건으로 검증한다. 다만 ViewModel은 공백 입력만 검증하고 이메일 형식, 비밀번호 길이, 약관 동의 검증은 UI 로컬 상태에 의존한다. 다른 진입점이나 테스트에서 Intent가 직접 호출되면 잘못된 입력이 use case까지 전달될 수 있다. | 인증 입력 규칙을 ViewModel/도메인 레벨에도 중복 적용하고, UI는 같은 규칙을 시각 피드백으로만 사용한다. 이메일 정규식, 비밀번호 최소 길이, 이름 trim/최대 길이, 약관 동의 누락 메시지를 테스트한다. | true |
| P1 | UX/인증 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/EmailSignupScreen.kt:160-207`, `:221-224` | 회원가입 약관/개인정보 동의는 UI 로컬 `agreedToTerms`로만 관리된다. 동의하지 않으면 버튼이 비활성화되지만 왜 저장이 불가능한지 별도 오류 문구가 없어 사용자가 입력 누락 원인을 놓칠 수 있다. | 버튼 비활성 상태만 두지 말고 약관 체크박스 주변에 미동의 상태 안내 또는 제출 시 스낵바를 제공한다. 동의 시각 상태와 실제 제출 가능 상태가 TalkBack/키보드 조작에서도 명확한지 확인한다. | true |
| P1 | UX/설정 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:154-195`, `:198-230`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsViewModel.kt:144-173` | 기기 이름 변경은 공백, 중복, 기존 이름, 20자 제한을 UI에서 막고, 프로필 이름은 공백/기존 이름/20자 제한을 막는다. 그러나 프로필 이름의 20자 제한은 초과 입력 자체를 잘라 사용자에게 제한 이유를 알려주지 않고, ViewModel은 빈 값 외 길이/중복 규칙을 보장하지 않는다. | 이름 검증 규칙을 ViewModel에도 둔다. 입력란에는 현재 글자 수와 제한 이유를 `supportingText`로 제공하고, 저장 실패 시 닫히지 않는 전역 오류 대신 해당 입력 다이얼로그에서 재시도 가능하게 처리한다. | true |
| P1 | 결제/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:116-170`, `:487-519`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModel.kt:42-59`, `:61-80`, `:83-101` | 구독 화면은 상품 로딩, 빈 상품, 오류, 구매 중 로딩, 오류 다이얼로그를 갖추고 있다. 다만 `state.error`가 있으면 상품 영역의 `ErrorState`와 전역 AlertDialog가 동시에 표시될 수 있어 같은 오류가 중복 노출된다. | 로드 오류는 인라인 오류/빈 상태로, 구매/복원 오류는 다이얼로그 또는 스낵바로 역할을 분리한다. `packages.isEmpty() && error != null`일 때 전역 AlertDialog를 띄우지 않도록 오류 타입을 구분한다. | true |
| P1 | UX/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsContract.kt:9-38`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsViewModel.kt:79-137`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/stats/StatsScreen.kt:137-146`, `:203-219`, `:623-655` | 통계 화면은 로딩 오버레이와 빈 기록 카드를 제공하고 ViewModel에는 `Retry` Intent가 있다. 그러나 `StatsScreen`에서 `state.error`를 렌더링하는 근거가 확인되지 않아 조회 실패 시 사용자가 오류/재시도 버튼을 보지 못할 수 있다. | 통계 화면 최상단 또는 콘텐츠 영역에 오류 배너와 `StatsIntent.Retry` 버튼을 연결한다. 로딩 중 기존 콘텐츠 위 오버레이를 유지할지, 첫 로딩은 별도 스켈레톤/빈 화면으로 보여줄지 정책을 정한다. | true |
| P1 | UX/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/DeviceListContent.kt:29-160`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:140-151`, `:402-408` | 연결된 기기 관리 바텀시트는 `devices.forEach`만 수행하고 빈 목록 안내가 없다. Firestore 권한/동기화 실패 또는 새 계정 상태에서 목록이 비어 있으면 사용자는 로딩 중인지, 등록된 기기가 없는지, 오류인지 구분하기 어렵다. | `DeviceListContent`에 빈 상태 문구와 필요 시 현재 기기 등록/새로고침 안내를 추가한다. 기기 목록 Flow 실패와 정상 빈 목록을 상태 모델에서 분리한다. | true |
| P1 | UX/알람 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmContract.kt:16-26`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmViewModel.kt:76-150`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmScreen.kt:61-86`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/WheelTimePicker.kt:63-89` | 알람 시간은 휠 피커로 0-23시/0-59분 범위만 선택 가능해 기본 입력 범위는 안전하다. 하지만 저장소 저장 및 스케줄링 실패에 대한 `isLoading`/`error` 상태가 없고, 취침/기상 시간이 같거나 너무 짧은 수면 시간이 되는 경우의 사용자 검증이 없다. | 알람 State에 저장 중/저장 실패 상태를 추가하고, 저장 실패 시 스낵바와 재시도 경로를 제공한다. 취침-기상 간격 최소/최대 허용 범위 정책을 정해 0분 또는 비현실적 루틴을 저장 전에 안내한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | UX/홈 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:296-345`, `:670-723`, `:725-766` | 홈 화면은 배터리 최적화, 오버레이 권한, 접근성 권한, 서비스 중단 등 보호 상태 복구 카드를 제공한다. 다만 정상 빈 기기 상태와 원격 동기화 실패 상태를 구분하는 별도 UI는 확인되지 않는다. | 홈 동기화 허브에 등록 기기 없음, 동기화 지연, 권한/네트워크 실패 상태를 분리해 표시한다. 출시 후 운영 로그와 고객 문의를 기준으로 가장 빈번한 복구 상태를 우선 개선한다. | true |
| P2 | 품질/테스트 | `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt`, 저장소 검색 결과 인증/설정/알람 UI 상태 테스트 제한적 | 일부 ViewModel 테스트는 있으나 입력 검증, 오류 다이얼로그 닫기, 빈 상태, 로딩 중 중복 클릭 방지, 알람 저장 실패 같은 UI 상태 회귀 테스트 범위가 제한적으로 보인다. | 출시 후 회귀 테스트에 인증 입력 조합, 설정 오류 다이얼로그 닫기, 구독 상품 빈 목록/로드 실패/구매 실패, 통계 오류 재시도, 기기 목록 빈 상태, 알람 저장 실패를 추가한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 품질/스토어 등록 | Google Play Console Pre-launch report, 실제 Android 단말 결과 | 저장소만으로는 로딩 오버레이가 실제 네트워크 지연에서 과도하게 오래 유지되는지, 오류 다이얼로그/스낵바가 화면 크기와 폰트 배율에서 정상 노출되는지, Pre-launch bot이 입력/빈 상태를 어떻게 탐색하는지 확인할 수 없다. | 내부 테스트 트랙 AAB로 Pre-launch report를 확인하고, 저속 네트워크/오프라인/권한 거부/구매 상품 미노출 시나리오를 실제 단말에서 수행한다. | false |
| P1 | 결제 | RevenueCat 대시보드, Play Console 구독 상품 | 구독 상품 빈 상태와 오류 UI는 코드로 확인되지만, 운영 상품이 실제로 반환되는지와 RevenueCat 상품/entitlement 설정이 맞는지는 저장소만으로 확인할 수 없다. 테스트 상품 또는 테스트 API 키가 남아 있으면 빈 상태/오류 화면이 운영 사용자에게 노출될 수 있다. | RevenueCat 운영 API 키, `premium` entitlement, Play Console 구독 상품 연결을 별도 검증한다. 내부 테스트에서 상품 목록 로딩, 구매, 복원, 취소 후 상태 갱신까지 확인한다. | false |
| P1 | 광고 SDK | AdMob Console, release 빌드 산출물 검증 | 본 Sub-AC의 직접 점검 대상은 입력/상태 처리지만, 광고 로드 실패나 no-fill 상태가 사용자 흐름에 미치는 영향은 운영 광고 단위 상태와 release 빌드 설정 없이는 확정할 수 없다. 기존 점검 문서에서 테스트 광고 ID 운영 ID 전환 필요가 확인되어 있다. | 출시 전 AdMob 테스트 광고 단위 ID가 운영 광고 단위 ID로 전환되었는지 산출물과 콘솔에서 확인하고, 광고 로드 실패 시 앱 진입/수면 시작/구독 흐름이 막히지 않는지 점검한다. | false |

## 결론

인증, 구독, 통계 일부 화면은 로딩/오류/빈 상태 모델을 갖추고 있으나, 설정 오류 다이얼로그 닫기 불가와 홈 수면 토글 오류 미노출은 출시 전 반드시 해소해야 할 UX 리스크다. 출시 전에는 입력 규칙을 UI 로컬 상태에만 두지 말고 ViewModel/도메인 검증으로 수렴시키고, 정상 빈 상태와 실패 상태를 구분해 사용자가 재시도 또는 복구 행동을 바로 알 수 있게 정리해야 한다.
