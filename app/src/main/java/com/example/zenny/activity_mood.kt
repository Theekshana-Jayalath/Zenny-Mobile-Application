package com.example.zenny

import android.app.Dialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.style.LineBackgroundSpan
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalAdjusters
import java.text.SimpleDateFormat
import java.util.*

class activity_mood : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var barChart: BarChart
    private lateinit var pref: MoodPreference

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val v = inflater.inflate(R.layout.activity_mood, container, false)

        pref = MoodPreference(requireContext())
        initializeViews(v)
        setupListeners()
        setupBarChart()

        toggleGroup.check(R.id.btnWeekly)
        refreshAll()

        return v
    }

    private fun initializeViews(v: android.view.View) {
        calendarView = v.findViewById(R.id.calendarView)
        toggleGroup = v.findViewById(R.id.buttonContainer)
        barChart = v.findViewById(R.id.barChart)
        calendarView.selectionColor = Color.TRANSPARENT
    }

    private fun setupListeners() {
        calendarView.setOnDateChangedListener { _, date, _ ->
            calendarView.clearSelection()
            val clickedDate = String.format("%04d-%02d-%02d", date.year, date.month, date.day)
            if (date.isAfter(CalendarDay.today())) {
                Toast.makeText(requireContext(), "Cannot add moods for future dates", Toast.LENGTH_SHORT).show()
            } else {
                val bs = AddMoodBottomSheet.newInstance(clickedDate)
                bs.onSaved = { entry ->
                    pref.addOrUpdate(entry)
                    Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
                    refreshAll()
                }
                bs.show(childFragmentManager, "addMood")
            }
        }

        calendarView.setOnMonthChangedListener { _, date ->
            if (toggleGroup.checkedButtonId == R.id.btnMonthly) {
                updateMonthlyAnalysis(date)
            }
        }

        toggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            refreshAll()
        }
    }

    private fun refreshAll() {
        refreshCalendarDecorators()
        if (toggleGroup.checkedButtonId == R.id.btnWeekly) {
            updateWeeklyAnalysis(calendarView.currentDate)
        } else {
            updateMonthlyAnalysis(calendarView.currentDate)
        }
    }

    private fun updateWeeklyAnalysis(day: CalendarDay) {
        val date = LocalDate.of(day.year, day.month, day.day)
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val moods = pref.getForDateRange(startOfWeek, endOfWeek)
        barChart.axisRight.axisMaximum = 7f
        barChart.axisLeft.axisMaximum = 7f
        updateChartData(moods)
    }

    private fun updateMonthlyAnalysis(day: CalendarDay) {
        val date = LocalDate.of(day.year, day.month, day.day)
        val startOfMonth = date.withDayOfMonth(1)
        val endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth())
        val moods = pref.getForDateRange(startOfMonth, endOfMonth)
        barChart.axisRight.axisMaximum = 31f
        barChart.axisLeft.axisMaximum = 31f
        updateChartData(moods)
    }

    private fun updateChartData(moods: List<MoodEntry>) {
        val emojiCounts = EMOJI_LIST.associateWith { emoji ->
            moods.count { it.emoji == emoji }
        }

        val entries = EMOJI_LIST.mapIndexed { index, emoji ->
            BarEntry(index.toFloat(), (emojiCounts[emoji] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Mood Count")
        dataSet.colors = listOf(
            Color.parseColor("#FFD700"), // 😡
            Color.parseColor("#F08080"), // 😭
            Color.parseColor("#ADD8E6"), // 😐
            Color.parseColor("#90EE90"), // 😞
            Color.parseColor("#FFA07A")  // 😀
        )
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) value.toInt().toString() else ""
            }
        }

        val barData = BarData(dataSet)
        // FIX: Reducing the bar width creates more space for labels
        barData.barWidth = 0.4f

        barChart.data = barData
        barChart.invalidate()
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setTouchEnabled(false)

        // --- X-Axis (Bottom - Emojis) ---
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(EMOJI_LIST)
        xAxis.textSize = 16f // Reduced size to ensure labels fit

        // FIX: These 3 lines work together to force all 5 labels to display correctly
        xAxis.setLabelCount(EMOJI_LIST.size, true)
        xAxis.setCenterAxisLabels(true)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = EMOJI_LIST.size - 0.5f

        // --- Y-Axis (Right - Count) ---
        barChart.axisLeft.isEnabled = false
        val yAxisRight = barChart.axisRight
        yAxisRight.axisMinimum = 0f
        yAxisRight.granularity = 1f
        yAxisRight.setDrawGridLines(false)
    }

    private fun refreshCalendarDecorators() {
        calendarView.removeDecorators()
        val moods = pref.getAll()
        moods.forEach { mood ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse( mood.dateIso )
            date?.let {
                val threeTenLocalDate =
                    org.threeten.bp.Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate()
                val calendarDay = CalendarDay.from(threeTenLocalDate)
                calendarView.addDecorator(EmojiDecorator(calendarDay, mood.emoji))
            }
        }
        calendarView.addDecorator(TodayDecorator())
    }

    private inner class EmojiDecorator(private val date: CalendarDay, private val emoji: String) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = day == date
        override fun decorate(view: DayViewFacade) {
            view.addSpan(CustomTextSpan(emoji))
        }
    }

    private inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()
        private val highlightDrawable: Drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E0E0E0"))
        }

        override fun shouldDecorate(day: CalendarDay): Boolean = day == today
        override fun decorate(view: DayViewFacade) {
            view.setBackgroundDrawable(highlightDrawable)
        }
    }

    private inner class CustomTextSpan(private val text: String) : LineBackgroundSpan {
        override fun drawBackground(
            canvas: Canvas, paint: Paint,
            left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int, lnum: Int
        ) {
            val oldTextSize = paint.textSize
            val oldAlign = paint.textAlign
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = oldTextSize * 1.2f
            val y = (baseline + oldTextSize * 1.1).toFloat()
            val x = (left + right) / 2f
            canvas.drawText(this.text, x, y, paint)
            paint.textSize = oldTextSize
            paint.textAlign = oldAlign
        }
    }

    class AddMoodBottomSheet : BottomSheetDialogFragment() {
        var onSaved: ((MoodEntry) -> Unit)? = null
        private var dateIso: String = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            dateIso = arguments?.getString("dateIso") ?: ""
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = BottomSheetDialog(requireContext())
            dialog.setContentView(R.layout.activity_bottom_sheet_add)

            val btns = listOfNotNull(
                dialog.findViewById<Button>(R.id.btnEmoji1),
                dialog.findViewById<Button>(R.id.btnEmoji2),
                dialog.findViewById<Button>(R.id.btnEmoji3),
                dialog.findViewById<Button>(R.id.btnEmoji4),
                dialog.findViewById<Button>(R.id.btnEmoji5)
            )

            btns.forEachIndexed { index, button ->
                button.text = EMOJI_LIST.getOrNull(index) ?: ""
            }

            var chosen = EMOJI_LIST.first()
            val existing = try {
                MoodPreference(requireContext()).getForDate(dateIso).firstOrNull()
            } catch (e: Exception) {
                null
            }
            existing?.let { chosen = it.emoji }

            fun updateEmojiSelection(selectedButton: Button) {
                btns.forEach { button ->
                    button.setBackgroundColor(
                        if (button == selectedButton) Color.parseColor("#E0E0E0")
                        else Color.TRANSPARENT
                    )
                }
            }

            btns.find { it.text.toString() == chosen }?.let { updateEmojiSelection(it) }

            btns.forEach { b ->
                b.setOnClickListener {
                    chosen = b.text.toString()
                    updateEmojiSelection(b)
                }
            }

            val editNote = dialog.findViewById<EditText>(R.id.editNote)
            existing?.note?.let { editNote?.setText(it) }

            val btnSave = dialog.findViewById<Button>(R.id.btnSaveMood)
            btnSave?.setOnClickListener {
                val entry = MoodEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    dateIso = dateIso, emoji = chosen,
                    note = editNote?.text.toString() ?: ""
                )
                onSaved?.invoke(entry)
                dismiss()
            }
            return dialog
        }

        companion object {
            fun newInstance(dateIso: String): AddMoodBottomSheet {
                val b = AddMoodBottomSheet()
                val args = Bundle()
                args.putString("dateIso", dateIso)
                b.arguments = args
                return b
            }
        }
    }

    companion object {
        val EMOJI_LIST = listOf("😡", "😭", "😐", "😞", "😀")
        fun newInstance(): activity_mood = activity_mood()
    }
}