# 시안 생성 프롬프트

내장 이미지 생성 도구를 사용했다. 아래 원본 프롬프트는 단계별 디자인 재현을 위한 것이다.

## 0단계 · 즉시 수정

파일: 01-immediate-fixes.png

```text
Use case: ui-mockup.
Asset type: high-fidelity Android product design review board for the existing AllSleep sleep-lock app.
Input images: reference 1 is brand palette/UI reference only; reference 2 is the exact mascot identity reference. Preserve purple-lavender rounded ghost, navy forehead sleep mask, little gold crown, gentle closed eyes. Do not copy the reference's dense glowing layout.
Style: calm premium Material 3 mobile interfaces, midnight navy #111326 background, elevated slate-indigo #202438 cards, solid violet #5746FF main action, off-white text, lavender secondary text, cyan confirmed states, restrained amber warnings. Minimal glow, generous space, consistent typography. Small mascot in functional screens, larger only for onboarding or morning reward.
Composition: a clean wide design board with distinct upright phone screen rectangles. Screens are equally sized, full straight-on flat UI, no perspective, no physical phone frames. Include clear stage heading above board and small screen number/title above each screen. Every screen must be fully visible including bottom controls. Most body text 14-16sp equivalent. Sparse Korean copy, correct Hangul, no overflowing copy. App name remains AllSleep; do not rename it. Main app tabs when shown: "홈", "기록", "수면 일정", "설정". Accent for active tab.
Truth constraints: these are future completion concepts. Never equate lock duration to actual sleep. Never show sleep stages, deep sleep, medical scores, surveillance, or fabricated app-usage analysis. Never call rule-based tips AI. Never show fake loading percent. Core free lock and records always accessible. Emergency access never delayed by countdown.

Stage heading verbatim: 0단계 · 즉시 수정
Exactly FOUR screens in one horizontal row. This stage fixes trust and recovery without adding major features.
Screen 1 "시작 처리": home title "잠금 준비 중", compact schedule 23:00 → 07:00, small mascot, status "실행 상태를 확인하고 있어요", inline spinner in disabled primary button "준비 중"; no green success label until confirmation, small text action "취소".
Screen 2 "시작 실패": same home background with dismissible compact modal "잠금이 시작되지 않았어요", body "기기 상태를 확인한 뒤 다시 시도해주세요", buttons "닫기" and "다시 시도". Show close icon. Background remains legible.
Screen 3 "예약 확인": title "수면 일정", weekday/weekend segment, sleep toggle ON at 23:00, wake toggle OFF at 07:00, clear status card "취침 예약 켜짐 · 기상 알림 꺼짐", note "일요일 밤 → 월요일 아침", feedback "저장됐어요". Use 24-hour time and no AM/PM.
Screen 4 "기기 교체": title "연결된 기기", named rows "기존 휴대폰", "이 휴대폰", an open bottom sheet "사용할 기기를 선택하세요", copy "무료 플랜은 한 기기에서 사용할 수 있어요", primary "이 기기로 변경", secondary "취소", tertiary "여러 기기 함께 사용". Cancel does not imply logout; no threatening restriction copy.
```

## 1단계 · 무료 첫 사용 완성

파일: 02-first-use-free.png

```text
Use case: ui-mockup.
Asset type: high-fidelity Android product design review board for the existing AllSleep sleep-lock app.
Input images: reference 1 is brand palette/UI reference only; reference 2 is the exact mascot identity reference. Preserve purple-lavender rounded ghost, navy forehead sleep mask, little gold crown, gentle closed eyes. Do not copy the reference's dense glowing layout.
Style: calm premium Material 3 mobile interfaces, midnight navy #111326 background, elevated slate-indigo #202438 cards, solid violet #5746FF main action, off-white text, lavender secondary text, cyan confirmed states, restrained amber warnings. Minimal glow, generous space, consistent typography. Small mascot in functional screens, larger only for onboarding or morning reward.
Composition: a clean wide design board with distinct upright phone screen rectangles. Screens are equally sized, full straight-on flat UI, no perspective, no physical phone frames. Include clear stage heading above board and small screen number/title above each screen. Every screen must be fully visible including bottom controls. Most body text 14-16sp equivalent. Sparse Korean copy, correct Hangul, no overflowing copy. App name remains AllSleep; do not rename it. Main app tabs when shown: "홈", "기록", "수면 일정", "설정". Accent for active tab.
Truth constraints: these are future completion concepts. Never equate lock duration to actual sleep. Never show sleep stages, deep sleep, medical scores, surveillance, or fabricated app-usage analysis. Never call rule-based tips AI. Never show fake loading percent. Core free lock and records always accessible. Emergency access never delayed by countdown.

Stage heading verbatim: 1단계 · 무료 첫 사용 완성
Exactly FOUR screens in one horizontal row. The complete free activation experience.
Screen 1 "가치 소개": title "자야 할 때, 폰은 쉬게", mascot holds warm crescent moon, subtitle "잠들기 전 휴대폰 사용을 멈추도록 도와요", primary "무료로 시작", secondary "로그인".
Screen 2 "시간 설정": title "오늘의 수면 일정", simple cards "잠금 시작 23:00", "자동 해제 07:00", chip "8시간 예정", primary "홈으로 이동", small note "언제든 바꿀 수 있어요". No mandatory account.
Screen 3 "권한 준비": title "수면 잠금 준비", checklist "다른 앱 위에 표시" status "설정 필요", "접근성" status "설정 필요", "알림" status "꺼짐", "배터리 설정" status "권장"; actions on each row. Explanation "필수 설정은 잠금 시작 전에 안내해요". Primary "설정 열기", secondary "나중에 준비". No fake completion.
Screen 4 "무료 기록": title "내 기록", label "무료", empty calendar illustration and small mascot, title "첫 기록을 기다리고 있어요", copy "수면 모드를 사용하면 유지 시간이 기록돼요", primary "수면 모드 시작", subtle secondary "기록은 이 기기에 저장돼요". Show records tab active. No paywall and no zero sleep score.
```

## 2단계 · 취침과 잠금 경험

파일: 03-bedtime-and-lock.png

```text
Use case: ui-mockup.
Asset type: high-fidelity Android product design review board for the existing AllSleep sleep-lock app.
Input images: reference 1 is brand palette/UI reference only; reference 2 is the exact mascot identity reference. Preserve purple-lavender rounded ghost, navy forehead sleep mask, little gold crown, gentle closed eyes. Do not copy the reference's dense glowing layout.
Style: calm premium Material 3 mobile interfaces, midnight navy #111326 background, elevated slate-indigo #202438 cards, solid violet #5746FF main action, off-white text, lavender secondary text, cyan confirmed states, restrained amber warnings. Minimal glow, generous space, consistent typography. Small mascot in functional screens, larger only for onboarding or morning reward.
Composition: a clean wide design board with distinct upright phone screen rectangles. Screens are equally sized, full straight-on flat UI, no perspective, no physical phone frames. Include clear stage heading above board and small screen number/title above each screen. Every screen must be fully visible including bottom controls. Most body text 14-16sp equivalent. Sparse Korean copy, correct Hangul, no overflowing copy. App name remains AllSleep; do not rename it. Main app tabs when shown: "홈", "기록", "수면 일정", "설정". Accent for active tab.
Truth constraints: these are future completion concepts. Never equate lock duration to actual sleep. Never show sleep stages, deep sleep, medical scores, surveillance, or fabricated app-usage analysis. Never call rule-based tips AI. Never show fake loading percent. Core free lock and records always accessible. Emergency access never delayed by countdown.

Stage heading verbatim: 2단계 · 취침과 잠금 경험
Exactly FOUR screens in one horizontal row.
Screen 1 "오늘의 홈": title "오늘 밤 준비 완료", compact mascot occupying under 20% screen, prominent schedule "오늘 23:00 → 내일 07:00", status "자동 예약 켜짐", small permission badge "잠금 준비 완료", large violet button "지금 수면 모드 시작", text "오늘만 시간 변경", small device chip "이 휴대폰".
Screen 2 "오늘만 변경": title "수면 일정", with open bottom sheet "오늘만 일정 변경", choices "30분 늦추기", "시간 직접 선택", "오늘 예약 건너뛰기", primary "적용", secondary "취소"; reassuring note "기본 루틴은 바뀌지 않아요"; concise preview "오늘 23:30 → 내일 07:00".
Screen 3 "잠금 유지": low-brightness calm screen, title "수면 모드 실행 중", clear "07:00 자동 해제", subtitle "남은 시간 6시간 20분", small sleeping mascot, allowed-app card "허용된 앱" with "전화" and "긴급 연락" accessible buttons. Bottom ordinary secondary "수면 모드 종료", separate clear "긴급 사용". No bottom app navigation and no glowing moving orbit.
Screen 4 "긴급 사용": over low-light lock screen, clear sheet title "긴급하게 사용하기", large immediate buttons "전화 열기", "긴급 연락", separate outlined "잠금 전체 종료", secondary "수면 모드로 돌아가기". No timer and no payment barrier. Small copy "긴급 사용은 바로 열 수 있어요".
```

## 3단계 · 아침 결과와 신뢰

파일: 04-morning-records-recovery.png

```text
Use case: ui-mockup.
Asset type: high-fidelity Android product design review board for the existing AllSleep sleep-lock app.
Input images: reference 1 is brand palette/UI reference only; reference 2 is the exact mascot identity reference. Preserve purple-lavender rounded ghost, navy forehead sleep mask, little gold crown, gentle closed eyes. Do not copy the reference's dense glowing layout.
Style: calm premium Material 3 mobile interfaces, midnight navy #111326 background, elevated slate-indigo #202438 cards, solid violet #5746FF main action, off-white text, lavender secondary text, cyan confirmed states, restrained amber warnings. Minimal glow, generous space, consistent typography. Small mascot in functional screens, larger only for onboarding or morning reward.
Composition: a clean wide design board with distinct upright phone screen rectangles. Screens are equally sized, full straight-on flat UI, no perspective, no physical phone frames. Include clear stage heading above board and small screen number/title above each screen. Every screen must be fully visible including bottom controls. Most body text 14-16sp equivalent. Sparse Korean copy, correct Hangul, no overflowing copy. App name remains AllSleep; do not rename it. Main app tabs when shown: "홈", "기록", "수면 일정", "설정". Accent for active tab.
Truth constraints: these are future completion concepts. Never equate lock duration to actual sleep. Never show sleep stages, deep sleep, medical scores, surveillance, or fabricated app-usage analysis. Never call rule-based tips AI. Never show fake loading percent. Core free lock and records always accessible. Emergency access never delayed by countdown.

Stage heading verbatim: 3단계 · 아침 결과와 신뢰
Exactly FOUR screens in one horizontal row.
Screen 1 "아침 결과": title "좋은 아침이에요", happy small mascot, metric "수면 모드 유지 7시간 12분", label "실제 수면 시간과 다를 수 있어요", factual row "시작 23:18 · 종료 06:30", optional feeling chips "개운해요", "보통", "피곤해요"; primary "오늘 일정 확인", secondary "나중에".
Screen 2 "기본 기록": title "최근 7일", large "평균 유지 시간 7시간 12분", accurate simple bar chart with one explicitly missing day labeled "기록 없음" instead of 0, two compact metrics "목표 달성 4/6회", "기록 6일"; one actionable note "오늘은 15분 일찍 시작해봐요", link "기록 기준 보기". Free summary fully visible.
Screen 3 "오프라인 복구": title "내 기록", previous cached records remain visible, small amber banner "오프라인 · 기기 내 기록 표시 중", clear row "오늘 기록 · 서버 전송 대기", card "이 기기 잠금은 계속 사용할 수 있어요", primary "다시 동기화", bottom secondary "홈으로 이동". No full-screen scary error and no fake claim server upload succeeded.
Screen 4 "기기별 확인": title "연결된 기기", "휴대폰" cyan check "잠금 적용 확인", "태블릿" amber icon "적용 확인 안 됨", subline "마지막 확인 12분 전", inline "다시 시도"; compact feedback "휴대폰의 수면 모드는 유지돼요", action "이 기기만 사용". No falsely universal Ready label; device count 2 labeled premium where appropriate.
```

## 4단계 · 유료 가치 확장

파일: 05-premium-value.png

```text
Use case: ui-mockup.
Asset type: high-fidelity Android product design review board for the existing AllSleep sleep-lock app.
Input images: reference 1 is brand palette/UI reference only; reference 2 is the exact mascot identity reference. Preserve purple-lavender rounded ghost, navy forehead sleep mask, little gold crown, gentle closed eyes. Do not copy the reference's dense glowing layout.
Style: calm premium Material 3 mobile interfaces, midnight navy #111326 background, elevated slate-indigo #202438 cards, solid violet #5746FF main action, off-white text, lavender secondary text, cyan confirmed states, restrained amber warnings. Minimal glow, generous space, consistent typography. Small mascot in functional screens, larger only for onboarding or morning reward.
Composition: a clean wide design board with distinct upright phone screen rectangles. Screens are equally sized, full straight-on flat UI, no perspective, no physical phone frames. Include clear stage heading above board and small screen number/title above each screen. Every screen must be fully visible including bottom controls. Most body text 14-16sp equivalent. Sparse Korean copy, correct Hangul, no overflowing copy. App name remains AllSleep; do not rename it. Main app tabs when shown: "홈", "기록", "수면 일정", "설정". Accent for active tab.
Truth constraints: these are future completion concepts. Never equate lock duration to actual sleep. Never show sleep stages, deep sleep, medical scores, surveillance, or fabricated app-usage analysis. Never call rule-based tips AI. Never show fake loading percent. Core free lock and records always accessible. Emergency access never delayed by countdown.

Stage heading verbatim: 4단계 · 유료 가치 확장
Exactly SIX upright phone screens arranged in TWO rows of THREE with generous spacing. Both rows use same screen size and full readable UI. The premium flow starts after core free use has succeeded.
Screen 1 "자동화": premium badge, title "매일 같은 시간에 준비", simple timeline "22:45 준비 알림", "23:00 잠금 시작", "07:00 자동 해제", rule chip "평일", primary "자동 루틴 저장", note "오늘만 변경 가능".
Screen 2 "세밀한 차단": premium badge, title "잠금 규칙", selected-app list using generic icons and labels "영상 앱", "SNS", "게임", allow section "전화 · 긴급 연락", option "일반 해제 전 확인", footer "긴급 사용은 항상 바로 가능", button "규칙 적용". No invasive tracking claims.
Screen 3 "주간 개선 계획": premium badge, title "이번 주 패턴", chart based only on lock-start times, evidence "지난 4주 수요일 평균 23:38 시작", suggestion "수요일 준비 알림을 15분 앞당겨볼까요?", primary "이번 주 적용", secondary "계획 수정". Do not say AI, actual sleep, or deep sleep.
Screen 4 "구매 안내": title "매일 더 쉽게 내려놓기", benefits "자동 루틴", "세밀한 차단 규칙", "여러 기기 동시 잠금", "장기 기록과 개선 계획", package selection "월간" and "연간", clear prominent text "요금과 체험 조건 확인 후 구매", button "구매 조건 확인", close and "무료로 계속", restore text "구매 복원". No invented prices, fixed trial length, or discounts.
Screen 5 "구매 완료": title "프리미엄이 준비됐어요", tiny celebration mascot, checkmark, copy "첫 자동 루틴을 설정해볼까요?", primary "자동 루틴 설정", secondary "기기 추가", tertiary "나중에". This page conveys entitlement confirmed, not merely payment submitted.
Screen 6 "구독 확인 지연": title "구매 상태 확인 중", spinner, factual copy "혜택이 아직 확인되지 않았어요", primary "구매 복원", secondary "무료 기능으로 계속", link "문의하기", reminder "다시 결제하기 전에 구매 내역을 확인하세요". Do not label the purchase as failed. Free core functionality remains usable.
```

## 1단계 문구 교정

알림 설명을 `기상 알림을 받을 수 있어요`로 수정하고 권장 배터리 설정에 완료 체크 대신 정보 아이콘을 사용하도록 별도 편집했다.

## 3단계 수치·표기 교정

주간 차트는 `7시간10분 / 7시간32분 / 6시간48분 / 기록 없음 / 7시간18분 / 7시간40분 / 6시간44분`으로 표시했다. 기록된 6일의 합계는 43시간12분이고 평균은 7시간12분이다. 영어 h/m 표기를 한국어로 변경했다.

## 4단계 무료·유료 구분과 긴급 접근 교정

유료 차단 화면 제목은 `루틴별 앱 규칙`, 설명은 `루틴마다 다른 차단 규칙을 정해요`로 교정했다. 기본 앱 선택과 고급 루틴별 규칙의 차이를 드러내기 위함이다. 전화·긴급 연락의 스위치는 제거하고 변경할 수 없는 `항상 허용` 상태로 표시했다.
