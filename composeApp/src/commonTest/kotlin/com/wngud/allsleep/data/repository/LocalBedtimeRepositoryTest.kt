package com.wngud.allsleep.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.bedtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

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

class LocalBedtimeRepositoryTest {
    private val dataStore = MemoryPreferencesStore()
    private val repository = LocalBedtimeRepositoryImpl(dataStore) { "guest:installation" }
    private val owner = LocalOwner("guest:installation")
    private val routine = BedtimeRoutine(RoutineKind.WEEKDAY, "23:30", "07:00")
    private val occurrence = routine.occurrence(owner, LocalDate(2026, 10, 12), TimeZone.of("Asia/Seoul"))
    private val at = occurrence.plannedStartAt

    @Test fun initializeRetainsExistingSettingsPremiumAndUnknownLegacyOwnership() = runTest {
        val before = preferencesOf(stringPreferencesKey("bedtime") to "22:40", stringPreferencesKey("weekend_wake_time") to "08:45", booleanPreferencesKey("is_premium") to true, booleanPreferencesKey("onboarding_completed") to true, longPreferencesKey("active_sleep_start_at") to 1234L)
        val store = MemoryPreferencesStore(before)
        val repo = LocalBedtimeRepositoryImpl(store) { "guest:preserved" }
        val first = repo.initialize().getOrThrow()
        assertEquals(first, repo.initialize().getOrThrow())
        before.asMap().forEach { (key, value) -> assertEquals(value, store.values.value.asMap()[key]) }
        val journal = repo.journal.first()
        assertTrue(journal.sessions.isEmpty())
        assertNull(journal.legacyRecords.single().owner)
        assertEquals(1234L, journal.legacyRecords.single().activeStartAt)
        assertEquals("22:40", repo.routines.first().first().bedtime)
    }

    @Test fun settingsCanBeRepairedWithoutDestroyingInvalidLegacyValuesOnRead() = runTest {
        val store = MemoryPreferencesStore(preferencesOf(stringPreferencesKey("bedtime") to "07:00", stringPreferencesKey("wake_time") to "07:00"))
        val repo = LocalBedtimeRepositoryImpl(store) { "guest:repair" }
        repo.initialize().getOrThrow()
        assertEquals("07:00", repo.routines.first().first().bedtime)
        assertTrue(repo.saveRoutine(routine).isSuccess)
        assertEquals("23:30", repo.routines.first().first().bedtime)
    }

    @Test fun sameNightMultipleSessionsSurviveRepositoryRecreation() = runTest {
        repository.initialize().getOrThrow()
        repository.requestSession("first", occurrence, at).getOrThrow()
        repository.transitionSession(owner, "first", RestrictionEvent.StopRequested(at + 10, RestrictionEndReason.EARLY)).getOrThrow()
        repository.transitionSession(owner, "first", RestrictionEvent.ReleaseConfirmed(at + 20)).getOrThrow()
        repository.requestSession("second", occurrence, at + 30).getOrThrow()
        val reopened = LocalBedtimeRepositoryImpl(dataStore) { error("Must reuse stored owner") }
        assertEquals(owner, reopened.initialize().getOrThrow())
        assertEquals(listOf("first", "second"), reopened.journal.first().sessions.map { it.id })
    }

    @Test fun duplicateRequestsAreIdempotentAndDoNotResurrectCompletedSessions() = runTest {
        repository.initialize().getOrThrow()
        repository.requestSession("one", occurrence, at).getOrThrow()
        repository.transitionSession(owner, "one", RestrictionEvent.StopRequested(at + 1, RestrictionEndReason.EMERGENCY)).getOrThrow()
        val ended = repository.transitionSession(owner, "one", RestrictionEvent.ReleaseConfirmed(at + 2)).getOrThrow()
        assertEquals(ended, repository.requestSession("one", occurrence, at).getOrThrow())
        assertEquals(1, repository.journal.first().sessions.size)
        assertTrue(repository.requestSession("one", occurrence, at + 100).isFailure)
    }

    @Test fun concurrentRequestsKeepOneActiveSessionPerOwner() = runTest {
        repository.initialize().getOrThrow()
        val first = async { repository.requestSession("a", occurrence, at) }
        val second = async { repository.requestSession("b", occurrence, at) }
        assertEquals(1, listOf(first.await(), second.await()).count { it.isSuccess })
        assertEquals(1, repository.journal.first().sessions.size)
    }

    @Test fun ownersRemainSeparateAndCannotMutateEachOthersRecord() = runTest {
        repository.initialize().getOrThrow()
        val account = LocalOwner("account:other")
        repository.requestSession("guest", occurrence, at).getOrThrow()
        repository.requestSession("account", occurrence.copy(owner = account), at).getOrThrow()
        assertTrue(repository.transitionSession(account, "guest", RestrictionEvent.StartConfirmed(at)).isFailure)
        assertEquals(listOf("guest"), repository.journal.first().sessionsFor(owner).map { it.id })
        assertEquals(listOf("account"), repository.journal.first().sessionsFor(account).map { it.id })
    }

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

    @Test fun corruptedOrFutureDataIsNotOverwrittenByInitializationOrWrites() = runTest {
        for (content in listOf("{broken", "{\"version\":99}")) {
            val store = MemoryPreferencesStore(preferencesOf(LOCAL_BEDTIME_JOURNAL_KEY to content))
            val repo = LocalBedtimeRepositoryImpl(store) { "guest:should-not-create" }
            assertTrue(repo.initialize().isFailure)
            assertTrue(repo.requestSession("bad", occurrence, at).isFailure)
            assertEquals(content, store.values.value[LOCAL_BEDTIME_JOURNAL_KEY])
        }
    }

    @Test fun legacyImportPreservesEveryOriginalFieldWithoutInventingExecution() = runTest {
        repository.initialize().getOrThrow()
        val original = SleepRecord("2026-10-01", "uid", "2026-10-01", 100L, 200L, "23:30", "07:00", 450, 430, 95.5f, 96.2f, true)
        val legacy = LegacyRestrictionRecord("cloud:uid:2026-10-01", LocalOwner("account:uid"), originalRecord = original)
        repository.preserveLegacyRecord(legacy).getOrThrow()
        repository.preserveLegacyRecord(legacy).getOrThrow()
        assertEquals(original, repository.journal.first().legacyRecords.single().originalRecord)
        assertTrue(repository.journal.first().sessions.isEmpty())
        assertTrue(repository.preserveLegacyRecord(legacy.copy(originalRecord = original.copy(bedtime = 999L))).isFailure)
    }

    @Test fun routineWriteIsAtomicAndPreservesWeekendAndOriginalJournal() = runTest {
        repository.initialize().getOrThrow()
        val before = repository.journal.first()
        val weekend = repository.routines.first().last()
        assertTrue(repository.saveRoutine(routine).isSuccess)
        assertEquals(routine, repository.routines.first().first())
        assertEquals(weekend, repository.routines.first().last())
        assertEquals(before, repository.journal.first())
        assertTrue(repository.saveRoutine(routine.copy(releaseTime = "23:30")).isFailure)
        SleepSettingsRepositoryImpl(dataStore).clear()
        assertEquals(before, repository.journal.first())
    }
}
