package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.LocalAppPreferences
import com.example.model.CareNotification
import com.example.model.CaretakerLink
import com.example.model.ElderProfile
import com.example.model.PairingInvite
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.WellnessTipsResult
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsScreen(
    notifications: List<CareNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: (List<CareNotification>) -> Unit
) {
    val tokens = LocalCareColors.current
    var selectedCategory by remember { mutableStateOf("all") }

    val filtered = remember(notifications, selectedCategory) {
        if (selectedCategory == "all") notifications
        else notifications.filter { it.category == selectedCategory }
    }
    val unreadCount = notifications.count { it.readStatus != "read" }
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

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
            CareCard(
                backgroundColor = tokens.butterYellow,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Notifications", style = MaterialTheme.typography.headlineSmall, color = tokens.ink)
                            Text("$unreadCount unread alert(s)", style = MaterialTheme.typography.bodyMedium, color = tokens.secondaryInk)
                        }
                    }
                    if (unreadCount > 0) {
                        CarePillButton(
                            text = "Mark All Read",
                            icon = Icons.Filled.DoneAll,
                            backgroundColor = tokens.cardSurface,
                            onClick = { onMarkAllRead(notifications) },
                            testTag = "notifications_mark_all_read"
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "All",
                        "sos" to "SOS",
                        "call" to "Calls"
                    ).forEach { (key, label) ->
                        CarePillButton(
                            text = label,
                            backgroundColor = if (selectedCategory == key) tokens.skyBlue else tokens.cardSurface,
                            onClick = { selectedCategory = key },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "medicine" to "Meds",
                        "task" to "Tasks",
                        "appointment" to "Visits"
                    ).forEach { (key, label) ->
                        CarePillButton(
                            text = label,
                            backgroundColor = if (selectedCategory == key) tokens.skyBlue else tokens.cardSurface,
                            onClick = { selectedCategory = key },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "No notifications here yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Medicine logs, task updates, video calls, appointments, and SOS alerts appear here in real time.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.secondaryInk
                    )
                }
            } else {
                filtered.forEach { notif ->
                    val unread = notif.readStatus != "read"
                    val categoryColor = when (notif.category) {
                        "sos" -> tokens.sosCoral
                        "call" -> tokens.mintGreen
                        "medicine" -> tokens.softPink
                        "task" -> tokens.butterYellow
                        "appointment" -> tokens.skyBlue
                        else -> tokens.lavender
                    }
                    val categoryIcon = when (notif.category) {
                        "sos" -> Icons.Filled.NotificationsActive
                        "call" -> Icons.Filled.Videocam
                        "medicine" -> Icons.Filled.Medication
                        "task" -> Icons.Filled.Checklist
                        "appointment" -> Icons.Filled.CalendarMonth
                        else -> Icons.Filled.Info
                    }

                    CareCard(
                        backgroundColor = if (unread) tokens.cardSurface else tokens.background,
                        onClick = { if (unread) onMarkRead(notif.id) },
                        modifier = Modifier.testTag("notification_card_${notif.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CareStatusBadge(
                                label = notif.category.uppercase(),
                                backgroundColor = categoryColor,
                                icon = categoryIcon
                            )
                            CareStatusBadge(
                                label = if (unread) "Unread" else "Read",
                                backgroundColor = if (unread) tokens.mintGreen else tokens.cardSurface
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = notif.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = notif.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.secondaryInk
                        )
                        notif.createdAt?.toDate()?.let { date ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = timeFormatter.format(date),
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AiHealthTipsScreen(
    elderName: String,
    waterProgressLabel: String,
    pendingTasksCount: Int,
    tipsState: UiState<WellnessTipsResult>,
    onBack: () -> Unit,
    onLoadTips: () -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current

    LaunchedEffect(Unit) {
        if (tipsState is UiState.Loading) {
            onLoadTips()
        }
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
            ScreenHeaderBar(
                title = "AI Health Tips",
                subtitle = "Daily Wellness for $elderName",
                onBack = onBack,
                action = {
                    CarePillButton(
                        text = "Refresh",
                        icon = Icons.Filled.Refresh,
                        backgroundColor = tokens.cardSurface,
                        onClick = onLoadTips,
                        testTag = "ai_tips_refresh_button"
                    )
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
                        text = stringResource(R.string.ai_wellness_disclaimer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }

            when (tipsState) {
                is UiState.Loading -> {
                    CareCard(backgroundColor = tokens.skyBlue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = tokens.ink)
                            Column {
                                Text(
                                    text = "Creating fresh wellness tips...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.ink
                                )
                                Text(
                                    text = "Tailored to $waterProgressLabel water and $pendingTasksCount task(s) left.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    CareCard(backgroundColor = tokens.sosCoral) {
                        Text(
                            text = "Could not load AI wellness tips",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tipsState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(12.dp))
                        CareButton(
                            text = "Try Again",
                            icon = Icons.Filled.Refresh,
                            backgroundColor = tokens.cardSurface,
                            onClick = onLoadTips,
                            testTag = "ai_tips_retry_button"
                        )
                    }
                }
                is UiState.Success -> {
                    tipsState.data.tips.forEachIndexed { idx, tip ->
                        val cardBg = when (tip.category.lowercase()) {
                            "hydration" -> tokens.mintGreen
                            "rest" -> tokens.lavender
                            "social" -> tokens.butterYellow
                            else -> tokens.skyBlue
                        }
                        CareCard(
                            backgroundColor = cardBg,
                            modifier = Modifier.testTag("ai_tip_card_$idx")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Whatshot,
                                        contentDescription = null,
                                        tint = tokens.ink,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = tip.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = tokens.ink
                                    )
                                }
                                CareStatusBadge(
                                    label = tip.category,
                                    backgroundColor = tokens.cardSurface
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = tip.tip,
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.ink
                            )
                        }
                    }
                    CareCard(backgroundColor = tokens.cardSurface, contentPadding = PaddingValues(12.dp)) {
                        Text(
                            text = tipsState.data.disclaimer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProfileSettingsScreen(
    userProfile: UserProfile?,
    userEmail: String,
    activeRole: UserRole,
    ownElderProfile: ElderProfile?,
    pairingInvites: List<PairingInvite>,
    caretakerLinks: List<CaretakerLink>,
    prefs: LocalAppPreferences,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onUpdateDisplayName: (String) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onGeneratePairingCode: (String) -> Unit,
    onRevokePairingCode: (String) -> Unit,
    onClaimPairingCode: (String) -> Unit,
    onRevokeCaretakerLink: (CaretakerLink) -> Unit,
    onSelectThemePreset: (ThemePreset) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectTextScale: (TextScaleOption) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onToggleLocationSharing: (ElderProfile, Boolean) -> Unit,
    onRefreshLocation: (ElderProfile) -> Unit,
    onOpenFallDetection: () -> Unit = {},
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit
) {
    val tokens = LocalCareColors.current
    val context = LocalContext.current

    var showEditNameDialog by remember { mutableStateOf(false) }
    var rolePendingConfirm by remember { mutableStateOf<UserRole?>(null) }
    var linkToRevoke by remember { mutableStateOf<CaretakerLink?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var claimCodeInput by remember { mutableStateOf("") }
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        onToggleReminders(granted)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted && ownElderProfile != null) {
            onToggleLocationSharing(ownElderProfile, true)
        }
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
            // 1. Account & Role Card
            CareCard(backgroundColor = tokens.headerSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(tokens.cardSurface)
                                .border(2.5.dp, tokens.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = tokens.ink, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userProfile?.displayName?.ifBlank { "Care Connect User" } ?: "Care Connect User",
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.ink
                            )
                            Text(
                                text = userEmail.ifBlank { "Signed in with Google" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    CarePillButton(
                        text = "Edit",
                        backgroundColor = tokens.cardSurface,
                        onClick = { showEditNameDialog = true },
                        testTag = "settings_edit_name_button"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Active Role",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.ink
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CareButton(
                        text = "Elder View",
                        backgroundColor = if (activeRole == UserRole.ELDER) tokens.skyBlue else tokens.cardSurface,
                        onClick = { if (activeRole != UserRole.ELDER) rolePendingConfirm = UserRole.ELDER },
                        modifier = Modifier.weight(1f),
                        testTag = "settings_role_elder"
                    )
                    CareButton(
                        text = "Caretaker View",
                        backgroundColor = if (activeRole == UserRole.CARETAKER) tokens.mintGreen else tokens.cardSurface,
                        onClick = { if (activeRole != UserRole.CARETAKER) rolePendingConfirm = UserRole.CARETAKER },
                        modifier = Modifier.weight(1f),
                        testTag = "settings_role_caretaker"
                    )
                }
            }

            // 2. Family Linking & Consent Pairing Code Card
            CareCard(backgroundColor = tokens.skyBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = tokens.ink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Family Caretaker Linking", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                }
                Spacer(Modifier.height(8.dp))

                if (activeRole == UserRole.ELDER) {
                    val elderName = ownElderProfile?.displayName ?: userProfile?.displayName ?: "Elder"
                    CareButton(
                        text = "Generate 6-Character Pairing Code",
                        backgroundColor = tokens.butterYellow,
                        onClick = { onGeneratePairingCode(elderName) },
                        testTag = "settings_generate_code_button"
                    )
                    if (copiedMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(copiedMessage ?: "", style = MaterialTheme.typography.bodyMedium, color = tokens.ink)
                    }
                    val activeInvites = pairingInvites.filter { it.status == "active" }
                    if (activeInvites.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        activeInvites.forEach { invite ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.cardSurface,
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
                                    Column {
                                        Text(
                                            text = invite.code,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = tokens.ink
                                        )
                                        Text(
                                            text = "Share only with a trusted caretaker",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.secondaryInk
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CarePillButton(
                                            text = "Copy",
                                            icon = Icons.Filled.ContentCopy,
                                            backgroundColor = tokens.mintGreen,
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Pairing Code", invite.code))
                                                copiedMessage = "Copied code ${invite.code} to clipboard."
                                            }
                                        )
                                        CarePillButton(
                                            text = "Revoke",
                                            backgroundColor = tokens.sosCoral,
                                            onClick = { onRevokePairingCode(invite.code) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (caretakerLinks.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Authorized Caretakers", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                        caretakerLinks.forEach { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(link.caretakerName, style = MaterialTheme.typography.bodyLarge, color = tokens.ink)
                                CarePillButton(
                                    text = "Revoke",
                                    icon = Icons.Filled.Delete,
                                    backgroundColor = tokens.sosCoral,
                                    onClick = { linkToRevoke = link }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Enter an elder's 6-character pairing code to connect and manage their care.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = claimCodeInput,
                        onValueChange = { claimCodeInput = it.uppercase().take(6) },
                        label = { Text("6-Character Pairing Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_claim_code_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    CareButton(
                        text = "Link Elder with Code",
                        backgroundColor = tokens.mintGreen,
                        enabled = claimCodeInput.trim().length == 6,
                        testTag = "settings_claim_code_button",
                        onClick = {
                            onClaimPairingCode(claimCodeInput.trim())
                            claimCodeInput = ""
                        }
                    )
                }
            }

            // 3. Appearance & Accessibility Card
            CareCard(backgroundColor = tokens.cardSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = tokens.ink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Appearance & Readability", style = MaterialTheme.typography.titleLarge, color = tokens.ink)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                        Text("Reduces glare at night while keeping thick borders", style = MaterialTheme.typography.bodyMedium, color = tokens.secondaryInk)
                    }
                    Switch(
                        checked = prefs.darkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Color Theme", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreset.entries.forEach { preset ->
                        CarePillButton(
                            text = preset.label,
                            backgroundColor = if (prefs.themePreset == preset) tokens.butterYellow else tokens.background,
                            onClick = { onSelectThemePreset(preset) },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_preset_${preset.id}"
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Text Size", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextScaleOption.entries.forEach { scale ->
                        CarePillButton(
                            text = scale.label,
                            backgroundColor = if (prefs.textScale == scale) tokens.mintGreen else tokens.background,
                            onClick = { onSelectTextScale(scale) },
                            modifier = Modifier.weight(1f),
                            testTag = "text_scale_${scale.id}"
                        )
                    }
                }
            }

            // 4. Notifications & Location Permissions Card
            CareCard(backgroundColor = tokens.mintGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medicine & Visit Reminders", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                        Text(
                            text = if (hasNotifPermission) "Local alerts enabled" else "Grant notification permission for alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    }
                    Switch(
                        checked = prefs.remindersEnabled && hasNotifPermission,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onToggleReminders(enable)
                            }
                        },
                        modifier = Modifier.testTag("settings_reminders_switch")
                    )
                }

                if (ownElderProfile != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share Approximate Location", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                            Text(
                                text = ownElderProfile.lastKnownArea ?: "Only shared when enabled with permission",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                        Switch(
                            checked = ownElderProfile.locationSharingEnabled && hasLocationPermission,
                            onCheckedChange = { enable ->
                                if (enable && !hasLocationPermission) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    onToggleLocationSharing(ownElderProfile, enable)
                                }
                            },
                            modifier = Modifier.testTag("settings_location_switch")
                        )
                    }
                    if (ownElderProfile.locationSharingEnabled && hasLocationPermission) {
                        Spacer(Modifier.height(8.dp))
                        CarePillButton(
                            text = "Refresh Location Now",
                            icon = Icons.Filled.LocationOn,
                            backgroundColor = tokens.cardSurface,
                            onClick = { onRefreshLocation(ownElderProfile) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fall Detection & Sensitivity", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                        Text(
                            text = if (prefs.fallDetectionEnabled) {
                                "Active • ${prefs.fallSensitivity.label} sensitivity"
                            } else {
                                "Paused • Tap to configure or simulate"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    }
                    CarePillButton(
                        text = "Configure",
                        icon = Icons.Filled.Sensors,
                        backgroundColor = tokens.cardSurface,
                        onClick = onOpenFallDetection,
                        testTag = "settings_fall_detection_button"
                    )
                }
            }

            // 5. Offline Sync & Sign Out Card
            CareCard(backgroundColor = tokens.lavender) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline Tolerance & Sync", style = MaterialTheme.typography.titleMedium, color = tokens.ink)
                        Text(
                            text = "${if (isOnline) "Connected" else "Offline"} • $pendingSyncCount queued update(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    }
                    CarePillButton(
                        text = "Sync Now",
                        icon = Icons.Filled.Sync,
                        backgroundColor = tokens.cardSurface,
                        onClick = onSyncNow,
                        testTag = "settings_sync_now_button"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.privacy_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryInk
                )
            }

            CareButton(
                text = "Sign Out",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                backgroundColor = tokens.sosCoral,
                onClick = { showSignOutConfirm = true },
                testTag = "settings_sign_out_button"
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(userProfile?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = tokens.cardSurface,
            titleContentColor = tokens.ink,
            textContentColor = tokens.ink,
            title = { Text("Update Display Name", style = MaterialTheme.typography.headlineSmall) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(60) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                CareButton(
                    text = "Save",
                    backgroundColor = tokens.mintGreen,
                    enabled = nameInput.trim().isNotEmpty(),
                    onClick = {
                        onUpdateDisplayName(nameInput.trim())
                        showEditNameDialog = false
                    }
                )
            },
            dismissButton = {
                CarePillButton(text = "Cancel", onClick = { showEditNameDialog = false }, backgroundColor = tokens.background)
            }
        )
    }

    rolePendingConfirm?.let { targetRole ->
        CareConfirmDialog(
            title = "Switch to ${targetRole.displayLabel} Mode?",
            message = "Switch your Care Connect workspace to ${targetRole.displayLabel} mode?",
            confirmLabel = "Switch",
            confirmColor = tokens.butterYellow,
            onConfirm = {
                onSwitchRole(targetRole)
                rolePendingConfirm = null
            },
            onDismiss = { rolePendingConfirm = null }
        )
    }

    linkToRevoke?.let { link ->
        CareConfirmDialog(
            title = "Revoke Caretaker Access?",
            message = "Remove ${link.caretakerName}'s access to your care schedule and SOS alerts?",
            confirmLabel = "Revoke",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                onRevokeCaretakerLink(link)
                linkToRevoke = null
            },
            onDismiss = { linkToRevoke = null }
        )
    }

    if (showSignOutConfirm) {
        CareConfirmDialog(
            title = "Sign Out?",
            message = "Sign out of Care Connect on this device?",
            confirmLabel = "Sign Out",
            confirmColor = tokens.sosCoral,
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }
}
