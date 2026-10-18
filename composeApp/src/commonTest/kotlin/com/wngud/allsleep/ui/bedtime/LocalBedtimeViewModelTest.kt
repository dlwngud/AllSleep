package com.wngud.allsleep.ui.bedtime

import androidx.lifecycle.viewModelScope
import com.wngud.allsleep.data.repository.*
import com.wngud.allsleep.domain.model.bedtime.*
import com.wngud.allsleep.domain.repository.LocalBedtimeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocalBedtimeViewModelTest {
    @Test fun guestSettingsLoadAndSuccessfulSaveUpdatesLocalState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = LocalBedtimeViewModel(LocalBedtimeRepositoryImpl(MemoryPreferencesStore()) { "guest:test" })
        try {
            runCurrent()
            assertFalse(vm.state.value.isLoading)
            assertEquals(LocalOwner("guest:test"), vm.state.value.owner)
            var saved = 0
            vm.saveRoutine(RoutineKind.WEEKEND, "00:45", "08:15", true) { saved++ }
            runCurrent()
            assertEquals(1, saved)
            assertEquals("00:45", vm.state.value.routines.last().bedtime)
            assertEquals("23:00", vm.state.value.routines.first().bedtime)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

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
