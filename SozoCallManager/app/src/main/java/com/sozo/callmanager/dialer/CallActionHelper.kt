package com.sozo.callmanager.dialer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/**
 * Places an outgoing call. When Sozo is the default dialer, TelecomManager
 * gives a slightly smoother experience (no extra system confirmation);
 * otherwise we fall back to a plain ACTION_CALL intent, which still works as
 * long as CALL_PHONE has been granted (handled in SetupScreen). If that
 * permission is somehow missing, we fall back further to ACTION_DIAL, which
 * just opens the dialer pre-filled instead of calling directly.
 */
object CallActionHelper {

    fun placeCall(context: Context, rawNumber: String) {
        val number = rawNumber.trim()
        if (number.isEmpty()) return
        val uri = Uri.fromParts("tel", number, null)

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

        if (isDefaultDialer && telecomManager != null) {
            telecomManager.placeCall(uri, null)
        } else {
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
