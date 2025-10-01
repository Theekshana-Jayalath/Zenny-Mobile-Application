package com.example.zenny

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.util.*
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan


// Mood data class
data class MoodEntry(val emoji: String, val note: String)

class activity_mood : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private val moodMap = mutableMapOf<CalendarDay, MoodEntry>()  // store moods by date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood)

        calendarView = findViewById(R.id.calendarView)

        // Set today
        calendarView.setSelectedDate(CalendarDay.today())

        // Click listener for date selection
        calendarView.setOnDateChangedListener { _, date, _ ->
            val today = CalendarDay.today()

            if (date.isAfter(today)) {
                Toast.makeText(this, "Cannot select future dates!", Toast.LENGTH_SHORT).show()
            } else {
                showAddMoodBottomSheet(date)
            }
        }
    }

    private fun showAddMoodBottomSheet(date: CalendarDay) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.activity_bottom_sheet_add, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnSaveMood: Button = bottomSheetView.findViewById(R.id.btnSaveMood)
        val editNote: EditText = bottomSheetView.findViewById(R.id.editNote)

        // Emoji buttons
        val emojis = listOf(
            bottomSheetView.findViewById<Button>(R.id.btnEmoji1),
            bottomSheetView.findViewById<Button>(R.id.btnEmoji2),
            bottomSheetView.findViewById<Button>(R.id.btnEmoji3),
            bottomSheetView.findViewById<Button>(R.id.btnEmoji4),
            bottomSheetView.findViewById<Button>(R.id.btnEmoji5)
        )

        var selectedEmoji: String? = null

        emojis.forEach { btn ->
            btn.setOnClickListener {
                selectedEmoji = btn.text.toString()
                Toast.makeText(this, "Selected $selectedEmoji", Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveMood.setOnClickListener {
            if (selectedEmoji == null) {
                Toast.makeText(this, "Please select an emoji first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val note = editNote.text.toString()
            val entry = MoodEntry(selectedEmoji!!, note)
            moodMap[date] = entry

            decorateCalendar()

            Toast.makeText(this, "Saved: $selectedEmoji for $date", Toast.LENGTH_LONG).show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun decorateCalendar() {
        // Clear old decorators
        calendarView.removeDecorators()

        // Add emoji for each saved date
        for ((date, mood) in moodMap) {
            calendarView.addDecorator(object : DayViewDecorator {
                override fun shouldDecorate(day: CalendarDay): Boolean {
                    return day == date
                }

                override fun decorate(view: DayViewFacade) {
                    view.addSpan(object : ReplacementSpan() {
                        override fun getSize(
                            paint: Paint,
                            text: CharSequence,
                            start: Int,
                            end: Int,
                            fm: Paint.FontMetricsInt?
                        ): Int {
                            // Width of emoji
                            return paint.measureText(mood.emoji).toInt()
                        }

                        override fun draw(
                            canvas: Canvas,
                            text: CharSequence,
                            start: Int,
                            end: Int,
                            x: Float,
                            top: Int,
                            y: Int,
                            bottom: Int,
                            paint: Paint
                        ) {
                            paint.textSize = 50f  // Emoji size
                            canvas.drawText(mood.emoji, x, y.toFloat(), paint)
                        }
                    })
                }

            })
            }
        }
    }

