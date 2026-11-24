package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ElderProfile
import com.example.model.FallSensitivity
import com.example.model.SosEvent
import com.example.sensors.FallDetectionAlert
import com.example.sensors.FallTelemetryState
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FallDetectionScreen(
    elder: ElderProfile?,
    fallDetectionEnabled: Boolean,
    sensitivity: FallSensitivity,
    telemetry: FallTelemetryState,
    fallSosEvents: List<SosEvent>,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectSensitivity: (FallSensitivity) -> Unit,
    onSimulateFall: () -> Unit,
    onOpenSosScreen: () -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current
    val timeFormatter = SimpleDateFormat("MMM d, h:mm a", Locale.US)

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
                title = stringResource(R.string.fall_detection_title),
                subtitle = elder?.displayName ?: "Motion Safety Sensor",
                onBack = onBack
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
                        text = stringResource(R.string.fall_detection_safety_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }

            // 1. Live Accelerometer Hardware Status Card
            CareCard(
                backgroundColor = if (fallDetectionEnabled && telemetry.monitoringActive) tokens.mintGreen else tokens.cardSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = "Fall sensor status",
                            tint = tokens.ink,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Motion Fall Sensor",
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.ink
                            )
                            Text(
                                text = when {
                                    !telemetry.sensorAvailable -> "Accelerometer unavailable on this device"
                                    fallDetectionEnabled && telemetry.monitoringActive -> "Monitoring active • Auto-SOS enabled"
                                    else -> "Monitoring paused"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    Switch(
                        checked = fallDetectionEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("fall_detection_switch")
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareStatusBadge(
                        label = "Live: ${String.format(Locale.US, "%.2f", telemetry.currentGForce)} G",
                        backgroundColor = tokens.cardSurface,
                        icon = Icons.Filled.HealthAndSafety
                    )
                    CareStatusBadge(
                        label = "Peak: ${String.format(Locale.US, "%.2f", telemetry.peakGForceObserved)} G",
                        backgroundColor = tokens.skyBlue
                    )
                }
            }

            // 2. Sensitivity Threshold Selector
            CareCard(backgroundColor = tokens.skyBlue) {
                Text(
                    text = "Detection Sensitivity",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Higher sensitivity detects gentler falls (threshold ${sensitivity.impactThresholdG}G) and still allows 15s to cancel false alarms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryInk
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FallSensitivity.entries.forEach { option ->
                        CarePillButton(
                            text = "${option.label} (${option.impactThresholdG}G)",
                            backgroundColor = if (sensitivity == option) tokens.butterYellow else tokens.cardSurface,
                            onClick = { onSelectSensitivity(option) },
                            modifier = Modifier.weight(1f),
                            testTag = "fall_sensitivity_${option.id}"
                        )
                    }
                }
            }

            // 3. Explicit Safety Simulation Mode for Safe Testing
            CareCard(
                backgroundColor = tokens.butterYellow,
                modifier = Modifier.testTag("fall_simulation_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Safety Simulation Mode",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    CareStatusBadge(
                        label = "TEST MODE",
                        backgroundColor = tokens.cardSurface,
                        icon = Icons.Filled.Warning
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Safely verify the 15-second fall countdown, caretaker SOS notification, and permitted location sharing without dropping your device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.secondaryInk
                )
                Spacer(Modifier.height(12.dp))
                CareButton(
                    text = "Simulate Fall Detection (Test)",
                    icon = Icons.Filled.NotificationsActive,
                    backgroundColor = tokens.sosCoral,
                    onClick = onSimulateFall,
                    testTag = "fall_simulate_button"
                )
            }

            // 4. Recent Fall-Triggered SOS Alerts
            CareCard(backgroundColor = tokens.cardSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fall SOS History",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    CarePillButton(
                        text = "Open SOS",
                        backgroundColor = tokens.butterYellow,
                        onClick = onOpenSosScreen
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (fallSosEvents.isEmpty()) {
                    Text(
                        text = "No fall-triggered SOS events recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk
                    )
                } else {
                    fallSosEvents.take(6).forEach { event ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.background,
                            border = BorderStroke(2.dp, tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (event.source == "fall_simulation") {
                                            "Safety Simulation Fall"
                                        } else {
                                            "Sensor Fall Detected"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.ink
                                    )
                                    CareStatusBadge(
                                        label = event.status.replaceFirstChar { it.uppercase() },
                                        backgroundColor = if (event.status == "active") tokens.sosCoral else tokens.mintGreen
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                val impactText = event.peakGForce?.let { "${String.format(Locale.US, "%.1f", it)}G • " } ?: ""
                                val timeText = event.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Just now"
                                Text(
                                    text = "$impactText${event.areaLabel} • $timeText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun FallSosCountdownDialog(
    secondsLeft: Int,
    alert: FallDetectionAlert,
    onCancelSafe: () -> Unit,
    onSendSosNow: () -> Unit
) {
    val tokens = LocalCareColors.current
    AlertDialog(
        onDismissRequest = onCancelSafe,
        containerColor = tokens.sosCoral,
        titleContentColor = tokens.ink,
        textContentColor = tokens.ink,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = tokens.ink,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (alert.isSimulation) {
                        "Simulated Fall Detected"
                    } else {
                        "Fall Detected!"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (alert.isSimulation) {
                    CareStatusBadge(
                        label = "SAFETY SIMULATION MODE",
                        backgroundColor = tokens.butterYellow
                    )
                }
                Text(
                    text = "Sending automatic SOS alert to linked caretakers in $secondsLeft second(s)...",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.ink
                )
                Text(
                    text = "Impact measured: ${String.format(Locale.US, "%.2f", alert.peakGForce)}G. Tap 'I'm Okay' if this was a false alarm.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.ink
                )
            }
        },
        confirmButton = {
            CareButton(
                text = "Send SOS Now",
                icon = Icons.Filled.NotificationsActive,
                backgroundColor = tokens.butterYellow,
                onClick = onSendSosNow,
                testTag = "fall_countdown_send_now"
            )
        },
        dismissButton = {
            CareButton(
                text = "I'm Okay — Cancel",
                icon = Icons.Filled.CheckCircle,
                backgroundColor = tokens.mintGreen,
                onClick = onCancelSafe,
                testTag = "fall_countdown_cancel"
            )
        }
    )
}
