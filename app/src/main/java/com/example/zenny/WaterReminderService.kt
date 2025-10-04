
package com.example.zenny

import android.app.Service
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class WaterReminderService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining: Long = 0
    private var initialInterval: Long = 0 // The full, original duration (e.g., 15 minutes)

    companion object {
        const val ACTION_START_OR_UPDATE_TIMER = "com.example.zenny.action.START_OR_UPDATE_TIMER"
        const val ACTION_STOP_TIMER = "com.example.zenny.action.STOP_TIMER"
        const val EXTRA_INTERVAL = "com.example.zenny.extra.INTERVAL" // The full, original duration
        const val EXTRA_TIME_LEFT = "com.example.zenny.extra.TIME_LEFT" // The time to actually count down from
        const val COUNTDOWN_TICK = "com.example.zenny.countdown_tick"
        const val EXTRA_TIME_REMAINING = "com.example.zenny.extra.TIME_REMAINING"
        const val EXTRA_INITIAL_TIME = "com.example.zenny.extra.INITIAL_TIME"

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OR_UPDATE_TIMER -> {
                // The full duration of the timer (e.g., 15 minutes)
                val interval = intent.getLongExtra(EXTRA_INTERVAL, 0)
                // The actual time left to count down (e.g., 9 minutes)
                // If not provided, it defaults to the full interval (for new timers)
                val timeLeft = intent.getLongExtra(EXTRA_TIME_LEFT, interval)

                if (interval > 0 && timeLeft > 0) {
                    initialInterval = interval // Always store the full, original duration
                    startTimer(timeLeft) // Start counting down from the *actual* time left
                }
            }
            ACTION_STOP_TIMER -> {
                stopTimer()
            }
        }
        return START_STICKY
    }

    private fun startTimer(countdownTime: Long) {
        countDownTimer?.cancel()
        timeRemaining = countdownTime
        countDownTimer = object : CountDownTimer(countdownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                broadcastTick()
            }

            override fun onFinish() {
                // When the timer finishes, broadcast a final tick to show it's done
                timeRemaining = 0
                broadcastTick()
                // And then restart the timer for the *full* duration for the next cycle
                startTimer(initialInterval)
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        // Also broadcast a final tick with 0 to reset the UI instantly
        timeRemaining = 0
        broadcastTick()
        stopSelf()
    }

    private fun broadcastTick() {
        val intent = Intent(COUNTDOWN_TICK).apply {
            putExtra(EXTRA_TIME_REMAINING, timeRemaining)
            // Always broadcast the FULL initial interval for consistent progress calculation
            putExtra(EXTRA_INITIAL_TIME, initialInterval)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
