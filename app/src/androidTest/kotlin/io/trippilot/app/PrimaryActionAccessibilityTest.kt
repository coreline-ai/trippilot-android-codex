package io.trippilot.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.trippilot.app.core.design.PrimaryAction
import io.trippilot.app.core.design.TripPilotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrimaryActionAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun primaryActionKeepsTwoXKoreanLabelInsideItsTouchTarget() {
        composeRule.setContent {
            TripPilotTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                    Box(Modifier.width(360.dp)) {
                        PrimaryAction(label = "새 여행 만들기", onClick = {})
                    }
                }
            }
        }

        val actionBounds = composeRule.onNodeWithTag("primary_action").fetchSemanticsNode().boundsInRoot
        val labelBounds = composeRule
            .onNodeWithText("새 여행 만들기", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("CTA must preserve the 52dp minimum touch target", actionBounds.height >= 52f)
        assertTrue("CTA label top must remain inside its button", labelBounds.top >= actionBounds.top)
        assertTrue("CTA label bottom must remain inside its button", labelBounds.bottom <= actionBounds.bottom)
    }
}
