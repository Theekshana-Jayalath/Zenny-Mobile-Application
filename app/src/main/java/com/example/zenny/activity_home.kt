package com.example.zenny

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class activity_home : AppCompatActivity() {

    private lateinit var habitsContainer: LinearLayout
    private lateinit var addHabitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentageText: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var homeContent: View
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var layoutTop: View

    private val PREFS_NAME = "ZennyPrefs"
    private val HABITS_KEY = "habits"
    private val LAST_OPENED_DATE_KEY = "lastOpenedDate"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var habits = mutableListOf<Habit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        habitsContainer = findViewById(R.id.habitsContainer)
        addHabitButton = findViewById(R.id.btn_add_box)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentageText = findViewById(R.id.tv_progress_percentage)
        bottomNav = findViewById(R.id.bottom_navigation)
        homeContent = findViewById(R.id.home_content)
        fragmentContainer = findViewById(R.id.fragment_container)
        layoutTop = findViewById(R.id.layoutTop)

        addHabitButton.setOnClickListener { showAddHabitDialog() }

        checkDateAndResetProgress()
        loadHabits()
        updateProgress()

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> {
                    showMainContent(true)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_mood -> {
                    selectedFragment = activity_mood.newInstance()
                }
                R.id.nav_water -> {
                    selectedFragment = HydrationFragment.newInstance()
                }
            }

            if (selectedFragment != null) {
                showMainContent(false)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }
    }

    private fun checkDateAndResetProgress() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastOpenedDate = sharedPreferences.getString(LAST_OPENED_DATE_KEY, null)

        if (today != lastOpenedDate) {
            val editor = sharedPreferences.edit()
            editor.putString(LAST_OPENED_DATE_KEY, today)
            // Reset completion status for all habits
            val json = sharedPreferences.getString(HABITS_KEY, null)
            if (json != null) {
                val type = object : TypeToken<MutableList<Habit>>() {}.type
                val savedHabits: MutableList<Habit> = gson.fromJson(json, type)
                savedHabits.forEach { it.isCompleted = false }
                editor.putString(HABITS_KEY, gson.toJson(savedHabits))
            }
            editor.apply()
        }
    }

    private fun saveHabits() {
        val json = gson.toJson(habits)
        sharedPreferences.edit().putString(HABITS_KEY, json).apply()
    }

    private fun loadHabits() {
        val json = sharedPreferences.getString(HABITS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            habits = gson.fromJson(json, type)
            habits.forEach { addHabitView(it) }
        }
    }

    private fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        homeContent.visibility = visibility
        layoutTop.visibility = visibility

        if (show) {
            fragmentContainer.visibility = View.GONE
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != null) {
                supportFragmentManager.beginTransaction().remove(currentFragment).commit()
            }
        } else {
            fragmentContainer.visibility = View.VISIBLE
        }
    }

    private fun updateProgress() {
        val total = habits.size
        if (total == 0) {
            progressBar.progress = 0
            progressPercentageText.text = "0%"
            return
        }
        val completed = habits.count { it.isCompleted }
        val progress = (completed * 100) / total
        progressBar.progress = progress
        progressPercentageText.text = "$progress%"
    }

    private fun showAddHabitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_add_habit, null)
        val etHabitName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val btnChooseTime = dialogView.findViewById<Button>(R.id.btnChooseTime)
        var selectedTime = ""

        btnChooseTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                selectedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time)
                btnChooseTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Habit")
            .setPositiveButton("Add") { _, _ ->
                val name = etHabitName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val habit = Habit(name, if (selectedTime.isNotEmpty()) selectedTime else "No time set")
                    habits.add(habit)
                    addHabitView(habit)
                    saveHabits()
                } else {
                    Toast.makeText(this, "Enter a habit name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addHabitView(habit: Habit) {
        val habitView = layoutInflater.inflate(R.layout.activity_list_item_habit, habitsContainer, false)
        val tvName = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val tvTime = habitView.findViewById<TextView>(R.id.habitTimeTextView)
        val delete = habitView.findViewById<ImageView>(R.id.deleteHabitIcon)
        val edit = habitView.findViewById<ImageView>(R.id.editHabitIcon)
        val checkBox = habitView.findViewById<CheckBox>(R.id.habitCheckBox)

        tvName.text = habit.name
        tvTime.text = habit.time
        checkBox.isChecked = habit.isCompleted

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isCompleted = isChecked
            updateProgress()
            saveHabits()
        }
        delete.setOnClickListener {
            habits.remove(habit)
            habitsContainer.removeView(habitView)
            updateProgress()
            saveHabits()
        }
        edit.setOnClickListener { showEditHabitDialog(habit, tvName, tvTime) }

        habitsContainer.addView(habitView)
        updateProgress()
    }

    private fun showEditHabitDialog(habit: Habit, tvName: TextView, tvTime: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_add_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val btnTime = dialogView.findViewById<Button>(R.id.btnChooseTime)

        etName.setText(habit.name)
        var selectedTime = habit.time
        btnTime.text = if (selectedTime != "No time set") selectedTime else "Choose Time"

        btnTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                selectedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
                btnTime.text = selectedTime
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Habit")
            .setPositiveButton("Save") { _, _ ->
                habit.name = etName.text.toString().trim()
                habit.time = if (selectedTime.isNotEmpty()) selectedTime else "No time set"
                tvName.text = habit.name
                tvTime.text = habit.time
                updateProgress()
                saveHabits()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
