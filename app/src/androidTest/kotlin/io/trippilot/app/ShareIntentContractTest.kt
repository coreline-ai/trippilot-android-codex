package io.trippilot.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentContractTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun onlyPlainTextSendIsRoutableToTripPilot() {
        fun isTripPilotTarget(intent: Intent): Boolean = context.packageManager
            .queryIntentActivities(intent, 0)
            .any { it.activityInfo.packageName == context.packageName }

        assertTrue(isTripPilotTarget(Intent(Intent.ACTION_SEND).setType("text/plain")))
        assertFalse(isTripPilotTarget(Intent(Intent.ACTION_SEND).setType("image/png")))
        assertFalse(
            plainTextFromSendIntent(
                Intent(Intent.ACTION_SEND).setType("*/*").putExtra(Intent.EXTRA_TEXT, "not accepted"),
            ) != null,
        )
    }
}
