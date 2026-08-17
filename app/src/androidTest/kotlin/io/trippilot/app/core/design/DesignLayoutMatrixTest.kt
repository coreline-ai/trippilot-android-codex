package io.trippilot.app.core.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.trippilot.app.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DesignLayoutMatrixTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longKoreanEmptyStateAndCtaStayInsideCompactWidthAtTwoX() {
        val title = "아직 보관한 예약이 없습니다"
        val body = "예약처, 확인번호, 시간, 위치, 링크는 직접 확인한 뒤 직접 기록합니다."
        composeRule.setContent {
            TripPilotTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                    Box(Modifier.width(360.dp)) {
                        EmptyState(title, body, R.drawable.trippilot_empty_reservations)
                        PrimaryAction("선택한 초안 반영", onClick = {})
                    }
                }
            }
        }

        composeRule.onNodeWithTag("empty_state").assertIsDisplayed()
        val action = composeRule.onNodeWithTag("primary_action").fetchSemanticsNode().boundsInRoot
        val label = composeRule.onNodeWithText("선택한 초안 반영", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("CTA height must stay at least 52dp", action.height >= 52f)
        assertTrue("CTA label must stay inside the button", label.top >= action.top && label.bottom <= action.bottom)
        assertTrue("CTA must stay inside the 360dp column", action.right <= 360f + 1f)
    }
}
