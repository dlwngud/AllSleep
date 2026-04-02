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
        bedtime: String,
        wakeTime: String,
        sleepDays: Set<Int>,
        wakeDays: Set<Int>
    ) {
        Log.d("SleepScheduler", "Scheduling alarms: Bedtime=$bedtime ($sleepDays), WakeTime=$wakeTime ($wakeDays)")

        scheduleAlarm(context, bedtime, sleepDays, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        scheduleAlarm(context, wakeTime, wakeDays, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)
    }

    override fun cancelAll() {
        cancelAlarm(context, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        cancelAlarm(context, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)
    }

    private fun scheduleAlarm(context: Context, timeStr: String, selectedDays: Set<Int>, action: String, requestCode: Int) {
        if (selectedDays.isEmpty()) {
            cancelAlarm(context, action, requestCode)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val parts = timeStr.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 선택된 요일에 맞게 다음 알람 시각 계산
        val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK) - 1 // 0=일, 1=월, ..., 6=토
        
        if (!(selectedDays.contains(todayDayOfWeek) && calendar.after(now))) {
            // 오늘이 아니거나 이미 시간이 지났다면, 다음 유효한 요일을 찾을 때까지 하루씩 더함
            for (i in 1..7) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
                if (selectedDays.contains(dayOfWeek)) {
                    break
                }
            }
        }

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
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        Log.d("SleepScheduler", "Alarm set for $action at ${calendar.time}")
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
        }
    }
}
