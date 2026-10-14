package com.bernaprioste.pocketlock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeComboDetectorTest {
    private val detector = VolumeComboDetector(windowMs = 600)

    @Test
    fun bothKeysTogetherTriggers() {
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_000))
        assertTrue(detector.onKeyDown(VolumeKey.DOWN, 1_100))
    }

    @Test
    fun orderDoesNotMatter() {
        assertFalse(detector.onKeyDown(VolumeKey.DOWN, 1_000))
        assertTrue(detector.onKeyDown(VolumeKey.UP, 1_050))
    }

    @Test
    fun separatePressesDoNotTrigger() {
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_000))
        detector.onKeyUp(VolumeKey.UP)
        assertFalse(detector.onKeyDown(VolumeKey.DOWN, 1_100))
    }

    @Test
    fun repeatedSameKeyDoesNotTrigger() {
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_000))
        detector.onKeyUp(VolumeKey.UP)
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_100))
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_200))
    }

    @Test
    fun secondKeyTooLateDoesNotTrigger() {
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_000))
        assertFalse(detector.onKeyDown(VolumeKey.DOWN, 2_000))
    }

    @Test
    fun resetForgetsHeldKeys() {
        assertFalse(detector.onKeyDown(VolumeKey.UP, 1_000))
        detector.reset()
        assertFalse(detector.onKeyDown(VolumeKey.DOWN, 1_100))
    }
}

class HoldTrackerTest {
    private val tracker = HoldTracker(holdMs = 1_500)

    @Test
    fun twoFingersHeldLongEnoughTriggers() {
        tracker.update(2, 0)
        assertFalse(tracker.isTriggered(1_499))
        assertTrue(tracker.isTriggered(1_500))
    }

    @Test
    fun oneFingerNeverTriggers() {
        tracker.update(1, 0)
        assertFalse(tracker.isHolding)
        assertFalse(tracker.isTriggered(10_000))
    }

    @Test
    fun liftingAFingerCancels() {
        tracker.update(2, 0)
        tracker.update(1, 1_000)
        assertFalse(tracker.isTriggered(2_000))
    }

    @Test
    fun extraFingersDoNotRestartTheTimer() {
        tracker.update(2, 0)
        tracker.update(3, 1_000)
        assertTrue(tracker.isTriggered(1_500))
    }

    @Test
    fun restartingTheHoldResetsTheTimer() {
        tracker.update(2, 0)
        tracker.update(0, 1_000)
        tracker.update(2, 1_200)
        assertFalse(tracker.isTriggered(2_000))
        assertTrue(tracker.isTriggered(2_700))
    }
}
