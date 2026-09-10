package com.sozo.callmanager.dialer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call as TelecomCall
import android.telecom.CallAudioState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sozo.callmanager.MainActivity
import com.sozo.callmanager.data.CallLogRepository
import com.sozo.callmanager.data.CallNotesStore
import com.sozo.callmanager.data.ContactsRepository
import com.sozo.callmanager.ui.theme.SozoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CallScreenBackground = Color(0xFF0A211D)
private val CallScreenTextPrimary = Color(0xFFEAF6F3)
private val CallScreenTextSecondary = Color(0xFFAFC7C1)
private val CallDeclineRed = Color(0xFFFF6B5E)
private val CallAcceptGreen = Color(0xFF4CD97B)

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SozoTheme {
                InCallScreen(
                    onAnswer = { CallHolder.currentCall?.answer(0) },
                    onReject = { CallHolder.currentCall?.reject(false, null) },
                    onHangUp = { CallHolder.currentCall?.disconnect() }
                )
            }
        }
    }
}

@Composable
fun InCallScreen(onAnswer: () -> Unit, onReject: () -> Unit, onHangUp: () -> Unit) {
    val context = LocalContext.current
    val state = CallHolder.callState.value
    val isIncoming = CallHolder.isIncoming.value
    val connectedAt = CallHolder.connectedAtMillis.value
    val audioState = CallHolder.audioState.value
    val number = CallHolder.phoneNumber.value
    val isOnHold = state == TelecomCall.STATE_HOLDING

    val activity = context as? ComponentActivity
    var showKeypad by remember { mutableStateOf(false) }
    var dtmfDigits by remember { mutableStateOf("") }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Caller-ID style lookup: show the saved contact name if we have one,
    // otherwise fall back to a best-effort country guess from the number.
    var subtitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(number) {
        subtitle = ContactsRepository.lookupNameForNumber(context, number) ?: guessCountryLabel(number)
    }

    fun returnToApp() {
        activity?.let { act ->
            CallHolder.shouldFocusRecentsOnReturn.value = true
            val returnIntent = Intent(act, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            act.startActivity(returnIntent)
            act.finish()
        }
    }

    // Once the call actually disconnects: briefly show "Call ended", then
    // offer to attach a note to THIS call before returning to the app.
    //
    // IMPORTANT: this is deliberately NOT keyed on `state` directly. As soon
    // as the call disconnects, CallHolder.detach() runs a moment later and
    // sets callState back to null — if this LaunchedEffect were keyed on
    // `state`, that null transition would cancel/restart it mid-delay and
    // the note dialog would never appear, leaving the screen stuck until the
    // user pressed the physical Back button. Keying on Unit + snapshotFlow
    // + a one-shot guard means we only ever react to DISCONNECTED once, and
    // finish what we started even if the state moves on to null right after.
    var hasHandledDisconnect by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { CallHolder.callState.value }.collect { s ->
            if (s == TelecomCall.STATE_DISCONNECTED && !hasHandledDisconnect) {
                hasHandledDisconnect = true
                delay(500)
                showNoteDialog = true
            }
        }
    }

    if (showNoteDialog) {
        val callNumberForNote = number
        val callCreationTime = CallHolder.callCreationTimeMillis.value

        AlertDialog(
            onDismissRequest = { /* require an explicit Save or Skip */ },
            title = { Text("Add a note for this call?") },
            text = {
                Column {
                    Text(callNumberForNote, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("e.g. Interested, call back tomorrow") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoteDialog = false
                    val noteToSave = noteText
                    if (noteToSave.isNotBlank()) {
                        coroutineScope.launch {
                            val callId = resolveCallId(context, callNumberForNote, callCreationTime)
                            if (callId != null) CallNotesStore.setNote(context, callId, noteToSave)
                        }
                    }
                    returnToApp()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNoteDialog = false
                    returnToApp()
                }) { Text("Skip") }
            }
        )
    }

    // Live "MM:SS" ticking timer — only runs once the call is genuinely connected.
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(connectedAt) {
        if (connectedAt != null) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - connectedAt) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    val statusText = when (state) {
        TelecomCall.STATE_RINGING -> if (isIncoming) "Incoming call\u2026" else "Ringing\u2026"
        TelecomCall.STATE_DIALING, TelecomCall.STATE_CONNECTING -> "Calling\u2026"
        TelecomCall.STATE_ACTIVE -> formatDuration(elapsedSeconds)
        TelecomCall.STATE_HOLDING -> "On hold"
        TelecomCall.STATE_DISCONNECTED -> "Call ended"
        else -> "\u2026"
    }

    // Quick actions (Keypad/Mute/Speaker/More) show for any call already in
    // progress — dialing, ringing-out, active, or held. For a genuinely
    // incoming, not-yet-answered call we keep it simple: just Decline/Answer.
    val showQuickActions = !(state == TelecomCall.STATE_RINGING && isIncoming)

    Surface(modifier = Modifier.fillMaxSize(), color = CallScreenBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(statusText, style = MaterialTheme.typography.titleMedium, color = CallScreenTextSecondary)
            Spacer(Modifier.height(12.dp))
            Text(
                number,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = CallScreenTextPrimary
            )
            subtitle?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = CallScreenTextSecondary)
            }

            Spacer(Modifier.weight(1f))

            if (showKeypad) {
                DtmfKeypad(
                    digitsSoFar = dtmfDigits,
                    onDigit = { digit ->
                        dtmfDigits += digit
                        CallHolder.currentCall?.let { call ->
                            call.playDtmfTone(digit)
                            Handler(Looper.getMainLooper()).postDelayed({ call.stopDtmfTone() }, 150)
                        }
                    }
                )
                Spacer(Modifier.height(28.dp))
            }

            if (showQuickActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    val muted = audioState?.isMuted == true
                    val speakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER

                    CircleActionButton(icon = Icons.Filled.Dialpad, label = "Keypad", active = showKeypad) {
                        showKeypad = !showKeypad
                    }
                    CircleActionButton(icon = Icons.Filled.MicOff, label = "Mute", active = muted) {
                        CallHolder.toggleMute(!muted)
                    }
                    CircleActionButton(icon = Icons.Filled.VolumeUp, label = "Speaker", active = speakerOn) {
                        CallHolder.setSpeakerOn(!speakerOn)
                    }
                    Box {
                        CircleActionButton(icon = Icons.Filled.MoreVert, label = "More", active = moreMenuExpanded) {
                            moreMenuExpanded = true
                        }
                        DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isOnHold) "Resume call" else "Hold call") },
                                onClick = {
                                    moreMenuExpanded = false
                                    val call = CallHolder.currentCall
                                    if (isOnHold) call?.unhold() else call?.hold()
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            if (state == TelecomCall.STATE_RINGING && isIncoming) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WidePillButton(icon = Icons.Filled.CallEnd, color = CallDeclineRed, modifier = Modifier.weight(1f), onClick = onReject)
                    WidePillButton(icon = Icons.Filled.Call, color = CallAcceptGreen, modifier = Modifier.weight(1f), onClick = onAnswer)
                }
            } else {
                WidePillButton(icon = Icons.Filled.CallEnd, color = CallDeclineRed, modifier = Modifier.fillMaxWidth(), onClick = onHangUp)
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

/** Very small heuristic — good enough to show a friendly label when there's no saved contact. */
private fun guessCountryLabel(number: String): String? {
    val clean = number.filter { it.isDigit() || it == '+' }
    return when {
        clean.startsWith("+91") -> "India"
        clean.startsWith("+1") -> "USA/Canada"
        clean.startsWith("+44") -> "United Kingdom"
        clean.startsWith("+971") -> "UAE"
        else -> null
    }
}

@Composable
private fun DtmfKeypad(digitsSoFar: String, onDigit: (Char) -> Unit) {
    val keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#')
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (digitsSoFar.isNotEmpty()) {
            Text(digitsSoFar, style = MaterialTheme.typography.titleLarge, color = CallScreenTextPrimary)
            Spacer(Modifier.height(16.dp))
        }
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDigit(digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.titleMedium, color = CallScreenTextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CircleActionButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) Color.Black else CallScreenTextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = CallScreenTextSecondary)
    }
}

@Composable
private fun WidePillButton(icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color,
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = Color.Black)
        }
    }
}
