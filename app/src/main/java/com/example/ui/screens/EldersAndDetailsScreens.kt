package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.CareAppointment
import com.example.model.CareNotification
import com.example.model.CareTask
import com.example.model.CaretakerLink
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.UserRole
import com.example.model.WaterLog
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors

@Composable
fun EldersListScreen(
    userRole: UserRole,
    elders: List<ElderProfile>,
    caretakerLinks: List<CaretakerLink>,
    medicines: List<MedicineSchedule>,
    todayMedLogs: List<MedicineLog>,
    todayWaterLogs: List<WaterLog>,
    tasks: List<CareTask>,
    appointments: List<CareAppointment>,
    onSelectAndOpenElder: (String) -> Unit,
    onClaimPairingCode: (String) -> Unit,
    onCreateManagedElder: (name: String, avatarKey: String, waterGoal: Int, ageLabel: String?) -> Unit,
    onRevokeCaretakerLink: (CaretakerLink) -> Unit,
    onOpenMedicines: (String) -> Unit,
    onOpenWater: (String) -> Unit,
    onOpenTasks: (String) -> Unit,
    onOpenContacts: (String) -> Unit,
    onOpenAppointments: (String) -> Unit,
    onOpenPairingSettings: () -> Unit
) {
    val tokens = LocalCareColors.current
    var showAddDialog by remember { mutableStateOf(false) }
    var linkToRevoke by remember { mutableStateOf<CaretakerLink?>(null) }

    val primaryElder = elders.firstOrNull()

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
            if (userRole == UserRole.CARETAKER) {
                CareCard(
                    backgroundColor = tokens.butterYellow,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connected Elders",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.ink
                            )
                            Text(
                                text = "${elders.size} elder profile(s) accessible",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                        CarePillButton(
                            text = "+ Add Elder",
                            backgroundColor = tokens.cardSurface,
                            onClick = { showAddDialog = true },
                            testTag = "elders_tab_add_button"
                        )
                    }
                }

                if (elders.isEmpty()) {
                    CareCard(backgroundColor = tokens.cardSurface) {
                        Text(
                            text = "No Connected Elders",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Link an elder using their 6-character pairing code or create a managed profile for your family member.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.secondaryInk
                        )
                        Spacer(Modifier.height(12.dp))
                        CareButton(
                            text = "Add Elder",
                            icon = Icons.Filled.Add,
                            backgroundColor = tokens.mintGreen,
                            onClick = { showAddDialog = true },
                            testTag = "elders_empty_add_button"
                        )
                    }
                } else {
                    elders.forEach { elder ->
                        val elderMeds = medicines.filter { it.elderId == elder.elderId && it.activeStatus == "active" }
                        val elderTaken = todayMedLogs.count { it.elderId == elder.elderId && it.status == "taken" }
                        val elderWater = todayWaterLogs.firstOrNull { it.elderId == elder.elderId }?.glasses ?: 0
                        val elderTasksLeft = tasks.count { it.elderId == elder.elderId && it.completionStatus != "completed" }
                        val elderAppts = appointments.count { it.elderId == elder.elderId }
                        val avatarRes = if (elder.avatarKey == "grandma") R.drawable.img_elder_grandma else R.drawable.img_elder_grandpa

                        CareCard(
                            backgroundColor = tokens.cardSurface,
                            onClick = { onSelectAndOpenElder(elder.elderId) },
                            modifier = Modifier.testTag("elders_list_card_${elder.elderId}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "${elder.displayName} avatar",
                                    modifier = Modifier
                                        .size(64.dp)
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
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Meds: $elderTaken/${elderMeds.size} • Water: $elderWater/${elder.dailyWaterGoal} • Tasks Left: $elderTasksLeft • Visits: $elderAppts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.secondaryInk
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open ${elder.displayName} details",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Elder Care Hub view
                CareCard(
                    backgroundColor = tokens.skyBlue,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "My Care & Family Circle",
                        style = MaterialTheme.typography.headlineSmall,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Manage your daily routines, upcoming doctor visits, and authorized caretakers.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                }

                if (primaryElder != null) {
                    CareModuleShortcutCard(
                        title = "Medicine Schedule",
                        subtitle = "${medicines.count { it.elderId == primaryElder.elderId && it.activeStatus == "active" }} scheduled medicines",
                        icon = Icons.Filled.Medication,
                        backgroundColor = tokens.softPink,
                        testTag = "care_hub_medicine",
                        onClick = { onOpenMedicines(primaryElder.elderId) }
                    )
                    CareModuleShortcutCard(
                        title = "Water Tracker",
                        subtitle = "${todayWaterLogs.firstOrNull { it.elderId == primaryElder.elderId }?.glasses ?: 0} / ${primaryElder.dailyWaterGoal} glasses today",
                        icon = Icons.Filled.WaterDrop,
                        backgroundColor = tokens.mintGreen,
                        testTag = "care_hub_water",
                        onClick = { onOpenWater(primaryElder.elderId) }
                    )
                    CareModuleShortcutCard(
                        title = "Daily Tasks",
                        subtitle = "${tasks.count { it.elderId == primaryElder.elderId && it.completionStatus != "completed" }} task(s) remaining",
                        icon = Icons.Filled.Checklist,
                        backgroundColor = tokens.butterYellow,
                        testTag = "care_hub_tasks",
                        onClick = { onOpenTasks(primaryElder.elderId) }
                    )
                    CareModuleShortcutCard(
                        title = "Appointments",
                        subtitle = "${appointments.count { it.elderId == primaryElder.elderId }} upcoming appointment(s)",
                        icon = Icons.Filled.CalendarMonth,
                        backgroundColor = tokens.skyBlue,
                        testTag = "care_hub_appointments",
                        onClick = { onOpenAppointments(primaryElder.elderId) }
                    )
                    CareModuleShortcutCard(
                        title = "Emergency Contacts",
                        subtitle = "Manage family & doctor contacts",
                        icon = Icons.Filled.People,
                        backgroundColor = tokens.lavender,
                        testTag = "care_hub_contacts",
                        onClick = { onOpenContacts(primaryElder.elderId) }
                    )
                }

                CareCard(backgroundColor = tokens.cardSurface) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Authorized Caretakers (${caretakerLinks.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.ink
                            )
                        }
                        CarePillButton(
                            text = "Pairing Code",
                            backgroundColor = tokens.butterYellow,
                            onClick = onOpenPairingSettings,
                            testTag = "care_hub_pairing_button"
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (caretakerLinks.isEmpty()) {
                        Text(
                            text = "No family caretakers linked yet. Generate a private 6-character pairing code in Settings to invite a caretaker.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.secondaryInk
                        )
                    } else {
                        caretakerLinks.forEach { link ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.background,
                                border = BorderStroke(2.dp, tokens.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.caretakerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.ink
                                        )
                                        Text(
                                            text = "Authorized Caretaker • Active",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.secondaryInk
                                        )
                                    }
                                    CarePillButton(
                                        text = "Revoke",
                                        icon = Icons.Filled.Delete,
                                        backgroundColor = tokens.sosCoral,
                                        onClick = { linkToRevoke = link },
                                        testTag = "revoke_caretaker_${link.caretakerId}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddDialog) {
        ElderCreateOrLinkDialog(
            onDismiss = { showAddDialog = false },
            onClaimCode = { code ->
                onClaimPairingCode(code)
                showAddDialog = false
            },
            onCreateManaged = { name, avatarKey, goal, age ->
                onCreateManagedElder(name, avatarKey, goal, age)
                showAddDialog = false
            }
        )
    }

    linkToRevoke?.let { link ->
        CareConfirmDialog(
            title = "Revoke Caretaker Access?",
            message = "Remove ${link.caretakerName}'s access to your care schedule, SOS status, and contacts?",
            confirmLabel = "Revoke Access",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onRevokeCaretakerLink(link)
                linkToRevoke = null
            },
            onDismiss = { linkToRevoke = null }
        )
    }
}

@Composable
fun ElderDetailsScreen(
    elder: ElderProfile?,
    currentUserUid: String,
    userRole: UserRole,
    caretakerLinks: List<CaretakerLink>,
    medicines: List<MedicineSchedule>,
    todayMedLogs: List<MedicineLog>,
    todayWater: WaterLog?,
    tasks: List<CareTask>,
    contacts: List<EmergencyContact>,
    appointments: List<CareAppointment>,
    recentNotifications: List<CareNotification>,
    onBack: () -> Unit,
    onOpenMedicines: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenSos: () -> Unit,
    onRevokeCaretakerLink: (CaretakerLink) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current
    var linkToRevoke by remember { mutableStateOf<CaretakerLink?>(null) }

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
                title = elder?.displayName ?: "Elder Details",
                subtitle = "Care Overview & Schedule",
                onBack = onBack
            )

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "Elder profile is not available.",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(10.dp))
                    CareButton(text = "Go Back", onClick = onBack)
                }
            } else {
                val avatarRes = if (elder.avatarKey == "grandma") R.drawable.img_elder_grandma else R.drawable.img_elder_grandpa
                val elderMeds = medicines.filter { it.activeStatus == "active" }
                val takenCount = todayMedLogs.count { it.status == "taken" }
                val waterGlasses = todayWater?.glasses ?: 0
                val waterGoal = (todayWater?.goal ?: elder.dailyWaterGoal).coerceAtLeast(1)
                val tasksPending = tasks.count { it.completionStatus != "completed" }

                CareCard(backgroundColor = tokens.skyBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = "${elder.displayName} avatar",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, tokens.border, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = elder.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.ink
                            )
                            if (!elder.ageLabel.isNullOrBlank()) {
                                Text(
                                    text = elder.ageLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CareStatusBadge(
                                    label = if (elder.onlineStatus == "online") "Online" else "Offline",
                                    backgroundColor = if (elder.onlineStatus == "online") tokens.mintGreen else tokens.butterYellow,
                                    dotColor = if (elder.onlineStatus == "online") tokens.onlineGreen else tokens.sosCoral
                                )
                                CareStatusBadge(
                                    label = if (elder.locationSharingEnabled) "Loc On" else "Loc Off",
                                    backgroundColor = tokens.cardSurface,
                                    icon = Icons.Filled.LocationOn
                                )
                            }
                        }
                    }
                    if (!elder.lastKnownArea.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Location: ${elder.lastKnownArea}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = tokens.ink
                        )
                    }
                }

                // Today's Care Summary Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CareModuleShortcutCard(
                        title = "Medicines",
                        subtitle = "$takenCount / ${elderMeds.size} Taken",
                        icon = Icons.Filled.Medication,
                        backgroundColor = tokens.softPink,
                        modifier = Modifier.weight(1f),
                        testTag = "details_medicines_button",
                        onClick = onOpenMedicines
                    )
                    CareModuleShortcutCard(
                        title = "Water",
                        subtitle = "$waterGlasses / $waterGoal Glasses",
                        icon = Icons.Filled.WaterDrop,
                        backgroundColor = tokens.mintGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "details_water_button",
                        onClick = onOpenWater
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CareModuleShortcutCard(
                        title = "Tasks",
                        subtitle = "$tasksPending Pending",
                        icon = Icons.Filled.Checklist,
                        backgroundColor = tokens.butterYellow,
                        modifier = Modifier.weight(1f),
                        testTag = "details_tasks_button",
                        onClick = onOpenTasks
                    )
                    CareModuleShortcutCard(
                        title = "Contacts",
                        subtitle = "${contacts.size} Saved",
                        icon = Icons.Filled.People,
                        backgroundColor = tokens.lavender,
                        modifier = Modifier.weight(1f),
                        testTag = "details_contacts_button",
                        onClick = onOpenContacts
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CareModuleShortcutCard(
                        title = "Appointments",
                        subtitle = "${appointments.size} Upcoming",
                        icon = Icons.Filled.CalendarMonth,
                        backgroundColor = tokens.cardSurface,
                        modifier = Modifier.weight(1f),
                        testTag = "details_appointments_button",
                        onClick = onOpenAppointments
                    )
                    CareModuleShortcutCard(
                        title = "SOS Status",
                        subtitle = "Alerts & Calls",
                        icon = Icons.Filled.NotificationsActive,
                        backgroundColor = tokens.sosCoral,
                        modifier = Modifier.weight(1f),
                        testTag = "details_sos_button",
                        onClick = onOpenSos
                    )
                }

                // Authorized Caretakers Section
                CareCard(backgroundColor = tokens.cardSurface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Authorized Caretakers (${caretakerLinks.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (caretakerLinks.isEmpty()) {
                        Text(
                            text = if (elder.elderId == currentUserUid) {
                                "No caretakers linked yet. Share a pairing code from Settings."
                            } else {
                                "Managed directly by your authorized family caretaker account."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    } else {
                        caretakerLinks.forEach { link ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.background,
                                border = BorderStroke(2.dp, tokens.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.caretakerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.ink
                                        )
                                        Text(
                                            text = "Authorized via code ${link.inviteCode}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.secondaryInk
                                        )
                                    }
                                    if (userRole == UserRole.ELDER || elder.elderId == currentUserUid) {
                                        CarePillButton(
                                            text = "Revoke",
                                            backgroundColor = tokens.sosCoral,
                                            onClick = { linkToRevoke = link }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Activity / Notifications Timeline
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "Recent Care Activity",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    if (recentNotifications.isEmpty()) {
                        Text(
                            text = "No recent alerts or updates recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    } else {
                        recentNotifications.take(5).forEach { notif ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                CareStatusBadge(
                                    label = notif.category.uppercase(),
                                    backgroundColor = if (notif.category == "sos") tokens.sosCoral else tokens.butterYellow
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.ink
                                    )
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.secondaryInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    linkToRevoke?.let { link ->
        CareConfirmDialog(
            title = "Revoke Caretaker Access?",
            message = "Remove ${link.caretakerName} from this elder's care circle?",
            confirmLabel = "Revoke",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onRevokeCaretakerLink(link)
                linkToRevoke = null
            },
            onDismiss = { linkToRevoke = null }
        )
    }
}

@Composable
fun ScreenHeaderBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    val tokens = LocalCareColors.current
    CareCard(
        backgroundColor = tokens.butterYellow,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("screen_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = tokens.ink,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (action != null) {
                Spacer(Modifier.width(8.dp))
                action()
            }
        }
    }
}

@Composable
private fun CareModuleShortcutCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalCareColors.current
    CareCard(
        backgroundColor = backgroundColor,
        modifier = modifier.testTag(testTag),
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
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = tokens.ink
            )
        }
    }
}

@Composable
private fun ElderCreateOrLinkDialog(
    onDismiss: () -> Unit,
    onClaimCode: (String) -> Unit,
    onCreateManaged: (name: String, avatarKey: String, goal: Int, ageLabel: String?) -> Unit
) {
    val tokens = LocalCareColors.current
    var modeTab by remember { mutableIntStateOf(0) }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(6) },
                        label = { Text("6-Character Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(60) },
                        label = { Text("Elder Name") },
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
                            text = "Grandpa",
                            backgroundColor = if (avatarKey == "grandpa") tokens.skyBlue else tokens.background,
                            onClick = { avatarKey = "grandpa" },
                            modifier = Modifier.weight(1f)
                        )
                        CarePillButton(
                            text = "Grandma",
                            backgroundColor = if (avatarKey == "grandma") tokens.softPink else tokens.background,
                            onClick = { avatarKey = "grandma" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Water Goal: $waterGoal", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CarePillButton(text = "-", onClick = { if (waterGoal > 1) waterGoal-- }, backgroundColor = tokens.background)
                            CarePillButton(text = "+", onClick = { if (waterGoal < 20) waterGoal++ }, backgroundColor = tokens.background)
                        }
                    }
                }
            }
        },
        confirmButton = {
            CareButton(
                text = if (modeTab == 0) "Link Elder" else "Create",
                backgroundColor = tokens.mintGreen,
                enabled = if (modeTab == 0) codeInput.trim().length == 6 else nameInput.trim().isNotEmpty(),
                onClick = {
                    if (modeTab == 0) onClaimCode(codeInput.trim())
                    else onCreateManaged(nameInput.trim(), avatarKey, waterGoal, ageInput.trim().ifEmpty { null })
                }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}
