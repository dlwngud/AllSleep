# Google Play 출시 전 접근성 점검 리포트

## 점검 범위
- 대상: 현재 저장소의 Android/Compose UI 코드, 테마 설정, README 및 기존 출시 점검 문서
- 제외: Google Play Console 등록 자료, 실제 단말 TalkBack 실행 결과, Pre-launch report 결과
- 관점: 텍스트 가독성, 터치 타깃, 콘텐츠 설명, 색상 대비

## 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
|---|---|---|---|---|---|
| P0 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:583`, `:587-624`, `:625-641`, 기존 `play_release_readiness_report_ac1_ux_user_journey.md:54-61` | 홈의 핵심 수면 시작 CTA가 `detectHorizontalDragGestures` 기반 드래그 전용으로 구현되어 있다. TalkBack, 스위치 접근, 운동 제약, 한 손 조작 사용자는 "밀어서 수면 모드 켜기" 액션을 실행하지 못하거나 실패할 수 있으며, 앱 핵심 기능 접근성이 출시 심사/Pre-launch report에서 문제로 드러날 수 있다. | 출시 전 드래그 제스처 외에 `semantics` 접근성 액션, 일반 버튼 대체 경로 또는 길게 누르기/확인형 대체 CTA를 제공한다. 성공/실패 상태는 스낵바 또는 명시 텍스트로 읽히게 하고, TalkBack으로 수면 시작이 가능한지 실제 단말에서 검증한다. | true |

## 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
|---|---|---|---|---|---|
| P1 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:599-610`, `:539-550`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:299-311`, `:471-478`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:444-445` | 커스텀 `Row`/`Box`/`Text`에 직접 `clickable`을 붙인 항목이 많고, 역할(role), 상태 설명, 명확한 접근성 라벨이 보이지 않는다. TalkBack에서는 "버튼" 역할이나 현재 선택/상태가 불명확하게 읽힐 수 있다. 특히 구독 플랜 카드와 홈 동기화 허브는 시각적 상태가 중요하다. | 직접 만든 클릭 영역에는 `semantics { role = Role.Button }`, 선택형 항목에는 `selectable`/`selected` 상태, 상태 텍스트에는 `stateDescription`을 추가한다. `SettingsRowArrow`, `PlanCard`, `OrbitalSyncHub`, `FooterLink` 같은 공통/반복 컴포넌트부터 보강한다. | true |
| P1 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/WheelTimePicker.kt:158-189`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/TimePickerDialog.kt:46-53`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmScreen.kt:61-86` | 시간 선택이 `LazyColumn` 휠 스크롤 중심이며 숫자 항목에 의미 정보나 증감 대체 조작이 없다. TalkBack 사용자는 현재 선택된 시/분을 이해하거나 정확히 조정하기 어렵다. | 휠 피커에 현재 선택 시간의 접근성 라벨을 제공하고, 시/분 증감 버튼 또는 Material3 시간 선택기처럼 접근성 지원이 검증된 컴포넌트 대안을 제공한다. 최소한 선택 완료 전 현재 값이 "취침 시간 23시 30분"처럼 읽히도록 한다. | true |
| P1 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:610-615`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:210-214`, `:253-257`, `:360-367`, `:447-451`, `:471-477`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:607-609` | 보조 텍스트에 `Color.White.copy(alpha = 0.4f~0.6f)` 또는 `onSurface.copy(alpha = 0.3f~0.5f)`가 반복된다. 다크 배경에서는 일부 12~14sp 텍스트가 WCAG 일반 텍스트 대비 4.5:1을 안정적으로 만족하지 못할 가능성이 있다. | 출시 전 주요 화면의 실제 배경색 합성 기준으로 대비를 산출한다. 12~14sp 정보 텍스트, 링크, 비활성처럼 보이지만 읽어야 하는 텍스트는 4.5:1 이상으로 올리고, 비활성 상태는 색상만이 아니라 문구/상태로 구분한다. | true |
| P1 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingProblemScreen.kt:50`, `OnboardingSolutionScreen.kt:50`, `OnboardingCompleteScreen.kt:54`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:467`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:190-193` | 장식성 캐릭터 이미지에 `"Sleep Character"` 또는 `"Sleep Sync Center"`처럼 영어/일반 라벨이 붙어 있다. 스크린 리더 사용자는 기능과 무관한 이미지를 반복해서 듣거나, 실제 의미가 필요한 이미지는 맥락을 충분히 얻지 못할 수 있다. | 순수 장식 이미지는 `contentDescription = null`로 제외한다. 기능 의미가 있는 이미지는 한국어로 구체화한다. 예: 홈 중앙 허브가 기기 관리 진입점이면 "동기화된 기기 관리 열기"처럼 액션 중심 라벨을 제공한다. | true |
| P1 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/settings/SettingsScreen.kt:604`, `:619`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/alarm/AlarmScreen.kt:183`, `:221`, `:255`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/DeviceListContent.kt:93-104` | 이모지와 색상 점(dot)이 상태/분류를 전달한다. 스크린 리더에서 이모지 이름이 과하게 읽히거나, 동기화 상태 점처럼 색상만으로 의미를 구분하는 요소가 보조기술에 전달되지 않을 수 있다. | 상태는 텍스트로 이미 병기하되, 이모지/점에는 장식 처리 또는 부모 Row의 의미 라벨을 부여한다. "Ready"는 한국어 상태 문구로 통일하고, 색상만으로 On/Off/위험 상태를 구분하지 않도록 한다. | true |

## 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
|---|---|---|---|---|---|
| P2 | 품질/접근성 | `README.md:171-177`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/stats/StatsViewModelTest.kt`, `composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionViewModelTest.kt`, 저장소 검색 결과 `semantics`, `minimumInteractiveComponentSize`, `TalkBack` 검증 문서 부재 | 테스트 전략은 ViewModel/비동기 로직 중심이고 접근성 스모크 테스트 또는 수동 체크리스트가 확인되지 않는다. 출시 후 UI 변경 시 터치 타깃, 라벨, 대비 회귀가 반복될 수 있다. | Compose UI 테스트 또는 릴리즈 체크리스트에 핵심 화면 접근성 검증을 추가한다. 최소 항목: 홈 수면 시작, 알람 시간 변경, 구독 플랜 선택/구매 CTA, 설정 권한 행, 약관/개인정보 링크의 TalkBack 라벨과 키보드/스위치 조작 가능 여부. | true |
| P2 | UX/접근성 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/theme/Theme.kt:64-74`, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/theme/Color.kt:10-41` | 앱 테마는 기본 다크 모드이고 고대비/동적 색상/사용자 글꼴 확대 대응에 대한 별도 정책이 확인되지 않는다. 큰 글꼴 설정에서 `maxLines = 1` 텍스트나 좁은 카드 텍스트가 잘릴 수 있다. | 출시 후 큰 글꼴, 굵은 글꼴, 고대비 텍스트, 다크/라이트 모드 시나리오를 정기 회귀 테스트에 포함한다. `maxLines = 1`인 구독/프로필 카드에는 말줄임 또는 레이아웃 확장 정책을 명시한다. | true |

## 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
|---|---|---|---|---|---|
| P1 | 품질/접근성 | Google Play Console Pre-launch report, 실제 Android 단말 TalkBack/스위치 접근 결과는 저장소에 없음 | 코드상 접근성 리스크는 확인되지만 실제 심사 봇, 단말 해상도, 폰트 배율, TalkBack 읽기 순서, 터치 타깃 판정 결과는 저장소만으로 확정할 수 없다. | 출시 전 내부/비공개 테스트 트랙에 AAB를 업로드하고 Pre-launch report의 접근성 경고를 확인한다. 최소 1대 이상 실제 단말에서 TalkBack 켜고 홈, 알람, 구독, 설정의 핵심 액션을 완료한다. | false |
| P1 | 스토어 등록/접근성 | Google Play Console 앱 콘텐츠/권한 선언 자료는 저장소에 없음, `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/AccessibilityDisclosureDialog.kt:14-16`, `composeApp/src/androidMain/res/xml/accessibility_service_config.xml:2-8` | 앱 내 접근성 서비스 공개 다이얼로그와 Android 접근성 서비스 설정 설명은 확인되지만, Play Console 권한 선언 문구와 정책 검토 결과는 직접 점검 대상 밖이다. 선언 문구가 앱 내 고지와 불일치하면 심사 지연 또는 반려 위험이 있다. | Console 권한 선언, 개인정보 처리방침, 앱 내 공개 문구가 "수면 모드 중 앱 전환 감지 및 잠금 유지" 목적과 수집/전송하지 않는 데이터 범위를 일관되게 설명하는지 별도 확인한다. | false |

## 요약 권장 순서
1. 홈 수면 시작 CTA에 접근성 대체 액션을 추가하고 TalkBack으로 실제 실행 가능 여부를 확인한다.
2. 커스텀 클릭 컴포넌트의 역할, 선택 상태, 상태 설명을 공통 컴포넌트부터 보강한다.
3. 저대비 보조 텍스트와 12~14sp 텍스트의 대비를 실제 배경 기준으로 재검증한다.
4. 장식 이미지/이모지/상태 점의 스크린 리더 노출 정책을 정리한다.
5. Pre-launch report와 실제 단말 TalkBack 검증 결과를 최종 출시 체크리스트에 반영한다.
