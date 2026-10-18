package com.wngud.allsleep.domain.model.bedtime

import com.wngud.allsleep.domain.model.SleepRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

/** 기기 내 소유권. 게스트 기록을 로그인만으로 다른 계정에 귀속시키지 않는다. */
data class LocalOwner(val id: String) {
    init { require(id.startsWith("guest:") || id.startsWith("account:")); require(id.substringAfter(':').isNotBlank()) }
}

enum class RoutineKind { WEEKDAY, WEEKEND }

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

/** 이전 값은 원본으로만 보존한다. 실행 확인·임시 사용·수면 상태를 추정하지 않는다. */
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
