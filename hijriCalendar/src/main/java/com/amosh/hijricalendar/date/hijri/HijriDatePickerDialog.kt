package com.amosh.hijricalendar.date.hijri

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import com.amosh.hijricalendar.HapticFeedbackController
import com.amosh.hijricalendar.R
import com.amosh.hijricalendar.TypefaceHelper
import com.amosh.hijricalendar.Utils
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Dialog allowing users to select a date.
 */
class HijriDatePickerDialog : DialogFragment(), View.OnClickListener, DatePickerController {
    enum class Version {
        VERSION_1, VERSION_2
    }

    private var mTimezone: TimeZone = TimeZone.getDefault()
    private val mCalendar: UmmalquraCalendar = trimToMidnight(UmmalquraCalendar(timeZone, Locale.getDefault()))
    private var mCallBack: OnDateSetListener? = null
    private val mListeners = HashSet<OnDateChangedListener>()
    private var mOnCancelListener: DialogInterface.OnCancelListener? = null
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null
    private var mAnimator: AccessibleDateAnimator? = null
    private var mDatePickerHeaderView: TextView? = null
    private var mMonthAndDayView: LinearLayout? = null
    private var mSelectedMonthTextView: TextView? = null
    private var mSelectedDayTextView: TextView? = null
    private var mYearView: TextView? = null
    private var mDayPickerView: DayPickerView? = null
    private var mYearPickerView: YearPickerView? = null
    private var mCurrentView = UNINITIALIZED
    private var mWeekStart: Int = mCalendar.firstDayOfWeek
    private var mMinYear = UmmalquraCalendar().get(UmmalquraCalendar.YEAR)
    private var mMaxYear = UmmalquraCalendar().get(UmmalquraCalendar.YEAR)
    private var mTitle: String? = null
    private var mMinDate: UmmalquraCalendar? = null
    private var mMaxDate: UmmalquraCalendar? = null
    private var highlightedDays: HashSet<UmmalquraCalendar> = HashSet<UmmalquraCalendar>()
    private var selectableDays: TreeSet<UmmalquraCalendar> = TreeSet<UmmalquraCalendar>()
    private var disabledDays: HashSet<UmmalquraCalendar> = HashSet<UmmalquraCalendar>()
    private var mThemeDark = false
    private var mThemeDarkChanged = false

    /**
     * Get the accent color of this dialog
     *
     * @return accent color
     */
    override var accentColor = -1
        private set
    private var mVibrate = true
    private var mDismissOnPause = false
    private var mAutoDismiss = false
    private var mDefaultView = MONTH_AND_DAY_VIEW
    private var mOkResid: Int = R.string.mdtp_ok
    private var mOkString: String? = null
    private var mOkColor = -1
    private var mCancelResid: Int = R.string.mdtp_cancel
    private var mCancelString: String? = null
    private var mCancelColor = -1
    private var mVersion: Version? = null
    private var mHapticFeedbackController: HapticFeedbackController? = null
    private var mDelayAnimation = true

    // Accessibility strings.
    private var mDayPickerDescription: String? = null
    private var mSelectDay: String? = null
    private var mYearPickerDescription: String? = null
    private var mSelectYear: String? = null

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    interface OnDateSetListener {
        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         * with [UmmalquraCalendar].
         * @param dayOfMonth  The day of the month that was set.
         */
        fun onDateSet(view: HijriDatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int)
    }

    /**
     * The callback used to notify other date picker components of a change in selected date.
     */
    interface OnDateChangedListener {
        fun onDateChanged()
    }

    fun initialize(callBack: OnDateSetListener?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        mCallBack = callBack
        mCalendar.set(Calendar.YEAR, year)
        mCalendar.set(Calendar.MONTH, monthOfYear)
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        timeZone = mCalendar.timeZone
        mVersion = Version.VERSION_2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        mCurrentView = UNINITIALIZED
        if (savedInstanceState != null) {
            mCalendar.set(UmmalquraCalendar.YEAR, savedInstanceState.getInt(KEY_SELECTED_YEAR))
            mCalendar.set(UmmalquraCalendar.MONTH, savedInstanceState.getInt(KEY_SELECTED_MONTH))
            mCalendar.set(UmmalquraCalendar.DAY_OF_MONTH, savedInstanceState.getInt(KEY_SELECTED_DAY))
            mDefaultView = savedInstanceState.getInt(KEY_DEFAULT_VIEW)
        }
        VERSION_2_FORMAT = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEMMMdd"), locale)
        VERSION_2_FORMAT!!.timeZone = timeZone
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_YEAR, mCalendar.get(UmmalquraCalendar.YEAR))
        outState.putInt(KEY_SELECTED_MONTH, mCalendar.get(UmmalquraCalendar.MONTH))
        outState.putInt(KEY_SELECTED_DAY, mCalendar.get(UmmalquraCalendar.DAY_OF_MONTH))
        outState.putInt(KEY_WEEK_START, mWeekStart)
        outState.putInt(KEY_YEAR_START, mMinYear)
        outState.putInt(KEY_YEAR_END, mMaxYear)
        outState.putInt(KEY_CURRENT_VIEW, mCurrentView)
        var listPosition = -1
        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            listPosition = mDayPickerView!!.mostVisiblePosition
        } else if (mCurrentView == YEAR_VIEW) {
            listPosition = mYearPickerView!!.firstVisiblePosition
            outState.putInt(KEY_LIST_POSITION_OFFSET, mYearPickerView!!.firstPositionOffset)
        }
        outState.putInt(KEY_LIST_POSITION, listPosition)
        outState.putSerializable(KEY_MIN_DATE, mMinDate)
        outState.putSerializable(KEY_MAX_DATE, mMaxDate)
        outState.putSerializable(KEY_HIGHLIGHTED_DAYS, highlightedDays)
        outState.putSerializable(KEY_SELECTABLE_DAYS, selectableDays)
        outState.putSerializable(KEY_DISABLED_DAYS, disabledDays)
        outState.putBoolean(KEY_THEME_DARK, mThemeDark)
        outState.putBoolean(KEY_THEME_DARK_CHANGED, mThemeDarkChanged)
        outState.putInt(KEY_ACCENT, accentColor)
        outState.putBoolean(KEY_VIBRATE, mVibrate)
        outState.putBoolean(KEY_DISMISS, mDismissOnPause)
        outState.putBoolean(KEY_AUTO_DISMISS, mAutoDismiss)
        outState.putInt(KEY_DEFAULT_VIEW, mDefaultView)
        outState.putString(KEY_TITLE, mTitle)
        outState.putInt(KEY_OK_RESID, mOkResid)
        outState.putString(KEY_OK_STRING, mOkString)
        outState.putInt(KEY_OK_COLOR, mOkColor)
        outState.putInt(KEY_CANCEL_RESID, mCancelResid)
        outState.putString(KEY_CANCEL_STRING, mCancelString)
        outState.putInt(KEY_CANCEL_COLOR, mCancelColor)
        outState.putSerializable(KEY_VERSION, mVersion)
        outState.putSerializable(KEY_TIMEZONE, mTimezone)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        var listPosition = -1
        var listPositionOffset = 0
        var currentView = mDefaultView
        if (savedInstanceState != null) {
            mWeekStart = savedInstanceState.getInt(KEY_WEEK_START)
            mMinYear = savedInstanceState.getInt(KEY_YEAR_START)
            mMaxYear = savedInstanceState.getInt(KEY_YEAR_END)
            currentView = savedInstanceState.getInt(KEY_CURRENT_VIEW)
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION)
            listPositionOffset = savedInstanceState.getInt(KEY_LIST_POSITION_OFFSET)
            mMinDate = savedInstanceState.getSerializable(KEY_MIN_DATE) as UmmalquraCalendar
            mMaxDate = savedInstanceState.getSerializable(KEY_MAX_DATE) as UmmalquraCalendar
            highlightedDays = savedInstanceState.getSerializable(KEY_HIGHLIGHTED_DAYS) as HashSet<UmmalquraCalendar>
            selectableDays = savedInstanceState.getSerializable(KEY_SELECTABLE_DAYS) as TreeSet<UmmalquraCalendar>
            disabledDays = savedInstanceState.getSerializable(KEY_DISABLED_DAYS) as HashSet<UmmalquraCalendar>
            mThemeDark = savedInstanceState.getBoolean(KEY_THEME_DARK)
            mThemeDarkChanged = savedInstanceState.getBoolean(KEY_THEME_DARK_CHANGED)
            accentColor = savedInstanceState.getInt(KEY_ACCENT)
            mVibrate = savedInstanceState.getBoolean(KEY_VIBRATE)
            mDismissOnPause = savedInstanceState.getBoolean(KEY_DISMISS)
            mAutoDismiss = savedInstanceState.getBoolean(KEY_AUTO_DISMISS)
            mTitle = savedInstanceState.getString(KEY_TITLE)
            mOkResid = savedInstanceState.getInt(KEY_OK_RESID)
            mOkString = savedInstanceState.getString(KEY_OK_STRING)
            mOkColor = savedInstanceState.getInt(KEY_OK_COLOR)
            mCancelResid = savedInstanceState.getInt(KEY_CANCEL_RESID)
            mCancelString = savedInstanceState.getString(KEY_CANCEL_STRING)
            mCancelColor = savedInstanceState.getInt(KEY_CANCEL_COLOR)
            mVersion = savedInstanceState.getSerializable(KEY_VERSION) as Version
            mTimezone = savedInstanceState.getSerializable(KEY_TIMEZONE) as TimeZone
        }
        val viewRes: Int =
            if (mVersion == Version.VERSION_1) R.layout.hdp_mdtp_hijri_date_picker_dialog else R.layout.hdp_mdtp_hijri_date_picker_dialog_v2
        val view: View = inflater.inflate(viewRes, container, false)
        // All options have been set at this point: round the initial selection if necessary
        setToNearestDate(mCalendar)
        mDatePickerHeaderView = view.findViewById<View>(R.id.mdtp_hijri_date_picker_header) as TextView
        mMonthAndDayView = view.findViewById<View>(R.id.mdtp_hijri_date_picker_month_and_day) as LinearLayout
        mMonthAndDayView?.setOnClickListener(this)
        mSelectedMonthTextView = view.findViewById<View>(R.id.mdtp_hijri_date_picker_month) as? TextView
        mSelectedDayTextView = view.findViewById<View>(R.id.mdtp_hijri_date_picker_day) as TextView
        mYearView = view.findViewById<View>(R.id.mdtp_hijri_date_picker_year) as TextView
        mYearView?.setOnClickListener(this)
        val activity = requireActivity()
        mDayPickerView = SimpleDayPickerView(activity, this)
        mYearPickerView = YearPickerView(activity, this)

        // if theme mode has not been set by java code, check if it is specified in Style.xml
        if (!mThemeDarkChanged) {
            mThemeDark = Utils.isDarkTheme(activity, mThemeDark)
        }
        val res = resources
        mDayPickerDescription = res.getString(R.string.mdtp_day_picker_description)
        mSelectDay = res.getString(R.string.mdtp_select_day)
        mYearPickerDescription = res.getString(R.string.mdtp_year_picker_description)
        mSelectYear = res.getString(R.string.mdtp_select_year)
        val bgColorResource: Int = if (mThemeDark) R.color.mdtp_date_picker_view_animator_dark_theme else R.color.mdtp_date_picker_view_animator
        view.setBackgroundColor(ContextCompat.getColor(activity, bgColorResource))
        mAnimator = view.findViewById<View>(R.id.mdtp_hijri_animator) as AccessibleDateAnimator
        mAnimator!!.addView(mDayPickerView)
        mAnimator!!.addView(mYearPickerView)
        mAnimator!!.setDateMillis(mCalendar.timeInMillis)

        val animation: Animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = ANIMATION_DURATION.toLong()
        mAnimator!!.inAnimation = animation

        val animation2: Animation = AlphaAnimation(1.0f, 0.0f)
        animation2.duration = ANIMATION_DURATION.toLong()
        mAnimator!!.outAnimation = animation2
        val okButton: MaterialButton = view.findViewById(R.id.mdtp_ok)
        okButton.setOnClickListener {
            tryVibrate()
            notifyOnDateListener()
            dismiss()
        }
        okButton.typeface = TypefaceHelper[activity, "Roboto-Medium"]
        if (mOkString != null) okButton.text = mOkString else okButton.setText(mOkResid)
        val cancelButton: MaterialButton = view.findViewById(R.id.mdtp_cancel)
        cancelButton.setOnClickListener {
            tryVibrate()
            if (dialog != null) dialog!!.cancel()
        }
        cancelButton.typeface = TypefaceHelper[activity, "Roboto-Medium"]
        if (mCancelString != null) cancelButton.text = mCancelString else cancelButton.setText(mCancelResid)
        cancelButton.visibility = if (isCancelable) View.VISIBLE else View.GONE

        // If an accent color has not been set manually, get it from the context
        if (accentColor == -1) {
            accentColor = Utils.getAccentColorFromThemeIfAvailable(requireActivity())
        }
        if (mDatePickerHeaderView != null) mDatePickerHeaderView?.setBackgroundColor(Utils.darkenColor(accentColor))
        view.findViewById<View>(R.id.mdtp_hijri_day_picker_selected_date_layout).setBackgroundColor(accentColor)

        // Buttons can have a different color
        if (mOkColor != -1) okButton.setTextColor(mOkColor) else okButton.setTextColor(accentColor)
        if (mCancelColor != -1) cancelButton.setTextColor(mCancelColor) else cancelButton.setTextColor(accentColor)
        if (dialog == null) {
            view.findViewById<View>(R.id.mdtp_done_background).visibility = View.GONE
        }
        updateDisplay(false)
        setCurrentView(currentView)
        if (listPosition != -1) {
            if (currentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView?.postSetSelection(listPosition)
            } else if (currentView == YEAR_VIEW) {
                mYearPickerView!!.postSetSelectionFromTop(listPosition, listPositionOffset)
            }
        }
        if (mThemeDark) {
            ViewCompat.setBackgroundTintList(okButton, ContextCompat.getColorStateList(requireActivity(), R.color.mdtp_light_gray))
            ViewCompat.setBackgroundTintList(cancelButton, ContextCompat.getColorStateList(requireActivity(), R.color.mdtp_light_gray))
        }
        mHapticFeedbackController = HapticFeedbackController(activity)
        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val viewGroup: ViewGroup? = view as ViewGroup?
        if (viewGroup != null) {
            viewGroup.removeAllViewsInLayout()
            val view = onCreateView(requireActivity().layoutInflater, viewGroup, null)
            viewGroup.addView(view)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onResume() {
        super.onResume()
        mHapticFeedbackController?.start()
    }

    override fun onPause() {
        super.onPause()
        mHapticFeedbackController?.stop()
        if (mDismissOnPause) dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (mOnCancelListener != null) mOnCancelListener!!.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mOnDismissListener != null) mOnDismissListener!!.onDismiss(dialog)
    }

    private fun setCurrentView(viewIndex: Int) {
        val millis: Long = mCalendar.timeInMillis
        when (viewIndex) {
            MONTH_AND_DAY_VIEW -> {
                if (mVersion == Version.VERSION_1) {
                    val pulseAnimator: ObjectAnimator = Utils.getPulseAnimator(mMonthAndDayView, 0.9f,
                        1.05f)
                    if (mDelayAnimation) {
                        pulseAnimator.startDelay = ANIMATION_DELAY.toLong()
                        mDelayAnimation = false
                    }
                    mDayPickerView!!.onDateChanged()
                    if (mCurrentView != viewIndex) {
                        mMonthAndDayView?.isSelected = true
                        mYearView?.isSelected = false
                        mAnimator!!.displayedChild = MONTH_AND_DAY_VIEW
                        mCurrentView = viewIndex
                    }
                    pulseAnimator.start()
                } else {
                    mDayPickerView!!.onDateChanged()
                    if (mCurrentView != viewIndex) {
                        mMonthAndDayView?.isSelected = true
                        mYearView?.isSelected = false
                        mAnimator!!.displayedChild = MONTH_AND_DAY_VIEW
                        mCurrentView = viewIndex
                    }
                }
                val flags: Int = DateUtils.FORMAT_SHOW_DATE
                val dayString: String = DateUtils.formatDateTime(activity, millis, flags)
                mAnimator!!.contentDescription = "$mDayPickerDescription: $dayString"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectDay)
            }
            YEAR_VIEW -> {
                if (mVersion == Version.VERSION_1) {
                    val pulseAnimator: ObjectAnimator = Utils.getPulseAnimator(mYearView, 0.85f, 1.1f)
                    if (mDelayAnimation) {
                        pulseAnimator.startDelay = ANIMATION_DELAY.toLong()
                        mDelayAnimation = false
                    }
                    mYearPickerView!!.onDateChanged()
                    if (mCurrentView != viewIndex) {
                        mMonthAndDayView?.isSelected = false
                        mYearView?.isSelected = true
                        mAnimator!!.displayedChild = YEAR_VIEW
                        mCurrentView = viewIndex
                    }
                    pulseAnimator.start()
                } else {
                    mYearPickerView!!.onDateChanged()
                    if (mCurrentView != viewIndex) {
                        mMonthAndDayView?.isSelected = false
                        mYearView?.isSelected = true
                        mAnimator!!.displayedChild = YEAR_VIEW
                        mCurrentView = viewIndex
                    }
                }
                val yearString: CharSequence = YEAR_FORMAT.format(millis)
                mAnimator!!.contentDescription = "$mYearPickerDescription: $yearString"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectYear)
            }
        }
    }

    private fun updateDisplay(announce: Boolean) {
        mYearView?.text = java.lang.String.format(locale, "%2d", mCalendar.get(Calendar.YEAR))
        if (mVersion == Version.VERSION_1) {
            if (mDatePickerHeaderView != null) {
                if (mTitle != null) mDatePickerHeaderView?.text = mTitle!!.uppercase(locale) else {
                    mDatePickerHeaderView?.text = mCalendar.getDisplayName(UmmalquraCalendar.DAY_OF_WEEK, UmmalquraCalendar.LONG,
                        locale)?.uppercase(locale)
                }
            }
            mSelectedMonthTextView?.text = java.lang.String.valueOf(mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale))
            mSelectedDayTextView?.text = java.lang.String.format(locale, "%2d", mCalendar.get(Calendar.DAY_OF_MONTH))
        }
        if (mVersion == Version.VERSION_2) {
            val day: String = ((mCalendar.getDisplayName(UmmalquraCalendar.DAY_OF_WEEK, UmmalquraCalendar.SHORT, locale)
                ?.toString() ?: "") + ", " + mCalendar.getDisplayName(UmmalquraCalendar.MONTH, UmmalquraCalendar.SHORT, locale)
                + " " + java.lang.String.format(locale, "%2d", mCalendar.get(Calendar.DAY_OF_MONTH)))
            mSelectedDayTextView?.text = day
            if (mTitle != null) mDatePickerHeaderView?.text = mTitle!!.uppercase(mLocale) else mDatePickerHeaderView?.visibility = View.GONE
        }

        // Accessibility.
        val millis: Long = mCalendar.timeInMillis
        mAnimator!!.setDateMillis(millis)
        var flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
        val monthAndDayText: String = DateUtils.formatDateTime(activity, millis, flags)
        mMonthAndDayView?.contentDescription = monthAndDayText
        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
            val fullDateText: String = DateUtils.formatDateTime(activity, millis, flags)
            Utils.tryAccessibilityAnnounce(mAnimator, fullDateText)
        }
    }

    /**
     * Set whether the device should vibrate when touching fields
     *
     * @param vibrate true if the device should vibrate when touching a field
     */
    fun vibrate(vibrate: Boolean) {
        mVibrate = vibrate
    }

    /**
     * Set whether the picker should dismiss itself when being paused or whether it should try to survive an orientation change
     *
     * @param dismissOnPause true if the dialog should dismiss itself when it's pausing
     */
    fun dismissOnPause(dismissOnPause: Boolean) {
        mDismissOnPause = dismissOnPause
    }

    /**
     * Set whether the picker should dismiss itself when a day is selected
     *
     * @param autoDismiss true if the dialog should dismiss itself when a day is selected
     */
    fun autoDismiss(autoDismiss: Boolean) {
        mAutoDismiss = autoDismiss
    }
    /**
     * Returns true when the dark theme should be used
     *
     * @return true if the dark theme should be used, false if the default theme should be used
     */
    /**
     * Set whether the dark theme should be used
     *
     * @param themeDark true if the dark theme should be used, false if the default theme should be used
     */
    fun setThemeDark(themeDark: Boolean) {
        mThemeDark = themeDark
        mThemeDarkChanged = true
    }

    override val isThemeDark: Boolean
        get() = mThemeDark

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    fun setAccentColor(color: String?) {
        accentColor = Color.parseColor(color)
    }

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    fun setAccentColor(@ColorInt color: Int) {
        accentColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    fun setOkColor(color: String?) {
        mOkColor = Color.parseColor(color)
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    fun setOkColor(@ColorInt color: Int) {
        mOkColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    fun setCancelColor(color: String?) {
        mCancelColor = Color.parseColor(color)
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    fun setCancelColor(@ColorInt color: Int) {
        mCancelColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Set whether the year picker of the month and day picker is shown first
     *
     * @param yearPicker boolean
     */
    fun showYearPickerFirst(yearPicker: Boolean) {
        mDefaultView = if (yearPicker) YEAR_VIEW else MONTH_AND_DAY_VIEW
    }

    fun setYearRange(startYear: Int, endYear: Int) {
        require(endYear >= startYear) { "Year end must be larger than or equal to year start" }
        mMinYear = startYear
        mMaxYear = endYear
        if (mDayPickerView != null) {
            mDayPickerView!!.onChange()
        }
    }
    /**
     * @return The minimal date supported by this DatePicker. Null if it has not been set.
     */
    /**
     * Sets the minimal date supported by this DatePicker. Dates before (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the mindate.
     */


    fun setMinDate(calendar: UmmalquraCalendar) {
        mMinDate = trimToMidnight((calendar.clone() as UmmalquraCalendar))
        minYear = mMinDate!!.get(Calendar.YEAR)
        if (mDayPickerView != null) {
            mDayPickerView!!.onChange()
        }
    }

    fun setMaxDate(calendar: UmmalquraCalendar) {
        mMaxDate = trimToMidnight(calendar.clone() as UmmalquraCalendar)
        if (mDayPickerView != null) {
            mDayPickerView!!.onChange()
        }
    }

    /**
     * Set a title to be displayed instead of the weekday
     *
     * @param title String - The title to be displayed
     */
    fun setTitle(title: String) {
        mTitle = title
    }

    /**
     * @return The maximal date supported by this DatePicker. Null if it has not been set.
     */
    /**
     * Sets the minimal date supported by this DatePicker. Dates after (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the maxdate.
     */

    /**
     * Sets an array of dates which should be highlighted when the picker is drawn
     *
     * @param highlightedDays an Array of Calendar objects containing the dates to be highlighted
     */
    fun setHighlightedDays(highlightedDays: Array<UmmalquraCalendar>) {
        for (highlightedDay in highlightedDays) trimToMidnight(highlightedDay)
        this.highlightedDays.addAll(Arrays.asList(*highlightedDays))
        if (mDayPickerView != null) mDayPickerView!!.onChange()
    }

    /**
     * @return The list of dates, as Calendar Objects, which should be highlighted. null is no dates should be highlighted
     */
    fun getHighlightedDays(): Array<UmmalquraCalendar>? {
        if (highlightedDays.isEmpty()) return null
        val output: Array<UmmalquraCalendar> = highlightedDays.toArray(arrayOfNulls<UmmalquraCalendar>(0))
        Arrays.sort(output)
        return output
    }

    override fun isHighlighted(year: Int, month: Int, day: Int): Boolean {
        val date = UmmalquraCalendar()
        date.set(UmmalquraCalendar.YEAR, year)
        date.set(UmmalquraCalendar.MONTH, month)
        date.set(UmmalquraCalendar.DAY_OF_MONTH, day)
        trimToMidnight(date)
        return highlightedDays.contains(date)
    }

    /**
     * Set the label for the Ok button (max 12 characters)
     *
     * @param okResid A resource ID to be used as the Ok button label
     */
    fun setOkText(@StringRes okResid: Int) {
        mOkString = null
        mOkResid = okResid
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelString A literal String to be used as the Cancel button label
     */
    fun setCancelText(cancelString: String?) {
        mCancelString = cancelString
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelResid A resource ID to be used as the Cancel button label
     */
    fun setCancelText(@StringRes cancelResid: Int) {
        mCancelString = null
        mCancelResid = cancelResid
    }

    /**
     * Set which layout version the picker should use
     *
     * @param version The version to use
     */
    fun setVersion(version: Version?) {
        mVersion = version
    }

    fun setOnDateSetListener(listener: OnDateSetListener?) {
        mCallBack = listener
    }

    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?) {
        mOnCancelListener = onCancelListener
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = onDismissListener
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2022 when Feb 29, 2012 is selected -> Feb 28, 2022
    private fun adjustDayInMonthIfNeeded(calendar: UmmalquraCalendar) {
        val day: Int = calendar.get(UmmalquraCalendar.DAY_OF_MONTH)
        val daysInMonth: Int = calendar.getActualMaximum(UmmalquraCalendar.DAY_OF_MONTH)
        if (day > daysInMonth) {
            calendar.set(UmmalquraCalendar.DAY_OF_MONTH, daysInMonth)
        }
        setToNearestDate(calendar)
    }

    override fun onClick(v: View) {
        tryVibrate()
        if (v.id == R.id.mdtp_hijri_date_picker_year) {
            setCurrentView(YEAR_VIEW)
        } else if (v.id == R.id.mdtp_hijri_date_picker_month_and_day) {
            setCurrentView(MONTH_AND_DAY_VIEW)
        }
    }

    override fun onYearSelected(year: Int) {
        mCalendar.set(UmmalquraCalendar.YEAR, year)
        adjustDayInMonthIfNeeded(mCalendar)
        updatePickers()
        setCurrentView(MONTH_AND_DAY_VIEW)
        updateDisplay(true)
    }

    override fun onDayOfMonthSelected(year: Int, month: Int, day: Int) {
        mCalendar.set(UmmalquraCalendar.YEAR, year)
        mCalendar.set(UmmalquraCalendar.MONTH, month)
        mCalendar.set(UmmalquraCalendar.DAY_OF_MONTH, day)
        updatePickers()
        updateDisplay(true)
        if (mAutoDismiss) {
            notifyOnDateListener()
            dismiss()
        }
    }

    private fun updatePickers() {
        for (listener in mListeners) listener.onDateChanged()
    }

    override val selectedDay: MonthAdapter.CalendarDay
        get() = MonthAdapter.CalendarDay(mCalendar, timeZone)
    override val startDate: UmmalquraCalendar
        get() {
            if (!selectableDays.isEmpty()) return selectableDays.first()
            if (mMinDate != null) return mMinDate!!
            val output = UmmalquraCalendar(timeZone, locale)
            output.set(UmmalquraCalendar.YEAR, mMinYear)
            output.set(UmmalquraCalendar.DAY_OF_MONTH, 1)
            output.set(UmmalquraCalendar.MONTH, UmmalquraCalendar.JANUARY)
            return output
        }
    override val endDate: UmmalquraCalendar
        get() {
            if (!selectableDays.isEmpty()) return selectableDays.last()
            if (mMaxDate != null) return mMaxDate!!
            val output = UmmalquraCalendar(timeZone, locale)
            output.set(UmmalquraCalendar.YEAR, mMaxYear)
            output.set(UmmalquraCalendar.DAY_OF_MONTH, 29)
            output.set(UmmalquraCalendar.MONTH, UmmalquraCalendar.DECEMBER)
            return output
        }

    // Ensure no years can be selected outside of the given minimum date
    override var minYear: Int
        get() {
            if (!selectableDays.isEmpty()) return selectableDays.first().get(UmmalquraCalendar.YEAR)
            // Ensure no years can be selected outside of the given minimum date
            return if (mMinDate != null && mMinDate!!.get(UmmalquraCalendar.YEAR) > mMinYear) mMinDate!!.get(UmmalquraCalendar.YEAR) else mMinYear
        }
        set(minYear) {
            mMinYear = minYear
        }

    // Ensure no years can be selected outside of the given maximum date
    override var maxYear: Int
        get() {
            if (!selectableDays.isEmpty()) return selectableDays.last().get(UmmalquraCalendar.YEAR)
            // Ensure no years can be selected outside of the given maximum date
            return if (mMaxDate != null && mMaxDate!!.get(UmmalquraCalendar.YEAR) < mMaxYear) mMaxDate!!.get(UmmalquraCalendar.YEAR) else mMaxYear
        }
        set(maxYear) {
            mMaxYear = maxYear
        }

    /**
     * @return true if the specified year/month/day are within the selectable days or the range set by minDate and maxDate.
     * If one or either have not been set, they are considered as Integer.MIN_VALUE and
     * Integer.MAX_VALUE.
     */
    override fun isOutOfRange(year: Int, month: Int, day: Int): Boolean {
        val date = UmmalquraCalendar()
        date.set(UmmalquraCalendar.YEAR, year)
        date.set(UmmalquraCalendar.MONTH, month)
        date.set(UmmalquraCalendar.DAY_OF_MONTH, day)
        return isOutOfRange(date)
    }

    private fun isOutOfRange(calendar: UmmalquraCalendar): Boolean {
        trimToMidnight(calendar)
        return isDisabled(calendar) || !isSelectable(calendar)
    }

    private fun isDisabled(c: UmmalquraCalendar): Boolean {
        return disabledDays.contains(trimToMidnight(c)) || isBeforeMin(c) || isAfterMax(c)
    }

    private fun isSelectable(c: UmmalquraCalendar): Boolean {
        return selectableDays.isEmpty() || selectableDays.contains(trimToMidnight(c))
    }

    private fun isBeforeMin(calendar: UmmalquraCalendar): Boolean {
        return mMinDate != null && calendar.before(mMinDate)
    }

    private fun isAfterMax(calendar: UmmalquraCalendar): Boolean {
        return mMaxDate != null && calendar.after(mMaxDate)
    }

    private fun setToNearestDate(calendar: UmmalquraCalendar) {
        if (!selectableDays.isEmpty()) {
            var newCalendar: UmmalquraCalendar? = null
            val higher: UmmalquraCalendar? = selectableDays.ceiling(calendar)
            val lower: UmmalquraCalendar? = selectableDays.lower(calendar)
            if (higher == null && lower != null) newCalendar = lower else if (lower == null && higher != null) newCalendar = higher
            if (newCalendar != null || higher == null) {
                newCalendar = newCalendar ?: calendar
                newCalendar.timeZone = timeZone
                calendar.timeInMillis = newCalendar.timeInMillis
                return
            }
            val highDistance: Long = abs(higher.timeInMillis - calendar.timeInMillis)
            val lowDistance: Long = abs(calendar.timeInMillis - lower!!.timeInMillis)
            if (lowDistance < highDistance) calendar.timeInMillis = lower.timeInMillis else calendar.timeInMillis = higher.timeInMillis
            return
        }
        if (disabledDays.isNotEmpty()) {
            val forwardDate: UmmalquraCalendar = calendar.clone() as UmmalquraCalendar
            val backwardDate: UmmalquraCalendar = calendar.clone() as UmmalquraCalendar
            while (isDisabled(forwardDate) && isDisabled(backwardDate)) {
                forwardDate.add(UmmalquraCalendar.DAY_OF_MONTH, 1)
                backwardDate.add(UmmalquraCalendar.DAY_OF_MONTH, -1)
            }
            if (!isDisabled(backwardDate)) {
                calendar.timeInMillis = backwardDate.timeInMillis
                return
            }
            if (!isDisabled(forwardDate)) {
                calendar.timeInMillis = forwardDate.timeInMillis
                return
            }
        }
        if (isBeforeMin(calendar)) {
            calendar.timeInMillis = mMinDate!!.timeInMillis
            return
        }
        if (isAfterMax(calendar)) {
            calendar.timeInMillis = mMaxDate!!.timeInMillis
            return
        }
    }

    /**
     * Trims off all time information, effectively setting it to midnight
     * Makes it easier to compare at just the day level
     *
     * @param calendar The Calendar object to trim
     * @return The trimmed Calendar object
     */
    private fun trimToMidnight(calendar: UmmalquraCalendar): UmmalquraCalendar {
        calendar.set(UmmalquraCalendar.HOUR_OF_DAY, 0)
        calendar.set(UmmalquraCalendar.MINUTE, 0)
        calendar.set(UmmalquraCalendar.SECOND, 0)
        calendar.set(UmmalquraCalendar.MILLISECOND, 0)
        return calendar
    }

    override var firstDayOfWeek: Int
        get() = mWeekStart
        set(startOfWeek) {
            require(!(startOfWeek < UmmalquraCalendar.SUNDAY || startOfWeek > UmmalquraCalendar.SATURDAY)) {
                "Value must be between Calendar.SUNDAY and " +
                    "Calendar.SATURDAY"
            }
            mWeekStart = startOfWeek
            if (mDayPickerView != null) {
                mDayPickerView!!.onChange()
            }
        }

    override fun registerOnDateChangedListener(listener: OnDateChangedListener) {
        mListeners.add(listener)
    }

    override fun unregisterOnDateChangedListener(listener: OnDateChangedListener) {
        mListeners.remove(listener)
    }

    override fun tryVibrate() {
        if (mVibrate) mHapticFeedbackController!!.tryVibrate()
    }

    /**
     * Set which timezone the picker should use
     *
     */
    override var timeZone: TimeZone
        get() = mTimezone
        set(timeZone) {
            mTimezone = timeZone
            mCalendar.timeZone = timeZone
            YEAR_FORMAT.timeZone = timeZone
            MONTH_FORMAT.timeZone = timeZone
            DAY_FORMAT.timeZone = timeZone
        }

    override val locale: Locale
        get() = mLocale

    fun setLocale(locale: Locale) {
        mLocale = locale
        mWeekStart = Calendar.getInstance(mTimezone, mLocale).firstDayOfWeek
        YEAR_FORMAT = SimpleDateFormat("yyyy", locale)
        MONTH_FORMAT = SimpleDateFormat("MMM", locale)
        DAY_FORMAT = SimpleDateFormat("dd", locale)
    }

    private fun notifyOnDateListener() {
        if (mCallBack != null) {
            mCallBack!!.onDateSet(this@HijriDatePickerDialog, mCalendar.get(UmmalquraCalendar.YEAR),
                mCalendar.get(UmmalquraCalendar.MONTH), mCalendar.get(UmmalquraCalendar.DAY_OF_MONTH))
        }
    }

    companion object {
        private const val UNINITIALIZED = -1
        private const val MONTH_AND_DAY_VIEW = 0
        private const val YEAR_VIEW = 1
        private const val KEY_SELECTED_YEAR = "year"
        private const val KEY_SELECTED_MONTH = "month"
        private const val KEY_SELECTED_DAY = "day"
        private const val KEY_LIST_POSITION = "list_position"
        private const val KEY_WEEK_START = "week_start"
        private const val KEY_YEAR_START = "year_start"
        private const val KEY_YEAR_END = "year_end"
        private const val KEY_CURRENT_VIEW = "current_view"
        private const val KEY_LIST_POSITION_OFFSET = "list_position_offset"
        private const val KEY_MIN_DATE = "min_date"
        private const val KEY_MAX_DATE = "max_date"
        private const val KEY_HIGHLIGHTED_DAYS = "highlighted_days"
        private const val KEY_SELECTABLE_DAYS = "selectable_days"
        private const val KEY_DISABLED_DAYS = "disabled_days"
        private const val KEY_THEME_DARK = "theme_dark"
        private const val KEY_THEME_DARK_CHANGED = "theme_dark_changed"
        private const val KEY_ACCENT = "accent"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_DISMISS = "dismiss"
        private const val KEY_AUTO_DISMISS = "auto_dismiss"
        private const val KEY_DEFAULT_VIEW = "default_view"
        private const val KEY_TITLE = "title"
        private const val KEY_OK_RESID = "ok_resid"
        private const val KEY_OK_STRING = "ok_string"
        private const val KEY_OK_COLOR = "ok_color"
        private const val KEY_CANCEL_RESID = "cancel_resid"
        private const val KEY_CANCEL_STRING = "cancel_string"
        private const val KEY_CANCEL_COLOR = "cancel_color"
        private const val KEY_VERSION = "version"
        private const val KEY_TIMEZONE = "timezone"
        private const val ANIMATION_DURATION = 300
        private const val ANIMATION_DELAY = 500
        private var YEAR_FORMAT = SimpleDateFormat("y", Locale.getDefault())
        private var MONTH_FORMAT = SimpleDateFormat("MMMM", Locale.getDefault())
        private var DAY_FORMAT = SimpleDateFormat("dd", Locale.getDefault())
        private var VERSION_2_FORMAT: SimpleDateFormat? = null
        var mLocale: Locale = Locale.getDefault()

        /**
         * @param callBack    How the parent is notified that the date is set.
         * @param year        The initial year of the dialog.
         * @param monthOfYear The initial month of the dialog.
         * @param dayOfMonth  The initial day of the dialog.
         */
        fun newInstance(
            callBack: OnDateSetListener?, year: Int,
            monthOfYear: Int,
            dayOfMonth: Int,
        ): HijriDatePickerDialog {
            val ret = HijriDatePickerDialog()
            ret.initialize(callBack, year, monthOfYear, dayOfMonth)
            return ret
        }
    }
}