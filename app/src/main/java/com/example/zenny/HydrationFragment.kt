package com.example.zenny

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Calendar

class HydrationFragment : Fragment() {

    private lateinit var halfProgress: HalfCircleProgress
    private lateinit var progressTextView: TextView
    private lateinit var plusButton: ImageButton
    private lateinit var centerButton: ImageButton
    private lateinit var countdownTextView: TextView
    private lateinit var reminderSpinner: Spinner

    private var countDownTimer: CountDownTimer? = null

    // State variables for tracking hydration
    private var dailyGoal: Int = 0
    private var currentIntake: Int = 0
    private var lastAddedAmount: Int = 0 // Default glass size is now 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hydration, container, false)
        initializeViews(view)
        setupListeners()
        setupReminderSpinner()
        createNotificationChannel()
        updateIntakeUI() // Initial UI update
        return view
    }

    private fun initializeViews(view: View) {
        halfProgress = view.findViewById(R.id.half_progress)
        progressTextView = view.findViewById(R.id.progress_text)
        plusButton = view.findViewById(R.id.plusButton)
        centerButton = view.findViewById(R.id.centerButton)
        countdownTextView = view.findViewById(R.id.countdown_text)
        reminderSpinner = view.findViewById(R.id.reminder_spinner)
    }

    private fun setupListeners() {
        plusButton.setOnClickListener {
            showAddWaterDialog()
        }

        centerButton.setOnClickListener {
            if (lastAddedAmount <= 0) {
                Toast.makeText(requireContext(), "Please set a glass size first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentIntake >= dailyGoal) {
                Toast.makeText(requireContext(), "Congratulations! You've reached your daily goal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentIntake += lastAddedAmount
            currentIntake = currentIntake.coerceAtMost(dailyGoal)
            updateIntakeUI()
        }
    }

    private fun setupReminderSpinner() {
        val reminderOptions = mapOf(
            "Off" to 0L,
            "10 Seconds (Test)" to 10 * 1000L,
            "15 minutes" to 15 * 60 * 1000L,
            "30 minutes" to 30 * 60 * 1000L,
            "45 minutes" to 45 * 60 * 1000L,
            "1 hour" to 60 * 60 * 1000L,
            "1 hour 30 minutes" to 90 * 60 * 1000L,
            "2 hours" to 120 * 60 * 1000L
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reminderOptions.keys.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reminderSpinner.adapter = adapter

        reminderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateDailyGoal()
                val selectedInterval = reminderOptions.values.toList()[position]
                if (selectedInterval > 0) {
                    checkPermissionsAndStartCountdown(selectedInterval)
                } else {
                    cancelCountdown()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showAddWaterDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_water, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val waterAmountEditText = dialogView.findViewById<EditText>(R.id.waterAmountEditText)
        val add100mlButton = dialogView.findViewById<Button>(R.id.add100mlButton)
        val add200mlButton = dialogView.findViewById<Button>(R.id.add200mlButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        waterAmountEditText.setText(lastAddedAmount.toString())

        add100mlButton.setOnClickListener {
            val current = waterAmountEditText.text.toString().toIntOrNull() ?: 0
            waterAmountEditText.setText((current + 100).toString())
        }
        add200mlButton.setOnClickListener {
            val current = waterAmountEditText.text.toString().toIntOrNull() ?: 0
            waterAmountEditText.setText((current + 200).toString())
        }

        addButton.setOnClickListener {
            val amount = waterAmountEditText.text.toString().toIntOrNull()
            if (amount != null && amount > 0) {
                lastAddedAmount = amount
                calculateDailyGoal()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun calculateDailyGoal() {
        val selectedPosition = reminderSpinner.selectedItemPosition
        if (selectedPosition < 0 || lastAddedAmount <= 0) {
            dailyGoal = 0
            currentIntake = 0
            updateIntakeUI()
            return
        }

        val reminderOptions = mapOf(
            "Off" to 0L,
            "10 Seconds (Test)" to 10 * 1000L,
            "15 minutes" to 15 * 60 * 1000L,
            "30 minutes" to 30 * 60 * 1000L,
            "45 minutes" to 45 * 60 * 1000L,
            "1 hour" to 60 * 60 * 1000L,
            "1 hour 30 minutes" to 90 * 60 * 1000L,
            "2 hours" to 120 * 60 * 1000L
        )
        val selectedInterval = reminderOptions.values.toList()[selectedPosition]

        if (selectedInterval > 0) {
            val wakingHoursInMillis = 16 * 60 * 60 * 1000L
            val drinksPerDay = wakingHoursInMillis / selectedInterval
            dailyGoal = (drinksPerDay * lastAddedAmount).toInt()
        } else {
            dailyGoal = 0
        }
        currentIntake = 0
        updateIntakeUI()
    }


    private fun updateIntakeUI() {
        progressTextView.text = "$currentIntake/${dailyGoal}ml"
        val progressPercentage = if (dailyGoal > 0) {
            (currentIntake.toFloat() / dailyGoal.toFloat() * 100).toInt()
        } else {
            0
        }
        halfProgress.setProgress(progressPercentage)
    }

    private fun checkPermissionsAndStartCountdown(interval: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startCountdown(interval)
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startCountdown(interval)
        }
    }

    private fun startCountdown(milliseconds: Long) {
        cancelCountdown()
        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                countdownTextView.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            }

            override fun onFinish() {
                countdownTextView.text = "Time's Up!"
                scheduleNotification(milliseconds)
            }
        }.start()
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        countdownTextView.text = "Count Down"
    }

    private fun scheduleNotification(interval: Long) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = Calendar.getInstance().timeInMillis + interval

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hydration Reminders"
            val descriptionText = "Notifications to remind you to drink water"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("HYDRATION_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HydrationFragment()
    }
}
