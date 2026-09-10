package com.sozo.callmanager.dialer

import android.content.Context
import com.sozo.callmanager.data.CallLogRepository
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Best-effort match of a just-ended Telecom call back to its eventual
 * Call Log entry. The Telecom `Call` object and the system Call Log are two
 * separate things with no shared ID, so we match on phone number + closest
 * start timestamp — reliable in practice since it's extremely unlikely two
 * calls from the same number are logged within moments of each other.
 *
 * Retries briefly (up to ~1.2s total) since the system can take a moment to
 * write the Call Log entry after a call ends — by the time someone finishes
 * typing a note this is almost always already available on the first try.
 */
suspend fun resolveCallId(context: Context, number: String, approxStartMillis: Long?): String? {
    repeat(4) { attempt ->
        val candidates = CallLogRepository.getRecentCalls(context, limit = 10).filter { it.number == number }
        val match = if (approxStartMillis != null) {
            candidates.minByOrNull { abs(it.timestampMillis - approxStartMillis) }
        } else {
            candidates.maxByOrNull { it.timestampMillis }
        }
        if (match != null) return match.id
        if (attempt < 3) delay(400)
    }
    return null
}
