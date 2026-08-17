package io.trippilot.app.core.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DesignTokenContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val tokens = JSONObject(
        InstrumentationRegistry.getInstrumentation().context.assets.open("design-tokens.json").use { it.readBytes().decodeToString() },
    )

    @Test
    fun lightThemeMatchesTokenContract() {
        assertTheme(dark = false)
    }

    @Test
    fun darkThemeMatchesTokenContract() {
        assertTheme(dark = true)
    }

    @Test
    fun actionAndChipShapesFollowContract() {
        composeRule.setContent {
            TripPilotTheme(darkTheme = false) {
                assertEquals(14.dp, cornerDp(MaterialTheme.shapes.small))
                assertEquals(20.dp, cornerDp(MaterialTheme.shapes.large))
                assertEquals(28.dp, cornerDp(MaterialTheme.shapes.extraLarge))
                assertEquals(TripPilotActionShape, RoundedCornerShape(14.dp))
                assertEquals(TripPilotHeroShape, RoundedCornerShape(28.dp))
                PrimaryAction(label = "새 여행 만들기", onClick = {})
                StatusChip("확인됨")
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertTheme(dark: Boolean) {
        val mode = if (dark) "dark" else "light"
        val colors = tokens.getJSONObject("color").getJSONObject(mode)
        composeRule.setContent {
            TripPilotTheme(darkTheme = dark) {
                val scheme = MaterialTheme.colorScheme
                assertColor(colors.getString("primary"), scheme.primary)
                assertColor(colors.getString("onPrimary"), scheme.onPrimary)
                assertColor(colors.getString("wayfinding"), scheme.secondary)
                assertColor(colors.getString("onWayfinding"), scheme.onSecondary)
                assertColor(colors.getString("ai"), scheme.tertiary)
                assertColor(colors.getString("onAi"), scheme.onTertiary)
                assertColor(colors.getString("error"), scheme.error)
                assertColor(colors.getString("surface"), scheme.surface)
                assertColor(colors.getString("onSurface"), scheme.onSurface)
                val boarding = if (dark) TripPilotBoardingOrangeDark else TripPilotBoardingOrange
                assertColor(colors.getString("boarding"), boarding)
                assertEquals(28, MaterialTheme.typography.displaySmall.fontSize.value.toInt())
                assertEquals(22, MaterialTheme.typography.headlineSmall.fontSize.value.toInt())
                assertEquals(16, MaterialTheme.typography.bodyLarge.fontSize.value.toInt())
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun cornerDp(shape: CornerBasedShape) = with(LocalDensity.current) {
        shape.topStart.toPx(Size.Zero, this).toDp()
    }

    private fun assertColor(expectedHex: String, actual: Color) {
        assertEquals(expectedHex.uppercase(), actual.toRgbHex())
    }

    private fun Color.toRgbHex(): String = "#%06X".format(0xFFFFFF and toArgb())
}
