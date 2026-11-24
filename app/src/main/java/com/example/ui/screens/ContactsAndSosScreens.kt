package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.SosEvent
import com.example.model.UserRole
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import java.util.Locale

fun launchDialerIntent(context: Context, phoneNumber: String): Boolean {
    val cleaned = phoneNumber.filter { it.isDigit() || it == '+' }
    if (cleaned.isEmpty()) return false
    return try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
fun EmergencyContactsScreen(
    elder: ElderProfile?,
    contacts: List<EmergencyContact>,
    onBack: () -> Unit,
    onSaveContact: (
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        phoneNumber: String,
        relationship: String,
        priorityOrder: Int
    ) -> Unit,
    onDeleteContact: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToCall by remember { mutableStateOf<EmergencyContact?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeaderBar(
                title = "Emergency Contacts",
                subtitle = "${contacts.size} saved • Recommended: 5+ contacts",
                onBack = onBack,
                action = {
                    if (elder != null) {
                        CarePillButton(
                            text = "+ Add",
                            backgroundColor = tokens.cardSurface,
                            onClick = { showAddDialog = true },
                            testTag = "add_contact_button"
                        )
                    }
                }
            )

            if (contacts.size < 5 && elder != null) {
                CareCard(backgroundColor = tokens.lavender, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = "Tip: Add at least 5 trusted contacts (${5 - contacts.size} more recommended) including family members, primary doctor, and a nearby neighbor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("Select an elder profile first.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (contacts.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("No emergency contacts added", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Save family, caretaker, neighbor, and doctor phone numbers for quick dialing during emergencies.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "Add Emergency Contact",
                        icon = Icons.Filled.Add,
                        backgroundColor = tokens.mintGreen,
                        onClick = { showAddDialog = true }
                    )
                }
            } else {
                contacts.forEach { contact ->
                    CareCard(
                        backgroundColor = tokens.cardSurface,
                        modifier = Modifier.testTag("contact_card_${contact.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CareStatusBadge(
                                label = "#${contact.priorityOrder}",
                                backgroundColor = tokens.butterYellow
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.ink
                                )
                                Text(
                                    text = "${contact.relationship} • ${contact.phoneNumber}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CareButton(
                                text = "Call",
                                icon = Icons.Filled.Call,
                                backgroundColor = tokens.mintGreen,
                                onClick = { contactToCall = contact },
                                modifier = Modifier.weight(1f),
                                testTag = "contact_call_${contact.id}"
                            )
                            IconButton(onClick = { editingContact = contact }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${contact.name}", tint = tokens.ink)
                            }
                            IconButton(onClick = { contactToDelete = contact }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${contact.name}", tint = tokens.ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if ((showAddDialog || editingContact != null) && elder != null) {
        ContactEditorDialog(
            initial = editingContact,
            defaultPriority = (contacts.maxOfOrNull { it.priorityOrder } ?: 0) + 1,
            onDismiss = {
                showAddDialog = false
                editingContact = null
            },
            onSave = { name, phone, relationship, priority ->
                onSaveContact(
                    editingContact?.id,
                    elder.elderId,
                    elder.authorizedUids,
                    name,
                    phone,
                    relationship,
                    priority
                )
                showAddDialog = false
                editingContact = null
            }
        )
    }

    contactToCall?.let { contact ->
        CareConfirmDialog(
            title = "Call ${contact.name}?",
            message = "Open your phone dialer to call ${contact.name} (${contact.relationship}) at ${contact.phoneNumber}?",
            confirmLabel = "Open Dialer",
            confirmColor = tokens.mintGreen,
            onConfirm = {
                launchDialerIntent(context, contact.phoneNumber)
                contactToCall = null
            },
            onDismiss = { contactToCall = null }
        )
    }

    contactToDelete?.let { contact ->
        CareConfirmDialog(
            title = "Remove Contact?",
            message = "Remove ${contact.name} from emergency contacts?",
            confirmLabel = "Remove",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onDeleteContact(contact.id)
                contactToDelete = null
            },
            onDismiss = { contactToDelete = null }
        )
    }
}

@Composable
private fun ContactEditorDialog(
    initial: EmergencyContact?,
    defaultPriority: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relationship: String, priority: Int) -> Unit
) {
    val tokens = LocalCareColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phoneNumber ?: "") }
    var relationship by remember { mutableStateOf(initial?.relationship ?: "Family") }
    var priority by remember { mutableIntStateOf(initial?.priorityOrder ?: defaultPriority.coerceIn(1, 20)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        title = { Text(if (initial == null) "Add Emergency Contact" else "Edit Contact", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(25) },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )
                Text("Relationship", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Family", "Doctor", "Neighbor").forEach { option ->
                        CarePillButton(
                            text = option,
                            backgroundColor = if (relationship == option) tokens.butterYellow else tokens.background,
                            onClick = { relationship = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it.take(40) },
                    label = { Text("Custom Relationship") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Priority Order: #$priority", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CarePillButton(text = "-", onClick = { if (priority > 1) priority-- }, backgroundColor = tokens.background)
                        CarePillButton(text = "+", onClick = { if (priority < 20) priority++ }, backgroundColor = tokens.background)
                    }
                }
            }
        },
        confirmButton = {
            CareButton(
                text = "Save",
                backgroundColor = tokens.mintGreen,
                enabled = name.trim().isNotEmpty() && phone.trim().length >= 3,
                testTag = "contact_save_button",
                onClick = { onSave(name.trim(), phone.trim(), relationship.trim(), priority) }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}

@Composable
fun SosScreen(
    elder: ElderProfile?,
    userRole: UserRole,
    activeSos: SosEvent?,
    contacts: List<EmergencyContact>,
    onBack: () -> Unit,
    onTriggerSos: (ElderProfile) -> Unit,
    onAcknowledgeSos: (String) -> Unit,
    onCancelSos: (String) -> Unit,
    onToggleLocationSharing: (ElderProfile, Boolean) -> Unit,
    onRefreshLocation: (ElderProfile) -> Unit,
    onOpenFallDetection: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted && elder != null) {
            onToggleLocationSharing(elder, true)
        }
    }

    var showConfirmSendSos by remember { mutableStateOf(false) }
    var showConfirmCancelSos by remember { mutableStateOf<SosEvent?>(null) }
    var contactToCall by remember { mutableStateOf<EmergencyContact?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeaderBar(
                title = stringResource(R.string.sos_title),
                subtitle = elder?.displayName ?: "Care Connect",
                onBack = onBack
            )

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("Select an elder profile first.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // Active SOS Panel
                if (activeSos != null) {
                    val deliveryLabel = when (activeSos.deliveryState) {
                        "acknowledged" -> "Acknowledged by Caretaker"
                        "queued_offline" -> "Queued Offline (Pending Sync)"
                        else -> "Delivered to Linked Caretakers"
                    }
                    CareCard(
                        backgroundColor = tokens.sosCoral,
                        modifier = Modifier.testTag("active_sos_panel")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SOS Alert Active",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = tokens.ink
                                )
                                Text(
                                    text = "${activeSos.elderName} • ${activeSos.areaLabel}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.ink
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CareStatusBadge(
                                label = deliveryLabel,
                                backgroundColor = tokens.cardSurface,
                                icon = Icons.Filled.CheckCircle
                            )
                            if (activeSos.source != "manual") {
                                val impactSuffix = activeSos.peakGForce?.let {
                                    " • ${String.format(Locale.US, "%.1f", it)}G"
                                } ?: ""
                                CareStatusBadge(
                                    label = (if (activeSos.source == "fall_simulation") "Safety Sim" else "Fall Detected") + impactSuffix,
                                    backgroundColor = tokens.butterYellow,
                                    icon = Icons.Filled.Sensors
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (userRole == UserRole.CARETAKER && activeSos.deliveryState != "acknowledged") {
                                CareButton(
                                    text = "Acknowledge",
                                    icon = Icons.Filled.CheckCircle,
                                    backgroundColor = tokens.mintGreen,
                                    onClick = { onAcknowledgeSos(activeSos.id) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "sos_acknowledge_button"
                                )
                            }
                            CareButton(
                                text = "Cancel SOS",
                                icon = Icons.Filled.Close,
                                backgroundColor = tokens.cardSurface,
                                onClick = { showConfirmCancelSos = activeSos },
                                modifier = Modifier.weight(1f),
                                testTag = "sos_cancel_button"
                            )
                        }
                    }
                }

                // Trigger SOS Card
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "Need Immediate Family Help?",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pressing Send SOS Alert notifies linked caretakers with your current status and permitted area. You will be asked to confirm first.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(14.dp))
                    CareButton(
                        text = "Send SOS Alert",
                        icon = Icons.Filled.NotificationsActive,
                        backgroundColor = tokens.sosCoral,
                        onClick = { showConfirmSendSos = true },
                        modifier = Modifier.heightIn(min = 68.dp),
                        testTag = "sos_trigger_button"
                    )
                }

                // Location Sharing Control Card
                CareCard(backgroundColor = tokens.skyBlue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SOS Location Sharing",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.ink
                                )
                                Text(
                                    text = when {
                                        !hasLocationPermission -> "Permission not granted"
                                        elder.locationSharingEnabled -> elder.lastKnownArea ?: "Active"
                                        else -> "Paused by user"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                        CarePillButton(
                            text = if (elder.locationSharingEnabled && hasLocationPermission) "Pause" else "Enable",
                            backgroundColor = tokens.cardSurface,
                            onClick = {
                                if (!hasLocationPermission) {
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    onToggleLocationSharing(elder, !elder.locationSharingEnabled)
                                }
                            },
                            testTag = "sos_location_toggle_button"
                        )
                    }
                    if (elder.locationSharingEnabled && hasLocationPermission) {
                        Spacer(Modifier.height(10.dp))
                        CarePillButton(
                            text = "Refresh Current Area",
                            icon = Icons.Filled.Refresh,
                            backgroundColor = tokens.butterYellow,
                            onClick = { onRefreshLocation(elder) }
                        )
                    }
                }

                // Fall Detection Sensor Shortcut Card
                CareCard(backgroundColor = tokens.butterYellow, onClick = onOpenFallDetection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Fall Detection Sensor",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.ink
                                )
                                Text(
                                    text = if (elder.fallDetectionEnabled) "Active • Auto-SOS countdown enabled" else "Paused • Tap to configure or test",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                        CarePillButton(
                            text = "Configure",
                            backgroundColor = tokens.cardSurface,
                            onClick = onOpenFallDetection,
                            testTag = "sos_open_fall_detection"
                        )
                    }
                }

                // Quick-Dial Emergency Contacts
                CareCard(backgroundColor = tokens.lavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Call Emergency Contacts",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (contacts.isEmpty()) {
                        Text(
                            text = "No emergency contacts saved yet. Add contacts in Emergency Contacts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    } else {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${contact.name} (${contact.relationship})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.ink
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.secondaryInk
                                    )
                                }
                                CarePillButton(
                                    text = "Call",
                                    icon = Icons.Filled.Call,
                                    backgroundColor = tokens.mintGreen,
                                    onClick = { contactToCall = contact }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showConfirmSendSos && elder != null) {
        CareConfirmDialog(
            title = "Send Emergency SOS Alert?",
            message = "Alert linked caretakers right now for ${elder.displayName}?",
            confirmLabel = "Send SOS",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onTriggerSos(elder)
                showConfirmSendSos = false
            },
            onDismiss = { showConfirmSendSos = false }
        )
    }

    showConfirmCancelSos?.let { event ->
        CareConfirmDialog(
            title = "Cancel Active SOS?",
            message = "Confirm that ${event.elderName} is safe and cancel this active SOS alert?",
            confirmLabel = "Cancel SOS",
            confirmColor = tokens.butterYellow,
            onConfirm = {
                onCancelSos(event.id)
                showConfirmCancelSos = null
            },
            onDismiss = { showConfirmCancelSos = null }
        )
    }

    contactToCall?.let { contact ->
        CareConfirmDialog(
            title = "Call ${contact.name}?",
            message = "Open phone dialer for ${contact.name} (${contact.phoneNumber})?",
            confirmLabel = "Open Dialer",
            confirmColor = tokens.mintGreen,
            onConfirm = {
                launchDialerIntent(context, contact.phoneNumber)
                contactToCall = null
            },
            onDismiss = { contactToCall = null }
        )
    }
}
