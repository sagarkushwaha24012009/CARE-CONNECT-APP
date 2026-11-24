package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalCareColors

@Composable
fun CareCard(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalCareColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier) {
        // Hard Neo-Brutalist offset shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(shape)
                .background(tokens.shadow)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier
                            .clickable(onClick = onClick)
                    } else Modifier
                ),
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(borderWidth, tokens.border)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun CareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalCareColors.current.butterYellow,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val tokens = LocalCareColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(shape)
                .background(tokens.shadow)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick),
            shape = shape,
            color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
            border = BorderStroke(2.5.dp, tokens.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CarePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalCareColors.current.cardSurface,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val tokens = LocalCareColors.current
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(2.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.ink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CareStatusBadge(
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    icon: ImageVector? = null
) {
    val tokens = LocalCareColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(2.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.5.dp, tokens.border, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.ink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.ink,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CareConnectLogoMark(modifier: Modifier = Modifier) {
    val tokens = LocalCareColors.current
    val logoDesc = stringResource(R.string.cd_app_logo)
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = logoDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // Coral Heart on top-right behind/beside house
            drawCircle(
                color = Color(0xFFFF5A5F),
                radius = w * 0.24f,
                center = Offset(w * 0.72f, h * 0.30f)
            )
            // House silhouette
            val housePath = Path().apply {
                moveTo(w * 0.08f, h * 0.50f)
                lineTo(w * 0.44f, h * 0.16f)
                lineTo(w * 0.80f, h * 0.50f)
                lineTo(w * 0.72f, h * 0.50f)
                lineTo(w * 0.72f, h * 0.88f)
                lineTo(w * 0.16f, h * 0.88f)
                lineTo(w * 0.16f, h * 0.50f)
                close()
            }
            drawPath(housePath, color = tokens.ink)
            // Door cutout
            drawRoundRect(
                color = tokens.headerSurface,
                topLeft = Offset(w * 0.36f, h * 0.58f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.30f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun CareTopBar(
    subtitle: String,
    unreadNotifications: Int,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showBell: Boolean = true,
    showSettingsGear: Boolean = true
) {
    val tokens = LocalCareColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.headerSurface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CareConnectLogoMark()
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Care Connect",
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_profile_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tokens.cardSurface)
                        .border(2.5.dp, tokens.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        tint = tokens.ink,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (showBell) {
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications),
                            tint = tokens.ink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(tokens.badgeRed)
                                .border(1.5.dp, tokens.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                color = tokens.badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            if (showSettingsGear) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = tokens.ink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(tokens.border)
        )
    }
}

@Composable
fun OfflineSyncBanner(
    isOnline: Boolean,
    pendingCount: Int,
    onSyncClick: () -> Unit
) {
    if (isOnline && pendingCount == 0) return
    val tokens = LocalCareColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = tokens.butterYellow,
        border = BorderStroke(2.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (!isOnline) Icons.Filled.CloudOff else Icons.Filled.Sync,
                contentDescription = null,
                tint = tokens.ink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    !isOnline && pendingCount > 0 -> "Offline mode • $pendingCount change(s) queued to sync"
                    !isOnline -> "Offline mode • Showing cached care records"
                    else -> "$pendingCount queued update(s) ready to sync"
                },
                style = MaterialTheme.typography.labelMedium,
                color = tokens.ink,
                modifier = Modifier.weight(1f)
            )
            if (isOnline && pendingCount > 0) {
                CarePillButton(
                    text = "Sync",
                    onClick = onSyncClick,
                    backgroundColor = tokens.cardSurface
                )
            }
        }
    }
}

enum class BottomNavTab(
    val route: String,
    val labelForElder: String,
    val labelForCaretaker: String,
    val icon: ImageVector
) {
    HOME("home", "Home", "Dashboard", Icons.Filled.Home),
    ELDERS("elders", "Care", "Elders", Icons.Filled.People),
    NOTIFICATIONS("notifications", "Notifications", "Notifications", Icons.Filled.Notifications),
    SETTINGS("settings", "Settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun CareBottomNavBar(
    currentRoute: String?,
    isCaretaker: Boolean,
    unreadCount: Int,
    onNavigate: (String) -> Unit
) {
    val tokens = LocalCareColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.cardSurface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(tokens.border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEachIndexed { index, tab ->
                val selected = currentRoute == tab.route
                val label = if (isCaretaker) tab.labelForCaretaker else tab.labelForElder
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(tokens.border)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) tokens.navActive else tokens.cardSurface)
                        .testTag("nav_${tab.route}")
                        .clickable { onNavigate(tab.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = label,
                                tint = tokens.ink,
                                modifier = Modifier.size(26.dp)
                            )
                            if (tab == BottomNavTab.NOTIFICATIONS && unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(tokens.badgeRed)
                                        .border(1.5.dp, tokens.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                        color = tokens.badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CareNavigationRail(
    currentRoute: String?,
    isCaretaker: Boolean,
    unreadCount: Int,
    onNavigate: (String) -> Unit
) {
    val tokens = LocalCareColors.current
    NavigationRail(
        containerColor = tokens.cardSurface,
        modifier = Modifier.border(BorderStroke(2.dp, tokens.border))
    ) {
        Spacer(Modifier.height(16.dp))
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val label = if (isCaretaker) tab.labelForCaretaker else tab.labelForElder
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Box {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = label,
                            tint = tokens.ink
                        )
                        if (tab == BottomNavTab.NOTIFICATIONS && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(tokens.badgeRed)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.ink
                    )
                }
            )
        }
    }
}

@Composable
fun CareConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color = LocalCareColors.current.butterYellow,
    dismissLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCareColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardSurface,
        titleContentColor = tokens.ink,
        textContentColor = tokens.secondaryInk,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            CareButton(
                text = confirmLabel,
                onClick = onConfirm,
                backgroundColor = confirmColor,
                modifier = Modifier.width(140.dp),
                testTag = "dialog_confirm_button"
            )
        },
        dismissButton = {
            CarePillButton(
                text = dismissLabel,
                onClick = onDismiss,
                backgroundColor = tokens.background,
                testTag = "dialog_dismiss_button"
            )
        }
    )
}
