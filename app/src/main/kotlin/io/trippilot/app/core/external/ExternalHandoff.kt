package io.trippilot.app.core.external

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.trippilot.app.core.model.TravelValidators
import io.trippilot.app.core.model.ValidationResult

/** Android handoff only. The caller must show a TripPilot confirmation before invoking either method. */
object ExternalHandoff {
    fun openMap(context: Context, place: String): Result<Unit> = runCatching {
        require(place.trim().isNotEmpty()) { "열 장소가 없습니다." }
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place.trim())}")))
    }

    fun openWebLink(context: Context, url: String): Result<Unit> = runCatching {
        require(TravelValidators.url(url) is ValidationResult.Valid) { "유효한 https 또는 http 링크가 아닙니다." }
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())))
    }

    private fun start(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) {
            throw ActivityNotFoundException("처리할 앱을 찾을 수 없습니다.")
        }
        context.startActivity(intent)
    }
}
