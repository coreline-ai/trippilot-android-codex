package io.trippilot.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import io.trippilot.app.core.design.TripPilotApp
import io.trippilot.app.core.design.TripPilotTheme
import io.trippilot.app.feature.trips.TripViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val tripViewModel: TripViewModel by viewModels()
    private var incomingShareText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShareText = plainTextFromSendIntent(intent)
        enableEdgeToEdge()
        setContent {
            TripPilotTheme {
                TripPilotApp(viewModel = tripViewModel, incomingShareText = incomingShareText)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShareText = plainTextFromSendIntent(intent)
    }
}

internal fun plainTextFromSendIntent(intent: Intent?): String? = intent
    ?.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
    ?.getStringExtra(Intent.EXTRA_TEXT)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
