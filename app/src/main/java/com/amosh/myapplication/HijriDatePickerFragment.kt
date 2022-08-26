package com.amosh.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amosh.hijricalendar.date.hijri.HijriDatePickerDialog
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.google.android.material.button.MaterialButton
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class HijriDatePickerFragment : Fragment(), HijriDatePickerDialog.OnDateSetListener {
    private var dateTextView: TextView? = null
    private var modeDarkDate: CheckBox? = null
    private var modeCustomAccentDate: CheckBox? = null
    private var vibrateDate: CheckBox? = null
    private var dismissDate: CheckBox? = null
    private var titleDate: CheckBox? = null
    private var showYearFirst: CheckBox? = null
    private var showVersion2: CheckBox? = null
    private var limitSelectableDays: CheckBox? = null
    private var highlightDays: CheckBox? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view: View = inflater.inflate(R.layout.datepicker_layout, container, false)

        // Find our View instances
        dateTextView = view.findViewById(R.id.date_textview)
        val dateButton: MaterialButton = view.findViewById(R.id.date_button)
        modeDarkDate = view.findViewById(R.id.mode_dark_date)
        modeCustomAccentDate = view.findViewById(R.id.mode_custom_accent_date)
        vibrateDate = view.findViewById(R.id.vibrate_date)
        dismissDate = view.findViewById(R.id.dismiss_date)
        titleDate = view.findViewById(R.id.title_date)
        showYearFirst = view.findViewById(R.id.show_year_first)
        showVersion2 = view.findViewById(R.id.show_version_2)
        limitSelectableDays = view.findViewById(R.id.limit_dates)
        highlightDays = view.findViewById(R.id.highlight_dates)

        // Show a datepicker when the dateButton is clicked
        dateButton.setOnClickListener {
            val now = UmmalquraCalendar()
            val dpd: HijriDatePickerDialog = HijriDatePickerDialog.newInstance(
                this@HijriDatePickerFragment,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            with(dpd) {
                val maxDate = UmmalquraCalendar()
                setMaxDate(maxDate)
                setMinDate(UmmalquraCalendar(maxDate.get(UmmalquraCalendar.YEAR)-10,1,1))
                setThemeDark(modeDarkDate?.isChecked ?: false)
                vibrate(vibrateDate?.isChecked ?: false)
                dismissOnPause(dismissDate?.isChecked ?: false)
                showYearPickerFirst(showYearFirst?.isChecked ?: false)
                setVersion(if (showVersion2?.isChecked == true) HijriDatePickerDialog.Version.VERSION_2 else HijriDatePickerDialog.Version.VERSION_1)
            }
            if (modeCustomAccentDate?.isChecked == true) {
                dpd.setAccentColor(Color.parseColor("#009688"))
            }
            if (titleDate?.isChecked == true) {
                dpd.setTitle("DatePicker Title")
            }
            if (highlightDays?.isChecked == true) {
                val date1 = UmmalquraCalendar()
                val date2 = UmmalquraCalendar()
                date2.add(Calendar.WEEK_OF_MONTH, -1)
                val date3 = UmmalquraCalendar()
                date3.add(Calendar.WEEK_OF_MONTH, 1)
                val days: Array<UmmalquraCalendar> = arrayOf(date1, date2, date3)
                dpd.setHighlightedDays(days)
            }
            if (limitSelectableDays?.isChecked == true) {
                val days: Array<UmmalquraCalendar?> = arrayOfNulls(13)
                for (i in -6..6) {
                    val day = UmmalquraCalendar()
                    day.add(Calendar.DAY_OF_MONTH, i * 2)
                    days[i + 6] = day
                }
            }
            //Change the language to any of supported language
            dpd.setLocale(Locale.getDefault())
            dpd.show(parentFragmentManager, "Datepickerdialog")
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        (parentFragmentManager.findFragmentByTag("Datepickerdialog") as HijriDatePickerDialog?)?.setOnDateSetListener(this)
    }

    override fun onDateSet(view: HijriDatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        var moy = monthOfYear
        val date = "You picked the following date: " + dayOfMonth + "/" + ++moy + "/" + year
        dateTextView?.text = date
    }
}