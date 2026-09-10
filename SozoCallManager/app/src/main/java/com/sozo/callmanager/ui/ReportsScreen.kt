package com.sozo.callmanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sozo.callmanager.data.CallLogRepository
import com.sozo.callmanager.data.CallNotesStore
import com.sozo.callmanager.data.CallRecord
import com.sozo.callmanager.data.CallType
import com.sozo.callmanager.data.DemoSession
import com.sozo.callmanager.data.TrackedCallStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ReportRange(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month") }

private data class Bucket(val label: String, val fullLabel: String, val count: Int)

private data class NumberGroup(val number: String, val calls: List<CallRecord>)

/**
 * Shows reports for the CURRENTLY LOGGED-IN employee only. This deliberately
 * never loads or displays another employee's calls or stats — each logged-in
 * user should only ever see their own numbers.
 */
@Composable
fun ReportsScreen(refreshTick: State<Int>) {
    val context = LocalContext.current
    var range by remember { mutableStateOf(ReportRange.DAY) }
    var myCalls by remember { mutableStateOf(emptyList<CallRecord>()) }

    LaunchedEffect(refreshTick.value) {
        // Look back far enough to cover the "Month" view (approx 6 months).
        val start = CallLogRepository.startOfDayMillis(daysAgo = 185)
        val allDeviceCalls = CallLogRepository.getCallsInRange(context, start, Long.MAX_VALUE, limit = 3000)
        TrackedCallStore.tagUntaggedCalls(context, allDeviceCalls.map { it.id }, DemoSession.employeeId)

        // Privacy: filter down to only this employee's own tagged calls before
        // computing anything — the chart and stats below never see the rest.
        val myIds = TrackedCallStore.callIdsForEmployee(context, DemoSession.employeeId, allDeviceCalls.map { it.id })
        myCalls = allDeviceCalls.filter { it.id in myIds }
    }

    val buckets = remember(myCalls, range) { buildBuckets(myCalls, range) }
    val maxCount = (buckets.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val hasAnyData = myCalls.isNotEmpty()

    val todayStart = CallLogRepository.startOfDayMillis()
    val todaysCalls = myCalls.filter { it.timestampMillis >= todayStart }

    // Groups every call by the customer's number — this is what shows
    // "how many times did this number call, and what happened each time".
    val numberGroups = remember(myCalls) {
        myCalls.groupBy { it.number }
            .map { (num, calls) -> NumberGroup(num, calls.sortedByDescending { it.timestampMillis }) }
            .sortedByDescending { it.calls.size }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("My Call Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${DemoSession.employeeName} \u00B7 ${DemoSession.employeeRole}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.values().forEach { r ->
                    FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) })
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!hasAnyData) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No calls recorded for you yet. Once you make or receive a call while logged in, it'll show up here.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when (range) {
                                    ReportRange.DAY -> "Your calls per day \u2014 last 7 days"
                                    ReportRange.WEEK -> "Your calls per week \u2014 last 6 weeks"
                                    ReportRange.MONTH -> "Your calls per month \u2014 last 6 months"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${buckets.sumOf { it.count }} total",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Each bar is one ${range.label.lowercase()}. Tap a bar to see its exact count.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        BarChart(
                            buckets = buckets,
                            maxCount = maxCount,
                            todayIndex = if (range == ReportRange.DAY) buckets.lastIndex else null,
                            modifier = Modifier.fillMaxWidth().height(210.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MyTodayCard(
                    total = todaysCalls.size,
                    incoming = todaysCalls.count { it.type == CallType.INCOMING },
                    outgoing = todaysCalls.count { it.type == CallType.OUTGOING },
                    missed = todaysCalls.count { it.type == CallType.MISSED }
                )

                Spacer(Modifier.height(24.dp))
                Text("Calls by number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tap a number to see every call and its note; tap a call to add or edit its note.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (hasAnyData) {
            items(numberGroups, key = { it.number }) { group ->
                NumberGroupCard(group)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private fun buildBuckets(calls: List<CallRecord>, range: ReportRange): List<Bucket> {
    return when (range) {
        ReportRange.DAY -> {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val fullDayFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
            (6 downTo 0).map { daysAgo ->
                val start = CallLogRepository.startOfDayMillis(daysAgo)
                val end = start + 24L * 60 * 60 * 1000
                val count = calls.count { it.timestampMillis in start until end }
                val label = if (daysAgo == 0) "Today" else dayFormat.format(Date(start))
                val fullLabel = if (daysAgo == 0) "Today, ${fullDayFormat.format(Date(start))}" else fullDayFormat.format(Date(start))
                Bucket(label, fullLabel, count)
            }
        }
        ReportRange.WEEK -> {
            val rangeFormat = SimpleDateFormat("d MMM", Locale.getDefault())
            (5 downTo 0).map { weeksAgo ->
                val start = CallLogRepository.startOfDayMillis(daysAgo = weeksAgo * 7 + 6)
                val end = CallLogRepository.startOfDayMillis(daysAgo = weeksAgo * 7) + 24L * 60 * 60 * 1000
                val count = calls.count { it.timestampMillis in start until end }
                val label = if (weeksAgo == 0) "This wk" else "W${6 - weeksAgo}"
                val fullLabel = "${rangeFormat.format(Date(start))} \u2013 ${rangeFormat.format(Date(end - 1))}"
                Bucket(label, fullLabel, count)
            }
        }
        ReportRange.MONTH -> {
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val fullMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            (5 downTo 0).map { monthsAgo ->
                val monthCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -monthsAgo)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = monthCal.timeInMillis
                val endCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val end = endCal.timeInMillis
                val count = calls.count { it.timestampMillis in start until end }
                Bucket(monthFormat.format(Date(start)), fullMonthFormat.format(Date(start)), count)
            }
        }
    }
}

@Composable
private fun BarChart(buckets: List<Bucket>, maxCount: Int, todayIndex: Int?, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val fadedBarColor = barColor.copy(alpha = 0.32f)
    val todayColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface.toArgb()

    // Default to highlighting "today" (Day view) so the chart is meaningful
    // even before the user taps anything.
    var selectedIndex by remember(buckets) { mutableStateOf(todayIndex) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val barCount = buckets.size
                        val spacing = size.width * 0.04f
                        val barWidth = (size.width - spacing * (barCount + 1)) / barCount
                        val tappedIndex = ((offset.x - spacing) / (barWidth + spacing))
                            .toInt()
                            .coerceIn(0, barCount - 1)
                        selectedIndex = tappedIndex
                    }
                }
        ) {
            if (buckets.isEmpty()) return@Canvas
            val barCount = buckets.size
            val spacing = size.width * 0.04f
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (size.width - totalSpacing) / barCount
            val chartHeight = size.height - 22.dp.toPx()

            buckets.forEachIndexed { index, bucket ->
                val heightRatio = bucket.count.toFloat() / maxCount.toFloat()
                val barHeight = (chartHeight * heightRatio).coerceAtLeast(if (bucket.count > 0) 6f else 0f)
                val left = spacing + index * (barWidth + spacing)
                val top = size.height - barHeight

                val color = when {
                    index == selectedIndex -> barColor
                    index == todayIndex -> todayColor
                    else -> fadedBarColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Always show the count above the bar — this is what makes
                // it clear "how many calls" without needing to tap anything.
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = valueColor
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 11.sp.toPx()
                        isFakeBoldText = index == selectedIndex
                    }
                    drawText(bucket.count.toString(), left + barWidth / 2, top - 8f, paint)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else labelColor
                )
            }
        }

        val selected = selectedIndex?.takeIf { it in buckets.indices }?.let { buckets[it] }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    selected?.fullLabel ?: "Tap any bar to see its exact count",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                if (selected != null) {
                    Text(
                        "${selected.count} call${if (selected.count != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MyTodayCard(total: Int, incoming: Int, outgoing: Int, missed: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total calls today", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("$total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniStat("In", incoming)
                MiniStat("Out", outgoing)
                MiniStat("Missed", missed)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int) {
    Column {
        Text(value.toString(), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One customer number, collapsed by default showing just the call count.
 * Expanding it lists every individual call from that number — each with its
 * own note, since the same number can call multiple times with completely
 * different context each time (e.g. "asked about pricing" vs "booked for Friday").
 */
@Composable
private fun NumberGroupCard(group: NumberGroup) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var editingCall by remember { mutableStateOf<CallRecord?>(null) }
    // Bumped after every save so the note text below refreshes immediately.
    var notesVersion by remember { mutableStateOf(0) }

    val notesForCalls = remember(group, notesVersion) {
        group.calls.associate { it.id to (CallNotesStore.getNote(context, it.id) ?: "") }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.number, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${group.calls.size} call${if (group.calls.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                group.calls.forEach { call ->
                    IndividualCallRow(
                        call = call,
                        note = notesForCalls[call.id].orEmpty(),
                        onClick = { editingCall = call }
                    )
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }

    editingCall?.let { call ->
        var text by remember(call.id) { mutableStateOf(notesForCalls[call.id].orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingCall = null },
            title = { Text("Note for ${call.number}") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. Interested, call back tomorrow") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CallNotesStore.setNote(context, call.id, text)
                    notesVersion++
                    editingCall = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingCall = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun IndividualCallRow(call: CallRecord, note: String, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val typeLabel = call.type.name.lowercase().replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${dateFormat.format(Date(call.timestampMillis))} \u00B7 $typeLabel \u00B7 ${call.durationSeconds}s",
                style = MaterialTheme.typography.bodySmall
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tap to add a note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.EditNote,
            contentDescription = "Edit note",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
