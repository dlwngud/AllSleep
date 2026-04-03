package com.wngud.allsleep.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wngud.allsleep.receiver.SleepAlarmReceiver
import java.util.*

class SleepSchedulerImpl(private val context: Context) : SleepScheduler {

    override fun scheduleNextEvents(
        weekdayBedtime: String,
        weekdayWakeTime: String,
        isWeekdayEnabled: Boolean,
        weekendBedtime: String,
        weekendWakeTime: String,
        isWeekendEnabled: Boolean
    ) {
        Log.d("SleepScheduler", "Scheduling alarms: Weekday($weekdayBedtime/$weekdayWakeTime, $isWeekdayEnabled), Weekend($weekendBedtime/$weekendWakeTime, $isWeekendEnabled)")

        val now = Calendar.getInstance()
        
        // 1. 가장 가까운 다음 평일(월-금) 기상 시각 계산
        val nextWeekdayWake = findNextOccurrence(weekdayWakeTime, setOf(2, 3, 4, 5, 6)) // Calendar.MONDAY=2, ..., FRIDAY=6
        
        // 2. 가장 가까운 다음 주말(토-일) 기상 시각 계산
        val nextWeekendWake = findNextOccurrence(weekendWakeTime, setOf(7, 1)) // Calendar.SATURDAY=7, SUNDAY=1

        // 3. 둘 중 무엇이 더 가까운지 판단하여 루틴 선택
        val useWeekday = when {
            !isWeekdayEnabled -> false
            !isWeekendEnabled -> true
            else -> nextWeekdayWake.before(nextWeekendWake)
        }

        val targetWakeupCalendar: Calendar
        val targetRoutineBedtime: String
        val isTargetEnabled: Boolean

        if (useWeekday) {
            targetWakeupCalendar = nextWeekdayWake
            targetRoutineBedtime = weekdayBedtime
            isTargetEnabled = isWeekdayEnabled
        } else {
            targetWakeupCalendar = nextWeekendWake
            targetRoutineBedtime = weekendBedtime
            isTargetEnabled = isWeekendEnabled
        }

        if (!isTargetEnabled) {
            cancelAll()
            return
        }

        // 4. 결정된 루틴에 따라 알람 예약
        // 4-1. 기상 알람 (ACTION_STOP_SLEEP)
        val wakeTriggerTime = targetWakeupCalendar.timeInMillis
        scheduleAlarm(context, wakeTriggerTime, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)

        // 4-2. 취침 알람 (ACTION_START_SLEEP)
        // 기상일 기준으로 취침 시각 계산. (취침 시각이 기상 시각보다 크면(예: 23시 vs 07시) 전날 밤으로 설정)
        val bedtimeParts = targetRoutineBedtime.split(":")
        val bedHour = bedtimeParts[0].toInt()
        val bedMin = bedtimeParts[1].toInt()
        val wakeHour = targetWakeupCalendar.get(Calendar.HOUR_OF_DAY)
        val wakeMin = targetWakeupCalendar.get(Calendar.MINUTE)

        val bedtimeCalendar = (targetWakeupCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, bedHour)
            set(Calendar.MINUTE, bedMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 취침 시각이 기상 시각보다 수치적으로 나중이라면, 논리적으로는 전날 밤의 사건임
        if (bedHour > wakeHour || (bedHour == wakeHour && bedMin >= wakeMin)) {
            bedtimeCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // 만약 계산된 취침 시간이 이미 과거라면 (예: 이미 밤 11시가 지남), 
        // 이번 세션의 취침 알람은 건너뛰거나(이미 알람 시점이 지남) 
        // 혹은 기상 알람만 활성화된 상태로 둠.
        if (bedtimeCalendar.after(now)) {
            scheduleAlarm(context, bedtimeCalendar.timeInMillis, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        } else {
            Log.d("SleepScheduler", "Next bedtime has already passed, skipping start alarm. Just wake alarm is active.")
            cancelAlarm(context, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        }
    }

    override fun cancelAll() {
        cancelAlarm(context, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        cancelAlarm(context, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)
    }

    /**
     * 특정 시각(timeStr)과 가능한 요일들(days) 중 현재 이후 가장 가까운 시점을 찾음
     */
    private fun findNextOccurrence(timeStr: String, days: Set<Int>): Calendar {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()
        
        // 오늘이 가능 요일이고 시간이 아직 안 지났다면 오늘 리턴
        if (days.contains(calendar.get(Calendar.DAY_OF_WEEK)) && calendar.after(now)) {
            return calendar
        }

        // 아니면 하루씩 더해가며 찾음 (최대 7일)
        for (i in 1..7) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            if (days.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                break
            }
        }
        return calendar
    }

    private fun scheduleAlarm(context: Context, triggerTime: Long, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        Log.d("SleepScheduler", "Alarm set for $action at ${Date(triggerTime)}")
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("SleepScheduler", "Cancelled alarm for $action")
        }
    }
}
