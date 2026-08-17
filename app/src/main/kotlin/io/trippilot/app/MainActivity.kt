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
import io.trippilot.app.feature.drafts.TripDraftViewModel
import io.trippilot.app.feature.external.TripExternalViewModel
import io.trippilot.app.feature.external.TripFileViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val tripViewModel: TripViewModel by viewModels()
    private val tripDraftViewModel: TripDraftViewModel by viewModels()
    private val tripExternalViewModel: TripExternalViewModel by viewModels()
    private val tripFileViewModel: TripFileViewModel by viewModels()
    private var incomingShareText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShareText = plainTextFromSendIntent(intent)
        enableEdgeToEdge()
        setContent {
            TripPilotTheme {
                TripPilotApp(
                    viewModel = tripViewModel,
                    draftViewModel = tripDraftViewModel,
                    externalViewModel = tripExternalViewModel,
                    fileViewModel = tripFileViewModel,
                    incomingShareText = incomingShareText,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShareText = plainTextFromSendIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // This checks only an already pending Device OAuth attempt after a browser return. It
        // never starts login or replays a travel request when the app comes back to foreground.
        tripDraftViewModel.refreshCodexAfterBrowserReturn()
    }
}

internal fun plainTextFromSendIntent(intent: Intent?): String? = intent
    ?.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
    ?.getStringExtra(Intent.EXTRA_TEXT)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
