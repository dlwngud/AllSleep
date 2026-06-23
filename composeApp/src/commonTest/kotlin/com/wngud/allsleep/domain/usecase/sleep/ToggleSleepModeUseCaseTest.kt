package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.DeviceState
import com.wngud.allsleep.domain.model.SleepRecord
import com.wngud.allsleep.domain.model.User
import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.repository.SleepRecordRepository
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import com.wngud.allsleep.domain.repository.SleepSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToggleSleepModeUseCaseTest {
    @Test
    fun start_sleep_mode_updates_remote_state_and_stores_active_start_time() = runTest {
        val settingsRepository = FakeSleepSettingsRepository()
        val syncRepository = FakeSleepSyncRepository()
        val useCase = ToggleSleepModeUseCase(
            sleepSettingsRepository = settingsRepository,
            updateUserSleepStateUseCase = UpdateUserSleepStateUseCase(syncRepository),
            recordSleepSessionUseCase = RecordSleepSessionUseCase(FakeSleepRecordRepository()),
        )

        val result = useCase(
            uid = "user-1",
            currentState = null,
            isSleeping = true,
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isSleeping)
        assertTrue(settingsRepository.savedActiveSleepStartAt > 0L)
        assertEquals(true, syncRepository.lastIsSleeping)
        assertEquals("23:00", syncRepository.lastWeekdayBedtime)
        assertEquals("07:00", syncRepository.lastWeekdayWakeTime)
    }

    private class FakeSleepSettingsRepository : SleepSettingsRepository {
        override val weekdayBedtime: Flow<String> = flowOf("23:00")
        override val weekdayWakeTime: Flow<String> = flowOf("07:00")
        override val isWeekdaySleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekdayWakeEnabled: Flow<Boolean> = flowOf(true)
        override val weekendBedtime: Flow<String> = flowOf("00:00")
        override val weekendWakeTime: Flow<String> = flowOf("09:00")
        override val isWeekendSleepEnabled: Flow<Boolean> = flowOf(true)
        override val isWeekendWakeEnabled: Flow<Boolean> = flowOf(true)
        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val deviceName: Flow<String?> = flowOf(null)
        override val isPremium: Flow<Boolean> = flowOf(false)
        override val activeSleepStartAt: Flow<Long> = flowOf(0L)
        override val lastAppOpenAdShownAt: Flow<Long> = flowOf(0L)

        var savedActiveSleepStartAt: Long = 0L

        override suspend fun saveWeekdaySchedule(bedtime: String, wakeTime: String) = Unit
        override suspend fun saveWeekdaySleepEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekdayWakeEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekendSchedule(bedtime: String, wakeTime: String) = Unit
        override suspend fun saveWeekendSleepEnabled(enabled: Boolean) = Unit
        override suspend fun saveWeekendWakeEnabled(enabled: Boolean) = Unit
        override suspend fun saveOnboardingCompleted(completed: Boolean) = Unit
        override suspend fun saveDeviceName(name: String) = Unit
        override suspend fun savePremiumStatus(isPremium: Boolean) = Unit
        override suspend fun saveActiveSleepStartAt(startTime: Long) {
            savedActiveSleepStartAt = startTime
        }
        override suspend fun saveLastAppOpenAdShownAt(time: Long) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeSleepSyncRepository : SleepSyncRepository {
        var lastIsSleeping: Boolean? = null
        var lastWeekdayBedtime: String? = null
        var lastWeekdayWakeTime: String? = null

        override fun observeUserSleepState(uid: String): Flow<UserSleepState?> = MutableStateFlow(null)

        override suspend fun updateUserSleepState(
            uid: String,
            isSleeping: Boolean?,
            targetWakeUpTime: Long?,
            weekdayBedtime: String?,
            weekdayWakeTime: String?,
            isWeekdaySleepEnabled: Boolean?,
            isWeekdayWakeEnabled: Boolean?,
            weekendBedtime: String?,
            weekendWakeTime: String?,
            isWeekendSleepEnabled: Boolean?,
            isWeekendWakeEnabled: Boolean?,
        ): Result<Unit> {
            lastIsSleeping = isSleeping
            lastWeekdayBedtime = weekdayBedtime
            lastWeekdayWakeTime = weekdayWakeTime
            return Result.success(Unit)
        }

        override suspend fun updateUserProfile(user: User): Result<Unit> = Result.success(Unit)
        override suspend fun registerDevice(uid: String, deviceState: DeviceState): Result<Unit> = Result.success(Unit)
        override suspend fun getRegisteredDevices(uid: String): Result<List<DeviceState>> = Result.success(emptyList())
        override suspend fun setMainAlarmDevice(uid: String, deviceId: String): Result<Unit> = Result.success(Unit)
        override suspend fun unregisterDevice(uid: String, deviceId: String): Result<Unit> = Result.success(Unit)
        override fun observeRegisteredDevices(uid: String): Flow<List<DeviceState>> = flowOf(emptyList())
    }

    private class FakeSleepRecordRepository : SleepRecordRepository {
        override suspend fun saveSleepRecord(record: SleepRecord): Result<Unit> = Result.success(Unit)
        override suspend fun getRecordsByMonth(uid: String, yearMonth: String): Result<List<SleepRecord>> = Result.success(emptyList())
        override suspend fun getRecordsByRange(uid: String, startDate: String, endDate: String): Result<List<SleepRecord>> = Result.success(emptyList())
        override suspend fun getLatestRecord(uid: String): Result<SleepRecord?> = Result.success(null)
    }
}
