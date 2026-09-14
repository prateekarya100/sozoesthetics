package com.sozo.callmanager.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import java.util.Calendar

/**
 * Reads real call-log data off the device (READ_CALL_LOG permission required).
 * This is intentionally simple — in the real Sozo system this is where we'd
 * instead push each record to the Spring Boot backend (see BackendApi).
 */
object CallLogRepository {

    fun getRecentCalls(context: Context, limit: Int = 30): List<CallRecord> {
        val records = mutableListOf<CallRecord>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val idCol = it.getColumnIndex(CallLog.Calls._ID)
            val numberCol = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
            val durationCol = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext() && records.size < limit) {
                val rawType = it.getInt(typeCol)
                val type = when (rawType) {
                    CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    // A call the user explicitly declined, or one blocked by
                    // the OS, never got through — for reporting purposes
                    // that's the same as "missed" (previously these silently
                    // fell into UNKNOWN, which is why Total didn't match
                    // Incoming + Outgoing + Missed).
                    CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                    CallLog.Calls.BLOCKED_TYPE -> CallType.MISSED
                    else -> CallType.UNKNOWN
                }
                val number = it.getString(numberCol) ?: "Unknown"
                val name = it.getString(nameCol) ?: number

                records.add(
                    CallRecord(
                        id = it.getString(idCol) ?: (number + it.getLong(dateCol)),
                        number = number,
                        displayName = name,
                        type = type,
                        timestampMillis = it.getLong(dateCol),
                        durationSeconds = it.getLong(durationCol)
                    )
                )
            }
        }
        return records
    }

    /** All calls between [startMillis] (inclusive) and [endMillis] (exclusive), newest first. */
    fun getCallsInRange(context: Context, startMillis: Long, endMillis: Long, limit: Int = 3000): List<CallRecord> {
        return getRecentCalls(context, limit = limit)
            .filter { it.timestampMillis in startMillis until endMillis }
    }

    fun getTodayStats(context: Context): CallStats {
        val startOfDay = startOfDayMillis()
        val all = getCallsInRange(context, startOfDay, Long.MAX_VALUE, limit = 1000)
        return statsFrom(all)
    }

    fun statsFrom(calls: List<CallRecord>): CallStats = CallStats(
        totalToday = calls.size,
        incomingToday = calls.count { it.type == CallType.INCOMING },
        outgoingToday = calls.count { it.type == CallType.OUTGOING },
        missedToday = calls.count { it.type == CallType.MISSED }
    )

    /** Midnight, [daysAgo] days before today (0 = today), in device-local time. */
    fun startOfDayMillis(daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Registers a ContentObserver on the call log so the caller can react
     * immediately when a new call is logged. Combined with an on-resume and
     * a periodic poll (see rememberCallLogRefreshTrigger) since some OEM
     * ROMs delay or skip this callback.
     */
    fun observeChanges(context: Context, onChange: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        return observer
    }

    fun stopObserving(context: Context, observer: ContentObserver) {
        context.contentResolver.unregisterContentObserver(observer)
    }
}
