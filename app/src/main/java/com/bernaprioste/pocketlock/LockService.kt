package com.bernaprioste.pocketlock

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Draws a black, touch-blocking overlay on top of whatever app is open.
 *
 * The app underneath stays in the foreground (and keeps window focus), so a
 * video keeps playing even though the screen looks off.
 */
class LockService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val volumeCombo = VolumeComboDetector()
    private var overlay: BlackoutView? = null

    val isLocked: Boolean get() = overlay != null

    private val screenOffReceiver = object : BroadcastReceiver() {
        // A real lock (power button) should not leave the blackout behind.
        override fun onReceive(context: Context, intent: Intent) = unlock()
    }

    private val accessibilityButtonCallback by lazy {
        object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) = toggle()
        }
    }

    override fun onServiceConnected() {
        instance = this
        ContextCompat.registerReceiver(
            this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        accessibilityButtonController.registerAccessibilityButtonCallback(accessibilityButtonCallback)
        refreshTile()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (instance !== this) return
        unlock()
        runCatching { unregisterReceiver(screenOffReceiver) }
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
        instance = null
        refreshTile()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun toggle() = if (isLocked) unlock() else lock()

    fun lock() {
        if (overlay != null) return
        val view = BlackoutView(this) { unlock() }
        windowManager().addView(view, overlayParams())
        overlay = view
        volumeCombo.reset()
        view.showHint()
    }

    fun unlock() {
        val view = overlay ?: return
        overlay = null
        runCatching { windowManager().removeView(view) }
    }

    /** Used by the Quick Settings tile: close the shade first so it isn't left above the video. */
    fun collapseShadeThenLock() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        } else {
            GLOBAL_ACTION_BACK
        }
        performGlobalAction(action)
        handler.postDelayed({ lock() }, SHADE_COLLAPSE_DELAY_MS)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = handleKey(event)

    /** Returns true when the key is consumed. Volume keys still change the volume while locked. */
    internal fun handleKey(event: KeyEvent): Boolean {
        if (!isLocked) return false
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> VolumeKey.UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeKey.DOWN
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH -> return true
            else -> return false
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0 && volumeCombo.onKeyDown(key, event.eventTime)) {
                unlock()
                return true
            }
            KeyEvent.ACTION_UP -> volumeCombo.onKeyUp(key)
        }
        return false
    }

    private fun windowManager() = getSystemService(WINDOW_SERVICE) as WindowManager

    private fun overlayParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        // NOT_FOCUSABLE lets the video app keep window focus; KEEP_SCREEN_ON stops the real sleep timeout.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.OPAQUE,
    ).apply {
        screenBrightness = MIN_BRIGHTNESS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fitInsetsTypes = 0
        }
    }

    private fun refreshTile() {
        TileService.requestListeningState(this, ComponentName(this, LockTileService::class.java))
    }

    companion object {
        @Volatile
        var instance: LockService? = null
            private set

        private const val SHADE_COLLAPSE_DELAY_MS = 450L
        private const val MIN_BRIGHTNESS = 0.01f
    }
}
