package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import com.example.model.UserRole
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConnectLogoMark
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors

@Composable
fun SplashScreen(
    statusMessage: String = "Preparing your family care space..."
) {
    val tokens = LocalCareColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CareCard(
            backgroundColor = tokens.headerSurface,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("splash_screen_card"),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CareConnectLogoMark(modifier = Modifier.size(72.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = tokens.ink
                )
                Text(
                    text = stringResource(R.string.tagline_elder),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.secondaryInk
                )
                CareStatusBadge(
                    label = statusMessage,
                    backgroundColor = tokens.cardSurface
                )
                CircularProgressIndicator(
                    color = tokens.ink,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    val tokens = LocalCareColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CareCard(backgroundColor = tokens.headerSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CareConnectLogoMark(modifier = Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_title),
                            style = MaterialTheme.typography.displaySmall,
                            color = tokens.ink
                        )
                        Text(
                            text = stringResource(R.string.tagline_elder),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.secondaryInk
                        )
                    }
                }
            }

            CareCard(
                backgroundColor = tokens.cardSurface,
                contentPadding = PaddingValues(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_onboarding_care),
                    contentDescription = "Family caretaker supporting an older adult at home",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, tokens.border, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.ink,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            OnboardingStepCard(
                stepNumber = "1",
                title = "Clear Daily Routines",
                description = "Track prescription-entered medicine reminders, daily water glasses, tasks, and appointments.",
                backgroundColor = tokens.mintGreen,
                icon = Icons.Filled.MedicalServices
            )

            OnboardingStepCard(
                stepNumber = "2",
                title = "Private Family Linking",
                description = "Elders share a private 6-character pairing code only with trusted caretakers.",
                backgroundColor = tokens.skyBlue,
                icon = Icons.Filled.People
            )

            OnboardingStepCard(
                stepNumber = "3",
                title = "SOS & Wellness Support",
                description = "Confirmed emergency SOS alerts, quick dial contacts, and gentle AI daily wellness tips.",
                backgroundColor = tokens.softPink,
                icon = Icons.Filled.NotificationsActive
            )

            Spacer(Modifier.height(4.dp))
            CareButton(
                text = "Get Started",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                backgroundColor = tokens.butterYellow,
                onClick = onContinue,
                testTag = "onboarding_continue_button"
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingStepCard(
    stepNumber: String,
    title: String,
    description: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val tokens = LocalCareColors.current
    CareCard(backgroundColor = backgroundColor, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tokens.cardSurface)
                    .border(2.dp, tokens.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step $stepNumber • $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryInk
                )
            }
        }
    }
}

@Composable
fun RoleSelectionAuthScreen(
    selectedRole: UserRole,
    onSelectRole: (UserRole) -> Unit,
    onAuthSuccess: (UserRole) -> Unit,
    onBackToOnboarding: () -> Unit
) {
    val tokens = LocalCareColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CareCard(backgroundColor = tokens.headerSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CareConnectLogoMark()
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Choose Your Role",
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.ink
                            )
                            Text(
                                text = "Sign in securely with Google",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tokens.cardSurface,
                        border = BorderStroke(2.dp, tokens.border),
                        modifier = Modifier
                            .clickable(onClick = onBackToOnboarding)
                            .testTag("auth_guide_button")
                    ) {
                        Text(
                            text = "Guide",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.ink,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            RoleChoiceCard(
                title = "I'm an Elder",
                subtitle = "View medicines, water intake, tasks, AI wellness tips, and trigger SOS.",
                isSelected = selectedRole == UserRole.ELDER,
                backgroundColor = tokens.skyBlue,
                avatarRes = R.drawable.img_elder_grandpa,
                testTag = "role_elder_card",
                onClick = { onSelectRole(UserRole.ELDER) }
            )

            RoleChoiceCard(
                title = "I'm a Caretaker",
                subtitle = "Link with elders via pairing code, manage schedules, contacts, and monitor SOS.",
                isSelected = selectedRole == UserRole.CARETAKER,
                backgroundColor = tokens.butterYellow,
                avatarRes = R.drawable.img_elder_grandma,
                testTag = "role_caretaker_card",
                onClick = { onSelectRole(UserRole.CARETAKER) }
            )

            CareCard(backgroundColor = tokens.lavender, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = tokens.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Privacy & Authorization",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.privacy_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryInk
                        )
                    }
                }
            }

            if (authError != null) {
                CareCard(backgroundColor = tokens.sosCoral, contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = authError ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = tokens.ink
                    )
                }
            }

            CareButton(
                text = if (isLoading) "Signing in..." else "Continue as ${selectedRole.displayLabel} with Google",
                icon = if (isLoading) null else Icons.Filled.Person,
                backgroundColor = tokens.mintGreen,
                enabled = !isLoading,
                testTag = "google_sign_in_button",
                onClick = {
                    authError = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess(selectedRole)
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            authError = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                }
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = tokens.ink)
                }
            }

            Text(
                text = "Uses Google Sign-In via Android Credential Manager. No passwords are stored in the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.secondaryInk,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RoleChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    backgroundColor: androidx.compose.ui.graphics.Color,
    avatarRes: Int,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalCareColors.current
    CareCard(
        backgroundColor = if (isSelected) backgroundColor else tokens.cardSurface,
        borderWidth = if (isSelected) 3.5.dp else 2.dp,
        shadowOffset = if (isSelected) 5.dp else 3.dp,
        modifier = Modifier.testTag(testTag),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, tokens.border, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.ink
                    )
                    if (isSelected) {
                        CareStatusBadge(
                            label = "Selected",
                            backgroundColor = tokens.cardSurface,
                            icon = Icons.Filled.CheckCircle
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryInk
                )
            }
        }
    }
}
