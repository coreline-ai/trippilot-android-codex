package io.trippilot.app.baselineprofile

import android.widget.EditText
import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Local-only critical user journey for the generated app profile.
 *
 * It covers cold start, empty trip list, local trip creation/detail, and fake structured-draft
 * review. This is still a local, deterministic fixture: it never starts OAuth, Calendar,
 * browser/map, SAF, or an external network request.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun startupAndLocalTripReview() = baselineProfileRule.collect(
        packageName = TRIPPILOT_PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        clickVisibleText("새 여행 만들기")
        val inputs = waitForTripEditorInputs()
        inputs[0].text = "Profile Seoul"
        inputs[1].text = "Seoul"
        clickVisibleText("여행 만들기")
        check(device.wait(Until.gone(By.text("여행 만들기")), UI_TIMEOUT_MS)) { "Trip editor did not close" }
        device.wait(Until.findObject(By.descContains("Profile Seoul, Seoul")), UI_TIMEOUT_MS)
            ?.let(::tap)
            ?: error("Timed out waiting for the created local trip card")
        clickVisibleText("초안 검토 열기")
        clickVisibleText("여행 초안 만들기")
        // The fake stream is in-memory and has no external side effect. A short settle time lets
        // its ViewModel/Compose review path execute during every profile collection iteration;
        // visual completion is asserted independently by the deterministic golden UI test.
        SystemClock.sleep(1_000)
    }

    private fun waitForTripEditorInputs() = run {
        device.wait(Until.hasObject(By.clazz(EditText::class.java)), UI_TIMEOUT_MS)
        device.findObjects(By.clazz(EditText::class.java)).also { inputs ->
            check(inputs.size >= 2) { "Expected title and destination inputs" }
        }
    }

    private fun clickVisibleText(text: String) {
        repeat(6) {
            val node = device.wait(Until.findObject(By.desc(text)), 500)
                ?: device.wait(Until.findObject(By.clickable(true).hasDescendant(By.text(text))), 500)
                // Compose can merge a Material button's text into the clickable semantics node.
                // In that case the text node itself is the reliable coordinate target.
                ?: device.wait(Until.findObject(By.text(text)), 500)
            node?.let {
                tap(it)
                return
            }
            device.swipe(device.displayWidth / 2, device.displayHeight - 320, device.displayWidth / 2, 420, 16)
        }
        error("Timed out waiting for actionable $text")
    }

    private fun tap(node: UiObject2) {
        val bounds = node.visibleBounds
        check(!bounds.isEmpty) { "Action target is outside the visible viewport" }
        check(device.click(bounds.centerX(), bounds.centerY())) { "Could not tap action target" }
    }

    private companion object {
        const val TRIPPILOT_PACKAGE = "io.trippilot.app"
        const val UI_TIMEOUT_MS = 10_000L
    }
}
