package com.sozo.callmanager.dialer

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.compose.runtime.mutableStateOf

/**
 * Bridges the raw Telecom Call/InCallService callbacks (which know nothing
 * about Compose) into simple, observable state that InCallActivity's UI reads
 * directly. This is what lets the in-call screen show the REAL call state
 * (ringing vs dialing vs active vs ended) instead of a locally-guessed one —
 * which was the root cause of "outgoing calls show Answer/Decline",
 * "duration never calculated", etc.
 */
object CallHolder {

    var currentCall: Call? = null
        private set

    /** Set by SozoInCallService itself so mute/speaker actions have something to call. */
    var service: InCallService? = null

    /** Mirrors Call.STATE_* (RINGING, DIALING, ACTIVE, DISCONNECTED...). Null = no active call. */
    val callState = mutableStateOf<Int?>(null)

    /** True only if this call was already ringing when it reached us — i.e. genuinely incoming. */
    val isIncoming = mutableStateOf(false)

    /** Wall-clock time the call actually became ACTIVE (answered/connected). Drives the live timer. */
    val connectedAtMillis = mutableStateOf<Long?>(null)

    /**
     * The number for the call currently showing. Captured once when a call
     * attaches and deliberately NOT cleared on detach — this is what stops
     * the screen flashing to "Unknown number" during the brief window after
     * the call disconnects but before this screen finishes.
     */
    val phoneNumber = mutableStateOf("Unknown number")

    /**
     * Real telecom creation time for the currently/most-recently held call.
     * Also deliberately kept after detach — used to match this call against
     * its eventual system Call Log row so a post-call note can be attached
     * to the right entry (see CallActivity's note dialog).
     */
    val callCreationTimeMillis = mutableStateOf<Long?>(null)

    val audioState = mutableStateOf<CallAudioState?>(null)

    /** Set right before returning to the app after a call ends, so MainScaffold can jump to Recents. */
    val shouldFocusRecentsOnReturn = mutableStateOf(false)

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            callState.value = state
            if (state == Call.STATE_ACTIVE && connectedAtMillis.value == null) {
                connectedAtMillis.value = System.currentTimeMillis()
            }
        }
    }

    fun attach(call: Call) {
        currentCall = call
        phoneNumber.value = call.details?.handle?.schemeSpecificPart ?: "Unknown number"
        callCreationTimeMillis.value = call.details?.creationTimeMillis
        val initialState = call.state
        // If it's already ringing when added, it's an incoming call;
        // dialing/connecting means we placed it ourselves.
        isIncoming.value = initialState == Call.STATE_RINGING
        callState.value = initialState
        connectedAtMillis.value = if (initialState == Call.STATE_ACTIVE) System.currentTimeMillis() else null
        call.registerCallback(callback)
    }

    fun detach(call: Call) {
        if (currentCall == call) {
            call.unregisterCallback(callback)
            currentCall = null
            callState.value = null
            connectedAtMillis.value = null
            isIncoming.value = false
            // phoneNumber is deliberately left as-is here — see its doc comment.
        }
    }

    fun toggleMute(mute: Boolean) {
        service?.setMuted(mute)
    }

    fun setSpeakerOn(on: Boolean) {
        val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        service?.setAudioRoute(route)
    }
}
