package com.example.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.EmergencyContact
import java.util.Locale

object EmergencySmsHelper {

    fun sanitizePhoneNumbers(contacts: List<EmergencyContact>): List<String> =
        contacts
            .sortedBy { it.priorityOrder }
            .map { contact ->
                contact.phoneNumber.filter { it.isDigit() || it == '+' }
            }
            .filter { it.isNotBlank() }
            .distinct()

    fun buildRecipientsString(contacts: List<EmergencyContact>): String =
        sanitizePhoneNumbers(contacts).joinToString(";")

    fun buildSosSmsBody(
        elderName: String,
        areaLabel: String,
        source: String,
        peakGForce: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): String {
        val safeElder = elderName.trim().ifEmpty { "Family Elder" }
        val triggerReason = when (source) {
            "fall_detection" -> {
                val impact = peakGForce?.let { " (${String.format(Locale.US, "%.1f", it)}G impact)" }.orEmpty()
                "Fall detected$impact"
            }
            "fall_simulation" -> {
                val impact = peakGForce?.let { " (${String.format(Locale.US, "%.1f", it)}G test)" }.orEmpty()
                "Fall detection SOS test$impact"
            }
            else -> "Emergency SOS requested"
        }
        val cleanArea = areaLabel.trim().ifEmpty { "Location not shared" }
        val coordsPart = if (latitude != null && longitude != null) {
            " Map: https://maps.google.com/?q=${String.format(Locale.US, "%.5f,%.5f", latitude, longitude)}"
        } else {
            ""
        }
        return "CARE CONNECT SOS ALERT: $safeElder needs help! $triggerReason. Area: $cleanArea.$coordsPart Please check in immediately."
    }

    fun buildSmsIntent(contacts: List<EmergencyContact>, messageBody: String): Intent? {
        val recipients = buildRecipientsString(contacts)
        if (recipients.isEmpty()) return null
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipients")).apply {
            putExtra("sms_body", messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchSmsComposer(
        context: Context,
        contacts: List<EmergencyContact>,
        messageBody: String
    ): Boolean {
        val intent = buildSmsIntent(contacts, messageBody) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
