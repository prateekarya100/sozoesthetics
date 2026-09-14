package com.sozo.callmanager.data

import android.content.Context

/**
 * A small session holder. In the real app this would come from the Spring
 * Boot /api/auth/login response (JWT), not a hardcoded employee list.
 *
 * IMPORTANT: this now also persists a tiny amount of state to
 * SharedPreferences (who's logged in + whether setup finished). Android can,
 * and does, kill an app's whole background process to free memory while a
 * phone call is in progress — especially on tighter-memory or aggressively
 * "battery optimised" devices (MIUI, ColorOS, etc). Without this, an
 * in-memory-only session would silently reset to logged-out, so returning
 * from a call would land back on the Login screen instead of Recents — which
 * is exactly the "doesn't go back to Recents" bug this fixes.
 */
object DemoSession {

    private const val PREFS_NAME = "sozo_session"
    private const val KEY_EMPLOYEE_ID = "employee_id"
    private const val KEY_SETUP_DONE = "setup_done"

    var currentEmployee: DemoEmployee? = null
        private set

    var authToken: String? = null
        private set

    val isLoggedIn: Boolean get() = currentEmployee != null
    val employeeId: String get() = currentEmployee?.id ?: ""
    val employeeName: String get() = currentEmployee?.displayName ?: ""
    val employeeRole: String get() = currentEmployee?.role ?: ""

    fun login(context: Context, employeeId: String, password: String): Boolean {
        val employee = DemoEmployees.authenticate(employeeId, password)
        return if (employee != null) {
            currentEmployee = employee
            authToken = "demo-token-${employee.id}"
            prefs(context).edit().putString(KEY_EMPLOYEE_ID, employee.id).apply()
            true
        } else {
            false
        }
    }

    fun logout(context: Context) {
        currentEmployee = null
        authToken = null
        prefs(context).edit().clear().apply()
    }

    fun markSetupComplete(context: Context) {
        prefs(context).edit().putBoolean(KEY_SETUP_DONE, true).apply()
    }

    fun hasCompletedSetup(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_DONE, false)

    /**
     * Call once, at cold start (MainActivity.onCreate), before deciding which
     * screen to show. If the process was killed while an employee was
     * logged in, this brings the session back so the app can skip straight
     * to the dashboard instead of forcing a fresh login.
     */
    fun restoreIfNeeded(context: Context) {
        if (currentEmployee != null) return
        val savedId = prefs(context).getString(KEY_EMPLOYEE_ID, null) ?: return
        DemoEmployees.byId(savedId)?.let { employee ->
            currentEmployee = employee
            authToken = "demo-token-${employee.id}"
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
