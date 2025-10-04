package com.example.zenny

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create and show the notification
        val notification = NotificationCompat.Builder(context, "HYDRATION_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle("Time to Hydrate!")
            .setContentText("Don't forget to drink some water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // --- Re-schedule the next alarm ---
        val prefs = context.getSharedPreferences("HydrationPrefs", Context.MODE_PRIVATE)
        val remindersEnabled = prefs.getBoolean("reminders_enabled", true)

        val interval = intent.getLongExtra("EXTRA_INTERVAL", 0)
        if (interval > 0 && remindersEnabled) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra("EXTRA_INTERVAL", interval)
            }

            // ** THE FIX: Use the SAME request code as the Fragment to ensure we are updating the same alarm **
            val pendingIntent = PendingIntent.getBroadcast(
                context, HydrationFragment.Companion.REQUEST_CODE_ALARM, nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val triggerTime = System.currentTimeMillis() + interval

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
