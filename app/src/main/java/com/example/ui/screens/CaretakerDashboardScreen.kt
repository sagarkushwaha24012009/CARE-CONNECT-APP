package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CareTask
import com.example.model.ElderProfile
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.SosEvent
import com.example.model.WaterLog
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors

@Composable
fun CaretakerDashboardScreen(
    elders: List<ElderProfile>,
    selectedElderId: String?,
    medicines: List<MedicineSchedule>,
    todayMedLogs: List<MedicineLog>,
    todayWaterLogs: List<WaterLog>,
    tasks: List<CareTask>,
    activeSosEvents: List<SosEvent>,
    onSelectElder: (String) -> Unit,
    onOpenElderDetails: (String) -> Unit,
    onClaimPairingCode: (String) -> Unit,
    onCreateManagedElder: (name: String, avatarKey: String, waterGoal: Int, ageLabel: String?) -> Unit,
    onCallSosElder: (SosEvent) -> Unit,
    onOpenSosDetail: (String) -> Unit,
    onQuickActionMedicine: (String) -> Unit,
    onQuickActionWater: (String) -> Unit,
    onQuickActionTasks: (String) -> Unit,
    onQuickActionContacts: (String) -> Unit,
    onQuickActionAppointments: (String) -> Unit
) {
    val tokens = LocalCareColors.current
    var showAddElderDialog by remember { mutableStateOf(false) }

    val primaryElder = elders.firstOrNull { it.elderId == selectedElderId } ?: elders.firstOrNull()
    val primarySos = activeSosEvents.firstOrNull()

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
            // 1. SOS Alert Banner (matches 02_caretaker_dashboard_9x16.png)
            if (primarySos != null) {
                CareCard(
                    backgroundColor = tokens.sosCoral,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.testTag("caretaker_sos_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "SOS Alert",
                            tint = tokens.ink,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOS Alert",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.ink
                            )
                            Text(
                                text = "${primarySos.elderName} needs help!",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.ink
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        CarePillButton(
                            text = "Call Now",
                            icon = Icons.Filled.Call,
                            backgroundColor = tokens.cardSurface,
                            onClick = { onCallSosElder(primarySos) },
                            testTag = "caretaker_sos_call_button"
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tokens.cardSurface.copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, tokens.border),
                        onClick = { onOpenSosDetail(primarySos.elderId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = tokens.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = primarySos.areaLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "View Details →",
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.ink
                            )
                        }
                    }
                }
            }

            // 2. Connected Elders Header Bar + "+ Add Elder"
            CareCard(
                backgroundColor = tokens.butterYellow,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Connected Elders (${elders.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                    }
                    CarePillButton(
                        text = "+ Add Elder",
                        backgroundColor = tokens.cardSurface,
                        onClick = { showAddElderDialog = true },
                        testTag = "caretaker_add_elder_button"
                    )
                }
            }

            // 3. Connected Elder Summary Cards
            if (elders.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "No elders linked yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap '+ Add Elder' to enter an elder's 6-character pairing code or create a managed elder profile.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "Add Elder Now",
                        icon = Icons.Filled.Add,
                        backgroundColor = tokens.mintGreen,
                        onClick = { showAddElderDialog = true }
                    )
                }
            } else {
                elders.forEach { elder ->
                    val elderMeds = medicines.filter { it.elderId == elder.elderId && it.activeStatus == "active" }
                    val elderTaken = todayMedLogs.count { it.elderId == elder.elderId && it.status == "taken" }
                    val elderWater = todayWaterLogs.firstOrNull { it.elderId == elder.elderId }?.glasses ?: 0
                    val elderTasksLeft = tasks.count { it.elderId == elder.elderId && it.completionStatus != "completed" }
                    val isSelected = elder.elderId == primaryElder?.elderId

                    val avatarDrawable = if (elder.avatarKey == "grandma") {
                        R.drawable.img_elder_grandma
                    } else {
                        R.drawable.img_elder_grandpa
                    }

                    CareCard(
                        backgroundColor = tokens.cardSurface,
                        borderWidth = if (isSelected && elders.size > 1) 3.5.dp else 2.5.dp,
                        onClick = {
                            onSelectElder(elder.elderId)
                            onOpenElderDetails(elder.elderId)
                        },
                        modifier = Modifier.testTag("caretaker_elder_card_${elder.elderId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = avatarDrawable),
                                contentDescription = "${elder.displayName} avatar",
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, tokens.border, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = elder.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = tokens.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    CareStatusBadge(
                                        label = if (elder.onlineStatus == "online") "Online" else "Offline",
                                        backgroundColor = if (elder.onlineStatus == "online") tokens.mintGreen else tokens.butterYellow,
                                        dotColor = if (elder.onlineStatus == "online") tokens.onlineGreen else tokens.sosCoral
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MetricItem(
                                        icon = Icons.Filled.Medication,
                                        value = "$elderTaken / ${elderMeds.size}",
                                        label = "Medicine",
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricItem(
                                        icon = Icons.Filled.WaterDrop,
                                        value = "$elderWater / ${elder.dailyWaterGoal}",
                                        label = "Water",
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricItem(
                                        icon = Icons.Filled.Checklist,
                                        value = "$elderTasksLeft Left",
                                        label = "Tasks",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.butterYellow,
                                border = BorderStroke(2.dp, tokens.border),
                                modifier = Modifier.size(48.dp),
                                onClick = { onOpenElderDetails(elder.elderId) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "View ${elder.displayName} details",
                                        tint = tokens.ink,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions Header + 2x2 Pastel Tiles
            CareCard(
                backgroundColor = tokens.butterYellow,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = tokens.ink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (primaryElder != null) "Quick Actions • ${primaryElder.displayName}" else "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (primaryElder != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        label = "Manage Medicine",
                        icon = Icons.Filled.Medication,
                        backgroundColor = tokens.skyBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_medicine"),
                        onClick = { onQuickActionMedicine(primaryElder.elderId) }
                    )
                    QuickActionTile(
                        label = "Water Reminders",
                        icon = Icons.Filled.WaterDrop,
                        backgroundColor = tokens.mintGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_water"),
                        onClick = { onQuickActionWater(primaryElder.elderId) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        label = "Manage Tasks",
                        icon = Icons.Filled.Checklist,
                        backgroundColor = tokens.lavender,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_tasks"),
                        onClick = { onQuickActionTasks(primaryElder.elderId) }
                    )
                    QuickActionTile(
                        label = "Emergency Contacts",
                        icon = Icons.Filled.People,
                        backgroundColor = tokens.butterYellow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_contacts"),
                        onClick = { onQuickActionContacts(primaryElder.elderId) }
                    )
                }
                CareCard(
                    backgroundColor = tokens.softPink,
                    onClick = { onQuickActionAppointments(primaryElder.elderId) },
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.testTag("quick_action_appointments")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Manage Appointments",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.ink
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open appointments",
                            tint = tokens.ink
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showAddElderDialog) {
        AddElderDialog(
            onDismiss = { showAddElderDialog = false },
            onClaimPairingCode = { code ->
                onClaimPairingCode(code)
                showAddElderDialog = false
            },
            onCreateManagedElder = { name, avatarKey, goal, ageLabel ->
                onCreateManagedElder(name, avatarKey, goal, ageLabel)
                showAddElderDialog = false
            }
        )
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCareColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.ink,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.ink,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.secondaryInk,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCareColors.current
    CareCard(
        backgroundColor = backgroundColor,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.ink,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddElderDialog(
    onDismiss: () -> Unit,
    onClaimPairingCode: (String) -> Unit,
    onCreateManagedElder: (name: String, avatarKey: String, goal: Int, ageLabel: String?) -> Unit
) {
    val tokens = LocalCareColors.current
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Pairing Code, 1 = Managed Profile
    var codeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var avatarKey by remember { mutableStateOf("grandpa") }
    var waterGoal by remember { mutableIntStateOf(8) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        title = { Text("Add Connected Elder", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarePillButton(
                        text = "Pairing Code",
                        backgroundColor = if (modeTab == 0) tokens.butterYellow else tokens.background,
                        onClick = { modeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    CarePillButton(
                        text = "New Profile",
                        backgroundColor = if (modeTab == 1) tokens.butterYellow else tokens.background,
                        onClick = { modeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (modeTab == 0) {
                    Text(
                        text = "Enter the 6-character consent code generated in the elder's Profile & Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(6) },
                        label = { Text("6-Character Code") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pairing_code_input")
                    )
                } else {
                    Text(
                        text = "Create a family-managed elder profile on your caretaker account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(60) },
                        label = { Text("Elder Name (e.g. Grandpa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.take(20) },
                        label = { Text("Age or Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CarePillButton(
                            text = "Grandpa Avatar",
                            backgroundColor = if (avatarKey == "grandpa") tokens.skyBlue else tokens.background,
                            onClick = { avatarKey = "grandpa" },
                            modifier = Modifier.weight(1f)
                        )
                        CarePillButton(
                            text = "Grandma Avatar",
                            backgroundColor = if (avatarKey == "grandma") tokens.softPink else tokens.background,
                            onClick = { avatarKey = "grandma" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Daily Water Goal: $waterGoal glasses", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CarePillButton(
                                text = "-",
                                onClick = { if (waterGoal > 1) waterGoal-- },
                                backgroundColor = tokens.background
                            )
                            CarePillButton(
                                text = "+",
                                onClick = { if (waterGoal < 20) waterGoal++ },
                                backgroundColor = tokens.background
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            CareButton(
                text = if (modeTab == 0) "Link Elder" else "Create Profile",
                backgroundColor = tokens.mintGreen,
                enabled = if (modeTab == 0) codeInput.trim().length == 6 else nameInput.trim().isNotEmpty(),
                onClick = {
                    if (modeTab == 0) {
                        onClaimPairingCode(codeInput.trim())
                    } else {
                        onCreateManagedElder(nameInput.trim(), avatarKey, waterGoal, ageInput.trim().ifEmpty { null })
                    }
                }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}
