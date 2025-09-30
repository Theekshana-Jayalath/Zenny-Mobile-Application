package com.example.zenny

import android.app.TimePickerDialog
import android.content.Intent // Make sure this is imported
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.ui.semantics.setText
//import androidx.compose.ui.semantics.text
import com.google.android.material.bottomnavigation.BottomNavigationView // Make sure this is imported
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class activity_home : AppCompatActivity() {

    private lateinit var habitsContainer: LinearLayout
    private lateinit var addHabitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentageText: TextView
    private lateinit var bottomNav: BottomNavigationView // NEW: Add variable for bottom navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- Initialization of your existing views ---
        habitsContainer = findViewById(R.id.habitsContainer)
        addHabitButton = findViewById(R.id.btn_add_box)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentageText = findViewById(R.id.tv_progress_percentage)

        // Set the click listener for the add habit button
        addHabitButton.setOnClickListener {
            showAddHabitDialog()
        }

        // Initial update for the progress bar
        updateProgress()
    }

    /**
     * Calculates and updates the progress bar and percentage text.
     */
    private fun updateProgress() {
        val totalHabits = habitsContainer.childCount
        if (totalHabits == 0) {
            progressBar.progress = 0
            progressPercentageText.text = "0%"
            return
        }

        var completedHabits = 0
        for (i in 0 until totalHabits) {
            val habitView = habitsContainer.getChildAt(i)
            val checkBox = habitView.findViewById<CheckBox>(R.id.habitCheckBox)
            if (checkBox.isChecked) {
                completedHabits++
            }
        }

        val progress = (completedHabits * 100) / totalHabits
        progressBar.progress = progress
        progressPercentageText.text = "$progress%"
    }

    /**
     * Shows a dialog to ADD a new habit.
     */
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_add_habit, null)
        val etHabitName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val btnChooseTime = dialogView.findViewById<Button>(R.id.btnChooseTime)

        var selectedTimeFormatted = ""

        btnChooseTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                selectedTimeFormatted = timeFormat.format(selectedCalendar.time)
                btnChooseTime.text = selectedTimeFormatted
            }, hour, minute, false)
            timePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Habit")
            .setPositiveButton("Add") { _, _ ->
                val habitName = etHabitName.text.toString().trim()
                if (habitName.isNotEmpty()) {
                    val habitTime = if (selectedTimeFormatted.isNotEmpty()) selectedTimeFormatted else "No time set"
                    addHabitView(habitName, habitTime)
                } else {
                    Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows a dialog to EDIT an existing habit.
     */
    private fun showEditHabitDialog(habitNameTextView: TextView, habitTimeTextView: TextView) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_add_habit, null)
        val etHabitName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val btnChooseTime = dialogView.findViewById<Button>(R.id.btnChooseTime)

        etHabitName.setText(habitNameTextView.text)
        val initialTime = habitTimeTextView.text.toString()
        var selectedTimeFormatted = if (initialTime != "No time set") initialTime else ""
        btnChooseTime.text = if (selectedTimeFormatted.isNotEmpty()) selectedTimeFormatted else "Choose Time"

        btnChooseTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                selectedTimeFormatted = timeFormat.format(selectedCalendar.time)
                btnChooseTime.text = selectedTimeFormatted
            }, hour, minute, false)
            timePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Habit")
            .setPositiveButton("Save") { _, _ ->
                val updatedHabitName = etHabitName.text.toString().trim()
                if (updatedHabitName.isNotEmpty()) {
                    habitNameTextView.text = updatedHabitName
                    habitTimeTextView.text = if (selectedTimeFormatted.isNotEmpty()) selectedTimeFormatted else "No time set"
                    Toast.makeText(this, "Habit updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Inflates the habit item layout and adds it to the container.
     */
    private fun addHabitView(habitName: String, habitTime: String) {
        val habitView = LayoutInflater.from(this).inflate(R.layout.activity_list_item_habit, habitsContainer, false)
        val habitNameTextView = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val habitTimeTextView = habitView.findViewById<TextView>(R.id.habitTimeTextView)
        val deleteIcon = habitView.findViewById<ImageView>(R.id.deleteHabitIcon)
        val editIcon = habitView.findViewById<ImageView>(R.id.editHabitIcon)
        val checkBox = habitView.findViewById<CheckBox>(R.id.habitCheckBox)

        habitNameTextView.text = habitName
        habitTimeTextView.text = habitTime

        checkBox.setOnCheckedChangeListener { _, _ ->
            updateProgress()
        }

        deleteIcon.setOnClickListener {
            habitsContainer.removeView(habitView)
            Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show()
            updateProgress()
        }

        editIcon.setOnClickListener {
            showEditHabitDialog(habitNameTextView, habitTimeTextView)
        }

        habitsContainer.addView(habitView)
        updateProgress()
    }
}