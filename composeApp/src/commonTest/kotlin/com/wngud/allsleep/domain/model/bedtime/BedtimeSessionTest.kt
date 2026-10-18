package com.wngud.allsleep.domain.model.bedtime

import kotlinx.datetime.*
import kotlin.test.*

class BedtimeSessionTest {
    private val owner = LocalOwner("guest:test")
    private val routine = BedtimeRoutine(RoutineKind.WEEKDAY, "23:30", "07:00")
    private val occurrence = routine.occurrence(owner, LocalDate(2026, 10, 12), TimeZone.of("Asia/Seoul"))
    private val start = occurrence.plannedStartAt
    private fun session() = BedtimeSession("session-1", occurrence, start)

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

    @Test fun equalOrInvalidTimeCannotCreateOccurrence() {
        listOf("07:00" to "07:00", "24:00" to "07:00", "9:00" to "07:00").forEach { (bed, release) ->
            assertFailsWith<IllegalArgumentException> { routine.copy(bedtime = bed, releaseTime = release).occurrence(owner, LocalDate(2026, 10, 12), TimeZone.UTC) }
        }
    }

    @Test fun requestDoesNotClaimExecutionOrElapsedTime() {
        assertEquals(RestrictionPhase.REQUESTED, session().phase)
        assertNull(session().startedAt)
        assertEquals(0L, session().confirmedDurationMillis)
    }

    @Test fun confirmedStartAndReleaseRecordOnlyConfirmedInterval() {
        val running = session().transition(RestrictionEvent.StartConfirmed(start + 8 * 60_000))
        assertEquals(0L, running.confirmedDurationMillis)
        val stopping = running.transition(RestrictionEvent.StopRequested(occurrence.plannedReleaseAt, RestrictionEndReason.SCHEDULED))
        assertNull(stopping.releasedAt)
        val ended = stopping.transition(RestrictionEvent.ReleaseConfirmed(occurrence.plannedReleaseAt))
        assertEquals(442 * 60_000L, ended.confirmedDurationMillis)
        assertTrue(ended.isFinished)
    }

    @Test fun lateStartCannotOverwriteStopRequestOrCompletedRelease() {
        val stopping = session().transition(RestrictionEvent.StopRequested(start + 10, RestrictionEndReason.EMERGENCY))
        assertFailsWith<IllegalArgumentException> { stopping.transition(RestrictionEvent.StartConfirmed(start + 20)) }
        val ended = stopping.transition(RestrictionEvent.ReleaseConfirmed(start + 20))
        assertEquals(0, ended.intervals.size)
        assertEquals(0L, ended.confirmedDurationMillis)
        assertFailsWith<IllegalArgumentException> { ended.transition(RestrictionEvent.StartConfirmed(start + 30)) }
    }

    @Test fun interruptionClosesObservedIntervalAndLaterReleaseExcludesGap() {
        val interrupted = session().transition(RestrictionEvent.StartConfirmed(start + 10))
            .transition(RestrictionEvent.Interrupted(start + 100))
        assertEquals(90L, interrupted.confirmedDurationMillis)
        val released = interrupted.transition(RestrictionEvent.StopRequested(start + 1000, RestrictionEndReason.TECHNICAL))
            .transition(RestrictionEvent.ReleaseConfirmed(start + 2000))
        assertEquals(90L, released.confirmedDurationMillis)
    }

    @Test fun duplicateConfirmationsAreIdempotentButEarlierEventsAreRejected() {
        val running = session().transition(RestrictionEvent.StartConfirmed(start + 10))
        assertEquals(running, running.transition(RestrictionEvent.StartConfirmed(start + 10)))
        assertFailsWith<IllegalArgumentException> { running.transition(RestrictionEvent.Interrupted(start + 9)) }
        assertFailsWith<IllegalArgumentException> { running.transition(RestrictionEvent.ReleaseConfirmed(start + 20)) }
    }
}
