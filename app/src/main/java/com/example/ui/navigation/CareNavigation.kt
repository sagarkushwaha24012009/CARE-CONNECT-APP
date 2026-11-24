package com.example.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.LocalAppPreferences
import com.example.data.local.UserPrefsDataStore
import com.example.data.repository.AiWellnessRepository
import com.example.data.repository.CareRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.signOutUser
import com.example.location.LocationHelper
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.UiState
import com.example.model.UserRole
import com.example.notifications.ReminderChannels
import com.example.notifications.ReminderScheduler
import com.example.sensors.FallDetectionMonitor
import com.example.ui.components.BottomNavTab
import com.example.ui.components.CareBottomNavBar
import com.example.ui.components.CareButton
import com.example.ui.components.CareCard
import com.example.ui.components.CareConfirmDialog
import com.example.ui.components.CareNavigationRail
import com.example.ui.components.CarePillButton
import com.example.ui.components.CareTopBar
import com.example.ui.components.OfflineSyncBanner
import com.example.ui.screens.AiHealthTipsScreen
import com.example.ui.screens.AppointmentsScreen
import com.example.ui.screens.CaretakerDashboardScreen
import com.example.ui.screens.ElderDetailsScreen
import com.example.ui.screens.ElderHomeScreen
import com.example.ui.screens.EldersListScreen
import com.example.ui.screens.EmergencyContactsScreen
import com.example.ui.screens.FallDetectionScreen
import com.example.ui.screens.FallSosCountdownDialog
import com.example.ui.screens.MedicineScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.RoleSelectionAuthScreen
import com.example.ui.screens.SosScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.screens.WaterTrackerScreen
import com.example.ui.screens.launchDialerIntent
import com.example.ui.theme.CareConnectTheme
import com.example.ui.theme.LocalCareColors
import com.example.viewmodel.CareDashboardData
import com.example.viewmodel.CareViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun CareConnectApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsDataStore = remember(context) { UserPrefsDataStore(context) }
    val localPrefs by prefsDataStore.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = LocalAppPreferences()
    )

    val authUser by remember { Firebase.auth.authStateFlow() }
        .collectAsState(initial = Firebase.auth.currentUser)

    val credentialManager = remember(context) { CredentialManager.create(context) }
    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }
    var viewingGuide by remember { mutableStateOf(false) }

    val hashRouter = rememberHashRouterState(initialRoute = PrimaryHashRoute.SPLASH)
    val targetPrimaryRoute by hashRouter.currentRoute.collectAsStateWithLifecycle()
    val primaryNavController = rememberNavController()

    LaunchedEffect(Unit) {
        ReminderChannels.ensureChannels(context)
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = scope
            )
        } else {
            autoSignInChecked = true
        }
    }

    val resolvedPrimaryRoute = resolveTargetPrimaryHash(
        isAuthResolved = autoSignInChecked,
        isAuthenticated = authUser != null,
        onboardingCompleted = localPrefs.onboardingCompleted,
        activeRole = localPrefs.selectedRole,
        viewingGuide = viewingGuide
    )

    LaunchedEffect(resolvedPrimaryRoute) {
        val replaceCurrent = targetPrimaryRoute == PrimaryHashRoute.SPLASH ||
            resolvedPrimaryRoute != PrimaryHashRoute.SPLASH ||
            authUser != null
        hashRouter.navigate(resolvedPrimaryRoute, replace = replaceCurrent)
    }

    LaunchedEffect(targetPrimaryRoute, authUser?.uid) {
        val currentPrimaryRoute = primaryNavController.currentBackStackEntry?.destination?.route
        if (currentPrimaryRoute != targetPrimaryRoute.navRoute) {
            val switchingDashboards = (currentPrimaryRoute == PrimaryHashRoute.ELDER_DASHBOARD.navRoute ||
                currentPrimaryRoute == PrimaryHashRoute.CARETAKER_DASHBOARD.navRoute) &&
                (targetPrimaryRoute == PrimaryHashRoute.ELDER_DASHBOARD ||
                    targetPrimaryRoute == PrimaryHashRoute.CARETAKER_DASHBOARD)
            primaryNavController.navigate(targetPrimaryRoute.navRoute) {
                when {
                    switchingDashboards && currentPrimaryRoute != null -> {
                        popUpTo(currentPrimaryRoute) { inclusive = true }
                    }
                    targetPrimaryRoute == PrimaryHashRoute.SPLASH &&
                        currentPrimaryRoute == PrimaryHashRoute.AUTH.navRoute -> {
                        // Keep Auth below Guide so Back returns to Auth cleanly
                    }
                    else -> {
                        popUpTo(0) { inclusive = true }
                    }
                }
                launchSingleTop = true
            }
        }
    }

    CareConnectTheme(
        themePreset = localPrefs.themePreset,
        darkMode = localPrefs.darkMode,
        textScale = localPrefs.textScale
    ) {
        NavHost(
            navController = primaryNavController,
            startDestination = PrimaryHashRoute.SPLASH.navRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(PrimaryHashRoute.SPLASH.navRoute) {
                val showOnboardingSteps = autoSignInChecked &&
                    authUser == null &&
                    (!localPrefs.onboardingCompleted || viewingGuide)

                if (showOnboardingSteps) {
                    BackHandler(enabled = viewingGuide) {
                        viewingGuide = false
                        hashRouter.navigate(PrimaryHashRoute.AUTH, replace = true)
                    }
                    OnboardingScreen(
                        onContinue = {
                            viewingGuide = false
                            scope.launch { prefsDataStore.setOnboardingCompleted(true) }
                            hashRouter.navigate(PrimaryHashRoute.AUTH, replace = true)
                        }
                    )
                } else {
                    SplashScreen(
                        statusMessage = if (authUser != null) {
                            "Opening Care Connect..."
                        } else {
                            "Checking your family care session..."
                        }
                    )
                }
            }

            composable(PrimaryHashRoute.AUTH.navRoute) {
                RoleSelectionAuthScreen(
                    selectedRole = localPrefs.selectedRole,
                    onSelectRole = { role ->
                        scope.launch { prefsDataStore.setSelectedRole(role) }
                    },
                    onAuthSuccess = { role ->
                        viewingGuide = false
                        scope.launch {
                            prefsDataStore.setOnboardingCompleted(true)
                            prefsDataStore.setSelectedRole(role)
                        }
                        hashRouter.navigate(PrimaryHashRoute.forRole(role), replace = true)
                    },
                    onBackToOnboarding = {
                        viewingGuide = true
                        hashRouter.navigate(PrimaryHashRoute.SPLASH)
                    }
                )
            }

            navigation(
                route = PrimaryHashRoute.WORKSPACE_GRAPH_ROUTE,
                startDestination = PrimaryHashRoute.ELDER_DASHBOARD.navRoute
            ) {
                composable(PrimaryHashRoute.ELDER_DASHBOARD.navRoute) { backStackEntry ->
                    val currentUser = authUser
                    if (currentUser == null) {
                        SplashScreen(statusMessage = "Signing out...")
                    } else {
                        val workspaceStoreOwner = remember(backStackEntry) {
                            primaryNavController.getBackStackEntry(PrimaryHashRoute.WORKSPACE_GRAPH_ROUTE)
                        }
                        AuthenticatedCareWorkspace(
                            user = currentUser,
                            workspaceRole = UserRole.ELDER,
                            workspaceStoreOwner = workspaceStoreOwner,
                            prefsDataStore = prefsDataStore,
                            localPrefs = localPrefs,
                            credentialManager = credentialManager,
                            onSwitchPrimaryRole = { role ->
                                hashRouter.navigate(PrimaryHashRoute.forRole(role), replace = true)
                            },
                            onSignedOut = {
                                hashRouter.navigate(PrimaryHashRoute.AUTH, replace = true)
                            }
                        )
                    }
                }

                composable(PrimaryHashRoute.CARETAKER_DASHBOARD.navRoute) { backStackEntry ->
                    val currentUser = authUser
                    if (currentUser == null) {
                        SplashScreen(statusMessage = "Signing out...")
                    } else {
                        val workspaceStoreOwner = remember(backStackEntry) {
                            primaryNavController.getBackStackEntry(PrimaryHashRoute.WORKSPACE_GRAPH_ROUTE)
                        }
                        AuthenticatedCareWorkspace(
                            user = currentUser,
                            workspaceRole = UserRole.CARETAKER,
                            workspaceStoreOwner = workspaceStoreOwner,
                            prefsDataStore = prefsDataStore,
                            localPrefs = localPrefs,
                            credentialManager = credentialManager,
                            onSwitchPrimaryRole = { role ->
                                hashRouter.navigate(PrimaryHashRoute.forRole(role), replace = true)
                            },
                            onSignedOut = {
                                hashRouter.navigate(PrimaryHashRoute.AUTH, replace = true)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedCareWorkspace(
    user: FirebaseUser,
    workspaceRole: UserRole,
    workspaceStoreOwner: ViewModelStoreOwner,
    prefsDataStore: UserPrefsDataStore,
    localPrefs: LocalAppPreferences,
    credentialManager: CredentialManager,
    onSwitchPrimaryRole: (UserRole) -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalCareColors.current

    val viewModel: CareViewModel = viewModel(
        viewModelStoreOwner = workspaceStoreOwner,
        key = "care_vm_${user.uid}",
        factory = remember(user.uid) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CareViewModel(
                        repository = CareRepository(context),
                        prefsDataStore = prefsDataStore,
                        aiRepository = AiWellnessRepository(),
                        reminderScheduler = ReminderScheduler(context),
                        locationHelper = LocationHelper(context),
                        fallDetectionMonitor = FallDetectionMonitor(context),
                        currentUserId = user.uid
                    ) as T
                }
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedElderId by viewModel.selectedElderId.collectAsStateWithLifecycle()
    val aiTipsState by viewModel.aiTipsState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val fallTelemetry by viewModel.fallTelemetry.collectAsStateWithLifecycle()
    val fallCountdownSeconds by viewModel.fallCountdownSeconds.collectAsStateWithLifecycle()
    val pendingFallAlert by viewModel.pendingFallAlert.collectAsStateWithLifecycle()

    // Ensure initial Firestore user/elder profile exists once signed in
    LaunchedEffect(user.uid) {
        viewModel.ensureProfileInitialized(role = workspaceRole)
    }

    // Monitor network connectivity for offline-tolerant sync
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeCaps = cm.getNetworkCapabilities(cm.activeNetwork)
        val initialOnline = activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        viewModel.setNetworkOnline(initialOnline)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.setNetworkOnline(true)
            }

            override fun onLost(network: Network) {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                val stillConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                viewModel.setNetworkOnline(stillConnected)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var contactToDialFromSos by remember { mutableStateOf<EmergencyContact?>(null) }
    var noContactSosAlertElder by remember { mutableStateOf<ElderProfile?>(null) }

    val data = (dashboardState as? UiState.Success<CareDashboardData>)?.data
    var initialFirestoreRoleSynced by remember(user.uid) { mutableStateOf(false) }
    LaunchedEffect(user.uid, data?.userProfile?.role) {
        val rawRole = data?.userProfile?.role ?: return@LaunchedEffect
        if (!initialFirestoreRoleSynced) {
            initialFirestoreRoleSynced = true
            val resolved = UserRole.fromWire(rawRole)
            if (resolved != workspaceRole) {
                prefsDataStore.setSelectedRole(resolved)
                onSwitchPrimaryRole(resolved)
            }
        }
    }
    val activeRole = workspaceRole
    val unreadCount = data?.notifications?.count { it.readStatus != "read" } ?: 0
    val pendingSyncCount = data?.pendingSyncOps?.size ?: 0
    val todayKey = remember { viewModel.todayDateKey() }

    val topLevelRoutes = remember { BottomNavTab.entries.map { it.route }.toSet() }
    val showBottomOrRailNav = currentRoute in topLevelRoutes

    val subtitle = if (activeRole == UserRole.CARETAKER) {
        stringResource(R.string.tagline_caretaker)
    } else {
        stringResource(R.string.tagline_elder)
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(BottomNavTab.HOME.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openElderRoute(base: String, elderId: String) {
        viewModel.selectElder(elderId)
        navController.navigate("$base/${Uri.encode(elderId)}")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = tokens.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CareTopBar(
                    subtitle = subtitle,
                    unreadNotifications = unreadCount,
                    onProfileClick = { navigateTopTab(BottomNavTab.SETTINGS.route) },
                    onNotificationsClick = { navigateTopTab(BottomNavTab.NOTIFICATIONS.route) },
                    onSettingsClick = { navigateTopTab(BottomNavTab.SETTINGS.route) }
                )
            },
            bottomBar = {
                if (!isWideScreen && showBottomOrRailNav) {
                    CareBottomNavBar(
                        currentRoute = currentRoute,
                        isCaretaker = activeRole == UserRole.CARETAKER,
                        unreadCount = unreadCount,
                        onNavigate = ::navigateTopTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen && showBottomOrRailNav) {
                    CareNavigationRail(
                        currentRoute = currentRoute,
                        isCaretaker = activeRole == UserRole.CARETAKER,
                        unreadCount = unreadCount,
                        onNavigate = ::navigateTopTab
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    OfflineSyncBanner(
                        isOnline = isOnline,
                        pendingCount = pendingSyncCount,
                        onSyncClick = { viewModel.syncPendingChanges() }
                    )

                    val incomingCall = data?.videoCalls?.firstOrNull {
                        it.callStatus == "ringing" && it.callerUid != user.uid
                    }
                    if (incomingCall != null) {
                        CareCard(
                            backgroundColor = tokens.mintGreen,
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("incoming_video_call_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = "Incoming video call",
                                        tint = tokens.ink,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Incoming Video Call",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.ink
                                        )
                                        Text(
                                            text = "${incomingCall.callerName} • ${incomingCall.elderName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.secondaryInk
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CarePillButton(
                                        text = "Answer",
                                        icon = Icons.Filled.Call,
                                        backgroundColor = tokens.butterYellow,
                                        onClick = {
                                            viewModel.answerVideoCall(incomingCall)
                                            openElderRoute("video_call", incomingCall.elderId)
                                        },
                                        testTag = "incoming_call_answer_button"
                                    )
                                    CarePillButton(
                                        text = "Decline",
                                        icon = Icons.Filled.CallEnd,
                                        backgroundColor = tokens.sosCoral,
                                        onClick = { viewModel.declineVideoCall(incomingCall) },
                                        testTag = "incoming_call_decline_button"
                                    )
                                }
                            }
                        }
                    }

                    when (val state = dashboardState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = tokens.ink)
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CareCard(backgroundColor = tokens.sosCoral) {
                                    Text(
                                        text = "Could not connect to Care Connect",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = tokens.ink
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = tokens.ink
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    CareButton(
                                        text = "Retry Profile Setup",
                                        backgroundColor = tokens.cardSurface,
                                        onClick = { viewModel.ensureProfileInitialized(activeRole) }
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            val d = state.data
                            val elders = d.elderProfiles
                            val ownElder = elders.firstOrNull { it.elderId == user.uid } ?: elders.firstOrNull()

                            NavHost(
                                navController = navController,
                                startDestination = BottomNavTab.HOME.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(BottomNavTab.HOME.route) {
                                    if (activeRole == UserRole.CARETAKER) {
                                        val activeSosList = d.sosEvents.filter { it.status == "active" }
                                        CaretakerDashboardScreen(
                                            elders = elders,
                                            selectedElderId = selectedElderId,
                                            medicines = d.medicines,
                                            todayMedLogs = d.medicineLogs.filter { it.dateKey == todayKey },
                                            todayWaterLogs = d.waterLogs.filter { it.dateKey == todayKey },
                                            tasks = d.tasks,
                                            activeSosEvents = activeSosList,
                                            onSelectElder = { viewModel.selectElder(it) },
                                            onOpenElderDetails = { openElderRoute("elder_details", it) },
                                            onClaimPairingCode = { code ->
                                                val cname = d.userProfile?.displayName?.ifBlank { user.displayName } ?: "Caretaker"
                                                viewModel.claimPairingCode(code, cname)
                                            },
                                            onCreateManagedElder = { name, avatar, goal, age ->
                                                viewModel.createManagedElder(name, avatar, goal, age)
                                            },
                                            onCallSosElder = { sos ->
                                                val elderContacts = d.emergencyContacts
                                                    .filter { it.elderId == sos.elderId }
                                                    .sortedBy { it.priorityOrder }
                                                val firstContact = elderContacts.firstOrNull()
                                                if (firstContact != null) {
                                                    contactToDialFromSos = firstContact
                                                } else {
                                                    noContactSosAlertElder = elders.firstOrNull { it.elderId == sos.elderId }
                                                        ?: ElderProfile(elderId = sos.elderId, displayName = sos.elderName)
                                                }
                                            },
                                            onOpenSosDetail = { openElderRoute("sos", it) },
                                            onCallVideoElder = { openElderRoute("video_call", it) },
                                            onQuickActionMedicine = { openElderRoute("medicines", it) },
                                            onQuickActionWater = { openElderRoute("water", it) },
                                            onQuickActionTasks = { openElderRoute("tasks", it) },
                                            onQuickActionContacts = { openElderRoute("contacts", it) },
                                            onQuickActionAppointments = { openElderRoute("appointments", it) },
                                            onQuickActionVideoCall = { openElderRoute("video_call", it) },
                                            onQuickActionFallDetection = { openElderRoute("fall_detection", it) }
                                        )
                                    } else {
                                        val elder = ownElder ?: ElderProfile(
                                            elderId = user.uid,
                                            displayName = d.userProfile?.displayName?.ifBlank { "Friend" } ?: "Friend",
                                            authorizedUids = listOf(user.uid)
                                        )
                                        val elderMeds = d.medicines.filter { it.elderId == elder.elderId }
                                        val elderMedLogs = d.medicineLogs.filter { it.elderId == elder.elderId && it.dateKey == todayKey }
                                        val elderWater = d.waterLogs.firstOrNull { it.elderId == elder.elderId && it.dateKey == todayKey }
                                        val elderTasks = d.tasks.filter { it.elderId == elder.elderId }
                                        val elderContacts = d.emergencyContacts.filter { it.elderId == elder.elderId }
                                        val elderSos = d.sosEvents.firstOrNull { it.elderId == elder.elderId && it.status == "active" }

                                        ElderHomeScreen(
                                            elderProfile = elder,
                                            medicines = elderMeds,
                                            todayMedLogs = elderMedLogs,
                                            todayWater = elderWater,
                                            tasks = elderTasks,
                                            contacts = elderContacts,
                                            activeSos = elderSos,
                                            fallDetectionEnabled = localPrefs.fallDetectionEnabled,
                                            onMarkMedicineTaken = { med -> viewModel.logMedicineStatus(med, "taken") },
                                            onAddWater = {
                                                val current = elderWater?.glasses ?: 0
                                                val goal = elderWater?.goal ?: elder.dailyWaterGoal
                                                viewModel.updateWaterCount(elder, current + 1, goal)
                                            },
                                            onOpenMedicines = { openElderRoute("medicines", elder.elderId) },
                                            onOpenWater = { openElderRoute("water", elder.elderId) },
                                            onOpenTasks = { openElderRoute("tasks", elder.elderId) },
                                            onOpenContacts = { openElderRoute("contacts", elder.elderId) },
                                            onOpenAiTips = { openElderRoute("ai_tips", elder.elderId) },
                                            onOpenSos = { openElderRoute("sos", elder.elderId) },
                                            onOpenVideoCall = { openElderRoute("video_call", elder.elderId) },
                                            onOpenFallDetection = { openElderRoute("fall_detection", elder.elderId) },
                                            onOpenLocationControls = { openElderRoute("sos", elder.elderId) }
                                        )
                                    }
                                }

                                composable(BottomNavTab.ELDERS.route) {
                                    EldersListScreen(
                                        userRole = activeRole,
                                        elders = elders,
                                        caretakerLinks = d.caretakerLinks,
                                        medicines = d.medicines,
                                        todayMedLogs = d.medicineLogs.filter { it.dateKey == todayKey },
                                        todayWaterLogs = d.waterLogs.filter { it.dateKey == todayKey },
                                        tasks = d.tasks,
                                        appointments = d.appointments,
                                        onSelectAndOpenElder = { openElderRoute("elder_details", it) },
                                        onClaimPairingCode = { code ->
                                            val cname = d.userProfile?.displayName?.ifBlank { user.displayName } ?: "Caretaker"
                                            viewModel.claimPairingCode(code, cname)
                                        },
                                        onCreateManagedElder = { name, avatar, goal, age ->
                                            viewModel.createManagedElder(name, avatar, goal, age)
                                        },
                                        onRevokeCaretakerLink = { viewModel.revokeCaretakerLink(it) },
                                        onOpenMedicines = { openElderRoute("medicines", it) },
                                        onOpenWater = { openElderRoute("water", it) },
                                        onOpenTasks = { openElderRoute("tasks", it) },
                                        onOpenContacts = { openElderRoute("contacts", it) },
                                        onOpenAppointments = { openElderRoute("appointments", it) },
                                        onOpenVideoCall = { openElderRoute("video_call", it) },
                                        onOpenFallDetection = { openElderRoute("fall_detection", it) },
                                        onOpenPairingSettings = { navigateTopTab(BottomNavTab.SETTINGS.route) }
                                    )
                                }

                                composable(BottomNavTab.NOTIFICATIONS.route) {
                                    NotificationsScreen(
                                        notifications = d.notifications,
                                        onMarkRead = { viewModel.markNotificationRead(it) },
                                        onMarkAllRead = { viewModel.markAllNotificationsRead(it) }
                                    )
                                }

                                composable(BottomNavTab.SETTINGS.route) {
                                    ProfileSettingsScreen(
                                        userProfile = d.userProfile,
                                        userEmail = user.email ?: "",
                                        activeRole = activeRole,
                                        ownElderProfile = ownElder,
                                        pairingInvites = d.pairingInvites,
                                        caretakerLinks = d.caretakerLinks.filter { it.elderId == (ownElder?.elderId ?: user.uid) },
                                        prefs = localPrefs,
                                        isOnline = isOnline,
                                        pendingSyncCount = pendingSyncCount,
                                        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                                        onSwitchRole = { targetRole ->
                                            viewModel.switchRole(targetRole)
                                            onSwitchPrimaryRole(targetRole)
                                        },
                                        onGeneratePairingCode = { viewModel.generatePairingCode(it) },
                                        onRevokePairingCode = { viewModel.revokePairingCode(it) },
                                        onClaimPairingCode = { code ->
                                            val cname = d.userProfile?.displayName?.ifBlank { user.displayName } ?: "Caretaker"
                                            viewModel.claimPairingCode(code, cname)
                                        },
                                        onRevokeCaretakerLink = { viewModel.revokeCaretakerLink(it) },
                                        onSelectThemePreset = { viewModel.updateThemePreset(it) },
                                        onToggleDarkMode = { viewModel.updateDarkMode(it) },
                                        onSelectTextScale = { viewModel.updateTextScale(it) },
                                        onToggleReminders = { viewModel.updateRemindersEnabled(it) },
                                        onToggleLocationSharing = { elder, enable -> viewModel.toggleLocationSharing(elder, enable) },
                                        onRefreshLocation = { viewModel.refreshLocationNow(it) },
                                        onOpenFallDetection = {
                                            val targetId = ownElder?.elderId ?: selectedElderId ?: user.uid
                                            openElderRoute("fall_detection", targetId)
                                        },
                                        onSyncNow = { viewModel.syncPendingChanges() },
                                        onSignOut = {
                                            signOutUser(credentialManager = credentialManager, scope = scope)
                                            onSignedOut()
                                        }
                                    )
                                }

                                composable(
                                    route = "elder_details/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    ElderDetailsScreen(
                                        elder = elder,
                                        currentUserUid = user.uid,
                                        userRole = activeRole,
                                        caretakerLinks = d.caretakerLinks.filter { it.elderId == elderId },
                                        medicines = d.medicines.filter { it.elderId == elderId },
                                        todayMedLogs = d.medicineLogs.filter { it.elderId == elderId && it.dateKey == todayKey },
                                        todayWater = d.waterLogs.firstOrNull { it.elderId == elderId && it.dateKey == todayKey },
                                        tasks = d.tasks.filter { it.elderId == elderId },
                                        contacts = d.emergencyContacts.filter { it.elderId == elderId },
                                        appointments = d.appointments.filter { it.elderId == elderId },
                                        recentNotifications = d.notifications.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onOpenMedicines = { openElderRoute("medicines", elderId) },
                                        onOpenWater = { openElderRoute("water", elderId) },
                                        onOpenTasks = { openElderRoute("tasks", elderId) },
                                        onOpenContacts = { openElderRoute("contacts", elderId) },
                                        onOpenAppointments = { openElderRoute("appointments", elderId) },
                                        onOpenSos = { openElderRoute("sos", elderId) },
                                        onOpenVideoCall = { openElderRoute("video_call", elderId) },
                                        onOpenFallDetection = { openElderRoute("fall_detection", elderId) },
                                        onRevokeCaretakerLink = { viewModel.revokeCaretakerLink(it) }
                                    )
                                }

                                composable(
                                    route = "medicines/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    MedicineScreen(
                                        elder = elder,
                                        medicines = d.medicines.filter { it.elderId == elderId },
                                        todayLogs = d.medicineLogs.filter { it.elderId == elderId && it.dateKey == todayKey },
                                        onBack = { navController.popBackStack() },
                                        onSaveMedicine = { id, eId, authUids, name, dosage, mins, label, repeat, note ->
                                            viewModel.saveMedicine(id, eId, authUids, name, dosage, mins, label, repeat, note)
                                        },
                                        onDeleteMedicine = { viewModel.deleteMedicine(it) },
                                        onLogStatus = { med, status -> viewModel.logMedicineStatus(med, status) }
                                    )
                                }

                                composable(
                                    route = "water/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    WaterTrackerScreen(
                                        elder = elder,
                                        todayWater = d.waterLogs.firstOrNull { it.elderId == elderId && it.dateKey == todayKey },
                                        onBack = { navController.popBackStack() },
                                        onUpdateWater = { target, glasses, goal ->
                                            viewModel.updateWaterCount(target, glasses, goal)
                                        }
                                    )
                                }

                                composable(
                                    route = "tasks/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    TasksScreen(
                                        elder = elder,
                                        tasks = d.tasks.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onSaveTask = { id, eId, authUids, title, dueDate, dueTime ->
                                            viewModel.saveTask(id, eId, authUids, title, dueDate, dueTime)
                                        },
                                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) }
                                    )
                                }

                                composable(
                                    route = "contacts/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    EmergencyContactsScreen(
                                        elder = elder,
                                        contacts = d.emergencyContacts.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onSaveContact = { id, eId, authUids, name, phone, rel, priority ->
                                            viewModel.saveEmergencyContact(id, eId, authUids, name, phone, rel, priority)
                                        },
                                        onDeleteContact = { viewModel.deleteEmergencyContact(it) }
                                    )
                                }

                                composable(
                                    route = "appointments/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    AppointmentsScreen(
                                        elder = elder,
                                        appointments = d.appointments.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onSaveAppointment = { id, eId, authUids, title, loc, dt, reminder, notes ->
                                            viewModel.saveAppointment(id, eId, authUids, title, loc, dt, reminder, notes)
                                        },
                                        onDeleteAppointment = { viewModel.deleteAppointment(it) }
                                    )
                                }

                                composable(
                                    route = "sos/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    SosScreen(
                                        elder = elder,
                                        userRole = activeRole,
                                        activeSos = d.sosEvents.firstOrNull { it.elderId == elderId && it.status == "active" },
                                        contacts = d.emergencyContacts.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onTriggerSos = { viewModel.triggerSos(it) },
                                        onAcknowledgeSos = { viewModel.acknowledgeSos(it) },
                                        onCancelSos = { viewModel.cancelSos(it) },
                                        onToggleLocationSharing = { target, enable ->
                                            viewModel.toggleLocationSharing(target, enable)
                                        },
                                        onRefreshLocation = { viewModel.refreshLocationNow(it) },
                                        onOpenFallDetection = { openElderRoute("fall_detection", elderId) }
                                    )
                                }

                                composable(
                                    route = "video_call/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    val callerName = d.userProfile?.displayName?.ifBlank { user.displayName } ?: "Family Member"
                                    VideoCallScreen(
                                        elder = elder,
                                        currentUserUid = user.uid,
                                        currentUserName = callerName,
                                        userRole = activeRole,
                                        caretakerLinks = d.caretakerLinks.filter { it.elderId == elderId },
                                        calls = d.videoCalls.filter { it.elderId == elderId },
                                        onBack = { navController.popBackStack() },
                                        onStartCall = { targetElder, name, role ->
                                            viewModel.startVideoCall(targetElder, name, role)
                                        },
                                        onAnswerCall = { viewModel.answerVideoCall(it) },
                                        onDeclineCall = { viewModel.declineVideoCall(it) },
                                        onEndCall = { viewModel.endVideoCall(it) },
                                        onToggleCamera = { call, enabled -> viewModel.toggleCallCamera(call, enabled) },
                                        onToggleMic = { call, muted -> viewModel.toggleCallMic(call, muted) }
                                    )
                                }

                                composable(
                                    route = "fall_detection/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    FallDetectionScreen(
                                        elder = elder,
                                        fallDetectionEnabled = localPrefs.fallDetectionEnabled,
                                        sensitivity = localPrefs.fallSensitivity,
                                        telemetry = fallTelemetry,
                                        fallSosEvents = d.sosEvents.filter {
                                            it.elderId == elderId && it.source != "manual"
                                        },
                                        onBack = { navController.popBackStack() },
                                        onToggleEnabled = { enable -> viewModel.toggleFallDetection(elder, enable) },
                                        onSelectSensitivity = { viewModel.updateFallSensitivity(it) },
                                        onSimulateFall = { viewModel.triggerFallSimulation() },
                                        onOpenSosScreen = { openElderRoute("sos", elderId) }
                                    )
                                }

                                composable(
                                    route = "ai_tips/{elderId}",
                                    arguments = listOf(navArgument("elderId") { type = NavType.StringType })
                                ) { entry ->
                                    val elderId = Uri.decode(entry.arguments?.getString("elderId").orEmpty())
                                    val elder = elders.firstOrNull { it.elderId == elderId }
                                    val elderName = elder?.displayName ?: d.userProfile?.displayName ?: "Friend"
                                    val waterLog = d.waterLogs.firstOrNull { it.elderId == elderId && it.dateKey == todayKey }
                                    val waterLabel = "${waterLog?.glasses ?: 0}/${waterLog?.goal ?: elder?.dailyWaterGoal ?: 8} glasses"
                                    val pendingTasks = d.tasks.count { it.elderId == elderId && it.completionStatus != "completed" }

                                    AiHealthTipsScreen(
                                        elderName = elderName,
                                        waterProgressLabel = waterLabel,
                                        pendingTasksCount = pendingTasks,
                                        tipsState = aiTipsState,
                                        onBack = { navController.popBackStack() },
                                        onLoadTips = { viewModel.loadAiWellnessTips(elderName, waterLabel, pendingTasks) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeFallCountdown = fallCountdownSeconds
    val activeFallAlert = pendingFallAlert
    if (activeFallCountdown != null && activeFallAlert != null) {
        val targetElder = data?.elderProfiles?.firstOrNull { it.elderId == user.uid }
            ?: data?.elderProfiles?.firstOrNull { it.elderId == selectedElderId }
            ?: data?.elderProfiles?.firstOrNull()
        FallSosCountdownDialog(
            secondsLeft = activeFallCountdown,
            alert = activeFallAlert,
            onCancelSafe = { viewModel.cancelFallCountdown() },
            onSendSosNow = {
                if (targetElder != null) {
                    viewModel.confirmFallSosNow(targetElder)
                } else {
                    viewModel.cancelFallCountdown()
                }
            }
        )
    }

    contactToDialFromSos?.let { contact ->
        CareConfirmDialog(
            title = "Call ${contact.name}?",
            message = "Open phone dialer to call ${contact.name} (${contact.relationship}) at ${contact.phoneNumber}?",
            confirmLabel = "Open Dialer",
            confirmColor = tokens.mintGreen,
            onConfirm = {
                launchDialerIntent(context, contact.phoneNumber)
                contactToDialFromSos = null
            },
            onDismiss = { contactToDialFromSos = null }
        )
    }

    noContactSosAlertElder?.let { elder ->
        CareConfirmDialog(
            title = "No Phone Number Saved",
            message = "No emergency contact phone number is saved yet for ${elder.displayName}. Open Emergency Contacts to add one?",
            confirmLabel = "Open Contacts",
            confirmColor = tokens.butterYellow,
            onConfirm = {
                val eId = elder.elderId
                noContactSosAlertElder = null
                openElderRoute("contacts", eId)
            },
            onDismiss = { noContactSosAlertElder = null }
        )
    }
}
