package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CareAppointment
import com.example.model.CareTask
import com.example.model.ElderProfile
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.WaterLog
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import java.util.Locale

@Composable
fun MedicineScreen(
    elder: ElderProfile?,
    medicines: List<MedicineSchedule>,
    todayLogs: List<MedicineLog>,
    onBack: () -> Unit,
    onSaveMedicine: (
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        dosage: String,
        timeMinutes: Int,
        timeLabel: String,
        repeatSchedule: String,
        note: String?
    ) -> Unit,
    onDeleteMedicine: (String) -> Unit,
    onLogStatus: (MedicineSchedule, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current

    var editingMed by remember { mutableStateOf<MedicineSchedule?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var medToDelete by remember { mutableStateOf<MedicineSchedule?>(null) }
    var pendingStatusConfirm by remember { mutableStateOf<Pair<MedicineSchedule, String>?>(null) }

    val logByMedId = remember(todayLogs) { todayLogs.associateBy { it.medicineId } }

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
                title = "Medicine Schedule",
                subtitle = elder?.displayName ?: "Care Connect",
                onBack = onBack,
                action = {
                    if (elder != null) {
                        CarePillButton(
                            text = "+ Add",
                            backgroundColor = tokens.cardSurface,
                            onClick = { showAddDialog = true },
                            testTag = "add_medicine_button"
                        )
                    }
                }
            )

            CareCard(backgroundColor = tokens.lavender, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = tokens.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.medicine_safety_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("Select or link an elder profile first.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (medicines.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("No medicines scheduled yet", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add medicines directly from your prescription label to receive daily reminders.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "Add Medicine Reminder",
                        icon = Icons.Filled.Add,
                        backgroundColor = tokens.softPink,
                        onClick = { showAddDialog = true },
                        testTag = "empty_add_medicine_button"
                    )
                }
            } else {
                medicines.forEach { med ->
                    val logStatus = logByMedId[med.id]?.status ?: "pending"
                    val statusBadgeBg = when (logStatus) {
                        "taken" -> tokens.mintGreen
                        "skipped" -> tokens.butterYellow
                        else -> tokens.skyBlue
                    }
                    val statusIcon = when (logStatus) {
                        "taken" -> Icons.Filled.CheckCircle
                        "skipped" -> Icons.Filled.Close
                        else -> Icons.Filled.Schedule
                    }
                    val statusLabel = when (logStatus) {
                        "taken" -> "Taken"
                        "skipped" -> "Skipped"
                        else -> "Pending"
                    }

                    CareCard(
                        backgroundColor = tokens.cardSurface,
                        modifier = Modifier.testTag("medicine_card_${med.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Filled.Medication,
                                    contentDescription = null,
                                    tint = tokens.ink,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = med.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = tokens.ink
                                    )
                                    Text(
                                        text = "${med.dosage} • ${med.timeLabel} (${med.repeatSchedule})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = tokens.secondaryInk
                                    )
                                }
                            }
                            CareStatusBadge(
                                label = statusLabel,
                                backgroundColor = statusBadgeBg,
                                icon = statusIcon
                            )
                        }

                        if (!med.prescriptionNote.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Note: ${med.prescriptionNote}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CareButton(
                                text = if (logStatus == "taken") "Taken ✓" else "Mark Taken",
                                backgroundColor = tokens.mintGreen,
                                onClick = { pendingStatusConfirm = med to "taken" },
                                modifier = Modifier.weight(1f),
                                testTag = "med_taken_${med.id}"
                            )
                            CarePillButton(
                                text = "Skip",
                                backgroundColor = tokens.butterYellow,
                                onClick = { pendingStatusConfirm = med to "skipped" },
                                testTag = "med_skip_${med.id}"
                            )
                            IconButton(
                                onClick = { editingMed = med },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${med.name}", tint = tokens.ink)
                            }
                            IconButton(
                                onClick = { medToDelete = med },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${med.name}", tint = tokens.ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if ((showAddDialog || editingMed != null) && elder != null) {
        MedicineEditorDialog(
            initial = editingMed,
            onDismiss = {
                showAddDialog = false
                editingMed = null
            },
            onSave = { name, dosage, timeMinutes, timeLabel, repeat, note ->
                onSaveMedicine(
                    editingMed?.id,
                    elder.elderId,
                    elder.authorizedUids,
                    name,
                    dosage,
                    timeMinutes,
                    timeLabel,
                    repeat,
                    note
                )
                showAddDialog = false
                editingMed = null
            }
        )
    }

    pendingStatusConfirm?.let { (med, status) ->
        val label = if (status == "taken") "Taken" else "Skipped"
        CareConfirmDialog(
            title = "Confirm Medicine Status",
            message = "Mark ${med.name} (${med.dosage}) at ${med.timeLabel} as $label?",
            confirmLabel = "Mark $label",
            confirmColor = if (status == "taken") tokens.mintGreen else tokens.butterYellow,
            onConfirm = {
                onLogStatus(med, status)
                pendingStatusConfirm = null
            },
            onDismiss = { pendingStatusConfirm = null }
        )
    }

    medToDelete?.let { med ->
        CareConfirmDialog(
            title = "Remove Medicine?",
            message = "Remove ${med.name} (${med.timeLabel}) from this elder's schedule?",
            confirmLabel = "Remove",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onDeleteMedicine(med.id)
                medToDelete = null
            },
            onDismiss = { medToDelete = null }
        )
    }
}

@Composable
private fun MedicineEditorDialog(
    initial: MedicineSchedule?,
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, timeMinutes: Int, timeLabel: String, repeat: String, note: String?) -> Unit
) {
    val tokens = LocalCareColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "") }
    var timeMinutes by remember { mutableIntStateOf(initial?.timeMinutes ?: 480) }
    var repeat by remember { mutableStateOf(initial?.repeatSchedule ?: "Daily") }
    var note by remember { mutableStateOf(initial?.prescriptionNote ?: "") }

    val formattedTime = remember(timeMinutes) { formatMinutesToLabel(timeMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        title = {
            Text(
                text = if (initial == null) "Add Medicine Reminder" else "Edit Medicine",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Medicine Name (from prescription)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("med_name_input")
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it.take(40) },
                    label = { Text("Prescription Dosage (e.g., 1 tablet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("med_dosage_input")
                )
                Text("Reminder Time: $formattedTime", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CarePillButton(
                        text = "-30m",
                        onClick = { timeMinutes = (timeMinutes - 30 + 1440) % 1440 },
                        backgroundColor = tokens.background,
                        modifier = Modifier.weight(1f)
                    )
                    CarePillButton(
                        text = "+30m",
                        onClick = { timeMinutes = (timeMinutes + 30) % 1440 },
                        backgroundColor = tokens.background,
                        modifier = Modifier.weight(1f)
                    )
                    CarePillButton(
                        text = "8 AM",
                        onClick = { timeMinutes = 480 },
                        backgroundColor = tokens.skyBlue
                    )
                    CarePillButton(
                        text = "8 PM",
                        onClick = { timeMinutes = 1200 },
                        backgroundColor = tokens.skyBlue
                    )
                }
                Text("Repeat", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekdays", "As Needed").forEach { option ->
                        CarePillButton(
                            text = option,
                            backgroundColor = if (repeat == option) tokens.butterYellow else tokens.background,
                            onClick = { repeat = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("Prescription Note (e.g., After food)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            CareButton(
                text = "Save",
                backgroundColor = tokens.mintGreen,
                enabled = name.trim().isNotEmpty() && dosage.trim().isNotEmpty(),
                testTag = "med_save_button",
                onClick = {
                    onSave(
                        name.trim(),
                        dosage.trim(),
                        timeMinutes,
                        formattedTime,
                        repeat,
                        note.trim().ifEmpty { null }
                    )
                }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}

private fun formatMinutesToLabel(totalMinutes: Int): String {
    val safe = ((totalMinutes % 1440) + 1440) % 1440
    val hour24 = safe / 60
    val minute = safe % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
}

@Composable
fun WaterTrackerScreen(
    elder: ElderProfile?,
    todayWater: WaterLog?,
    onBack: () -> Unit,
    onUpdateWater: (ElderProfile, Int, Int) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current

    val currentGlasses = todayWater?.glasses ?: 0
    val goal = (todayWater?.goal ?: elder?.dailyWaterGoal ?: 8).coerceIn(1, 20)
    val fraction = (currentGlasses.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

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
                title = "Water Tracker",
                subtitle = elder?.displayName ?: "Care Connect",
                onBack = onBack
            )

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("No elder profile selected.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                CareCard(backgroundColor = tokens.mintGreen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$currentGlasses / $goal Glasses",
                                style = MaterialTheme.typography.displaySmall,
                                color = tokens.ink
                            )
                            Text(
                                text = if (currentGlasses >= goal) {
                                    "Daily hydration target reached!"
                                } else {
                                    "${goal - currentGlasses} more glass(es) to reach today's target"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(tokens.progressTrack)
                            .border(2.5.dp, tokens.border, RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(tokens.progressFill)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CareButton(
                            text = "+1 Glass",
                            icon = Icons.Filled.LocalDrink,
                            backgroundColor = tokens.skyBlue,
                            onClick = { onUpdateWater(elder, currentGlasses + 1, goal) },
                            modifier = Modifier.weight(1f),
                            testTag = "water_add_glass_button"
                        )
                        CareButton(
                            text = "-1 Undo",
                            backgroundColor = tokens.cardSurface,
                            enabled = currentGlasses > 0,
                            onClick = { onUpdateWater(elder, (currentGlasses - 1).coerceAtLeast(0), goal) },
                            modifier = Modifier.weight(1f),
                            testTag = "water_undo_glass_button"
                        )
                    }
                }

                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "Daily Target Setting",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$goal glasses per day",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarePillButton(
                                text = "-",
                                backgroundColor = tokens.butterYellow,
                                onClick = {
                                    if (goal > 1) onUpdateWater(elder, currentGlasses, goal - 1)
                                },
                                testTag = "water_goal_minus_button"
                            )
                            CarePillButton(
                                text = "+",
                                backgroundColor = tokens.butterYellow,
                                onClick = {
                                    if (goal < 20) onUpdateWater(elder, currentGlasses, goal + 1)
                                },
                                testTag = "water_goal_plus_button"
                            )
                        }
                    }
                }

                CareCard(backgroundColor = tokens.lavender, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = stringResource(R.string.water_safety_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }
        }
    }
}

@Composable
fun TasksScreen(
    elder: ElderProfile?,
    tasks: List<CareTask>,
    onBack: () -> Unit,
    onSaveTask: (existingId: String?, elderId: String, authorizedUids: List<String>, title: String, dueDate: String, dueTime: String) -> Unit,
    onToggleTask: (CareTask) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<CareTask?>(null) }
    var taskToDelete by remember { mutableStateOf<CareTask?>(null) }

    val pendingCount = tasks.count { it.completionStatus != "completed" }

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
                title = "Daily Tasks",
                subtitle = "$pendingCount pending • ${tasks.size - pendingCount} completed",
                onBack = onBack,
                action = {
                    if (elder != null) {
                        CarePillButton(
                            text = "+ Add",
                            backgroundColor = tokens.cardSurface,
                            onClick = { showDialog = true },
                            testTag = "add_task_button"
                        )
                    }
                }
            )

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("Select an elder profile first.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (tasks.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("No tasks added yet", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add gentle daily reminders like a short walk, calling family, or picking up groceries.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "Add First Task",
                        icon = Icons.Filled.Add,
                        backgroundColor = tokens.butterYellow,
                        onClick = { showDialog = true }
                    )
                }
            } else {
                tasks.forEach { task ->
                    val done = task.completionStatus == "completed"
                    CareCard(
                        backgroundColor = if (done) tokens.mintGreen else tokens.cardSurface,
                        modifier = Modifier.testTag("task_card_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleTask(task) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("task_toggle_${task.id}")
                            ) {
                                Icon(
                                    imageVector = if (done) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = if (done) "Mark ${task.title} pending" else "Mark ${task.title} complete",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleTask(task) }
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = tokens.ink
                                )
                                Text(
                                    text = "${task.dueDateText} • ${task.dueTimeText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                            CareStatusBadge(
                                label = if (done) "Done" else "Pending",
                                backgroundColor = if (done) tokens.cardSurface else tokens.butterYellow
                            )
                            IconButton(onClick = { editingTask = task }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit task", tint = tokens.ink)
                            }
                            IconButton(onClick = { taskToDelete = task }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = tokens.ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if ((showDialog || editingTask != null) && elder != null) {
        TaskEditorDialog(
            initial = editingTask,
            onDismiss = {
                showDialog = false
                editingTask = null
            },
            onSave = { title, dueDate, dueTime ->
                onSaveTask(editingTask?.id, elder.elderId, elder.authorizedUids, title, dueDate, dueTime)
                showDialog = false
                editingTask = null
            }
        )
    }

    taskToDelete?.let { task ->
        CareConfirmDialog(
            title = "Delete Task?",
            message = "Delete \"${task.title}\" from the task list?",
            confirmLabel = "Delete",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onDeleteTask(task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
private fun TaskEditorDialog(
    initial: CareTask?,
    onDismiss: () -> Unit,
    onSave: (title: String, dueDate: String, dueTime: String) -> Unit
) {
    val tokens = LocalCareColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var dueDate by remember { mutableStateOf(initial?.dueDateText ?: "Today") }
    var dueTime by remember { mutableStateOf(initial?.dueTimeText ?: "5:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        title = { Text(if (initial == null) "Add Task" else "Edit Task", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Task Description") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today", "Tomorrow", "This Week").forEach { option ->
                        CarePillButton(
                            text = option,
                            backgroundColor = if (dueDate == option) tokens.butterYellow else tokens.background,
                            onClick = { dueDate = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it.take(30) },
                    label = { Text("Time (e.g., 5:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            CareButton(
                text = "Save",
                backgroundColor = tokens.mintGreen,
                enabled = title.trim().isNotEmpty(),
                testTag = "task_save_button",
                onClick = { onSave(title.trim(), dueDate.trim(), dueTime.trim()) }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}

@Composable
fun AppointmentsScreen(
    elder: ElderProfile?,
    appointments: List<CareAppointment>,
    onBack: () -> Unit,
    onSaveAppointment: (
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        title: String,
        location: String,
        dateTimeText: String,
        reminderEnabled: Boolean,
        notes: String?
    ) -> Unit,
    onDeleteAppointment: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current

    var showDialog by remember { mutableStateOf(false) }
    var editingAppt by remember { mutableStateOf<CareAppointment?>(null) }
    var apptToDelete by remember { mutableStateOf<CareAppointment?>(null) }

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
                title = "Appointments",
                subtitle = elder?.displayName ?: "Care Connect",
                onBack = onBack,
                action = {
                    if (elder != null) {
                        CarePillButton(
                            text = "+ Add",
                            backgroundColor = tokens.cardSurface,
                            onClick = { showDialog = true },
                            testTag = "add_appointment_button"
                        )
                    }
                }
            )

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("Select an elder profile first.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (appointments.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text("No appointments scheduled", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Keep track of clinic visits, eye checkups, or family visits with reminders.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "Schedule Appointment",
                        icon = Icons.Filled.CalendarMonth,
                        backgroundColor = tokens.skyBlue,
                        onClick = { showDialog = true }
                    )
                }
            } else {
                appointments.forEach { appt ->
                    CareCard(
                        backgroundColor = tokens.cardSurface,
                        modifier = Modifier.testTag("appointment_card_${appt.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appt.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.ink
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = appt.dateTimeText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.ink
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = tokens.ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = appt.location,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = tokens.secondaryInk
                                    )
                                }
                                if (!appt.notes.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Notes: ${appt.notes}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.secondaryInk
                                    )
                                }
                            }
                            CareStatusBadge(
                                label = if (appt.reminderFlag == "enabled") "Reminder On" else "Reminder Off",
                                backgroundColor = if (appt.reminderFlag == "enabled") tokens.mintGreen else tokens.background,
                                icon = Icons.Filled.NotificationsActive
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarePillButton(
                                text = "Edit",
                                icon = Icons.Filled.Edit,
                                backgroundColor = tokens.butterYellow,
                                onClick = { editingAppt = appt }
                            )
                            CarePillButton(
                                text = "Delete",
                                icon = Icons.Filled.Delete,
                                backgroundColor = tokens.sosCoral,
                                onClick = { apptToDelete = appt }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if ((showDialog || editingAppt != null) && elder != null) {
        AppointmentEditorDialog(
            initial = editingAppt,
            onDismiss = {
                showDialog = false
                editingAppt = null
            },
            onSave = { title, location, dateTime, reminder, notes ->
                onSaveAppointment(
                    editingAppt?.id,
                    elder.elderId,
                    elder.authorizedUids,
                    title,
                    location,
                    dateTime,
                    reminder,
                    notes
                )
                showDialog = false
                editingAppt = null
            }
        )
    }

    apptToDelete?.let { appt ->
        CareConfirmDialog(
            title = "Delete Appointment?",
            message = "Delete \"${appt.title}\" on ${appt.dateTimeText}?",
            confirmLabel = "Delete",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onDeleteAppointment(appt.id)
                apptToDelete = null
            },
            onDismiss = { apptToDelete = null }
        )
    }
}

@Composable
private fun AppointmentEditorDialog(
    initial: CareAppointment?,
    onDismiss: () -> Unit,
    onSave: (title: String, location: String, dateTime: String, reminder: Boolean, notes: String?) -> Unit
) {
    val tokens = LocalCareColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var dateTime by remember { mutableStateOf(initial?.dateTimeText ?: "Tomorrow, 10:30 AM") }
    var reminder by remember { mutableStateOf((initial?.reminderFlag ?: "enabled") == "enabled") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        title = { Text(if (initial == null) "Add Appointment" else "Edit Appointment", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Appointment Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("appt_title_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(100) },
                    label = { Text("Clinic / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it.take(60) },
                    label = { Text("Date & Time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Send Reminder Notification", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(200) },
                    label = { Text("Preparation Notes (optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            CareButton(
                text = "Save",
                backgroundColor = tokens.mintGreen,
                enabled = title.trim().isNotEmpty() && location.trim().isNotEmpty() && dateTime.trim().isNotEmpty(),
                testTag = "appt_save_button",
                onClick = {
                    onSave(title.trim(), location.trim(), dateTime.trim(), reminder, notes.trim().ifEmpty { null })
                }
            )
        },
        dismissButton = {
            CarePillButton(text = "Cancel", onClick = onDismiss, backgroundColor = tokens.background)
        }
    )
}
