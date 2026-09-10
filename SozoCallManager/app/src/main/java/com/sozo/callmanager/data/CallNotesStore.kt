package com.sozo.callmanager.data

import android.content.Context

/**
 * Stores a free-text note against one specific call-log entry (by call ID) —
 * not against a phone number. The same customer's different calls can each
 * carry their own note (e.g. "Asked about pricing" on Monday's call vs
 * "Booked appointment for Friday" on Wednesday's call).
 */
object CallNotesStore {

    private const val PREFS_NAME = "sozo_call_notes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNote(context: Context, callId: String): String? =
        prefs(context).getString(callId, null)?.takeIf { it.isNotBlank() }

    fun setNote(context: Context, callId: String, note: String) {
        prefs(context).edit().putString(callId, note).apply()
    }

    fun clearNote(context: Context, callId: String) {
        prefs(context).edit().remove(callId).apply()
    }
}
