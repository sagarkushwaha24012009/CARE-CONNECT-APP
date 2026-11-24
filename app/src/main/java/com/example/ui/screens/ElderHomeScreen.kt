package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CareTask
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.SosEvent
import com.example.model.WaterLog
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import java.util.Calendar

@Composable
fun ElderHomeScreen(
    elderProfile: ElderProfile,
    medicines: List<MedicineSchedule>,
    todayMedLogs: List<MedicineLog>,
    todayWater: WaterLog?,
    tasks: List<CareTask>,
    contacts: List<EmergencyContact>,
    activeSos: SosEvent?,
    fallDetectionEnabled: Boolean = true,
    onMarkMedicineTaken: (MedicineSchedule) -> Unit,
    onAddWater: () -> Unit,
    onOpenMedicines: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenAiTips: () -> Unit,
    onOpenSos: () -> Unit,
    onOpenVideoCall: () -> Unit = {},
    onOpenFallDetection: () -> Unit = {},
    onOpenLocationControls: () -> Unit
) {
    val tokens = LocalCareColors.current
    var pendingMedToConfirm by remember { mutableStateOf<MedicineSchedule?>(null) }

    val takenIds = todayMedLogs.filter { it.status == "taken" }.map { it.medicineId }.toSet()
    val skippedIds = todayMedLogs.filter { it.status == "skipped" }.map { it.medicineId }.toSet()
    val pendingMeds = medicines.filter { it.activeStatus == "active" && it.id !in takenIds && it.id !in skippedIds }
    val nextMed = pendingMeds.firstOrNull()

    val waterGlasses = todayWater?.glasses ?: 0
    val waterGoal = (todayWater?.goal ?: elderProfile.dailyWaterGoal).coerceAtLeast(1)
    val waterFraction = (waterGlasses.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f)
    val tasksLeft = tasks.count { it.completionStatus != "completed" }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning!"
            in 12..16 -> "Good Afternoon!"
            else -> "Good Evening!"
        }
    }

    val avatarDrawable = if (elderProfile.avatarKey == "grandma") {
        R.drawable.img_elder_grandma
    } else {
        R.drawable.img_elder_grandpa
    }

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
            // Active SOS Alert reminder banner if SOS is currently running
            if (activeSos != null) {
                CareCard(
                    backgroundColor = tokens.sosCoral,
                    onClick = onOpenSos,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SOS Active (${activeSos.deliveryState.replaceFirstChar { it.uppercase() }})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.ink
                                )
                                Text(
                                    text = "Tap to view status or cancel alert",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.ink
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open SOS",
                            tint = tokens.ink
                        )
                    }
                }
            }

            // 1. Greeting Banner (matches 03_elder_home_dashboard_9x16.png)
            CareCard(
                backgroundColor = tokens.skyBlue,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Take care, ${elderProfile.displayName}!",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.secondaryInk
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = avatarDrawable),
                        contentDescription = stringResource(R.string.cd_elder_avatar),
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, tokens.border, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 2. Medicine Reminder Card (Soft Pink + Yellow Mark Taken)
            CareCard(
                backgroundColor = tokens.softPink,
                onClick = onOpenMedicines,
                modifier = Modifier.testTag("elder_medicine_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = "Medicine reminder",
                        tint = tokens.ink,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medicine Reminder",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                        if (nextMed != null) {
                            Text(
                                text = "Next: ${nextMed.timeLabel} • ${nextMed.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pendingMeds.size} medicine pending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        } else {
                            Text(
                                text = if (medicines.isEmpty()) "No reminders added yet" else "All scheduled doses logged",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    if (nextMed != null) {
                        CareButton(
                            text = "Mark Taken",
                            icon = Icons.Filled.CheckCircle,
                            backgroundColor = tokens.butterYellow,
                            onClick = { pendingMedToConfirm = nextMed },
                            modifier = Modifier.width(136.dp),
                            testTag = "elder_mark_taken_button"
                        )
                    } else {
                        CarePillButton(
                            text = "Schedule",
                            backgroundColor = tokens.butterYellow,
                            onClick = onOpenMedicines,
                            testTag = "elder_open_meds_button"
                        )
                    }
                }
            }

            // 3. Water Today Card (Mint Green + Progress Bar + Sky Blue Add Water)
            CareCard(
                backgroundColor = tokens.mintGreen,
                onClick = onOpenWater,
                modifier = Modifier.testTag("elder_water_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Water tracker",
                        tint = tokens.ink,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Water Today",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                        Text(
                            text = "$waterGlasses / $waterGoal Glasses",
                            style = MaterialTheme.typography.headlineSmall,
                            color = tokens.ink
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    CareButton(
                        text = "Add Water",
                        icon = Icons.Filled.LocalDrink,
                        backgroundColor = tokens.skyBlue,
                        onClick = onAddWater,
                        modifier = Modifier.width(136.dp),
                        testTag = "elder_add_water_button"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tokens.progressTrack)
                        .border(2.5.dp, tokens.border, RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(waterFraction)
                            .fillMaxHeight()
                            .background(tokens.progressFill)
                    )
                }
            }

            // 4. 2x2 Grid Row 1: Tasks (Yellow) + Emergency Contacts (Lavender)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElderGridTile(
                    title = "Tasks",
                    subtitle = if (tasksLeft == 0) "All Done" else "$tasksLeft Left",
                    icon = Icons.Filled.Checklist,
                    backgroundColor = tokens.butterYellow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_tasks_tile"),
                    onClick = onOpenTasks
                )
                ElderGridTile(
                    title = "Emergency Contacts",
                    subtitle = "${contacts.size} Contacts",
                    icon = Icons.Filled.People,
                    backgroundColor = tokens.lavender,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_contacts_tile"),
                    onClick = onOpenContacts
                )
            }

            // 5. 2x2 Grid Row 2: AI Health Tips (Sky Blue) + SOS (Coral Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElderGridTile(
                    title = "AI Health Tips",
                    subtitle = "Stay Active!",
                    icon = Icons.Filled.Whatshot,
                    backgroundColor = tokens.skyBlue,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_ai_tips_tile"),
                    onClick = onOpenAiTips
                )
                ElderGridTile(
                    title = "SOS",
                    subtitle = "Get Help Now",
                    icon = Icons.Filled.NotificationsActive,
                    backgroundColor = tokens.sosCoral,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_sos_tile"),
                    onClick = onOpenSos
                )
            }

            // 5b. Grid Row 3: Video Call (Mint Green) + Fall Sensor (Butter Yellow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElderGridTile(
                    title = "Video Call",
                    subtitle = "Family Check-in",
                    icon = Icons.Filled.Videocam,
                    backgroundColor = tokens.mintGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_video_call_tile"),
                    onClick = onOpenVideoCall
                )
                ElderGridTile(
                    title = "Fall Sensor",
                    subtitle = if (fallDetectionEnabled) "Active" else "Paused",
                    icon = Icons.Filled.Sensors,
                    backgroundColor = tokens.butterYellow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("elder_fall_detection_tile"),
                    onClick = onOpenFallDetection
                )
            }

            // 6. Location Sharing Status Bar
            CareCard(
                backgroundColor = tokens.cardSurface,
                contentPadding = PaddingValues(14.dp),
                onClick = onOpenLocationControls
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location sharing",
                        tint = tokens.ink,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location Sharing",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CareStatusBadge(
                                label = if (elderProfile.locationSharingEnabled) "Active" else "Paused",
                                backgroundColor = if (elderProfile.locationSharingEnabled) tokens.mintGreen else tokens.butterYellow,
                                dotColor = if (elderProfile.locationSharingEnabled) tokens.onlineGreen else tokens.sosCoral
                            )
                            Text(
                                text = elderProfile.lastKnownArea ?: if (elderProfile.locationSharingEnabled) "Ready" else "Only with permission",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    CarePillButton(
                        text = "Manage",
                        backgroundColor = tokens.background,
                        onClick = onOpenLocationControls,
                        testTag = "elder_location_manage_button"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    pendingMedToConfirm?.let { med ->
        CareConfirmDialog(
            title = "Confirm Medicine Status",
            message = "Mark ${med.name} (${med.dosage}) for ${med.timeLabel} as Taken?",
            confirmLabel = "Mark Taken",
            confirmColor = tokens.mintGreen,
            onConfirm = {
                onMarkMedicineTaken(med)
                pendingMedToConfirm = null
            },
            onDismiss = { pendingMedToConfirm = null }
        )
    }
}

@Composable
private fun ElderGridTile(
    title: String,
    subtitle: String,
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
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = tokens.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = tokens.ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
