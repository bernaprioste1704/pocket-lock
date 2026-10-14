package com.bernaprioste.pocketlock

/**
 * Pure logic for the two ways to unlock the blackout screen, kept free of
 * Android classes so it can be unit tested on the JVM.
 */

enum class VolumeKey {
    UP, DOWN;

    val other: VolumeKey get() = if (this == UP) DOWN else UP
}

/** Detects Volume Up and Volume Down being held down together. */
class VolumeComboDetector(private val windowMs: Long = 600) {
    private val pressedAt = mutableMapOf<VolumeKey, Long>()

    /** Returns true when this press completes the combo. */
    fun onKeyDown(key: VolumeKey, now: Long): Boolean {
        if (pressedAt.containsKey(key)) return false
        pressedAt[key] = now
        val otherAt = pressedAt[key.other] ?: return false
        return now - otherAt <= windowMs
    }

    fun onKeyUp(key: VolumeKey) {
        pressedAt.remove(key)
    }

    fun reset() = pressedAt.clear()
}

/** Tracks a hold with at least [requiredPointers] fingers on the screen. */
class HoldTracker(private val holdMs: Long, private val requiredPointers: Int = 2) {
    private var startedAt: Long? = null

    val isHolding: Boolean get() = startedAt != null

    fun update(pointerCount: Int, now: Long) {
        if (pointerCount >= requiredPointers) {
            if (startedAt == null) startedAt = now
        } else {
            startedAt = null
        }
    }

    fun isTriggered(now: Long): Boolean = startedAt?.let { now - it >= holdMs } ?: false
}
