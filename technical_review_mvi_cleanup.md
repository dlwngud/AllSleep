# 기술 검토 문서 - MVI 리팩토링 및 코드 클린업

## 1. 개요
* 작업 브랜치: `feature/refactor-mvi-cleanup`
* 주요 목표: 미사용 코드 삭제(HomeViewModel), MVI 아키텍처 패턴 보강(SettingsViewModel), 잠재적 버그 요인 및 Race-Condition 제거(GlobalSleepViewModel, StatsViewModel).

## 2. 작업 상세 내용
### 2.1. HomeViewModel 및 의존성 삭제
* `HomeViewModel.kt` 및 `HomeContract.kt` 완전히 삭제.
* `HomeScreen.kt`에서 불필요한 Koin 의존성과 State 매개변수를 제거하고, UI Preview 대응 등을 수정.
* `AppModule.kt`에 존재하던 의존성 주입(`viewModelOf(::HomeViewModel)`) 및 import 문 삭제.

### 2.2. SettingsViewModel MVI 리팩토링 보강
* 기존에 뷰(Screen)에서 직접 public 함수(`updateAccessibilityStatus`, `onNotificationPermissionResult`)를 호출하던 안티패턴을 수정.
* `SettingsContract.kt`에 명시적인 인텐트(`UpdateAccessibilityStatus`, `UpdateNotificationStatus`) 추가.
* `SettingsScreen.kt`에서는 해당 인텐트만을 `LaunchedEffect`를 통해 `viewModel.handleIntent` 파이프라인으로 일관되게 전달하도록 수정.

### 2.3. 잠재적 Race-Condition 및 오류 덮어쓰기 수정 (Silent Bug)
* `GlobalSleepViewModel.kt`: 동기화 딜레이 처리용 `kotlinx.coroutines.delay(1000)` 하드코딩을 삭제하고 에러 발생 시 early return(`return@launch`) 하도록 논리 보강.
* `StatsViewModel.kt` & `RecordSleepSessionUseCase.kt`: `calculateTimeDiffMinutes()` 함수 내부에서 파싱 에러 발생 시, 에러 로깅 없이 기본값(480분)을 묵시적(silently)으로 반환하던 catch 블록 최상단에 `println` 에러 로깅 추가.

## 3. 검증 결과
* ✅ `./verify.sh` 스크립트를 통한 린트(ktlint), 문법 검사 및 단위 테스트 `SUCCESS` 확인.
* ✅ `./gradlew :composeApp:assembleDebug` 패키징 무결성 `SUCCESS` 및 APK 정상 추출 확보.
* ✅ 빌드 환경 복원 (`local.properties`, `keystore`, `google-services.json` 등 필수 파일 매핑 성공).

## 4. 후속 작업 (선택)
* `develop` 브랜치로 병합(Merge) 및 충돌 사항 해소 여부 확인.
