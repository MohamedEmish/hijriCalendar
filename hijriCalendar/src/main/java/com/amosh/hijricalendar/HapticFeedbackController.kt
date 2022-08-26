package com.amosh.hijricalendar

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.SystemClock
import android.os.Vibrator
import android.provider.Settings

/**
 * A simple utility class to handle haptic feedback.
 */
class HapticFeedbackController(private val mContext: Context) {
    private val mContentObserver: ContentObserver
    private var mVibrator: Vibrator? = null
    private var mIsGloballyEnabled = false
    private var mLastVibrate: Long = 0

    /**
     * Call to setup the controller.
     */
    fun start() {
        if (hasVibratePermission(mContext)) {
            mVibrator = mContext.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        }

        // Setup a listener for changes in haptic feedback settings
        mIsGloballyEnabled = checkGlobalSetting(mContext)
        val uri = Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED)
        mContext.contentResolver.registerContentObserver(uri, false, mContentObserver)
    }

    /**
     * Method to verify that vibrate permission has been granted.
     *
     * Allows users of the library to disabled vibrate support if desired.
     * @return true if Vibrate permission has been granted
     */
    private fun hasVibratePermission(context: Context): Boolean {
        val pm = context.packageManager
        val hasPerm = pm.checkPermission(Manifest.permission.VIBRATE, context.packageName)
        return hasPerm == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Call this when you don't need the controller anymore.
     */
    fun stop() {
        mVibrator = null
        mContext.contentResolver.unregisterContentObserver(mContentObserver)
    }

    /**
     * Try to vibrate. To prevent this becoming a single continuous vibration, nothing will
     * happen if we have vibrated very recently.
     */
    fun tryVibrate() {
        if (mVibrator != null && mIsGloballyEnabled) {
            val now = SystemClock.uptimeMillis()
            // We want to try to vibrate each individual tick discretely.
            if (now - mLastVibrate >= VIBRATE_DELAY_MS) {
                mVibrator!!.vibrate(VIBRATE_LENGTH_MS.toLong())
                mLastVibrate = now
            }
        }
    }

    companion object {
        private const val VIBRATE_DELAY_MS = 125
        private const val VIBRATE_LENGTH_MS = 50
        private fun checkGlobalSetting(context: Context): Boolean {
            return Settings.System.getInt(context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 1
        }
    }

    init {
        mContentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                mIsGloballyEnabled = checkGlobalSetting(mContext)
            }
        }
    }
}
