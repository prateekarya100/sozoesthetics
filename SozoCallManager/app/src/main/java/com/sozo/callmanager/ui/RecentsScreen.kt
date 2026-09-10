package com.sozo.callmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sozo.callmanager.data.CallLogRepository
import com.sozo.callmanager.data.CallRecord
import com.sozo.callmanager.data.CallType
import com.sozo.callmanager.data.DemoSession
import com.sozo.callmanager.data.TrackedCallStore
import com.sozo.callmanager.dialer.CallActionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RecentsFilter(val label: String) {
    ALL("All"), MISSED("Missed"), INCOMING("Incoming"), OUTGOING("Outgoing")
}

@Composable
fun RecentsScreen(refreshTick: State<Int>) {
    val context = LocalContext.current
    var allCalls by remember { mutableStateOf(emptyList<CallRecord>()) }
    var filter by remember { mutableStateOf(RecentsFilter.ALL) }

    LaunchedEffect(refreshTick.value) {
        val calls = CallLogRepository.getRecentCalls(context, limit = 100)
        // Demo-only: claim any new calls for whoever is currently logged in.
        TrackedCallStore.tagUntaggedCalls(context, calls.map { it.id }, DemoSession.employeeId)
        // Privacy: only ever show the currently logged-in employee's own calls —
        // never another employee's, even on this shared demo device.
        val myIds = TrackedCallStore.callIdsForEmployee(context, DemoSession.employeeId, calls.map { it.id })
        allCalls = calls.filter { it.id in myIds }
    }

    val visibleCalls = when (filter) {
        RecentsFilter.ALL -> allCalls
        RecentsFilter.MISSED -> allCalls.filter { it.type == CallType.MISSED }
        RecentsFilter.INCOMING -> allCalls.filter { it.type == CallType.INCOMING }
        RecentsFilter.OUTGOING -> allCalls.filter { it.type == CallType.OUTGOING }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecentsFilter.values().forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) }
                )
            }
        }

        if (visibleCalls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No calls here yet. Make or receive a call — this list updates automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(visibleCalls, key = { it.id }) { call ->
                    RecentCallRow(call) { CallActionHelper.placeCall(context, call.number) }
                }
            }
        }
    }
}

@Composable
private fun RecentCallRow(call: CallRecord, onCallBack: () -> Unit) {
    val icon: ImageVector = when (call.type) {
        CallType.INCOMING -> Icons.Filled.CallReceived
        CallType.OUTGOING -> Icons.Filled.CallMade
        CallType.MISSED -> Icons.Filled.CallMissed
        CallType.UNKNOWN -> Icons.Filled.Phone
    }
    val tint = if (call.type == CallType.MISSED)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.secondary
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(call.displayName, fontWeight = FontWeight.Medium)
            Text(
                "${dateFormat.format(Date(call.timestampMillis))} \u00B7 ${call.durationSeconds}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onCallBack) {
            Icon(Icons.Filled.Call, contentDescription = "Call back", tint = MaterialTheme.colorScheme.primary)
        }
    }
    Divider()
}
