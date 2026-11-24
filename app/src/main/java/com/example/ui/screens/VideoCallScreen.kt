package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.CaretakerLink
import com.example.model.ElderProfile
import com.example.model.UserRole
import com.example.model.VideoCallSession
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareStatusBadge
import com.example.ui.theme.LocalCareColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VideoCallScreen(
    elder: ElderProfile?,
    currentUserUid: String,
    currentUserName: String,
    userRole: UserRole,
    caretakerLinks: List<CaretakerLink>,
    calls: List<VideoCallSession>,
    onBack: () -> Unit,
    onStartCall: (ElderProfile, String, UserRole) -> Unit,
    onAnswerCall: (VideoCallSession) -> Unit,
    onDeclineCall: (VideoCallSession) -> Unit,
    onEndCall: (VideoCallSession) -> Unit,
    onToggleCamera: (VideoCallSession, Boolean) -> Unit,
    onToggleMic: (VideoCallSession, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val tokens = LocalCareColors.current
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    var useFrontCamera by remember { mutableStateOf(true) }
    var speakerOn by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    val activeCall = remember(calls) {
        calls.firstOrNull { it.callStatus == "ringing" || it.callStatus == "active" }
    }

    // Track call duration while callStatus == "active"
    LaunchedEffect(activeCall?.id, activeCall?.callStatus) {
        if (activeCall?.callStatus == "active") {
            elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        } else {
            elapsedSeconds = 0
        }
    }

    // Configure real AudioManager speakerphone & mute routing during active calls
    DisposableEffect(activeCall?.id, activeCall?.callStatus, speakerOn) {
        val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (activeCall != null && (activeCall.callStatus == "active" || activeCall.callStatus == "ringing")) {
            try {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = speakerOn
            } catch (_: Exception) {
            }
        }
        onDispose {
            try {
                audioManager?.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = false
            } catch (_: Exception) {
            }
        }
    }

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
            ScreenHeaderBar(
                title = stringResource(R.string.video_call_title),
                subtitle = elder?.displayName ?: "Authorized Family Call",
                onBack = onBack
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
                    Text(
                        text = stringResource(R.string.video_call_security_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.ink
                    )
                }
            }

            if (elder == null) {
                CareCard(backgroundColor = tokens.cardSurface) {
                    Text(
                        text = "Select or link an elder profile first to start a video call.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.ink
                    )
                }
            } else if (activeCall != null) {
                val isCaller = activeCall.callerUid == currentUserUid
                val localCameraEnabled = if (isCaller) activeCall.callerCameraEnabled else activeCall.calleeCameraEnabled
                val localMicMuted = if (isCaller) activeCall.callerMicMuted else activeCall.calleeMicMuted
                val peerCameraEnabled = if (isCaller) activeCall.calleeCameraEnabled else activeCall.callerCameraEnabled
                val peerMicMuted = if (isCaller) activeCall.calleeMicMuted else activeCall.callerMicMuted
                val peerDisplayName = if (isCaller) {
                    if (userRole == UserRole.CARETAKER) elder.displayName else "Linked Caretaker(s)"
                } else {
                    activeCall.callerName
                }

                // 1. Remote Peer Status Card
                CareCard(
                    backgroundColor = if (activeCall.callStatus == "ringing") tokens.butterYellow else tokens.skyBlue,
                    modifier = Modifier.testTag("video_call_active_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            val avatarRes = if (elder.avatarKey == "grandma") R.drawable.img_elder_grandma else R.drawable.img_elder_grandpa
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "$peerDisplayName avatar",
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, tokens.border, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = peerDisplayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.ink
                                )
                                Text(
                                    text = when (activeCall.callStatus) {
                                        "ringing" -> if (isCaller) "Ringing authorized circle..." else "Incoming Video Call"
                                        "active" -> "Connected • ${formatCallDuration(elapsedSeconds)}"
                                        else -> activeCall.callStatus.replaceFirstChar { it.uppercase() }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                        CareStatusBadge(
                            label = "Room ${activeCall.roomCode.take(6)}",
                            backgroundColor = tokens.cardSurface
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CareStatusBadge(
                            label = if (peerCameraEnabled) "Peer Camera On" else "Peer Camera Off",
                            backgroundColor = if (peerCameraEnabled) tokens.mintGreen else tokens.cardSurface,
                            icon = if (peerCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff
                        )
                        CareStatusBadge(
                            label = if (peerMicMuted) "Peer Muted" else "Peer Mic Live",
                            backgroundColor = if (peerMicMuted) tokens.softPink else tokens.mintGreen,
                            icon = if (peerMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic
                        )
                    }

                    if (activeCall.callStatus == "ringing" && !isCaller) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CareButton(
                                text = "Answer",
                                icon = Icons.Filled.Call,
                                backgroundColor = tokens.mintGreen,
                                onClick = {
                                    if (!hasCameraPermission || !hasAudioPermission) {
                                        mediaPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                        )
                                    }
                                    onAnswerCall(activeCall)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "video_call_answer_button"
                            )
                            CareButton(
                                text = "Decline",
                                icon = Icons.Filled.CallEnd,
                                backgroundColor = tokens.sosCoral,
                                onClick = { onDeclineCall(activeCall) },
                                modifier = Modifier.weight(1f),
                                testTag = "video_call_decline_button"
                            )
                        }
                    }
                }

                // 2. Local CameraX Live Video Preview Card
                CareCard(backgroundColor = tokens.cardSurface, contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Camera Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink
                        )
                        if (!hasCameraPermission || !hasAudioPermission) {
                            CarePillButton(
                                text = "Grant Permissions",
                                backgroundColor = tokens.butterYellow,
                                onClick = {
                                    mediaPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                },
                                testTag = "video_grant_permissions_button"
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    if (hasCameraPermission && localCameraEnabled) {
                        CameraPreviewFrame(
                            useFrontCamera = useFrontCamera,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(2.5.dp, tokens.border, RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tokens.background)
                                .border(2.5.dp, tokens.border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.VideocamOff,
                                    contentDescription = "Camera paused",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (!hasCameraPermission) {
                                        "Grant camera permission for live video preview"
                                    } else {
                                        "Your camera is paused"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = tokens.secondaryInk
                                )
                            }
                        }
                    }
                }

                // 3. Call Controls Grid
                CareCard(backgroundColor = tokens.mintGreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CareButton(
                            text = if (localMicMuted) "Unmute" else "Mute Mic",
                            icon = if (localMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            backgroundColor = if (localMicMuted) tokens.softPink else tokens.cardSurface,
                            onClick = { onToggleMic(activeCall, !localMicMuted) },
                            modifier = Modifier.weight(1f),
                            testTag = "video_toggle_mic_button"
                        )
                        CareButton(
                            text = if (localCameraEnabled) "Camera Off" else "Camera On",
                            icon = if (localCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                            backgroundColor = if (localCameraEnabled) tokens.cardSurface else tokens.softPink,
                            onClick = {
                                if (!hasCameraPermission) {
                                    mediaPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                } else {
                                    onToggleCamera(activeCall, !localCameraEnabled)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "video_toggle_camera_button"
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CareButton(
                            text = "Flip Camera",
                            icon = Icons.Filled.Cameraswitch,
                            backgroundColor = tokens.skyBlue,
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier.weight(1f),
                            testTag = "video_flip_camera_button"
                        )
                        CareButton(
                            text = if (speakerOn) "Speaker On" else "Earpiece",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            backgroundColor = if (speakerOn) tokens.butterYellow else tokens.cardSurface,
                            onClick = { speakerOn = !speakerOn },
                            modifier = Modifier.weight(1f),
                            testTag = "video_speaker_button"
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    CareButton(
                        text = "End Video Call",
                        icon = Icons.Filled.CallEnd,
                        backgroundColor = tokens.sosCoral,
                        onClick = { onEndCall(activeCall) },
                        testTag = "video_end_call_button"
                    )
                }
            } else {
                // Start Call Card
                val targetDescription = if (userRole == UserRole.CARETAKER) {
                    "Call ${elder.displayName}"
                } else {
                    if (caretakerLinks.isNotEmpty()) {
                        "Call Authorized Caretakers (${caretakerLinks.size})"
                    } else {
                        "Call Family Circle"
                    }
                }
                CareCard(backgroundColor = tokens.skyBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = tokens.ink,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetDescription,
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.ink
                            )
                            Text(
                                text = "Initiates a real-time encrypted session visible only to ${elder.displayName}'s authorized circle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryInk
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!hasCameraPermission || !hasAudioPermission) {
                        CarePillButton(
                            text = "Allow Camera & Microphone",
                            backgroundColor = tokens.butterYellow,
                            onClick = {
                                mediaPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "video_request_perms_idle_button"
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    CareButton(
                        text = "Start Video Call",
                        icon = Icons.Filled.Videocam,
                        backgroundColor = tokens.mintGreen,
                        onClick = {
                            if (!hasCameraPermission || !hasAudioPermission) {
                                mediaPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            }
                            onStartCall(elder, currentUserName, userRole)
                        },
                        testTag = "video_start_call_button"
                    )
                }
            }

            // Recent Calls History
            CareCard(backgroundColor = tokens.cardSurface) {
                Text(
                    text = "Recent Video Calls",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.ink
                )
                Spacer(Modifier.height(8.dp))
                if (calls.isEmpty()) {
                    Text(
                        text = "No video calls placed yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryInk
                    )
                } else {
                    calls.take(6).forEach { call ->
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
                                        text = "${call.callerName} → ${call.elderName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.ink
                                    )
                                    val timeStr = call.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Just now"
                                    Text(
                                        text = "$timeStr • Room ${call.roomCode.take(6)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.secondaryInk
                                    )
                                }
                                CareStatusBadge(
                                    label = call.callStatus.replaceFirstChar { it.uppercase() },
                                    backgroundColor = when (call.callStatus) {
                                        "active" -> tokens.mintGreen
                                        "ringing" -> tokens.butterYellow
                                        "declined" -> tokens.sosCoral
                                        else -> tokens.cardSurface
                                    }
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
private fun CameraPreviewFrame(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tokens = LocalCareColors.current
    var cameraBindError by remember { mutableStateOf<String?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null

        val runnable = Runnable {
            try {
                val provider = providerFuture.get()
                boundProvider = provider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val preferredSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val selector = when {
                    provider.hasCamera(preferredSelector) -> preferredSelector
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (selector == null) {
                    cameraBindError = "No camera hardware detected on this device."
                } else {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                    cameraBindError = null
                }
            } catch (e: Exception) {
                Log.w("VideoCallCamera", "Unable to bind CameraX preview", e)
                cameraBindError = "Camera preview unavailable on this device."
            }
        }
        providerFuture.addListener(runnable, mainExecutor)

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    if (cameraBindError != null) {
        Box(
            modifier = modifier.background(tokens.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cameraBindError ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.secondaryInk,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    }
}

private fun formatCallDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
