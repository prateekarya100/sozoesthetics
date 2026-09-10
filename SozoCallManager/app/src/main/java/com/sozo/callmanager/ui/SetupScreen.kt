package com.sozo.callmanager.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val requiredPermissions = buildList {
    add(android.Manifest.permission.READ_CALL_LOG)
    add(android.Manifest.permission.READ_PHONE_STATE)
    add(android.Manifest.permission.READ_CONTACTS)
    add(android.Manifest.permission.CALL_PHONE)
    add(android.Manifest.permission.ANSWER_PHONE_CALLS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun allPermissionsGranted(context: Context): Boolean =
    requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun isDefaultDialer(context: Context): Boolean {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    return telecomManager?.defaultDialerPackage == context.packageName
}

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(allPermissionsGranted(context)) }
    var isDefaultDialerNow by remember { mutableStateOf(isDefaultDialer(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted = allPermissionsGranted(context) }

    val dialerRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefaultDialerNow = isDefaultDialer(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("One-time setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Sozo needs these to track calls and match them to customers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            SetupItem(
                title = "Call & contacts permissions",
                subtitle = "Read call log, phone state, contacts",
                done = permissionsGranted,
                actionLabel = "Allow",
                onAction = { permissionLauncher.launch(requiredPermissions.toTypedArray()) }
            )

            Spacer(Modifier.height(16.dp))

            SetupItem(
                title = "Set as default dialer",
                subtitle = "Required so Sozo can log every call automatically",
                done = isDefaultDialerNow,
                actionLabel = "Set default",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        dialerRoleLauncher.launch(intent)
                    } else {
                        val intent = android.content.Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                        dialerRoleLauncher.launch(intent)
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSetupComplete,
                enabled = permissionsGranted && isDefaultDialerNow,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Continue to Dashboard")
            }

            if (!(permissionsGranted && isDefaultDialerNow)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Complete both steps above to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SetupItem(title: String, subtitle: String, done: Boolean, actionLabel: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (done) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!done) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
