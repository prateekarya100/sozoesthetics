package com.sozo.callmanager.data

/**
 * Very small in-memory "session". In the real app this would come from the
 * Spring Boot /api/auth/login response (JWT), not a hardcoded employee list.
 */
object DemoSession {

    var currentEmployee: DemoEmployee? = null
        private set

    var authToken: String? = null
        private set

    val isLoggedIn: Boolean get() = currentEmployee != null
    val employeeId: String get() = currentEmployee?.id ?: ""
    val employeeName: String get() = currentEmployee?.displayName ?: ""
    val employeeRole: String get() = currentEmployee?.role ?: ""

    fun login(employeeId: String, password: String): Boolean {
        val employee = DemoEmployees.authenticate(employeeId, password)
        return if (employee != null) {
            currentEmployee = employee
            authToken = "demo-token-${employee.id}"
            true
        } else {
            false
        }
    }

    fun logout() {
        currentEmployee = null
        authToken = null
    }
}
