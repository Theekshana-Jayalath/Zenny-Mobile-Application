package com.example.zenny

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private lateinit var layoutTop: View // Added this line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        habitsContainer = findViewById(R.id.habitsContainer)
        addHabitButton = findViewById(R.id.btn_add_box)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentageText = findViewById(R.id.tv_progress_percentage)
        bottomNav = findViewById(R.id.bottom_navigation)
        homeContent = findViewById(R.id.home_content)
        fragmentContainer = findViewById(R.id.fragment_container)
        layoutTop = findViewById(R.id.layoutTop) // Added this line

        addHabitButton.setOnClickListener { showAddHabitDialog() }
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

    private fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        homeContent.visibility = visibility
        layoutTop.visibility = visibility // Added this line

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
        val total = habitsContainer.childCount
        if (total == 0) {
            progressBar.progress = 0
            progressPercentageText.text = "0%"
            return
        }
        var completed = 0
        for (i in 0 until total) {
            val checkBox = habitsContainer.getChildAt(i).findViewById<CheckBox>(R.id.habitCheckBox)
            if (checkBox.isChecked) completed++
        }
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
                if (name.isNotEmpty()) addHabitView(name, if (selectedTime.isNotEmpty()) selectedTime else "No time set")
                else Toast.makeText(this, "Enter a habit name", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addHabitView(name: String, time: String) {
        val habitView = layoutInflater.inflate(R.layout.activity_list_item_habit, habitsContainer, false)
        val tvName = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val tvTime = habitView.findViewById<TextView>(R.id.habitTimeTextView)
        val delete = habitView.findViewById<ImageView>(R.id.deleteHabitIcon)
        val edit = habitView.findViewById<ImageView>(R.id.editHabitIcon)
        val checkBox = habitView.findViewById<CheckBox>(R.id.habitCheckBox)

        tvName.text = name
        tvTime.text = time

        checkBox.setOnCheckedChangeListener { _, _ -> updateProgress() }
        delete.setOnClickListener { habitsContainer.removeView(habitView); updateProgress() }
        edit.setOnClickListener { showEditHabitDialog(tvName, tvTime) }

        habitsContainer.addView(habitView)
        updateProgress()
    }

    private fun showEditHabitDialog(tvName: TextView, tvTime: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_add_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val btnTime = dialogView.findViewById<Button>(R.id.btnChooseTime)

        etName.setText(tvName.text)
        var selectedTime = tvTime.text.toString()
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
                tvName.text = etName.text.toString().trim()
                tvTime.text = if (selectedTime.isNotEmpty()) selectedTime else "No time set"
                updateProgress()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}