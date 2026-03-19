package com.wngud.allsleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wngud.allsleep.domain.usecase.auth.GetCurrentUserUseCase
import com.wngud.allsleep.domain.usecase.sleep.UpdateUserSleepStateUseCase
import com.wngud.allsleep.domain.repository.SleepSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SleepAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val updateUserSleepStateUseCase: UpdateUserSleepStateUseCase by inject()
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val sleepSettingsRepository: SleepSettingsRepository by inject()
    private val sleepScheduler: com.wngud.allsleep.platform.SleepScheduler by inject()

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SleepAlarmReceiver", "Received Alarm Action: $action")

        receiverScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                Log.w("SleepAlarmReceiver", "No user logged in, skipping schedule update")
                return@launch
            }

            when (action) {
                ACTION_START_SLEEP -> {
                    Log.d("SleepAlarmReceiver", "Auto-starting sleep mode for user: ${user.uid}")
                    updateUserSleepStateUseCase(
                        uid = user.uid,
                        isSleeping = true,
                        targetWakeUpTime = calculateWakeUpTime()
                    )
                }
                ACTION_STOP_SLEEP -> {
                    Log.d("SleepAlarmReceiver", "Auto-stopping sleep mode for user: ${user.uid}")
                    updateUserSleepStateUseCase(
                        uid = user.uid,
                        isSleeping = false,
                        targetWakeUpTime = null
                    )
                }
            }
            
            // 다음 알람 재스케줄링 (24시간 주기 순환을 위해)
            val bedtime = sleepSettingsRepository.bedtime.first()
            val wakeTime = sleepSettingsRepository.wakeTime.first()
            sleepScheduler.scheduleNextEvents(bedtime, wakeTime)
        }
    }

    private suspend fun calculateWakeUpTime(): Long {
        // 현재 설정된 기상 시각(HH:mm)을 바탕으로 오늘 혹은 내일의 타임스탬프 계산
        val wakeTimeStr = sleepSettingsRepository.wakeTime.first()
        val parts = wakeTimeStr.split(":")
        if (parts.size != 2) return System.currentTimeMillis() + 8 * 3600 * 1000

        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    companion object {
        const val ACTION_START_SLEEP = "com.wngud.allsleep.ACTION_START_SLEEP"
        const val ACTION_STOP_SLEEP = "com.wngud.allsleep.ACTION_STOP_SLEEP"
    }
}
