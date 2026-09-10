package com.sozo.callmanager.data

data class CallRecord(
    val id: String,
    val number: String,
    val displayName: String,
    val type: CallType,
    val timestampMillis: Long,
    val durationSeconds: Long
)

enum class CallType { INCOMING, OUTGOING, MISSED, UNKNOWN }

data class CallStats(
    val totalToday: Int,
    val incomingToday: Int,
    val outgoingToday: Int,
    val missedToday: Int
)
