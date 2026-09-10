package com.sozo.callmanager.dialer

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * Android requires an app to implement InCallService before RoleManager will
 * let it become the default Phone/Dialer app. Once set as default, every
 * incoming/outgoing call on the device is routed through here, which is what
 * gives Sozo legitimate, policy-compliant access to call events.
 */
class SozoInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallHolder.service = this
    }

    override fun onDestroy() {
        CallHolder.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallHolder.attach(call)
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallHolder.detach(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallHolder.audioState.value = audioState
    }
}
