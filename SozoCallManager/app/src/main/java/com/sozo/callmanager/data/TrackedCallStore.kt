package com.sozo.callmanager.data

import android.content.Context

/**
 * DEMO-ONLY mechanism: since this environment tests multiple "employees"
 * from a single physical phone, we need a way to know which demo employee
 * was logged in when a given call happened. We do this by tagging any
 * not-yet-tagged call-log entries to whichever employee is currently logged
 * in, every time the app refreshes (see rememberCallLogRefreshTrigger).
 *
 * IMPORTANT: In the real deployment, every employee has their own phone
 * running their own instance of the app — every call on that phone
 * automatically belongs to that employee. This whole file becomes
 * unnecessary and should be deleted once real multi-device rollout happens.
 */
object TrackedCallStore {

    private const val PREFS_NAME = "sozo_tracked_calls"
    private const val KEY_PREFIX = "call_"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEmployeeForCall(context: Context, callId: String): String? =
        prefs(context).getString(KEY_PREFIX + callId, null)

    /**
     * Call this every time the call log is (re)read. Any call ID not already
     * tagged gets tagged to whichever employee is currently logged in.
     */
    fun tagUntaggedCalls(context: Context, allCallIds: List<String>, currentEmployeeId: String) {
        if (currentEmployeeId.isBlank()) return
        val p = prefs(context)
        val editor = p.edit()
        var changed = false
        for (id in allCallIds) {
            val key = KEY_PREFIX + id
            if (!p.contains(key)) {
                editor.putString(key, currentEmployeeId)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    /** Of the given call IDs, returns the subset tagged to [employeeId]. */
    fun callIdsForEmployee(context: Context, employeeId: String, allCallIds: List<String>): Set<String> {
        val p = prefs(context)
        return allCallIds.filter { p.getString(KEY_PREFIX + it, null) == employeeId }.toSet()
    }

    /** Wipes all demo tagging — useful for resetting a test run. */
    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
