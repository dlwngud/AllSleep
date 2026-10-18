# Google Play 출시 전 점검 리포트 - AC8 이미지 및 리소스 사용

점검 범위: 현재 저장소의 Android/KMP 앱 코드, Gradle 설정, Compose Multiplatform 리소스, Android 리소스, 저장소에 있는 release AAB 기준. Google Play Console, Play Asset Delivery 결과, 실제 단말 메모리/프레임 프로파일링 결과는 직접 점검하지 않았다. 코드 변경은 수행하지 않았다.

## 1. 출시 차단

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 품질/성능 | `composeApp/release/composeApp-release.aab`; `composeApp/src/commonMain/composeResources/drawable/character_alarm_routine.png`; `composeApp/src/commonMain/composeResources/drawable/character_bedtime.png`; `composeApp/src/commonMain/composeResources/drawable/character_sleep_stats.png`; `composeApp/src/androidMain/ic_launcher-playstore.png`; `composeApp/src/androidMain/res/mipmap-*/ic_launcher*.webp` | 저장소의 release AAB 수정 시각은 2026-04-29 17:44:47이고, 여러 이미지/아이콘 리소스는 2026-05-04 이후 변경 또는 추가되어 있다. 현재 AAB를 그대로 제출하면 최신 이미지/아이콘 변경과 실제 출시 바이너리가 불일치할 수 있으며, 용량/리소스 최적화 판단도 최신 소스 기준으로 확정할 수 없다. | Google Play 업로드 전 현재 소스에서 release AAB를 재생성하고, `bundletool` 또는 AAB 압축 목록으로 포함 리소스와 파일 크기를 다시 확인한다. 내부 테스트 트랙에는 최신 리소스가 반영된 빌드만 업로드한다. | true |

## 2. 출시 전 권장

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P1 | 성능/메모리 | `composeApp/src/commonMain/composeResources/drawable/character_phone.png` 1280x1113, 972K; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:465-469`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionScreen.kt:190-194`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/subscription/SubscriptionManageScreen.kt:240-244` | `character_phone.png`는 큰 원본인데 홈에서는 200dp, 구독 화면에서는 80-84dp로 표시된다. PNG는 압축 파일 크기와 별개로 디코딩 시 수 MB 메모리를 사용할 수 있어, 화면 전환/재구성 시 저사양 기기에서 GC, 프레임 드롭, 시작 직후 메모리 피크를 만들 수 있다. | 표시 크기별 파생 리소스를 만들거나 WebP/AVIF 변환, 해상도 축소를 적용한다. 80dp 이하 아이콘성 사용은 별도 소형 자산으로 분리하고, release 후보에서 홈/구독 화면 진입 시 메모리 할당과 jank를 측정한다. | true |
| P1 | 성능/메모리 | `composeApp/src/commonMain/composeResources/drawable/character_tablet.png` 1280x1184, 1.2M; `composeApp/src/commonMain/composeResources/drawable/character_lock.png` 1280x737, 866K; `composeApp/src/commonMain/composeResources/drawable/character_sleep.png` 1280x1031, 1.2M; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingProblemScreen.kt:48-52`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingSolutionScreen.kt:48-52`; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/onboarding/OnboardingCompleteScreen.kt:52-56` | 온보딩 화면은 240dp 표시용으로 1280px급 캐릭터 PNG를 사용한다. 신규 사용자 첫 실행 경로에 포함되므로 다운로드 크기와 최초 디코딩 비용이 체감 UX에 직접 영향을 준다. | 온보딩 표시 크기에 맞춘 축소 리소스를 만들고 압축 포맷을 WebP/AVIF로 비교한다. 온보딩 1->2->완료 화면 연속 이동 시 메모리 피크와 프레임 드롭을 release 빌드에서 확인한다. | true |
| P1 | 성능/용량 | `composeApp/src/commonMain/composeResources/drawable/character_alarm_routine.png` 1.1M; `character_bedtime.png` 840K; `character_deep_sleep.png` 1.1M; `character_phone_away.png` 916K; `character_sleep_stats.png` 1.2M; `character_wakeup.png` 958K; 저장소 검색 결과 해당 이름의 코드 참조 0건 | 현재 소스에는 사용 흔적이 없는 대형 캐릭터 PNG가 약 6MB 이상 존재한다. 최신 빌드에서 Compose resources에 포함되면 AAB/설치 크기를 불필요하게 키우고, 향후 코드에서 잘못 참조될 경우 큰 원본 디코딩 리스크가 반복된다. | 실제 출시 UX에 쓰지 않는 리소스는 제거하거나 디자인 원본/후보 폴더로 분리한다. 남겨야 한다면 사용 예정 화면과 목표 표시 크기를 문서화하고, release AAB 재생성 후 해당 파일들이 패키징되는지 확인한다. | true |
| P1 | 성능/용량 | `composeApp/src/commonMain/composeResources/drawable/character_orbit_bg.jpg` 1024x1024, 172K; `composeApp/src/commonMain/composeResources/drawable/character_orbit_no_bg.png` 799x615, 360K; `composeApp/release/composeApp-release.aab` 압축 목록 | 저장소 코드 검색에서는 두 리소스의 참조가 없지만 기존 AAB에는 `base/assets/composeResources/.../character_orbit_bg.jpg`, `character_orbit_no_bg.png`가 포함되어 있다. 사용하지 않는 리소스가 실제 패키지에 포함되는 경로가 이미 확인되어 용량 최적화 공백이 있다. | 최신 release AAB에서 미사용 Compose 리소스가 포함되는지 재검증하고, 사용하지 않는 파일은 source set에서 제거한다. Compose resources는 Android `res` shrink와 동작이 다를 수 있으므로 AAB 목록 검증을 출시 체크리스트에 포함한다. | true |
| P1 | 성능/품질 | `composeApp/build.gradle.kts:152-154`; 저장소 검색 결과 `shrinkResources`, `resConfigs`, `baselineProfiles`, `macrobenchmark` 설정 부재 | release 빌드는 `isMinifyEnabled = true`만 확인되고 리소스 축소나 앱 크기 회귀 측정 기준은 확인되지 않는다. Android `res` 및 라이브러리 리소스는 줄어들 수 있는 여지가 있으며, 현재 AAB도 21M으로 표시된다. | Android 리소스 축소 적용 가능성을 검토하고, Compose resources 포함 여부는 별도 AAB 검사로 보완한다. 최소 기준으로 release AAB 크기, 설치 APK 크기, 주요 이미지 총량을 CI 또는 출시 체크리스트에 기록한다. | true |
| P1 | 성능/메모리 | `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:183-189`; `composeApp/src/commonMain/composeResources/drawable/charachter_no_phone.png` 1280x1113, 941K | 잠금 오버레이는 서비스 기반 UX이며 200dp 표시용으로 1280px급 PNG를 사용한다. 수면 잠금 중 오버레이가 오래 유지되거나 기기 목록 애니메이션과 함께 표시될 때 메모리 상주 비용과 프레임 비용이 커질 수 있다. | 오버레이 전용 축소 이미지를 제공하고, 잠금 오버레이 표시/해제 반복 시 메모리 누수와 프레임 안정성을 확인한다. 서비스 화면이 백그라운드 상태와 맞물리므로 일반 화면보다 저메모리 상황 테스트를 우선한다. | true |

## 3. 출시 후 개선

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P2 | 성능/품질 | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/home/HomeScreen.kt:473-488`; `composeApp/src/androidMain/kotlin/com/wngud/allsleep/platform/LockOverlayManager.android.kt:197-248` | 홈/오버레이의 기기 아이콘은 벡터 리소스라 용량 리스크는 낮지만, 기기 수만큼 무한 회전 애니메이션과 radial gradient가 반복된다. 이미지 자체보다 애니메이션/그리기 비용이 프레임 드롭 원인이 될 수 있다. | 출시 후 실제 기기 수 1개, 3개, 5개 이상 조건에서 홈과 오버레이의 frame time을 측정한다. 필요하면 애니메이션 개수 제한, 저전력 모드, 오버레이 단순 렌더링 모드를 검토한다. | true |
| P2 | 성능/용량 | `composeApp/src/commonMain/composeResources/drawable/ic_apple_logo.png` 256x256, 4.3K; `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/LoginButtonGroup.kt:121-150` | Apple 로그인 버튼 코드는 주석 처리되어 있지만 `ic_apple_logo.png`는 리소스에 남아 있다. 용량 영향은 작지만, 미사용 자산이 누적되면 리소스 관리 품질이 떨어지고 실제 지원 기능과 UI 자산이 어긋날 수 있다. | 출시 후 미사용 자산 정리 규칙을 정하고, 기능 플래그/플랫폼별 지원 계획이 없는 리소스는 제거한다. 작은 로고류도 주기적으로 사용 여부를 검사한다. | true |
| P2 | 품질/UX | `composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/auth/login/LoginButtonGroup.kt:64-68`, `:106-110`; `composeApp/src/commonMain/composeResources/drawable/ic_google_logo.png`; `ic_kakao_logo.png` | 소셜 로그인 로고는 크기가 작고 즉시 로컬 로딩되어 출시 차단 리스크는 낮다. 다만 브랜드 가이드 변경이나 고대비/다크 배경 가독성은 저장소만으로 실제 심사 결과를 확정할 수 없다. | 출시 후 브랜드 가이드 업데이트와 접근성 대비를 정기 점검한다. 다크 배경 버튼에서 로고가 흐릿하게 보이지 않는지 저해상도 단말 캡처로 확인한다. | true |

## 4. 저장소만으로 확인 불가

| 우선순위 | 범주 | 위치 | 리스크 | 권장 조치 | 저장소 근거 |
| --- | --- | --- | --- | --- | --- |
| P0 | 성능/스토어 등록 | Google Play Console App bundle explorer, Pre-launch report, 실제 내부 테스트 트랙 빌드 | 저장소만으로는 최신 release AAB의 download size, install size, device tier별 texture/bitmap 메모리, Play 최적화 후 실제 설치 크기를 확정할 수 없다. 현재 저장소의 AAB도 최신 리소스 변경 전 산출물이다. | 최신 AAB를 내부 테스트 트랙에 업로드한 뒤 App bundle explorer에서 download/install size를 확인하고, Pre-launch report에서 저사양 기기의 메모리/렌더링 문제를 확인한다. | false |
| P1 | 성능/메모리 | Android Studio Memory Profiler, Perfetto, 실제 저사양 단말 | PNG 원본 크기와 코드 사용 위치로 메모리 리스크는 추정할 수 있지만, Compose resource loader의 실제 디코딩 시점, 캐시 유지 기간, 화면 전환 후 해제 여부는 저장소만으로 확인할 수 없다. | 홈, 온보딩, 구독, 잠금 오버레이를 연속 탐색하며 bitmap allocation, GC 빈도, frame time을 release 빌드에서 측정한다. 특히 Android 8-10 저RAM 기기와 화면 회전/프로세스 복구를 포함한다. | false |
| P1 | 광고 SDK/결제 | `admob.properties`, `composeApp/build.gradle.kts:158-163`; `local.properties`, `composeApp/build.gradle.kts:103-113`; AdMob/RevenueCat/Play Console 운영 콘솔 | 이미지/리소스 점검과 별개로, 출시 후보 빌드가 광고 및 AdMob 테스트 ID와 RevenueCat 테스트 구독관리/API 키를 운영 ID로 전환했는지는 운영 콘솔과 실제 빌드 런타임 검증 없이는 확정할 수 없다. 테스트 ID가 남으면 출시 후보의 광고/구독 SDK 리소스 로딩과 수익화 경로를 운영 조건으로 검증하지 못한다. | 광고 및 RevenueCat 테스트 ID/테스트 구독관리 설정을 실제 운영 ID/운영 상품으로 전환한 release 후보에서 앱 시작, 광고 로드, 구독 화면 진입, 구매/복원 시나리오를 함께 측정한다. | false |

## 결론

현재 저장소 기준으로 이미지/리소스 사용의 직접적인 출시 전 리스크는 최신 리소스 변경보다 오래된 release AAB, 1280px급 캐릭터 PNG를 80-240dp로 표시하는 패턴, 사용 흔적이 없는 대형 이미지 파일, 그리고 미사용 Compose 리소스가 AAB에 포함될 수 있는 점이다. 출시 전에는 최신 AAB를 재생성한 뒤 패키징된 이미지 목록과 App bundle size를 확인하고, 홈/온보딩/구독/잠금 오버레이에서 실제 메모리와 프레임 성능을 측정해야 한다.
