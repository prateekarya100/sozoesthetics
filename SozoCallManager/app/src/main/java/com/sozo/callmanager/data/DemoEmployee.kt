package com.sozo.callmanager.data

/**
 * Demo employees used purely for testing multi-employee behaviour on a
 * single physical device. In the real deployment, each employee logs into
 * the real Spring Boot backend and this hardcoded list disappears entirely.
 */
data class DemoEmployee(
    val id: String,
    val password: String,
    val displayName: String,
    val role: String
)

object DemoEmployees {

    val ALL = listOf(
        DemoEmployee(id = "demo", password = "demo123", displayName = "Reception Desk", role = "Reception"),
        DemoEmployee(id = "sales1", password = "sales123", displayName = "Aditi (Sales)", role = "Sales"),
        DemoEmployee(id = "sales2", password = "sales123", displayName = "Rohit (Sales)", role = "Sales"),
        DemoEmployee(id = "doctor", password = "doctor123", displayName = "Dr. Mehta", role = "Doctor")
    )

    fun authenticate(id: String, password: String): DemoEmployee? =
        ALL.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) && it.password == password }

    fun byId(id: String): DemoEmployee? = ALL.firstOrNull { it.id == id }
}
