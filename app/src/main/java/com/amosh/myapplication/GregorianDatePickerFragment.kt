package com.amosh.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amosh.hijricalendar.date.gregorian.GregorianDatePickerDialog
import com.google.android.material.button.MaterialButton
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class GregorianDatePickerFragment : Fragment(), GregorianDatePickerDialog.OnDateSetListener {
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
    private var dateButton: MaterialButton? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.datepicker_layout, container, false)

        // Find our View instances
        dateTextView = view.findViewById(R.id.date_textview)
        dateButton = view.findViewById(R.id.date_button)
        modeDarkDate = view.findViewById(R.id.mode_dark_date)
        modeCustomAccentDate = view.findViewById(R.id.mode_custom_accent_date)
        vibrateDate = view.findViewById(R.id.vibrate_date)
        dismissDate = view.findViewById(R.id.dismiss_date)
        titleDate = view.findViewById(R.id.title_date)
        showYearFirst = view.findViewById(R.id.show_year_first)
        showVersion2 = view.findViewById(R.id.show_version_2)
        limitSelectableDays = view.findViewById(R.id.limit_dates)
        highlightDays = view.findViewById(R.id.highlight_dates)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show a date picker when the dateButton is clicked
        dateButton?.setOnClickListener {
            val now = Calendar.getInstance()
            val dpd: GregorianDatePickerDialog = GregorianDatePickerDialog.newInstance(
                this@GregorianDatePickerFragment,
                now[Calendar.YEAR],
                now[Calendar.MONTH],
                now[Calendar.DAY_OF_MONTH]
            )
            dpd.setThemeDark(modeDarkDate?.isChecked ?: false)
            dpd.vibrate(vibrateDate?.isChecked ?: false)
            dpd.dismissOnPause(dismissDate?.isChecked ?: false)
            dpd.showYearPickerFirst(showYearFirst?.isChecked ?: false)
            dpd.setVersion(when (showVersion2?.isChecked) {
                true -> GregorianDatePickerDialog.Version.VERSION_2
                else -> GregorianDatePickerDialog.Version.VERSION_1
            })
            if (modeCustomAccentDate?.isChecked == true) {
                dpd.setAccentColor(Color.parseColor("#009688"))
            }
            if (titleDate?.isChecked == true) {
                dpd.setTitle("DatePicker Title")
            }
            if (highlightDays?.isChecked == true) {
                val date1 = Calendar.getInstance()
                val date2 = Calendar.getInstance()
                date2.add(Calendar.WEEK_OF_MONTH, -1)
                val date3 = Calendar.getInstance()
                date3.add(Calendar.WEEK_OF_MONTH, 1)
                val days = arrayOf(date1, date2, date3)
                dpd.setHighlightedDays(days)
            }
            if (limitSelectableDays?.isChecked == true) {
                val days = arrayOfNulls<Calendar>(13)
                for (i in -6..6) {
                    val day = Calendar.getInstance()
                    day.add(Calendar.DAY_OF_MONTH, i * 2)
                    days[i + 6] = day
                }
            }
            //Change the language to any of supported language
            dpd.setLocale(Locale(Locale.getDefault().displayLanguage))
            dpd.setMinDate(Calendar.getInstance().apply {
                add(Calendar.YEAR ,- 10)
            })
            dpd.setMaxDate(Calendar.getInstance())
            dpd.show(parentFragmentManager, "Datepickerdialog")
        }
    }
    override fun onResume() {
        super.onResume()
        (parentFragmentManager.findFragmentByTag("Datepickerdialog") as GregorianDatePickerDialog?)?.setOnDateSetListener(this)
    }

    override fun onDateSet(view: GregorianDatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        var moy = monthOfYear
        val date = "You picked the following date: " + dayOfMonth + "/" + ++moy + "/" + year
        dateTextView?.text = date
    }
}