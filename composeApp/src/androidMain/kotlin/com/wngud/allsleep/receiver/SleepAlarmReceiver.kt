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

            val wdB = sleepSettingsRepository.weekdayBedtime.first()
            val wdW = sleepSettingsRepository.weekdayWakeTime.first()
            val wdSE = sleepSettingsRepository.isWeekdaySleepEnabled.first()
            val wdWE = sleepSettingsRepository.isWeekdayWakeEnabled.first()
            
            val weB = sleepSettingsRepository.weekendBedtime.first()
            val weW = sleepSettingsRepository.weekendWakeTime.first()
            val weSE = sleepSettingsRepository.isWeekendSleepEnabled.first()
            val weWE = sleepSettingsRepository.isWeekendWakeEnabled.first()

            when (action) {
                ACTION_START_SLEEP -> {
                    Log.d("SleepAlarmReceiver", "Auto-starting sleep mode for user: ${user.uid}")
                    val now = System.currentTimeMillis()
                    sleepSettingsRepository.saveActiveSleepStartAt(now)
                    updateUserSleepStateUseCase(
                        uid = user.uid,
                        isSleeping = true,
                        targetWakeUpTime = calculateWakeUpTime(wdW, weW),
                        weekdayBedtime = wdB,
                        weekdayWakeTime = wdW,
                        isWeekdaySleepEnabled = wdSE,
                        isWeekdayWakeEnabled = wdWE,
                        weekendBedtime = weB,
                        weekendWakeTime = weW,
                        isWeekendSleepEnabled = weSE,
                        isWeekendWakeEnabled = weWE
                    )
                }
                ACTION_STOP_SLEEP -> {
                    Log.d("SleepAlarmReceiver", "Auto-stopping sleep mode for user: ${user.uid}")
                    updateUserSleepStateUseCase(
                        uid = user.uid,
                        isSleeping = false,
                        targetWakeUpTime = null,
                        weekdayBedtime = wdB,
                        weekdayWakeTime = wdW,
                        isWeekdaySleepEnabled = wdSE,
                        isWeekdayWakeEnabled = wdWE,
                        weekendBedtime = weB,
                        weekendWakeTime = weW,
                        isWeekendSleepEnabled = weSE,
                        isWeekendWakeEnabled = weWE
                    )
                }
            }
            
            // 다음 알람 재스케줄링 (24시간 주기 순환을 위해)
            sleepScheduler.scheduleNextEvents(wdB, wdW, wdSE, weB, weW, weSE)
        }
    }

    private fun calculateWakeUpTime(wdW: String, weW: String): Long {
        val calendar = java.util.Calendar.getInstance()
        
        // 정오 이후라면 기상일은 내일로 간주
        if (calendar.get(java.util.Calendar.HOUR_OF_DAY) >= 12) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        // 차기 기상일이 평일(월-금)인지 주말(토-일)인지 판단
        val isNextWakeWeekday = dayOfWeek !in listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)
        val wakeTimeStr = if (isNextWakeWeekday) wdW else weW
        
        val parts = wakeTimeStr.split(":")
        if (parts.size != 2) return System.currentTimeMillis() + 8 * 3600 * 1000

        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // 이미 지난 시각이면(예: 새벽 1시인데 기상 타겟이 어제 오전 7시로 잡히는 경우) 내일로 보정
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        return calendar.timeInMillis
    }

    companion object {
        const val ACTION_START_SLEEP = "com.wngud.allsleep.ACTION_START_SLEEP"
        const val ACTION_STOP_SLEEP = "com.wngud.allsleep.ACTION_STOP_SLEEP"
    }
}
