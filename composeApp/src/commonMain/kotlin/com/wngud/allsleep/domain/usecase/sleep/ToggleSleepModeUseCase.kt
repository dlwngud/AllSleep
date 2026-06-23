package com.wngud.allsleep.domain.usecase.sleep

import com.wngud.allsleep.domain.model.UserSleepState
import com.wngud.allsleep.domain.model.formatTimestampToDate
import com.wngud.allsleep.domain.model.isWeekday
import com.wngud.allsleep.domain.model.platformTimeMillis
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.flow.first

class ToggleSleepModeUseCase(
    private val sleepSettingsRepository: SleepSettingsRepository,
    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase,
    private val recordSleepSessionUseCase: RecordSleepSessionUseCase,
) {
    suspend operator fun invoke(
        uid: String,
        currentState: UserSleepState?,
        isSleeping: Boolean,
        targetWakeUpTime: Long? = null,
    ): Result<UserSleepState> = runCatching {
        val weekdayBedtime = sleepSettingsRepository.weekdayBedtime.first()
        val weekdayWakeTime = sleepSettingsRepository.weekdayWakeTime.first()
        val isWeekdaySleepEnabled = sleepSettingsRepository.isWeekdaySleepEnabled.first()
        val isWeekdayWakeEnabled = sleepSettingsRepository.isWeekdayWakeEnabled.first()

        val weekendBedtime = sleepSettingsRepository.weekendBedtime.first()
        val weekendWakeTime = sleepSettingsRepository.weekendWakeTime.first()
        val isWeekendSleepEnabled = sleepSettingsRepository.isWeekendSleepEnabled.first()
        val isWeekendWakeEnabled = sleepSettingsRepository.isWeekendWakeEnabled.first()

        val now = platformTimeMillis()
        if (isSleeping) {
            sleepSettingsRepository.saveActiveSleepStartAt(now)
        } else {
            saveCompletedSleepRecord(
                uid = uid,
                wakeTimeMs = now,
                weekdayBedtime = weekdayBedtime,
                weekdayWakeTime = weekdayWakeTime,
                weekendBedtime = weekendBedtime,
                weekendWakeTime = weekendWakeTime,
            )
        }

        val optimisticState = currentState?.copy(
            isSleeping = isSleeping,
            targetWakeUpTime = targetWakeUpTime,
            lastUpdatedAt = 0L,
        ) ?: UserSleepState(
            uid = uid,
            isSleeping = isSleeping,
            targetWakeUpTime = targetWakeUpTime,
            weekdayBedtime = weekdayBedtime,
            weekdayWakeTime = weekdayWakeTime,
            isWeekdaySleepEnabled = isWeekdaySleepEnabled,
            isWeekdayWakeEnabled = isWeekdayWakeEnabled,
            weekendBedtime = weekendBedtime,
            weekendWakeTime = weekendWakeTime,
            isWeekendSleepEnabled = isWeekendSleepEnabled,
            isWeekendWakeEnabled = isWeekendWakeEnabled,
        )

        updateUserSleepStateUseCase(
            uid = uid,
            isSleeping = isSleeping,
            targetWakeUpTime = targetWakeUpTime,
            weekdayBedtime = weekdayBedtime,
            weekdayWakeTime = weekdayWakeTime,
            isWeekdaySleepEnabled = isWeekdaySleepEnabled,
            isWeekdayWakeEnabled = isWeekdayWakeEnabled,
            weekendBedtime = weekendBedtime,
            weekendWakeTime = weekendWakeTime,
            isWeekendSleepEnabled = isWeekendSleepEnabled,
            isWeekendWakeEnabled = isWeekendWakeEnabled,
        ).getOrThrow()

        optimisticState
    }

    private suspend fun saveCompletedSleepRecord(
        uid: String,
        wakeTimeMs: Long,
        weekdayBedtime: String,
        weekdayWakeTime: String,
        weekendBedtime: String,
        weekendWakeTime: String,
    ) {
        val sleepStartAt = sleepSettingsRepository.activeSleepStartAt.first()
        if (sleepStartAt <= 0L || wakeTimeMs <= sleepStartAt) return

        val isWeekdaySession = isWeekday(sleepStartAt)
        val targetBedtime = if (isWeekdaySession) weekdayBedtime else weekendBedtime
        val targetWakeTime = if (isWeekdaySession) weekdayWakeTime else weekendWakeTime

        recordSleepSessionUseCase(
            uid = uid,
            date = formatTimestampToDate(wakeTimeMs),
            sleepStartAt = sleepStartAt,
            wakeTimeMs = wakeTimeMs,
            targetBedtime = targetBedtime,
            targetWakeTime = targetWakeTime,
            isLockUsed = true,
        ).onSuccess {
            sleepSettingsRepository.saveActiveSleepStartAt(0L)
        }.onFailure { e ->
            println("[SleepDebug] 수면 기록 저장 실패: ${e.message}")
        }
    }
}
