package com.sozo.callmanager.ui

import android.database.ContentObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sozo.callmanager.data.CallLogRepository
import kotlinx.coroutines.delay

/**
 * Returns an Int "tick" that increases whenever the call log might have
 * changed: on first composition, whenever the screen resumes (e.g. coming
 * back from a call), whenever Android notifies us via ContentObserver, and
 * every few seconds as a safety net (some OEM ROMs — MIUI, ColorOS, etc. —
 * delay or skip the ContentObserver callback).
 *
 * Screens key their reload logic off this tick, e.g.:
 *   val refreshTick = rememberCallLogRefreshTrigger()
 *   LaunchedEffect(refreshTick.value) { reload() }
 *
 * This is what makes the dashboard/recents/reports update automatically —
 * no more "log out and back in to see fresh data".
 */
@Composable
fun rememberCallLogRefreshTrigger(pollMillis: Long = 4000L): State<Int> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tick = remember { mutableStateOf(0) }

    // 1) React immediately to call-log content changes.
    DisposableEffect(Unit) {
        val observer: ContentObserver = CallLogRepository.observeChanges(context) {
            tick.value++
        }
        onDispose { CallLogRepository.stopObserving(context, observer) }
    }

    // 2) Refresh whenever the screen comes back to the foreground.
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick.value++
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    // 3) Safety-net poll, in case an OEM ROM swallows the ContentObserver callback.
    LaunchedEffect(Unit) {
        while (true) {
            delay(pollMillis)
            tick.value++
        }
    }

    return tick
}
