package com.wngud.allsleep.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.wngud.allsleep.domain.model.bedtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import java.nio.file.Files
import kotlin.test.*

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
