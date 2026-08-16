package io.trippilot.app.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PilotNavy = Color(0xFF10243F)
private val CloudPaper = Color(0xFFF5F8FA)
private val SurfaceInk = Color(0xFF16202A)
private val Cyan = Color(0xFF16A8B8)
private val BoardingOrange = Color(0xFFF26A3D)
private val StampViolet = Color(0xFF6C63D9)
private val MossGreen = Color(0xFF25785C)
private val SignalRed = Color(0xFFB3261E)

private val LightTripPilotScheme = lightColorScheme(
    primary = PilotNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F4),
    onPrimaryContainer = Color(0xFF0D1F35),
    secondary = Cyan,
    onSecondary = Color(0xFF002F35),
    tertiary = StampViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6E2FF),
    onTertiaryContainer = Color(0xFF282352),
    error = SignalRed,
    onError = Color.White,
    background = CloudPaper,
    onBackground = SurfaceInk,
    surface = CloudPaper,
    onSurface = SurfaceInk,
    surfaceVariant = Color(0xFFE0E7ED),
    onSurfaceVariant = Color(0xFF38454F),
)

private val DarkTripPilotScheme = darkColorScheme(
    primary = Color(0xFFB8CCE8),
    onPrimary = PilotNavy,
    primaryContainer = Color(0xFF1A3655),
    onPrimaryContainer = Color(0xFFD6E4F4),
    secondary = Color(0xFF72D8E1),
    onSecondary = Color(0xFF00363B),
    tertiary = Color(0xFFC5BFFF),
    onTertiary = Color(0xFF292454),
    tertiaryContainer = Color(0xFF3D376C),
    onTertiaryContainer = Color(0xFFE6E1FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF0E151C),
    onBackground = Color(0xFFEAF1F6),
    surface = Color(0xFF0E151C),
    onSurface = Color(0xFFEAF1F6),
    surfaceVariant = Color(0xFF25313B),
    onSurfaceVariant = Color(0xFFD6E3EC),
)

@Composable
fun TripPilotTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkTripPilotScheme else LightTripPilotScheme,
        typography = TripPilotTypography,
        content = content,
    )
}

internal val TripPilotBoardingOrange = BoardingOrange
internal val TripPilotMossGreen = MossGreen
