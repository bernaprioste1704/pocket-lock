package com.bernaprioste.pocketlock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView

/** Full-screen black view that swallows every touch until it is unlocked. */
@SuppressLint("ViewConstructor")
class BlackoutView(context: Context, private val onUnlock: () -> Unit) : FrameLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val hold = HoldTracker(HOLD_TO_UNLOCK_MS)

    private val hint = TextView(context).apply {
        setTextColor(Color.rgb(90, 90, 90))
        textSize = 15f
        gravity = Gravity.CENTER
        alpha = 0f
        val pad = (32 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    private val unlockIfStillHolding = Runnable {
        if (hold.isTriggered(SystemClock.uptimeMillis())) onUnlock()
    }

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            showHint()
            return true
        }
    })

    init {
        setBackgroundColor(Color.BLACK)
        addView(hint, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    fun showHint(text: CharSequence = context.getString(R.string.overlay_hint)) {
        hint.text = text
        hint.animate().cancel()
        hint.alpha = 1f
        hint.animate().alpha(0f).setStartDelay(HINT_VISIBLE_MS).setDuration(600).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestures.onTouchEvent(event)

        val fingersDown = when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> 0
            MotionEvent.ACTION_POINTER_UP -> event.pointerCount - 1
            else -> event.pointerCount
        }
        val wasHolding = hold.isHolding
        hold.update(fingersDown, event.eventTime)

        if (hold.isHolding && !wasHolding) {
            showHint(context.getString(R.string.overlay_keep_holding))
            handler.postDelayed(unlockIfStillHolding, HOLD_TO_UNLOCK_MS)
        } else if (!hold.isHolding && wasHolding) {
            handler.removeCallbacks(unlockIfStillHolding)
        }
        return true
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    companion object {
        const val HOLD_TO_UNLOCK_MS = 1500L
        private const val HINT_VISIBLE_MS = 2500L
    }
}
