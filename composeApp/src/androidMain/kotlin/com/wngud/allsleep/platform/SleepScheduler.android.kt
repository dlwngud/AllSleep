package com.wngud.allsleep.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wngud.allsleep.receiver.SleepAlarmReceiver
import java.util.*

actual object SleepScheduler {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun scheduleNextEvents(bedtime: String, wakeTime: String) {
        val context = appContext ?: return
        Log.d("SleepScheduler", "Scheduling alarms: Bedtime=$bedtime, WakeTime=$wakeTime")

        scheduleAlarm(context, bedtime, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        scheduleAlarm(context, wakeTime, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)
    }

    actual fun cancelAll() {
        val context = appContext ?: return
        cancelAlarm(context, SleepAlarmReceiver.ACTION_START_SLEEP, 100)
        cancelAlarm(context, SleepAlarmReceiver.ACTION_STOP_SLEEP, 101)
    }

    private fun scheduleAlarm(context: Context, timeStr: String, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val parts = timeStr.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 시각이 이미 지났다면 내일로 예약
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
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
