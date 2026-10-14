package com.bernaprioste.pocketlock

import android.app.UiAutomation
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs on a real device/emulator with the accessibility service turned on,
 * and checks the behaviour that matters: the app underneath stays resumed
 * and focused, touches are blocked, and both unlock methods work.
 */
@RunWith(AndroidJUnit4::class)
class LockServiceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    // The default UiAutomation would suppress other accessibility services, including ours.
    private val uiAutomation: UiAutomation
        get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    private val service: LockService get() = LockService.instance!!

    @Before
    fun enableService() {
        val pkg = instrumentation.targetContext.packageName
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("settings put secure enabled_accessibility_services $pkg/${LockService::class.java.name}")
        shell("settings put secure accessibility_enabled 1")
        waitUntil(20_000) { LockService.instance != null }
        assertNotNull("Accessibility service never connected", LockService.instance)
    }

    @After
    fun unlockAfterTest() {
        instrumentation.runOnMainSync { LockService.instance?.unlock() }
    }

    @Test
    fun lockingKeepsTheAppUnderneathResumedAndFocused() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForFocus(scenario)

            lock()

            assertTrue(service.isLocked)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            var focused = false
            scenario.onActivity { focused = it.hasWindowFocus() }
            assertTrue("Underlying app lost window focus", focused)

            instrumentation.runOnMainSync { service.unlock() }
            assertFalse(service.isLocked)
        }
    }

    @Test
    fun touchesDoNotReachTheAppWhileLocked() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForFocus(scenario)
            val touches = AtomicInteger()
            scenario.onActivity { countTouches(it.window, touches) }
            val (x, y) = screenPoint(0.5f, 0.12f)

            lock()
            tap(x, y)
            instrumentation.waitForIdleSync()
            assertEquals("Touch leaked through the overlay", 0, touches.get())

            instrumentation.runOnMainSync { service.unlock() }
            SystemClock.sleep(300)
            tap(x, y)
            waitUntil(3_000) { touches.get() > 0 }
            assertTrue("Sanity check: app should get touches after unlock", touches.get() > 0)
        }
    }

    @Test
    fun volumeUpPlusVolumeDownUnlocks() {
        lock()
        var first = true
        var second = false
        instrumentation.runOnMainSync {
            val t = SystemClock.uptimeMillis()
            first = service.handleKey(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0))
            second = service.handleKey(KeyEvent(t, t + 100, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0))
        }
        assertFalse("A single volume press should still change the volume", first)
        assertTrue("The combo should be consumed", second)
        assertFalse(service.isLocked)
    }

    @Test
    fun singleVolumePressDoesNotUnlock() {
        lock()
        instrumentation.runOnMainSync {
            val t = SystemClock.uptimeMillis()
            service.handleKey(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0))
            service.handleKey(KeyEvent(t, t + 80, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0))
        }
        assertTrue(service.isLocked)
    }

    @Test
    fun backKeyIsBlockedOnlyWhileLocked() {
        val t = SystemClock.uptimeMillis()
        val back = KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
        var whileUnlocked = true
        instrumentation.runOnMainSync { whileUnlocked = service.handleKey(back) }
        assertFalse(whileUnlocked)

        lock()
        var whileLocked = false
        instrumentation.runOnMainSync { whileLocked = service.handleKey(back) }
        assertTrue(whileLocked)
    }

    @Test
    fun twoFingerHoldUnlocks() {
        lock()
        val (x1, y1) = screenPoint(0.35f, 0.5f)
        val (x2, y2) = screenPoint(0.65f, 0.5f)
        val down = SystemClock.uptimeMillis()

        inject(motion(down, MotionEvent.ACTION_DOWN, floatArrayOf(x1, y1)))
        inject(motion(down, pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1), floatArrayOf(x1, y1, x2, y2)))
        waitUntil(BlackoutView.HOLD_TO_UNLOCK_MS + 2_000) { !service.isLocked }
        assertFalse("Two-finger hold should unlock", service.isLocked)

        inject(motion(down, pointerAction(MotionEvent.ACTION_POINTER_UP, 1), floatArrayOf(x1, y1, x2, y2)))
        inject(motion(down, MotionEvent.ACTION_UP, floatArrayOf(x1, y1)))
    }

    @Test
    fun oneFingerHoldDoesNotUnlock() {
        lock()
        val (x, y) = screenPoint(0.5f, 0.5f)
        val down = SystemClock.uptimeMillis()
        inject(motion(down, MotionEvent.ACTION_DOWN, floatArrayOf(x, y)))
        SystemClock.sleep(BlackoutView.HOLD_TO_UNLOCK_MS + 1_000)
        inject(motion(down, MotionEvent.ACTION_UP, floatArrayOf(x, y)))
        instrumentation.waitForIdleSync()
        assertTrue(service.isLocked)
    }

    // --- helpers ---

    private fun lock() {
        instrumentation.runOnMainSync { service.lock() }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500)
        assertTrue(service.isLocked)
    }

    private fun shell(command: String) {
        uiAutomation.executeShellCommand(command).use { FileInputStream(it.fileDescriptor).readBytes() }
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
    }

    private fun waitForFocus(scenario: ActivityScenario<MainActivity>) {
        waitUntil(5_000) {
            var focused = false
            scenario.onActivity { focused = it.hasWindowFocus() }
            focused
        }
    }

    private fun screenPoint(fx: Float, fy: Float): Pair<Float, Float> {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        return metrics.widthPixels * fx to metrics.heightPixels * fy
    }

    private fun countTouches(window: Window, counter: AtomicInteger) {
        val original = window.callback
        window.callback = Proxy.newProxyInstance(
            Window.Callback::class.java.classLoader, arrayOf(Window.Callback::class.java),
        ) { _, method, args ->
            if (method.name == "dispatchTouchEvent") counter.incrementAndGet()
            method.invoke(original, *(args ?: emptyArray()))
        } as Window.Callback
    }

    private fun tap(x: Float, y: Float) {
        val down = SystemClock.uptimeMillis()
        inject(motion(down, MotionEvent.ACTION_DOWN, floatArrayOf(x, y)))
        inject(motion(down, MotionEvent.ACTION_UP, floatArrayOf(x, y)))
    }

    private fun inject(event: MotionEvent) {
        uiAutomation.injectInputEvent(event, true)
        event.recycle()
    }

    private fun pointerAction(action: Int, index: Int) =
        action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)

    /** [xy] holds x,y pairs, one per finger. */
    private fun motion(downTime: Long, action: Int, xy: FloatArray): MotionEvent {
        val count = xy.size / 2
        val props = Array(count) { i ->
            MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER }
        }
        val coords = Array(count) { i ->
            MotionEvent.PointerCoords().apply { x = xy[i * 2]; y = xy[i * 2 + 1]; pressure = 1f; size = 1f }
        }
        return MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), action, count, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }
}
