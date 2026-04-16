# 🧭 AllSleep Project Map & Agent Rules

이 파일은 Antigravity 에이전트의 '지침서'가 아닌, 프로젝트 전반을 안내하는 **지점(Pointers)**과 **6단계 정밀 루프(Hooks)**를 담고 있는 프로젝트 헌법입니다.

---

### 🌿 브랜칭 전략: Gitflow (Branching Strategy)

Antigravity는 다음 브랜치 구조를 엄격히 준수합니다.

1. **`main`**: 실제 스토어에 출시되는 버전만 존재합니다. 직접 커밋은 금지되며, 오직 `release` 혹은 `hotfix` 브랜치만 병합될 수 있습니다.
2. **`develop`**: 모든 개발의 기준점입니다. 기능 개발이 완료되면 이곳으로 모입니다.
3. **`feature/기능명`**: 새로운 기능을 개발하는 독립 공간입니다. 반드시 `develop`에서 생성하고 작업 완료 후 `develop`에 병합합니다.
4. **`hotfix/이슈명`**: 운영 중인 제품(`main`)의 긴급 버그를 수정합니다. `main`에서 생성하고 `main`과 `develop` 양쪽으로 병합합니다.

---

### 🛡️ Antigravity 6단계 정밀 워크플로우 (Mandatory)

Antigravity는 모든 명령에 대해 다음 단계를 한 단계도 빠짐없이 수행합니다.

#### 1단계: 정밀 계획 (Research & Detailed Plan)
- **기준**: 항상 **`develop`** 브랜치(기능 개발) 또는 **`main`** 브랜치(핫픽스)를 기준으로 리서치를 수행합니다.
- **문서화**: 제안할 내용을 `implementation_plan.md`에 정리합니다. 이때 반드시 **[Technical Rationale & Alternatives]** 섹션을 포함하여 설계 근거와 대안을 명시합니다.
- **대기**: **사용자의 "승인" 또는 "진행" 메시지 전까지 절대 코드 수정을 시작하지 않습니다.**

#### 2단계: 환경 격리 (Worktree Setup)
- **생성**: 계획 승인 직후 `git worktree add -b <branch-name> ../<folder-name> [base-branch]`를 실행하여 독립 작업 공간을 확보합니다.
- **이동**: 생성된 디렉터리로 이동하여 이후 단계를 진행합니다. 

#### 3단계: 선언적 구현 (Implementation & Task Tracking)
- **진행 관리**: `task.md`를 생성/업데이트하여 작업 단위를 세분화하고 진행률을 표시합니다 `[/]`, `[x]`.
- **코딩**: 계획된 파일에 대해 KMP 아키텍처 규칙을 준수하며 클린 코드를 작성합니다.

#### 4단계: 단위 테스트 (Unit Testing)
- **타겟**: 수정한 ViewModel, Repository, UseCase에 대한 테스트 코드를 작성합니다.
- **범위**: `commonTest` 또는 `androidMain`에서 성공/예외 케이스를 모두 검증합니다.

#### 5단계: 하네스 검증 (Strict Verification)
- **명령**: `./verify.sh`를 실행하여 컴파일, 린트, **단위 테스트**가 모두 `SUCCESS`임을 확인합니다.
- **결과**: 실패 시 스스로 수정하여 `Green` 상태가 될 때까지 3~5단계를 반복합니다.

#### 6단계: 심층 기술 리뷰 및 시맨틱 커밋 (Detailed Tech Review & Commit)
- **문서화**: 설계 철학/기술 선택/대안 분석/테스트 전략을 총망라한 **심층 기술 리뷰(`technical_review.md`)**와 워크스루를 작성합니다.
- **커밋**: `type(scope): 설명` 형식을 준수하여 커밋 메시지 초안을 제안합니다.
- **정리**: 푸시 및 Merge 완료 후 생성했던 `worktree` 디렉터리를 삭제하여 환경을 초기화합니다.

---

### 🗺️ Project Navigation (Index)
- **[아키텍처]**: `composeApp/src/commonMain/kotlin/.../domain` (핵심 규칙)
- **[결제]**: `composeApp/src/commonMain/kotlin/.../platform/BillingProvider.kt`
- **[디자인]**: `composeApp/src/commonMain/kotlin/.../ui/theme` (컬러/폰트 토큰)

---

*이 지도는 에이전트의 품질을 보증하는 유일무이한 헌법입니다.*
