# AllSleep 1~3단계 핵심 코드 학습 노트

2026-10-13 현재 소스 기준. 신입 Android 개발자가 문제·선택·검증·한계를 설명하는 데 초점을 둔다. 앱 기능을 수정하지 않고 현재 구현을 읽어 만든 문서다. 아래 코드는 실제 파일에서 추출했다. 일부는 함수나 클래스의 발췌이며, 독립 실행 예제가 아니다.

새 로컬 세션과 실제 Android 제한 서비스의 연결은 4단계다. 이 문서의 실행 확인 이벤트는 저장 가능한 계약이며, 아직 OS가 이 새 계약으로 실행 성공을 보고하는 엔진까지 구현한 것은 아니다.

## 먼저 따라갈 흐름

```text
사용자가 주말 약속 편집 → 입력 초안 변경 → 약속 저장 클릭
  → LocalBedtimeViewModel.saveRoutine()
  → BedtimeRoutine.validated()
  → LocalBedtimeRepository.saveRoutine()
  → DataStore.edit(): 시각 두 개 + 사용 여부를 함께 저장
  → 저장된 routines Flow 방출
  → ViewModel의 combine → StateFlow<LocalBedtimeUiState>
  → Compose의 collectAsState → 화면 반영
```

저장 성공 콜백은 저장소 갱신 성공 뒤 실행한다. 데이터 방출과 콜백의 스케줄링은 구분하며, UI를 수동으로 성공 상태로 조작해서 저장을 대신하지 않는다.

기본 설명 틀: **어떤 사용자 문제가 있었나 → 어떤 대안이 있었나 → 왜 이 선택이 맞았나 → 어떤 실패를 시험했나 → 어떤 한계가 남았나.**

## 1. ViewModel: 읽는 상태와 변경하는 동작 분리

문제: 여러 UI가 각자 저장하고 로딩·실패를 판단하면 결과가 어긋난다. ViewModel이 화면 상태를 관리하고 저장소가 실제 데이터를 관리하도록 했다. 데이터는 아래로, 사용자 동작은 위로 전달하는 단방향 흐름이다. Intent 타입을 반드시 모든 화면에 추가해야 MVI가 되는 것은 아니다. 이 작은 화면은 이름 있는 메서드로 의도를 표현한다.

원본: [LocalBedtimeViewModel.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/bedtime/LocalBedtimeViewModel.kt:16)

```kotlin
data class LocalBedtimeUiState(
    val isLoading: Boolean = true,
    val routines: List<BedtimeRoutine> = emptyList(),
    val owner: LocalOwner? = null,
    val sessions: List<BedtimeSession> = emptyList(),
    val todayKind: RoutineKind = RoutineKind.WEEKDAY,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null,
)

class LocalBedtimeViewModel(private val repository: LocalBedtimeRepository) : ViewModel() {
    private val _state = MutableStateFlow(LocalBedtimeUiState())
    val state = _state.asStateFlow()
    private var observeJob: Job? = null
    init { reload() }

    fun reload() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.initialize().onFailure { error ->
                _state.update { it.copy(isLoading = false, error = "로컬 설정을 읽지 못했어요. 저장된 원본은 유지했어요.") }
                return@launch
            }
            combine(repository.routines, repository.journal) { routines, journal -> routines to journal }
                .catch { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(isLoading = false, error = "로컬 기록을 읽지 못했어요. 다시 확인해 주세요.") }
                }
                .collect { (routines, journal) ->
                    val day = Clock.System.todayIn(TimeZone.currentSystemDefault()).dayOfWeek
                    val todayKind = if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) RoutineKind.WEEKEND else RoutineKind.WEEKDAY
                    _state.update { it.copy(isLoading = false, routines = routines, todayKind = todayKind, owner = journal.owner, sessions = journal.owner?.let(journal::sessionsFor) ?: emptyList()) }
                }
        }
    }

    fun saveRoutine(kind: RoutineKind, bedtime: String, release: String, enabled: Boolean, onSaved: () -> Unit) {
        if (_state.value.isSaving) return
        val routine = try { BedtimeRoutine(kind, bedtime.trim(), release.trim(), enabled).validated() } catch (error: IllegalArgumentException) {
            _state.update { it.copy(error = "시각은 23:30처럼 입력하고 시작과 해제 시각을 다르게 선택해 주세요.") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null, savedMessage = null) }
        viewModelScope.launch {
            repository.saveRoutine(routine).onSuccess {
                _state.update { it.copy(isSaving = false, savedMessage = "이 기기에 약속을 저장했어요.") }
                onSaved()
            }.onFailure {
                _state.update { it.copy(isSaving = false, error = "저장하지 못했어요. 입력한 내용을 유지했어요.") }
            }
        }
    }
    fun dismissMessage() = _state.update { it.copy(error = null, savedMessage = null) }
}
```

읽는 순서:

1. `_state`는 내부에서 수정할 MutableStateFlow, `state`는 외부에 제공할 읽기용 StateFlow다. 화면이 저장 결과를 마음대로 바꾸는 경로를 줄인다.
2. `copy`로 새 상태를 만들고 `update`로 반영한다. 리스트와 도메인 값도 복사해 쓰며, 외부에서 MutableList를 변경하는 사용 방식은 피한다. `val`과 `List`만으로 깊은 불변성이 자동 보장되는 것은 아니다.
3. `viewModelScope`는 ViewModel의 수명에 맞춘 작업 범위다. 화면 회전으로 ViewModel이 유지될 때 작업도 유지되지만, 소유한 ViewModel이 정리되면 취소된다. 백그라운드 서비스의 작업 범위로 사용해서는 안 된다.
4. `reload()`는 이전 관찰 Job을 취소하고 시작한다. 재시도마다 Flow 구독이 늘어나는 것을 방지한다.
5. `combine`은 각 Flow가 적어도 한 값을 방출한 뒤 양쪽 최신 값으로 화면 상태를 만든다. 이 두 Flow는 동일 DataStore에서 나오지만 별도로 변환되므로 모든 결합 값이 반드시 같은 저장 버전이라고 보장하는 코드는 아니다. 서로 엄격히 연동되는 필드가 생기면 저장 스냅샷 하나에서 함께 파생해야 한다.
6. `saveRoutine`은 UI 입력을 정리하고 검증한 뒤 `isSaving`을 **launch 전에** 설정한다. 첫 코루틴이 실행되기 전 두 번째 탭이 와도 같은 메인 스레드 경로에서 중복 작업을 막는다. 여러 스레드 호출에 대한 잠금이나 원자적 compare-and-set은 아니다.
7. `onSaved`는 저장 성공 후에만 부른다. 실패 시 다이얼로그를 닫지 않고 입력을 유지한다. 저장 시각을 화면에 먼저 덮어쓰는 낙관적 갱신을 선택하지 않았다.
8. `savedMessage`는 현재 UI 상태에 남는 메시지다. 한 번만 소비해야 하는 내비게이션 이벤트와 같은 수명으로 취급하지 않는다. 현재 콜백은 작게 유지하기 위한 선택이고, 수명·복구 요구가 늘면 명시적인 저장 결과 상태나 effect로 분리할 수 있다.

면접 설명: “화면이 저장 성공을 추정하지 않도록 저장소 결과를 기준으로 상태를 바꾸고, 반복 입력과 쓰기 실패에서도 UI와 저장 데이터가 어긋나지 않게 했습니다.”

## 2. Repository 계약과 DI

ViewModel은 파일 경로나 JSON 형식 대신 필요한 기능의 계약에 의존한다. 인터페이스는 실제 Fake 교체·외부 경계 분리에 사용된다. 모든 클래스에 기계적으로 인터페이스를 만드는 규칙은 아니다.

원본: [LocalBedtimeRepository.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/repository/LocalBedtimeRepository.kt:6)

```kotlin
interface LocalBedtimeRepository {
    val journal: Flow<LocalBedtimeJournal>
    val routines: Flow<List<BedtimeRoutine>>
    suspend fun initialize(): Result<LocalOwner>
    suspend fun saveRoutine(routine: BedtimeRoutine): Result<Unit>
    suspend fun requestSession(id: String, occurrence: BedtimeOccurrence, requestedAt: Long): Result<BedtimeSession>
    suspend fun transitionSession(owner: LocalOwner, id: String, event: RestrictionEvent): Result<BedtimeSession>
    suspend fun preserveLegacyRecord(record: LegacyRestrictionRecord): Result<Unit>
}
```

원본: [AppModule.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt:105)

```kotlin
single { createDataStore() }
    singleOf(::SleepSettingsRepositoryImpl) bind SleepSettingsRepository::class
    single<LocalBedtimeRepository> { LocalBedtimeRepositoryImpl(get()) }
```

원본: [AppModule.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/androidMain/kotlin/com/wngud/allsleep/di/AppModule.kt:140)

```kotlin
viewModelOf(::LocalBedtimeViewModel)
    viewModelOf(::AppRootViewModel)
```

`single`은 등록한 의존성을 같은 Koin 컨테이너에서 재사용한다. 두 저장소가 같은 DataStore를 주입받아 같은 파일의 데이터를 함께 갱신한다. `viewModelOf`는 ViewModel 소유 범위와 연동되며 모든 화면이 하나의 ViewModel 객체를 공유한다는 뜻은 아니다. 현재 탭별 Navigation entry는 각각의 LocalBedtimeViewModel을 가질 수 있고, 영속 데이터의 기준은 공유 저장소다.

현재 로컬 설정은 저장소 호출만으로 충분해 1줄짜리 UseCase를 매번 추가하지 않았다. 여러 화면에서 공통 규칙·여러 저장소 조합이 필요해질 때 UseCase를 추가한다.

## 3. 시각 검증, 자정 경계, 회차 스냅샷

“23:30부터 07:00”은 날짜 없이 빼면 음수가 된다. 사용자 입력 시각과 실제 실행 시점은 다른 자료형으로 다룬다. LocalTime으로 시각을 확인하고 LocalDate와 TimeZone을 함께 사용해 절대 시각으로 변환한다.

원본: [BedtimeModels.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeModels.kt:19)

```kotlin
data class BedtimeRoutine(
    val kind: RoutineKind,
    val bedtime: String,
    val releaseTime: String,
    val enabled: Boolean = true,
) {
    fun validated(): BedtimeRoutine {
        require(Regex("\\d{2}:\\d{2}").matches(bedtime) && Regex("\\d{2}:\\d{2}").matches(releaseTime))
        require(LocalTime.parse(bedtime) != LocalTime.parse(releaseTime)) { "시작과 해제 시각을 다르게 선택해 주세요." }
        return this
    }

    fun occurrence(owner: LocalOwner, date: LocalDate, zone: TimeZone): BedtimeOccurrence {
        validated()
        val start = LocalTime.parse(bedtime)
        val release = LocalTime.parse(releaseTime)
        val releaseDate = if (release < start) date.plus(1, DateTimeUnit.DAY) else date
        return BedtimeOccurrence(
            owner = owner,
            routine = this,
            date = date.toString(),
            timeZoneId = zone.id,
            plannedStartAt = LocalDateTime(date, start).toInstant(zone).toEpochMilliseconds(),
            plannedReleaseAt = LocalDateTime(releaseDate, release).toInstant(zone).toEpochMilliseconds(),
        )
    }
}

/** 저장 당시 약속의 스냅샷. 이후 설정·시간대가 바뀌어도 진행 중 회차의 해제를 바꾸지 않는다. */
data class BedtimeOccurrence(
    val owner: LocalOwner,
    val routine: BedtimeRoutine,
    val date: String,
    val timeZoneId: String,
    val plannedStartAt: Long,
    val plannedReleaseAt: Long,
) {
    init {
        routine.validated()
        LocalDate.parse(date)
        TimeZone.of(timeZoneId)
        require(plannedReleaseAt > plannedStartAt)
    }
}
```

`validated()`는 형식과 실제 시간 범위를 확인하고 같은 시작·해제 시각을 거절한다. 같은 시각이 0시간인지 24시간인지 모호한 제품 규칙을 명확히 한 것이다. 읽는 순간 검증 실패로 앱을 막지 않도록 기존 raw 설정은 생성자에서 즉시 거절하지 않고, 저장·회차 생성 경계에서 검증한다.

해제 시각이 시작보다 앞이면 달력 날짜에 하루를 더한다. 무조건 24시간의 밀리초를 더하는 방식은 서머타임 날짜에 달력상 다음날 같은 시각과 어긋날 수 있다. 절대 시각으로 바꾼 뒤 실제 기간을 계산한다. 현재 코드는 kotlinx.datetime의 시간대 변환 정책을 사용하며 DST의 모호한 시각에 대한 별도 사용자 정책까지 구현한 것은 아니다.

회차에는 당시 routine·date·timeZoneId·plannedStartAt·plannedReleaseAt을 저장한다. 이후 루틴을 08:00으로 변경해도 이미 생성한 회차의 07:00 해제를 다시 계산하지 않는다. UUID 등 회차 식별의 더 복잡한 모델은 필요한 단계에서 확장한다.

면접 설명: “사용자의 반복 일정과 특정 날짜의 실행 회차를 분리해 자정과 시간대 변경을 처리하고, 설정 변경이 과거 기록에 영향을 주지 않도록 당시 값을 보관했습니다.”

## 4. Boolean 대신 상태 전환 규칙

시작 버튼을 누른 것과 Android 서비스가 실제 제한한 것은 다르다. `isRunning` 하나로는 시작 대기·해제 대기·기술 중단을 표현하기 어렵다. enum으로 현재 단계를, sealed interface로 들어오는 이벤트를 표현한다.

원본: [BedtimeModels.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeModels.kt:64)

```kotlin
enum class RestrictionPhase { REQUESTED, RUNNING, STOP_REQUESTED, ENDED, INTERRUPTED }
enum class RestrictionEndReason { SCHEDULED, EARLY, EMERGENCY, TECHNICAL }

data class ConfirmedRestrictionInterval(val startedAt: Long, val endedAt: Long? = null) {
    init { require(endedAt == null || endedAt >= startedAt) }
}

data class BedtimeSession(
    val id: String,
    val occurrence: BedtimeOccurrence,
    val requestedAt: Long,
    val phase: RestrictionPhase = RestrictionPhase.REQUESTED,
    val startedAt: Long? = null,
    val stopRequestedAt: Long? = null,
    val releasedAt: Long? = null,
    val interruptedAt: Long? = null,
    val endReason: RestrictionEndReason? = null,
    val intervals: List<ConfirmedRestrictionInterval> = emptyList(),
) {
    init {
        require(id.isNotBlank()); require(requestedAt >= 0)
        val times = listOfNotNull(requestedAt, startedAt, stopRequestedAt, interruptedAt, releasedAt)
        require(times.all { it >= requestedAt })
        require(startedAt == null || stopRequestedAt == null || stopRequestedAt >= startedAt)
        require(startedAt == null || interruptedAt == null || interruptedAt >= startedAt)
        require(releasedAt == null || releasedAt >= listOfNotNull(requestedAt, startedAt, stopRequestedAt, interruptedAt).max())
        require((startedAt == null && intervals.isEmpty()) || (startedAt != null && intervals.firstOrNull()?.startedAt == startedAt))
        when (phase) {
            RestrictionPhase.REQUESTED -> require(startedAt == null && stopRequestedAt == null && releasedAt == null && interruptedAt == null && endReason == null)
            RestrictionPhase.RUNNING -> require(startedAt != null && stopRequestedAt == null && releasedAt == null && interruptedAt == null && intervals.lastOrNull()?.endedAt == null)
            RestrictionPhase.STOP_REQUESTED -> require(stopRequestedAt != null && endReason != null && releasedAt == null)
            RestrictionPhase.ENDED -> require(stopRequestedAt != null && releasedAt != null && endReason != null && intervals.all { it.endedAt != null })
            RestrictionPhase.INTERRUPTED -> require(interruptedAt != null && releasedAt == null && endReason == RestrictionEndReason.TECHNICAL && intervals.all { it.endedAt != null })
        }
    }
    val isFinished: Boolean get() = phase == RestrictionPhase.ENDED
    val confirmedDurationMillis: Long get() = intervals.sumOf { interval -> interval.endedAt?.let { it - interval.startedAt } ?: 0L }

    fun transition(event: RestrictionEvent): BedtimeSession {
        require(event.at >= listOfNotNull(requestedAt, startedAt, stopRequestedAt, releasedAt, interruptedAt).max()) { "이전 상태보다 늦은 확인 시각이 필요합니다." }
        return when (event) {
            is RestrictionEvent.StartConfirmed -> {
                if (phase == RestrictionPhase.RUNNING && startedAt == event.at) return this
                require(phase == RestrictionPhase.REQUESTED) { "종료 요청 뒤의 늦은 시작 응답은 적용할 수 없습니다." }
                copy(phase = RestrictionPhase.RUNNING, startedAt = event.at, intervals = listOf(ConfirmedRestrictionInterval(event.at)))
            }
            is RestrictionEvent.StopRequested -> {
                if (phase == RestrictionPhase.STOP_REQUESTED && stopRequestedAt == event.at && endReason == event.reason) return this
                require(phase != RestrictionPhase.ENDED && phase != RestrictionPhase.STOP_REQUESTED)
                copy(phase = RestrictionPhase.STOP_REQUESTED, stopRequestedAt = event.at, endReason = event.reason)
            }
            is RestrictionEvent.ReleaseConfirmed -> {
                if (phase == RestrictionPhase.ENDED && releasedAt == event.at) return this
                require(phase == RestrictionPhase.STOP_REQUESTED) { "해제 요청과 실행 확인을 구분해야 합니다." }
                copy(phase = RestrictionPhase.ENDED, releasedAt = event.at, intervals = closeIntervals(event.at))
            }
            is RestrictionEvent.Interrupted -> {
                if (phase == RestrictionPhase.INTERRUPTED && interruptedAt == event.at) return this
                require(phase != RestrictionPhase.ENDED && phase != RestrictionPhase.INTERRUPTED)
                copy(phase = RestrictionPhase.INTERRUPTED, interruptedAt = event.at, endReason = RestrictionEndReason.TECHNICAL, intervals = closeIntervals(event.at))
            }
        }
    }

    private fun closeIntervals(at: Long) = intervals.map { if (it.endedAt == null) it.copy(endedAt = at) else it }
}

sealed interface RestrictionEvent {
    val at: Long
    data class StartConfirmed(override val at: Long) : RestrictionEvent
    data class StopRequested(override val at: Long, val reason: RestrictionEndReason) : RestrictionEvent
    data class ReleaseConfirmed(override val at: Long) : RestrictionEvent
    data class Interrupted(override val at: Long) : RestrictionEvent
}
```

이 코드의 규칙:

- REQUESTED → StartConfirmed → RUNNING → StopRequested → STOP_REQUESTED → ReleaseConfirmed → ENDED.
- 실행 확인 뒤 기술 중단이 관찰되면 열린 구간을 닫고 INTERRUPTED로 기록한다. 현재 `isFinished`는 ENDED에서만 true다. 기술 중단을 실제 해제 확인과 같게 취급하지 않아, 이후 해제 요청·확인으로 마무리해야 새 회차를 시작할 수 있다.
- `init`의 require는 새 객체와 저장 복원 객체에 기본 일관성 검사를 적용한다. 시간 필드가 요청 이전이거나 ENDED인데 해제 시각이 없는 값 등을 거절한다. 현재 모든 가능한 interval 중첩·순서 오류를 검사하는 완전한 검증기는 아니다.
- `transition()`은 허용된 전환을 모은 순수 함수다. 저장·Context·네트워크 없이 테스트할 수 있다. `copy`를 반환하므로 저장 전 검증 실패는 원본을 변경하지 않는다.
- 종료 요청 이후 늦은 StartConfirmed를 적용하지 않는다. 이벤트 timestamp가 이전 확인보다 앞선 경우도 거절한다.
- 같은 이벤트와 시각을 다시 적용하면 같은 상태를 돌려준다. 모든 과거 이벤트를 어느 단계에서나 받아들이는 멱등성은 아니다. 저장소와 엔진이 이벤트 ID·전달 정책을 확장할 필요가 생길 수 있다.
- 완료 시간은 `endedAt`이 있는 구간만 합산한다. 실행 중 열린 구간은 0분으로 포함한다. 중단 확인 뒤 실제 해제까지 빈 시간은 더하지 않는다. 이것은 완료된 제한 기록이며 실시간 타이머나 실제 수면 측정이 아니다.

면접 설명: “OS 응답이 늦거나 반복될 수 있어 요청과 확인을 분리하고 허용된 전환을 한 곳에서 검증했습니다. 확인되지 않은 구간을 제한 시간으로 계산하지 않도록 했습니다.”

## 5. 로컬 소유자와 원본 데이터 모델

게스트가 로그인했다고 이전 모든 기록을 새 계정 소유로 바꾸면 개인정보·기록 귀속이 잘못될 수 있다. 소유자를 명시하고, 출처를 확인할 수 없는 기존 값은 별도 원본으로 보존한다.

원본: [BedtimeModels.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeModels.kt:13)

```kotlin
data class LocalOwner(val id: String) {
    init { require(id.startsWith("guest:") || id.startsWith("account:")); require(id.substringAfter(':').isNotBlank()) }
}
```

원본: [BedtimeModels.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeModels.kt:140)

```kotlin
data class LegacyRestrictionRecord(
    val sourceId: String,
    val owner: LocalOwner? = null,
    val activeStartAt: Long? = null,
    val originalRecord: SleepRecord? = null,
)

data class LocalBedtimeJournal(
    val owner: LocalOwner? = null,
    val sessions: List<BedtimeSession> = emptyList(),
    val legacyRecords: List<LegacyRestrictionRecord> = emptyList(),
) {
    fun sessionsFor(owner: LocalOwner) = sessions.filter { it.occurrence.owner == owner }
    fun activeSession(owner: LocalOwner) = sessionsFor(owner).lastOrNull { !it.isFinished }
}
```

`guest:`/`account:`는 로컬 데이터의 구분 규칙이다. 인증 증명이나 Firestore 보안 규칙을 대체하지 않는다. 실제 계정 UID 검증과 병합·백업은 별도의 신뢰 경계다. `owner = null`인 legacy 자료는 추정해서 게스트나 계정에 붙이지 않는다.

## 6. DataStore: 영속성, 원자성, 중복 처리

아래 구현은 읽기·최초 준비·루틴 저장·세션 생성·전환·원본 보존·실패 처리의 핵심 전체다. 작은 기록은 기존 저장소에 version JSON 한 키로 추가해 구조 변경을 최소화했다.

원본: [LocalBedtimeRepositoryImpl.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/data/repository/LocalBedtimeRepositoryImpl.kt:13)

```kotlin
internal val LOCAL_BEDTIME_JOURNAL_KEY = stringPreferencesKey("local_bedtime_journal_v1")

@OptIn(ExperimentalUuidApi::class)
class LocalBedtimeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val newOwnerId: () -> String = { "guest:${Uuid.random()}" },
) : LocalBedtimeRepository {
    override val journal = dataStore.data.map { p -> p[LOCAL_BEDTIME_JOURNAL_KEY]?.let(BedtimeJournalCodec::decode) ?: LocalBedtimeJournal() }
    override val routines = dataStore.data.map { p ->
        listOf(
            BedtimeRoutine(RoutineKind.WEEKDAY, p[stringPreferencesKey("bedtime")] ?: "23:00", p[stringPreferencesKey("wake_time")] ?: "07:00", p[booleanPreferencesKey("sleep_alarm_enabled")] ?: true),
            BedtimeRoutine(RoutineKind.WEEKEND, p[stringPreferencesKey("weekend_bedtime")] ?: "00:00", p[stringPreferencesKey("weekend_wake_time")] ?: "09:00", p[booleanPreferencesKey("weekend_sleep_enabled")] ?: true),
        )
    }

    override suspend fun initialize(): Result<LocalOwner> = storageResult {
        val saved = dataStore.edit { p ->
            if (p[LOCAL_BEDTIME_JOURNAL_KEY] == null) {
                val legacyStart = p[longPreferencesKey("active_sleep_start_at")]?.takeIf { it > 0L }
                p[LOCAL_BEDTIME_JOURNAL_KEY] = BedtimeJournalCodec.encode(LocalBedtimeJournal(
                    owner = LocalOwner(newOwnerId()),
                    legacyRecords = legacyStart?.let { listOf(LegacyRestrictionRecord("settings:active_sleep_start_at:$it", activeStartAt = it)) } ?: emptyList(),
                ))
            } else {
                BedtimeJournalCodec.decode(p[LOCAL_BEDTIME_JOURNAL_KEY]!!)
            }
        }
        BedtimeJournalCodec.decode(saved[LOCAL_BEDTIME_JOURNAL_KEY]!!).owner!!
    }

    override suspend fun saveRoutine(routine: BedtimeRoutine): Result<Unit> = storageResult {
        routine.validated()
        dataStore.edit { p ->
            val weekday = routine.kind == RoutineKind.WEEKDAY
            p[stringPreferencesKey(if (weekday) "bedtime" else "weekend_bedtime")] = routine.bedtime
            p[stringPreferencesKey(if (weekday) "wake_time" else "weekend_wake_time")] = routine.releaseTime
            p[booleanPreferencesKey(if (weekday) "sleep_alarm_enabled" else "weekend_sleep_enabled")] = routine.enabled
        }
        Unit
    }

    override suspend fun requestSession(id: String, occurrence: BedtimeOccurrence, requestedAt: Long): Result<BedtimeSession> = storageResult {
        val session = BedtimeSession(id, occurrence, requestedAt)
        val saved = updateJournal { current ->
            val existing = current.sessions.find { it.id == id }
            if (existing != null) {
                require(existing.occurrence == occurrence && existing.requestedAt == requestedAt) { "다른 기록과 ID가 겹칩니다." }
                current
            } else {
                require(current.activeSession(occurrence.owner) == null) { "진행 중인 회차를 먼저 해제해 주세요." }
                current.copy(sessions = current.sessions + session)
            }
        }
        saved.sessions.first { it.id == id }
    }

    override suspend fun transitionSession(owner: LocalOwner, id: String, event: RestrictionEvent): Result<BedtimeSession> = storageResult {
        val saved = updateJournal { current ->
            val previous = current.sessions.firstOrNull { it.id == id } ?: error("회차를 찾을 수 없습니다.")
            require(previous.occurrence.owner == owner) { "다른 소유자의 기록을 변경할 수 없습니다." }
            val next = previous.transition(event)
            current.copy(sessions = current.sessions.map { if (it.id == id) next else it })
        }
        saved.sessions.first { it.id == id }
    }

    override suspend fun preserveLegacyRecord(record: LegacyRestrictionRecord): Result<Unit> = storageResult {
        require(record.sourceId.isNotBlank())
        updateJournal { current ->
            val previous = current.legacyRecords.firstOrNull { it.sourceId == record.sourceId }
            require(previous == null || previous == record) { "보존한 원본을 덮어쓸 수 없습니다." }
            if (previous == null) current.copy(legacyRecords = current.legacyRecords + record) else current
        }
        Unit
    }

    private suspend fun updateJournal(transform: (LocalBedtimeJournal) -> LocalBedtimeJournal): LocalBedtimeJournal {
        val saved = dataStore.edit { p ->
            val current = BedtimeJournalCodec.decode(p[LOCAL_BEDTIME_JOURNAL_KEY] ?: error("로컬 기록을 먼저 준비해 주세요."))
            p[LOCAL_BEDTIME_JOURNAL_KEY] = BedtimeJournalCodec.encode(transform(current))
        }
        return BedtimeJournalCodec.decode(saved[LOCAL_BEDTIME_JOURNAL_KEY]!!)
    }

    private suspend fun <T> storageResult(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { Result.failure(error) }
}
```

판단 근거:

1. `initialize`는 키가 없을 때만 UUID 소유자를 만들고, 기존 키가 있으면 해독한다. 앱 진입·재시도마다 새 소유자를 만들지 않는다. `newOwnerId` 함수를 주입해 테스트에서는 고정된 값을 쓴다.
2. `saveRoutine`의 세 필드를 하나의 `edit`에서 쓴다. 필드를 따로 저장하는 중간 상태를 다른 관찰자가 완성된 약속으로 읽지 않도록 한다.
3. `updateJournal`은 읽기·검사·변경·쓰기 모두를 트랜잭션 안에 둔다. 미리 `first()`로 읽고 밖에서 검사한 뒤 저장하면 두 요청이 같은 이전 값을 보고 둘 다 통과하거나 갱신을 잃을 수 있다.
4. 같은 세션 ID와 같은 원본 요청의 재시도는 기존 기록을 반환한다. ID는 같고 내용이 다르면 거절한다. 종료된 회차도 되살리지 않는다. 같은 밤의 별도 요청은 새로운 ID로 따로 남는다.
5. `activeSession(owner)` 검사도 트랜잭션 안에 있어 같은 소유자에게 두 진행 회차를 동시에 추가하지 않는다. 저장소가 다른 소유자의 진행 회차도 보관할 수 있지만 실제 기기 엔진이 동시에 여러 제한 세션을 실행한다는 뜻은 아니다.
6. 변경 전 세션의 owner를 확인한다. 원본 보존은 sourceId 중복을 검사해 동일 재요청은 허용하고 다른 원본 덮어쓰기는 거절한다.
7. `edit` 완료 후 반환된 저장값을 읽어 결과를 만든다. 저장 실패를 성공으로 바꾸지 않는다. 취소는 실패 Result로 삼키지 않고 다시 던진다.
8. JSON 전체를 매번 변환하고 저장하는 O(n) 구조다. 현재 작은 데이터의 최소 설계이며 장기·대용량 기록이나 부분 조회가 필요하면 Room 등의 저장소로 바꾸는 판단이 필요하다. DataStore를 만능 DB로 소개하지 않는다.

DataStore의 트랜잭션, 동일 파일의 단일 인스턴스, 작은 데이터에 적합하다는 제약은 [Android 공식 문서](https://developer.android.com/topic/libraries/architecture/datastore)에서 확인할 수 있다.

면접 설명: “설정과 작은 기록을 기존 DataStore에서 원자적으로 갱신해 초기 복잡도를 줄였습니다. 동시 요청의 검사도 트랜잭션 안에 두었고, 데이터가 커질 때의 조회·전체 쓰기 비용을 알고 있습니다.”

## 7. 코루틴 취소를 실패로 삼키지 않기

위 `storageResult`는 결과 성공/실패 계약을 제공하지만 CancellationException은 반드시 재전파한다. `runCatching`처럼 Exception을 넓게 잡는 코드는 취소까지 Result.failure로 감쌀 수 있다. 취소를 사용자 오류로 다루면 이미 끝나야 할 작업이 계속되는 흐름을 만들 수 있다. `suspend`가 자동으로 백그라운드 스레드를 만든다는 설명도 피한다. 여기서 저장의 비동기 실행과 원자성은 DataStore가 제공한다.

[Kotlin CancellationException 문서](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-cancellation-exception/)의 의미와 함께 읽는다. 현재 ViewModel의 저장 플래그는 취소 후 살아 있는 동일 ViewModel에 대한 finally 복구까지 처리한 형태는 아니다. 요구가 생기면 그 경로를 별도 검증해야 한다.

## 8. 저장 형식의 버전과 손상 데이터 보호

데이터가 없을 때와 데이터가 있는데 읽을 수 없을 때는 다르다. 읽기 실패를 빈 데이터로 취급한 뒤 저장하면 원본을 잃는다. 명시적인 저장 버전을 확인하고 지원하지 않는 버전은 실패하게 했다.

원본: [BedtimeJournalCodec.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/data/source/local/BedtimeJournalCodec.kt:8)

```kotlin
internal object BedtimeJournalCodec {
    fun encode(journal: LocalBedtimeJournal): String = buildJsonObject {
        put("version", 1)
        put("owner", journal.owner?.id)
        put("sessions", JsonArray(journal.sessions.map(::sessionJson)))
        put("legacyRecords", JsonArray(journal.legacyRecords.map { legacy -> buildJsonObject {
            put("sourceId", legacy.sourceId)
            put("owner", legacy.owner?.id)
            put("activeStartAt", legacy.activeStartAt)
            put("originalRecord", legacy.originalRecord?.let(::recordJson) ?: JsonNull)
        } }))
    }.toString()

    fun decode(value: String): LocalBedtimeJournal {
        val root = Json.parseToJsonElement(value).jsonObject
        require(root.getValue("version").jsonPrimitive.int == 1) { "지원하지 않는 로컬 기록 버전입니다." }
        val journal = LocalBedtimeJournal(
            owner = root.optionalString("owner")?.let(::LocalOwner),
            sessions = root.getValue("sessions").jsonArray.map { readSession(it.jsonObject) },
            legacyRecords = root.getValue("legacyRecords").jsonArray.map { value ->
                val legacy = value.jsonObject
                LegacyRestrictionRecord(
                    sourceId = legacy.string("sourceId"),
                    owner = legacy.optionalString("owner")?.let(::LocalOwner),
                    activeStartAt = legacy.optionalLong("activeStartAt"),
                    originalRecord = legacy["originalRecord"]?.takeIf { it != JsonNull }?.jsonObject?.let(::readRecord),
                )
            },
        )
        require(journal.owner != null)
        require(journal.sessions.map { it.id }.distinct().size == journal.sessions.size)
        require(journal.legacyRecords.map { it.sourceId }.distinct().size == journal.legacyRecords.size)
        return journal
    }

    private fun sessionJson(s: BedtimeSession) = buildJsonObject {
        put("id", s.id)
        put("occurrence", buildJsonObject {
            put("owner", s.occurrence.owner.id)
            put("kind", s.occurrence.routine.kind.name)
            put("bedtime", s.occurrence.routine.bedtime)
            put("releaseTime", s.occurrence.routine.releaseTime)
            put("enabled", s.occurrence.routine.enabled)
            put("date", s.occurrence.date)
            put("timeZoneId", s.occurrence.timeZoneId)
            put("plannedStartAt", s.occurrence.plannedStartAt)
            put("plannedReleaseAt", s.occurrence.plannedReleaseAt)
        })
        put("requestedAt", s.requestedAt)
        put("phase", s.phase.name)
        put("startedAt", s.startedAt)
        put("stopRequestedAt", s.stopRequestedAt)
        put("releasedAt", s.releasedAt)
        put("interruptedAt", s.interruptedAt)
        put("endReason", s.endReason?.name)
        put("intervals", JsonArray(s.intervals.map { interval -> buildJsonObject {
            put("startedAt", interval.startedAt)
            put("endedAt", interval.endedAt)
        } }))
    }

    private fun readSession(s: JsonObject): BedtimeSession {
        val o = s.getValue("occurrence").jsonObject
        return BedtimeSession(
            id = s.string("id"),
            occurrence = BedtimeOccurrence(
                owner = LocalOwner(o.string("owner")),
                routine = BedtimeRoutine(RoutineKind.valueOf(o.string("kind")), o.string("bedtime"), o.string("releaseTime"), o.getValue("enabled").jsonPrimitive.boolean),
                date = o.string("date"),
                timeZoneId = o.string("timeZoneId"),
                plannedStartAt = o.long("plannedStartAt"),
                plannedReleaseAt = o.long("plannedReleaseAt"),
            ),
            requestedAt = s.long("requestedAt"),
            phase = RestrictionPhase.valueOf(s.string("phase")),
            startedAt = s.optionalLong("startedAt"),
            stopRequestedAt = s.optionalLong("stopRequestedAt"),
            releasedAt = s.optionalLong("releasedAt"),
            interruptedAt = s.optionalLong("interruptedAt"),
            endReason = s.optionalString("endReason")?.let(RestrictionEndReason::valueOf),
            intervals = s.getValue("intervals").jsonArray.map {
                ConfirmedRestrictionInterval(it.jsonObject.long("startedAt"), it.jsonObject.optionalLong("endedAt"))
            },
        )
    }

    private fun recordJson(r: SleepRecord) = buildJsonObject {
        put("id", r.id); put("uid", r.uid); put("date", r.date)
        put("bedtime", r.bedtime); put("wakeTime", r.wakeTime)
        put("targetBedtime", r.targetBedtime); put("targetWakeTime", r.targetWakeTime)
        put("targetMinutes", r.targetMinutes); put("durationMinutes", r.durationMinutes)
        put("sleepEfficiency", r.sleepEfficiency); put("achievementRate", r.achievementRate); put("isLockUsed", r.isLockUsed)
    }

    private fun readRecord(r: JsonObject) = SleepRecord(
        id = r.string("id"), uid = r.string("uid"), date = r.string("date"),
        bedtime = r.long("bedtime"), wakeTime = r.long("wakeTime"),
        targetBedtime = r.string("targetBedtime"), targetWakeTime = r.string("targetWakeTime"),
        targetMinutes = r.getValue("targetMinutes").jsonPrimitive.int,
        durationMinutes = r.getValue("durationMinutes").jsonPrimitive.int,
        sleepEfficiency = r.getValue("sleepEfficiency").jsonPrimitive.float,
        achievementRate = r.getValue("achievementRate").jsonPrimitive.float,
        isLockUsed = r.getValue("isLockUsed").jsonPrimitive.boolean,
    )
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.long
    private fun JsonObject.optionalString(key: String) = getValue(key).jsonPrimitive.contentOrNull
    private fun JsonObject.optionalLong(key: String) = getValue(key).takeIf { it != JsonNull }?.jsonPrimitive?.long
}
```

JSON 코드가 data 계층에 있어 domain 모델에 저장 형식 의존성을 넣지 않는다. 이 선택 때문에 직렬화 코드를 직접 유지해야 한다. 별도 DTO + `@Serializable`도 유효한 대안이며, 필드가 늘면 수동 코드의 누락·오타 비용과 비교해야 한다.

`require(version == 1)`은 이관 그 자체가 아니다. v2 형식을 추가하면 v1 원본을 읽는 변환과 이전/손상 데이터 검증이 필요하다. 현재 중복 session ID와 sourceId를 거절한다. 데이터가 손상된 경우 재시도로 자동 복구된다는 보장도 없고, 원본 보존 후 별도 복구 경로가 필요하다.

## 9. 설정 초기화와 기록 삭제의 경계

일반 설정 초기화가 취침 원본 기록과 소유권까지 지우면 사용자가 기대한 범위를 넘는다. 기존 clear 작업에서 새 journal을 보존하도록 했다.

원본: [SleepSettingsRepositoryImpl.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/data/repository/SleepSettingsRepositoryImpl.kt:160)

```kotlin
override suspend fun clear() {
        dataStore.edit { preferences ->
            val journal = preferences[LOCAL_BEDTIME_JOURNAL_KEY]
            preferences.clear()
            if (journal != null) preferences[LOCAL_BEDTIME_JOURNAL_KEY] = journal
        }
    }
}
```

이 API는 설정 초기화에 관한 계약이다. 계정 삭제나 사용자가 명시한 전체 로컬 기록 삭제 정책과 같게 적용해서는 안 된다. V2-13의 데이터 관리에서는 삭제 범위와 소유자를 명확히 정해야 한다.

## 10. 로그인·인터넷 조회가 로컬 진입을 막지 않기

핵심 기능은 한 기기에서 저장한 약속을 사용하는 것이다. 계정 서비스의 응답 시간 전체를 시작 화면의 대기 시간으로 만들지 않는다. 로컬 준비 뒤 원격 관찰과 조회를 시작한다.

원본: [AppRootViewModel.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/app/AppRootViewModel.kt:80)

```kotlin
private var initializationJob: Job? = null
    private var observersStarted = false
    private var authObservationVersion = 0L

    private fun initializeData() {
        if (initializationJob?.isActive == true) return
        initializationJob = viewModelScope.launch {
            _state.update { it.copy(isStateInitialized = false, localInitializationError = null) }
            try {
                val cachedIsPremium = sleepSettingsRepository.isPremium.first()
                val onboardingCompleted = observeOnboardingCompletedUseCase().first()
                localBedtimeRepository.initialize().getOrThrow()
                _state.update { it.copy(isPremium = cachedIsPremium, isOnboardingCompleted = onboardingCompleted, isStateInitialized = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isStateInitialized = true, localInitializationError = "로컬 설정을 읽지 못했어요. 원본은 유지했어요.") }
                return@launch
            }
            // 로컬 진입 완료 뒤 원격 조회를 별도 작업으로 시작한다.
            if (!observersStarted) {
                observersStarted = true
                val initialAuthVersion = authObservationVersion
                setupAuthObserver()
                setupOnboardingObserver()
                viewModelScope.launch {
                    try {
                        val user = withTimeoutOrNull(5_000) { getCurrentUserUseCase() }
                        if (user != null && authObservationVersion == initialAuthVersion && currentUid == null) {
                            handleUserSessionChange(user)
                        }
                        if (user != null && currentUid == user.uid) refreshPremiumStatus()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _state.update { it.copy(error = "계정 연결을 확인하지 못했어요. 이 기기 기능은 계속 사용할 수 있어요.") }
                    }
                }
            }
        }
    }

    private fun setupAuthObserver() {
        viewModelScope.launch {
            observeUserUseCase().catch { error ->
                if (error is CancellationException) throw error
                _state.update { it.copy(error = "계정 연결을 확인하지 못했어요.") }
            }.collect { user ->
                authObservationVersion++
                handleUserSessionChange(user)
            }
        }
    }
```

`initializationJob?.isActive`는 반복 초기화를 막는다. `observersStarted`는 로컬 재시도 뒤 원격 구독이 중복되는 것을 막는다. 저장 오류는 화면 진입 차단의 이유가 될 수 있어 일반 계정 오류와 별도 필드로 표현한다.

`withTimeoutOrNull(5_000)`은 계정 조회의 대기 정책이다. 이 코드에서 로컬 화면을 열기 위해 그 5초를 기다리지 않는다. 취소 가능한 비동기 호출에 적용하는 타임아웃이며 모든 blocking 호출을 강제로 중단하는 수단으로 설명해서는 안 된다.

주요 경합: 오래된 계정 조회가 시작됨 → 인증 관찰에서 로그아웃 null 수신 → 이전 계정 조회가 늦게 완료됨. `authObservationVersion`을 비교해 새 인증 관찰이 있었으면 초기 조회 결과를 적용하지 않는다. 요청 순서가 응답 순서와 같다는 가정을 제거한 것이다. 현재 ViewModel 메인 실행 경로를 전제로 하는 카운터이며 임의의 여러 스레드를 위한 원자적 카운터는 아니다.

원본: [AppRootViewModel.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/app/AppRootViewModel.kt:179)

```kotlin
private fun refreshPremiumStatus() {
        val userId = _state.value.currentUser?.uid ?: return
        val authVersion = authObservationVersion
        viewModelScope.launch {
            val isPremiumNow = syncPremiumStatusUseCase() ?: return@launch
            val currentUser = getCurrentUserUseCase()
            if (authObservationVersion == authVersion && _state.value.currentUser?.uid == userId && currentUser?.uid == userId) {
                _state.update {
                    it.copy(
                        currentUser = currentUser.copy(isPremium = isPremiumNow),
                        isPremium = isPremiumNow,
                    )
                }
            }
        }
    }
```

구독 갱신도 조회 전 계정·관찰 버전과 조회 후 상태를 비교한다. 이 검사는 ViewModel의 늦은 상태 덮어쓰기를 방지한다. 기존 SyncPremiumStatusUseCase 안에서 일어나는 저장·원격 쓰기 전체의 경합을 완전히 해결한 것으로 설명하면 안 된다. 계정별 자격·결제는 이후 단계의 작업이다.

면접 설명: “기기 내 사용과 계정 연결의 실패 범위를 분리하고, 인증 상태가 바뀌었으면 이전 요청의 응답을 버려 로그아웃이 뒤집히지 않도록 했습니다.”

## 11. Compose: 화면 연결과 표시의 분리

Screen은 ViewModel 관찰·편집 열기·저장 요청을 연결한다. Content는 상태와 콜백으로 렌더링한다. UI를 실제 Firebase와 연결하지 않고도 검증할 수 있도록 경계를 나눴다. Content 내부 Material·스크롤 상태까지 모두 없는 완전한 순수 함수라는 의미는 아니다.

원본: [LocalBedtimeScreen.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/bedtime/LocalBedtimeScreen.kt:24)

```kotlin
enum class BedtimeTab(val label: String) { TODAY("오늘"), ROUTINES("루틴"), RECORDS("기록"), MY("마이") }

@Composable
fun LocalBedtimeScreen(
    tab: BedtimeTab,
    contentPadding: PaddingValues,
    onOpenRoutines: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onAccountSettings: () -> Unit = {},
    viewModel: LocalBedtimeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editKind by rememberSaveable { mutableStateOf<String?>(null) }
    LocalBedtimeContent(tab, state, contentPadding, onOpenRoutines, onLogin, { editKind = it.name }, viewModel::reload, modifier, accountName, onAccountSettings)
    editKind?.let { name ->
        val kind = RoutineKind.valueOf(name)
        state.routines.find { it.kind == kind }?.let { routine ->
            RoutineEditor(routine, state.isSaving, state.error, onDismiss = { if (!state.isSaving) { editKind = null; viewModel.dismissMessage() } }, onSave = { bedtime, release, enabled ->
                viewModel.saveRoutine(kind, bedtime, release, enabled) { editKind = null }
            })
        }
    }
}

@Composable
fun LocalBedtimeContent(
    tab: BedtimeTab,
    state: LocalBedtimeUiState,
    contentPadding: PaddingValues,
    onOpenRoutines: () -> Unit,
    onLogin: () -> Unit,
    onEditRoutine: (RoutineKind) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onAccountSettings: () -> Unit = {},
) {
    BedtimePage(contentPadding, modifier) {
        Text("AllSleep", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        Text(when (tab) {
            BedtimeTab.TODAY -> "오늘 밤도,\n내 약속대로."
            BedtimeTab.ROUTINES -> "매일 다시\n결심하지 않도록."
            BedtimeTab.RECORDS -> "확인된 기록을\n차근차근."
            BedtimeTab.MY -> "이 기기에서\n약속을 이어가요."
        }, style = MaterialTheme.typography.headlineMedium)
        if (state.isLoading) {
            BedtimeStatus("설정을 불러오는 중", "이 기기에 저장된 약속을 확인하고 있어요.")
        } else {
            state.error?.let { BedtimeStatus("확인이 필요해요", it, action = { BedtimeTextButton(onRetry) { Text("다시 확인") } }) }
            when (tab) {
                BedtimeTab.TODAY -> {
                    Image(painterResource(Res.drawable.character_phone_away), contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp))
                    state.routines.firstOrNull { it.kind == state.todayKind }?.let { routine ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 취침 약속" else "주말 취침 약속", style = MaterialTheme.typography.labelLarge)
                            Text(routine.bedtime, style = MaterialTheme.typography.displayMedium)
                            Text("해제 ${routine.releaseTime} · ${if (routine.enabled) "사용 설정 켜짐" else "사용 설정 꺼짐"}")
                            Text("설정이 저장됐어요. 자동 제한 준비와 예약은 별도로 확인해야 해요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val active = state.sessions.lastOrNull { !it.isFinished }
                    BedtimeStatus(if (active == null) "제한 실행 준비가 필요해요" else active.phase.label(), if (active == null) "먼저 약속 시간을 확인해 주세요. 실행이 확인되지 않은 상태를 제한 중이라고 표시하지 않아요." else "저장된 회차의 상태예요. 현재 서비스 실행 여부는 별도로 확인해야 해요.")
                    BedtimePrimaryButton(onOpenRoutines, Modifier.fillMaxWidth()) { Text("내 약속 확인") }
                }
                BedtimeTab.ROUTINES -> {
                    Text("평일·주말 약속은 이 기기에 저장돼요. 로그인과 인터넷 연결 없이 편집할 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.routines.forEach { routine ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(if (routine.kind == RoutineKind.WEEKDAY) "평일" else "주말", style = MaterialTheme.typography.titleLarge)
                            Text("${routine.bedtime} → ${routine.releaseTime}", style = MaterialTheme.typography.headlineSmall)
                            Text(if (routine.enabled) "사용 설정 켜짐" else "사용 설정 꺼짐")
                            BedtimePrimaryButton({ onEditRoutine(routine.kind) }, Modifier.fillMaxWidth()) { Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 약속 편집" else "주말 약속 편집") }
                        }
                    }
                    Text("자동 제한을 사용하려면 실행 준비를 확인해 주세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BedtimeTab.RECORDS -> {
                    Text("사용 제한 기록이며 실제 수면 시간과 달라요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.sessions.isEmpty()) {
                        BedtimeStatus("첫 약속부터 시작해 볼까요?", "아직 이 기기에서 확인한 제한 기록이 없어요. 기존 계정의 원본 기록은 별도로 보존돼요.")
                        BedtimePrimaryButton(onOpenRoutines, Modifier.fillMaxWidth()) { Text("내 약속 확인") }
                    } else state.sessions.sortedByDescending { it.requestedAt }.forEach { session ->
                        BedtimeCard(Modifier.fillMaxWidth()) {
                            Text(session.occurrence.date, style = MaterialTheme.typography.titleMedium)
                            Text(session.phase.label())
                            Text("약속 ${session.occurrence.routine.bedtime} → ${session.occurrence.routine.releaseTime}")
                            Text(session.startedAt?.let { "실행 확인 ${formatTime(it, session.occurrence.timeZoneId)}" } ?: "실행이 확인되지 않았어요.")
                            Text("완료된 확인 구간 ${session.confirmedDurationMillis / 60_000}분", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            session.endReason?.let { Text("종료 유형 ${it.label()}") }
                        }
                    }
                }
                BedtimeTab.MY -> {
                    BedtimeStatus(if (accountName == null) "계정 없이 사용 중" else accountName, if (accountName == null) "기본 약속과 로컬 기록을 사용할 수 있어요. 앱 삭제나 기기 변경 전에 기록 보관을 확인해 주세요." else "계정 정보와 이 기기의 로컬 기록 소유권은 구분해요.")
                    BedtimeSettingsRow("취침 약속", onOpenRoutines, Modifier.fillMaxWidth(), "평일·주말 시간 확인")
                    if (accountName == null) BedtimeSettingsRow("로그인", onLogin, Modifier.fillMaxWidth(), "계정 기능이 필요할 때 로그인해요.")
                    else BedtimeSettingsRow("계정과 설정", onAccountSettings, Modifier.fillMaxWidth(), "기존 계정·구독 관리")
                    Text("계정 백업과 기기 연동은 아직 사용할 수 없어요. 로그인만으로 게스트 기록을 자동 병합하지 않아요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}
```

`collectAsState`는 Flow 값을 Compose가 관찰하는 State로 바꾼다. State를 읽는 UI는 값 변경에 따라 재구성된다. 공통 소스에서 플랫폼 공통 API를 택했다. Android 생명주기 자원 관리에는 공식 권장 방식인 `collectAsStateWithLifecycle`을 검토해야 하며, 프로젝트의 CMP 의존성·지원 플랫폼에 맞춰 판단해야 한다. [Compose 공식 상태 문서](https://developer.android.com/develop/ui/compose/state).

records UI는 저장된 phase를 “실행 확인 기록 있음” 등으로 표현한다. 저장된 RUNNING만 보고 현재 서비스가 살아 있다고 단정하지 않는다. 기존 계정 기록을 새 게스트 데이터와 임의 결합하지 않는다.

## 12. 입력 초안의 수명과 성공 뒤 다이얼로그 닫기

편집 중 모든 키 입력을 영구 저장하지 않는다. 입력 초안은 UI가 소유하고, 저장 버튼을 누르면 검증과 영속 저장을 요청한다.

원본: [LocalBedtimeScreen.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/bedtime/LocalBedtimeScreen.kt:131)

```kotlin
@Composable
private fun RoutineEditor(routine: BedtimeRoutine, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var bedtime by rememberSaveable(routine.kind) { mutableStateOf(routine.bedtime) }
    var release by rememberSaveable(routine.kind) { mutableStateOf(routine.releaseTime) }
    var enabled by rememberSaveable(routine.kind) { mutableStateOf(routine.enabled) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (routine.kind == RoutineKind.WEEKDAY) "평일 취침 약속" else "주말 취침 약속") }, text = {
        Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BedtimeTextField(bedtime, { bedtime = it }, "사용 제한 시작", Modifier.fillMaxWidth())
            BedtimeTextField(release, { release = it }, "해제 시각", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("이 약속 사용", Modifier.weight(1f))
                Switch(enabled, { enabled = it }, modifier = Modifier.semantics { contentDescription = "이 약속 사용" }, enabled = !saving)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { BedtimeTextButton({ onSave(bedtime, release, enabled) }, enabled = !saving) { Text(if (saving) "저장 중" else "약속 저장") } }, dismissButton = { BedtimeTextButton(onDismiss, enabled = !saving) { Text("취소") } })
}
```

`rememberSaveable(routine.kind)`는 재구성 및 저장 상태를 통한 Activity 재생성에서 초안을 유지하고, 편집 대상이 바뀌면 초기값을 다시 만든다. 강제 종료·최근 앱에서 제거·재설치 후까지 영구 보관하는 기능은 아니다. 저장 확정 값은 DataStore에 둔다.

현재 입력 칸은 저장 중에도 편집할 수 있고 버튼·스위치는 막는다. 느린 쓰기 동안 입력 변경이 성공 콜백과 엇갈리는 UX는 향후 개선 대상이다. 저장 콜백이 Composable에서 닫히는 상태를 잡고 있으므로 화면을 떠났을 때의 결과 수명도 요구가 늘면 검토해야 한다.

[rememberSaveable의 저장·복원 범위](https://developer.android.com/develop/ui/compose/state#restoring-state)와 함께 읽는다.

## 13. 공통 컴포넌트: Modifier, 슬롯, 키보드·터치 영역

공통 컴포넌트의 형태를 boolean 옵션 수십 개로 늘리는 대신, 루트 Modifier와 content/action/trailingContent 슬롯을 사용했다. 화면이 원하는 여백·너비·내용을 구성할 수 있다.

원본: [BedtimeComponents.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/BedtimeComponents.kt:11)

```kotlin
@Composable
fun BedtimePage(contentPadding: PaddingValues, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding).imePadding(),
        contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        item { Column(verticalArrangement = Arrangement.spacedBy(Spacing.large), content = content) }
    }
}

@Composable
fun BedtimeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium), content = content)
    }
}

@Composable
fun BedtimePrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp), content = content)
}

@Composable
fun BedtimeTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), content = content)
}

@Composable
fun BedtimeSettingsRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, supportingText: String? = null, trailingContent: (@Composable () -> Unit)? = null) {
    Surface(onClick, modifier = modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                supportingText?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
fun BedtimeStatus(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    BedtimeCard(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun BedtimeTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    OutlinedTextField(value, onValueChange, modifier = modifier, label = { Text(label) }, singleLine = true, isError = isError, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = MaterialTheme.colorScheme.secondary, focusedBorderColor = MaterialTheme.colorScheme.secondary, cursorColor = MaterialTheme.colorScheme.secondary))
}
```

호출자가 준 Modifier를 루트에 전달한다. `padding(contentPadding)`은 Scaffold가 계산한 영역을 화면에 적용하고 `imePadding`은 키보드가 차지하는 영역에 대응한다. Modifier 순서는 측정·배치에 영향을 주므로 무작정 추가하지 않는다.

버튼은 고정 높이보다 최소 높이를 사용해 큰 글꼴일 때 늘어날 수 있도록 한다. 키보드·큰 글꼴에서는 스크롤로 내용에 접근한다. Switch에는 의미 있는 contentDescription을 주며 장식 캐릭터는 null로 읽기 순서의 불필요한 항목을 줄였다.

현재 BedtimePage의 LazyColumn은 안에 하나의 큰 Column item을 넣는다. 루틴 두 개에는 간단하지만 기록이 많이 쌓이면 items를 개별 항목으로 구성하고 안정적인 key·목록 조회를 추가해야 한다. “LazyColumn이라 자동으로 모든 기록을 효율적으로 렌더링한다”는 설명은 틀리다.

## 14. 네 탭과 백 스택

공통 Scaffold 한 곳에서 탭을 관리하고 route와 화면을 연결한다. 내비게이션 객체가 없는 NavigationBar 함수도 따로 두어 선택 상태·콜백을 테스트할 수 있게 했다.

원본: [BottomNavScaffold.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/components/BottomNavScaffold.kt:20)

```kotlin
@Composable
fun BottomNavScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) BedtimeNavigationBar(currentDestination?.route, navController::navigateToTab)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues -> content(paddingValues) }
}

@Composable
fun BedtimeNavigationBar(selectedRoute: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(Screen.Home.route, Res.drawable.ic_home, BedtimeTab.TODAY.label),
        Triple(Screen.Alarm.route, Res.drawable.ic_alarm, BedtimeTab.ROUTINES.label),
        Triple(Screen.Stats.route, Res.drawable.ic_analytics, BedtimeTab.RECORDS.label),
        Triple(Screen.Settings.route, Res.drawable.ic_more, BedtimeTab.MY.label),
    )
    NavigationBar(modifier = modifier.padding(bottom = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(painterResource(icon), contentDescription = null) },
                label = { Text(label) },
                selected = selectedRoute == route,
                onClick = { onNavigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
```

원본: [AppNavigation.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/AppNavigation.kt:29)

```kotlin
NavHost(navController = navController, startDestination = startDestination) {
        onboardingGraph(navController)
        listOf(
            Screen.Home.route to BedtimeTab.TODAY,
            Screen.Alarm.route to BedtimeTab.ROUTINES,
            Screen.Stats.route to BedtimeTab.RECORDS,
            Screen.Settings.route to BedtimeTab.MY,
        ).forEach { (route, tab) ->
            composable(route) {
                LocalBedtimeScreen(
                    tab = tab,
                    contentPadding = contentPadding,
                    onOpenRoutines = { navController.navigateToTab(Screen.Alarm.route) },
                    onLogin = { navController.navigate(Screen.Auth.Login.route) { launchSingleTop = true } },
                    accountName = accountName,
                    onAccountSettings = { navController.navigate(Screen.AccountSettings.route) { launchSingleTop = true } },
                )
            }
        }
```

원본: [AppNavigation.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/navigation/AppNavigation.kt:118)

```kotlin
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().route!!) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

`launchSingleTop`은 현재 스택 맨 위와 같은 destination을 중복 추가하지 않는다. 스택 전체에서 같은 화면을 전부 제거하는 기능은 아니다. `popUpTo`는 탭 전환에서 이전 경로가 계속 쌓이지 않게 하고, `saveState`/`restoreState`는 Navigation이 관리하는 화면 상태 복원을 요청한다. 영구 데이터 보존은 여전히 DataStore의 책임이다.

현재 navigation의 기본 파라미터는 기존 Onboarding 경로지만 App에서 startDestination을 Home으로 전달한다. 여기서 강제 온보딩까지 재구현하지 않았고 전체 신규 온보딩은 7단계다.

## 15. 색상·글자 토큰과 광고 제거의 검증

기존 브랜드를 유지하면서 역할에 따라 색과 글자 스타일을 지정했다. 여러 화면에 직접 색상 값을 복사하면 가독성 수정이 누락되기 쉬워 MaterialTheme를 통해 사용한다.

원본: [Typography.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/theme/Typography.kt:8)

```kotlin
val AllSleepTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)
```

원본: [Theme.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonMain/kotlin/com/wngud/allsleep/ui/theme/Theme.kt:72)

```kotlin
fun AllSleepTheme(
    darkTheme: Boolean = true,  // 기본적으로 다크 모드 사용
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AllSleepTypography,
        content = content
    )
}
```

밝은 lavender를 활성 탭·입력 포커스에 쓰고 카드의 주요 글자를 onSurface로 지정했다. 소스의 색 이름만 보고 가독성이 확보됐다고 판단하지 않고 실기기 캡처 후 대비·하단 간격을 조정했다.

앱 오픈 광고는 호출만 막지 않고 SDK 의존성·DI·관리자·초기화·manifest metadata·필수 키 요구를 함께 제거했다. 다음 검사는 실제 설치 APK에 SDK가 남지 않았는지 확인한다.

원본: [BedtimeFoundationUiTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/androidInstrumentedTest/kotlin/com/wngud/allsleep/ui/bedtime/BedtimeFoundationUiTest.kt:51)

```kotlin
@Suppress("DEPRECATION")
    @Test fun installedAppDoesNotContainAdMobSdkOrInitializationMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
        assertFalse(info.metaData?.containsKey("com.google.android.gms.ads.APPLICATION_ID") == true)
        try {
            Class.forName("com.google.android.gms.ads.MobileAds", false, context.classLoader)
            fail("AdMob SDK must not be packaged")
        } catch (_: ClassNotFoundException) {
            // 실제 설치 APK에 광고 초기화 클래스가 없는 정상 결과.
        }
    }
```

개발 코드가 없어 보여도 전이 의존성이나 manifest merge로 SDK가 들어갈 수 있어 설치 결과를 검증했다. 구매 상품 조회·구독자 데이터는 광고 제거와 별개의 범위로 보존했다.

## 16. Fake 저장소가 검증하는 것과 못 하는 것

외부 경계는 Fake로 바꿔 실패·취소·중복 호출을 제어한다. 아래 Fake는 메모리 저장소이며 진짜 디스크 영속성의 증거가 아니다.

원본: [LocalBedtimeRepositoryTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/data/repository/LocalBedtimeRepositoryTest.kt:16)

```kotlin
internal class MemoryPreferencesStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val values = MutableStateFlow(initial)
    var writeError: Exception? = null
    private val mutex = Mutex()
    override val data: Flow<Preferences> = values
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        writeError?.let { throw it }
        transform(values.value).also { values.value = it }
    }
}
```

원본: [LocalBedtimeRepositoryTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/data/repository/LocalBedtimeRepositoryTest.kt:79)

```kotlin
@Test fun concurrentRequestsKeepOneActiveSessionPerOwner() = runTest {
        repository.initialize().getOrThrow()
        val first = async { repository.requestSession("a", occurrence, at) }
        val second = async { repository.requestSession("b", occurrence, at) }
        assertEquals(1, listOf(first.await(), second.await()).count { it.isSuccess })
        assertEquals(1, repository.journal.first().sessions.size)
    }
```

원본: [LocalBedtimeRepositoryTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/data/repository/LocalBedtimeRepositoryTest.kt:97)

```kotlin
@Test fun writeFailureAndCancellationDoNotPublishSuccessfulState() = runTest {
        repository.initialize().getOrThrow()
        val before = dataStore.values.value
        dataStore.writeError = IllegalStateException("disk write failed")
        assertTrue(repository.requestSession("failure", occurrence, at).isFailure)
        assertEquals(before, dataStore.values.value)
        dataStore.writeError = CancellationException("cancelled")
        assertFailsWith<CancellationException> { repository.requestSession("cancelled", occurrence, at) }
        assertEquals(before, dataStore.values.value)
    }
```

Mutex는 Fake의 updateData 경로를 직렬화해 DataStore의 핵심 계약을 흉내 낸다. `runTest`의 두 async 작업은 통제된 스케줄러의 논리적 동시 요청을 검증하며 실제 두 CPU 스레드의 경쟁을 그대로 재현하는 것은 아니다.

## 17. 시간·응답 순서의 회귀 테스트

알람 앱은 자정·시간대·이벤트 지연이 사용자 데이터와 제어 상태를 바꾼다. 이 규칙은 플랫폼 없이 빠르게 시험한다.

원본: [BedtimeSessionTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeSessionTest.kt:13)

```kotlin
@Test fun midnightReleaseUsesNextDateAndKeepsSnapshot() {
        val release = Instant.fromEpochMilliseconds(occurrence.plannedReleaseAt).toLocalDateTime(TimeZone.of("Asia/Seoul"))
        assertEquals(LocalDate(2026, 10, 13), release.date)
        assertEquals(LocalTime(7, 0), release.time)
        assertEquals(450 * 60_000L, occurrence.plannedReleaseAt - start)
        routine.copy(releaseTime = "08:00").occurrence(owner, LocalDate(2026, 10, 12), TimeZone.UTC)
        assertEquals("07:00", occurrence.routine.releaseTime)
        assertEquals("Asia/Seoul", occurrence.timeZoneId)
    }

    @Test fun daylightSavingUsesAbsoluteReleaseTime() {
        val o = routine.occurrence(owner, LocalDate(2026, 10, 31), TimeZone.of("America/New_York"))
        assertEquals(510 * 60_000L, o.plannedReleaseAt - o.plannedStartAt)
    }
```

원본: [BedtimeSessionTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/domain/model/bedtime/BedtimeSessionTest.kt:50)

```kotlin
@Test fun lateStartCannotOverwriteStopRequestOrCompletedRelease() {
        val stopping = session().transition(RestrictionEvent.StopRequested(start + 10, RestrictionEndReason.EMERGENCY))
        assertFailsWith<IllegalArgumentException> { stopping.transition(RestrictionEvent.StartConfirmed(start + 20)) }
        val ended = stopping.transition(RestrictionEvent.ReleaseConfirmed(start + 20))
        assertEquals(0, ended.intervals.size)
        assertEquals(0L, ended.confirmedDurationMillis)
        assertFailsWith<IllegalArgumentException> { ended.transition(RestrictionEvent.StartConfirmed(start + 30)) }
    }
```

원본: [AppRootLocalInitializationTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/app/AppRootLocalInitializationTest.kt:46)

```kotlin
@Test fun lateAccountQueryCannotRestoreAnObservedSignedOutSession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val response = CompletableDeferred<User?>()
        val observedUser = MutableStateFlow<User?>(null)
        val auth = object : AuthRepository by OfflineAuth(hangs = true) {
            override suspend fun getCurrentUser() = response.await()
            override fun observeUser(): Flow<User?> = observedUser
        }
        val vm = createRoot(auth)
        try {
            runCurrent()
            assertTrue(vm.state.value.isStateInitialized)
            response.complete(User("stale-account", null, "Stale account", null, AuthProvider.EMAIL))
            runCurrent()
            assertNull(vm.state.value.currentUser)
            assertNull(vm.state.value.localInitializationError)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
```

CompletableDeferred로 응답 완료 시점을 제어해 “로그아웃 관찰 후 이전 조회 완료” 순서를 만든다. 단순히 성공 값만 확인하는 테스트보다 실제 오류를 찾아낼 수 있다. 실기기 인터넷 차단을 수행한 테스트와 Fake 실패 테스트는 구분한다.

## 18. ViewModel 반복 저장·실패 테스트

ViewModel 테스트는 디스패처를 바꾸고 `runCurrent`로 대기 작업을 실행한다. sleep으로 시간만 기다리지 않아 순서를 재현하기 쉽다.

원본: [LocalBedtimeViewModelTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/bedtime/LocalBedtimeViewModelTest.kt:29)

```kotlin
@Test fun invalidTimesAndWriteFailureDoNotCallSuccessOrChangeSavedRoutine() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = MemoryPreferencesStore()
        val vm = LocalBedtimeViewModel(LocalBedtimeRepositoryImpl(store) { "guest:test" })
        try {
            runCurrent()
            var saved = 0
            vm.saveRoutine(RoutineKind.WEEKDAY, "07:00", "07:00", true) { saved++ }
            assertNotNull(vm.state.value.error)
            assertFalse(vm.state.value.isSaving)
            store.writeError = IllegalStateException("disk full")
            vm.saveRoutine(RoutineKind.WEEKDAY, "22:30", "07:00", true) { saved++ }
            runCurrent()
            assertEquals(0, saved)
            assertEquals("23:00", vm.state.value.routines.first().bedtime)
            assertNotNull(vm.state.value.error)
            assertFalse(vm.state.value.isSaving)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
```

원본: [LocalBedtimeViewModelTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/commonTest/kotlin/com/wngud/allsleep/ui/bedtime/LocalBedtimeViewModelTest.kt:49)

```kotlin
@Test fun fastRepeatedSaveStartsOnlyOneWrite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val backing = LocalBedtimeRepositoryImpl(MemoryPreferencesStore()) { "guest:test" }
        var writes = 0
        val repo = object : LocalBedtimeRepository by backing {
            override suspend fun saveRoutine(routine: BedtimeRoutine): Result<Unit> { writes++; return backing.saveRoutine(routine) }
        }
        val vm = LocalBedtimeViewModel(repo)
        try {
            runCurrent()
            var saved = 0
            repeat(2) { vm.saveRoutine(RoutineKind.WEEKDAY, "22:30", "07:00", true) { saved++ } }
            runCurrent()
            assertEquals(1, writes)
            assertEquals(1, saved)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
```

빠른 두 번 저장은 `launch`가 실제 실행되기 전 호출되도록 만든다. 저장 상태를 launch 안에서만 바꾸면 이 테스트가 두 쓰기를 발견한다. 테스트 후 ViewModel scope를 취소하고 Main dispatcher를 복구해 다른 테스트에 영향을 남기지 않는다.

## 19. 실제 디스크 종료·재열기 테스트

Repository 객체만 새로 만들면 메모리에 데이터가 남았을 수 있다. 실제 DataStore를 종료하고 같은 파일을 새 인스턴스로 연 뒤 값과 상태를 확인한다.

원본: [LocalBedtimeDiskTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/androidUnitTest/kotlin/com/wngud/allsleep/data/repository/LocalBedtimeDiskTest.kt:12)

```kotlin
class LocalBedtimeDiskTest {
    @Test fun actualDataStoreReopensSettingsOwnerSessionsAndConfirmedState() = runTest {
        val directory = Files.createTempDirectory("allsleep-journal-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val firstJob = SupervisorJob()
        val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO), produceFile = { file })
        val firstRepository = LocalBedtimeRepositoryImpl(first) { "guest:persisted" }
        try {
            val owner = firstRepository.initialize().getOrThrow()
            val routine = BedtimeRoutine(RoutineKind.WEEKEND, "00:30", "08:30")
            firstRepository.saveRoutine(routine).getOrThrow()
            val occurrence = routine.occurrence(owner, LocalDate(2026, 10, 11), TimeZone.of("Asia/Seoul"))
            firstRepository.requestSession("one", occurrence, occurrence.plannedStartAt).getOrThrow()
            firstRepository.transitionSession(owner, "one", RestrictionEvent.StartConfirmed(occurrence.plannedStartAt + 100)).getOrThrow()
            firstJob.cancelAndJoin()
            val secondJob = SupervisorJob()
            val second = PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO), produceFile = { file })
            try {
                val reopened = LocalBedtimeRepositoryImpl(second) { error("Must not reset persisted owner") }
                assertEquals(owner, reopened.initialize().getOrThrow())
                assertEquals(routine, reopened.routines.first().last())
                val recovered = reopened.journal.first().sessions.single()
                assertEquals("one", recovered.id)
                assertEquals(RestrictionPhase.RUNNING, recovered.phase)
                assertEquals(occurrence.plannedReleaseAt, recovered.occurrence.plannedReleaseAt)
                assertNull(recovered.releasedAt)
            } finally { secondJob.cancelAndJoin() }
        } finally { firstJob.cancelAndJoin(); directory.deleteRecursively() }
    }
}
```

`cancelAndJoin`으로 이전 DataStore scope 종료를 기다린다. 같은 파일에 두 활성 DataStore를 만들지 않는다. `newOwnerId`를 error 함수로 바꿔 저장된 소유자를 잃고 다시 만드는 코드가 생기면 테스트가 실패하도록 했다. 설정뿐 아니라 RUNNING 상태·회차 ID·절대 해제 시각도 복원했는지 확인한다.

## 20. Compose 계측: 화면·콜백·진짜 저장 결과

화면 검사는 Compose semantics를 통해 수행한다. ComponentActivity 기반 규칙과 합성 UI 상태를 써 실제 계정에 영향 없이 표시와 액션을 확인한다.

원본: [BedtimeFoundationUiTest.kt](/Users/wngud/Workspace/AllSleep/composeApp/src/androidInstrumentedTest/kotlin/com/wngud/allsleep/ui/bedtime/BedtimeFoundationUiTest.kt:110)

```kotlin
@Test fun nativeEditorWritesAndReopensLocalSettingsWithoutLogin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "bedtime-ui-${System.nanoTime()}.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val repository = LocalBedtimeRepositoryImpl(store) { "guest:device-test" }
        lateinit var vm: LocalBedtimeViewModel
        compose.runOnUiThread { vm = LocalBedtimeViewModel(repository) }
        try {
            compose.setContent { AllSleepTheme { Scaffold { padding -> LocalBedtimeScreen(BedtimeTab.ROUTINES, padding, {}, {}, viewModel = vm) } } }
            compose.waitUntil(10_000) { !vm.state.value.isLoading && vm.state.value.routines.size == 2 }
            compose.onNodeWithText("주말 약속 편집").performScrollTo().performClick()
            compose.onNodeWithText("사용 제한 시작").performTextReplacement("00:45")
            compose.onNodeWithText("해제 시각").performTextReplacement("08:15")
            compose.onNodeWithText("약속 저장").assertIsDisplayed()
            capture("routine-editor-keyboard")
            compose.onNodeWithText("약속 저장").performClick()
            compose.waitUntil(10_000) { vm.state.value.routines.last().bedtime == "00:45" && !vm.state.value.isSaving }
            compose.onNodeWithText("00:45 → 08:15").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { vm.viewModelScope.cancel() }
            runBlocking { job.cancelAndJoin() }
            val reopenedJob = SupervisorJob()
            val reopenedStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO), produceFile = { file })
            try {
                runBlocking {
                    val reopened = LocalBedtimeRepositoryImpl(reopenedStore) { error("Must retain owner") }
                    assertEquals(LocalOwner("guest:device-test"), reopened.initialize().getOrThrow())
                    assertEquals("00:45", reopened.routines.first().last().bedtime)
                    assertEquals("23:00", reopened.routines.first().first().bedtime)
                }
            } finally { runBlocking { reopenedJob.cancelAndJoin() } }
        } finally { vm.viewModelScope.cancel(); runBlocking { job.cancelAndJoin() }; file.delete() }
    }
```

이 테스트는 클릭→입력→저장→화면 반영→같은 임시 파일 재열기를 연결한다. 성공 토스트가 표시됐다는 주장보다 실제 저장 결과를 확인한다. 추가 계측은 네 탭과 로그인 콜백, 360dp·fontScale 2f에서 편집 버튼 접근을 검증한다. 시스템 전체 글꼴과 사용자 DataStore를 바꾸지 않는다.

자동 스크린샷은 실제 렌더링 검수 자료다. 현재 기준 이미지와 자동 픽셀 비교를 구성한 스크린샷 회귀 테스트는 아니다. 한 기기 통과로 모든 버전·태블릿·TalkBack 경로가 검증된 것도 아니다. [Compose 테스트 공식 문서](https://developer.android.com/develop/ui/compose/testing).

## 현재 구현을 설명할 때 같이 말해야 하는 개선점

| 항목 | 현재 선택과 다음 판단 |
| --- | --- |
| 데이터 규모 | JSON 전체 읽기·쓰기와 단일 Column 목록이다. 실제 기록 규모가 커지면 개별 lazy item과 Room/부분 조회를 검토한다. |
| 시간 갱신 | todayKind는 저장 Flow 방출 시 계산한다. 앱을 켜 둔 채 자정·시간대가 바뀌는 경우의 자동 갱신은 아직 필요하다. Clock과 TimeZoneProvider 주입도 시간 테스트를 쉽게 한다. |
| UI 관찰 | commonMain의 collectAsState를 사용한다. 사용 중인 CMP Lifecycle API 호환성과 Android 백그라운드 수집 요구를 확인한다. |
| 상태 모델 | 요청·확인 분리는 구현했다. 모든 interval 검증과 실서비스 이벤트 전달·복구는 아직 완료하지 않았다. |
| 저장 UX | 저장 중 이중 요청을 막는다. 입력 칸 잠금, 결과 수명, 취소 finally 복구는 요구에 맞게 보강한다. |
| 원본 복구 | 손상 데이터를 덮어쓰지 않는다. 재시도만으로 복구된다는 의미는 아니며 별도 복구·내보내기 요구가 필요하다. |
| 계정·권한 | 로컬 owner는 데이터 구분이다. 인증·보안 규칙·계정 이관·구독 자격을 대체하지 않는다. |
| 아키텍처 | 단일 모듈·기존 Koin·Navigation 2를 유지했다. 단순함의 이유를 설명하고 필요가 확인될 때만 추가 구조를 도입한다. |

## 면접 답변 예시

“AllSleep의 로그인 없는 로컬 사용 기반을 구현했습니다. 시작 요청과 OS 실행 확인을 구분하는 모델을 만들고, DataStore 트랜잭션 안에서 중복 요청과 진행 회차 충돌을 검사했습니다. 화면은 저장소 Flow에서 파생한 StateFlow를 관찰하며 저장 실패 시 입력을 보존합니다. 자정·시간대, 늦은 응답, 반복 저장, 실제 파일 재열기와 Compose 입력 경로를 검증했습니다. 현재는 1~3단계 기반이며 실제 제한 서비스·예약 연결과 대용량 기록 최적화는 다음 작업입니다.”

기술 이름을 나열하는 것보다 사용자 문제와 실패 시나리오를 먼저 설명한다. 실제로 작업한 범위·미검증 범위를 구분하고, 스스로 이해한 뒤 본인의 말로 바꾼다.

## 직접 해볼 연습

1. 주말 00:30→08:00 저장 과정을 UI→ViewModel→Repository→DataStore→Flow 순서로 손으로 그린다.
2. `isSaving` 변경을 launch 내부로 옮겼을 때 기존 반복 저장 테스트가 왜 실패하는지 예측한다. 별도 연습 브랜치에서 확인하고 복구한다.
3. 저장소의 activeSession 검사를 트랜잭션 밖으로 옮겼을 때 가능한 두 요청 순서를 적는다.
4. 같은 세션 ID 재시도와 같은 밤의 다른 세션을 어떻게 구분하는지 설명한다.
5. start 요청·실행 확인·해제 요청·해제 확인 각각의 시각과 완료 시간 계산을 예제로 적는다.
6. Clock을 주입하도록 바꾸고 금요일→토요일 경계를 고정 시간 테스트로 검증하는 방법을 설계한다.
7. 기록 1,000개가 생겼을 때 현재 렌더링·저장 비용을 측정하고, 먼저 개별 lazy item과 DB 중 무엇을 바꿀지 근거를 적는다.
8. v2 JSON 저장 형식을 추가할 때 v1·손상 데이터·이관 재시도에서 원본을 지키는 테스트를 설계한다.

## 검증 기록과 코드 범위

이번 구현의 단위 테스트 43개(신규 25개)와 실기기 계측 4개가 통과했다. 실제 설치 앱 재실행과 기존 12개 설정 값 보존도 확인했다. 상세 결과는 [구현·검증 기록](/Users/wngud/Workspace/AllSleep/design-previews/bedtime-product-v2/IMPLEMENTATION-V2-01-03.md)에 있다.

이 문서 작성에서는 제품 코드를 변경하거나 테스트를 새로 실행하지 않았다. 링크와 발췌가 현재 소스를 가리키는지만 확인한다. 이후 코드가 바뀌면 문서의 코드 스냅샷도 갱신해야 한다.
