package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalAppPreferences
import com.example.data.local.PendingSyncOpEntity
import com.example.data.local.UserPrefsDataStore
import com.example.data.repository.AiWellnessRepository
import com.example.data.repository.CareRepository
import com.example.location.LocationHelper
import com.example.model.CareAppointment
import com.example.model.CareNotification
import com.example.model.CareTask
import com.example.model.CaretakerLink
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.FallSensitivity
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.PairingInvite
import com.example.model.SosEvent
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VideoCallSession
import com.example.model.WaterLog
import com.example.model.WellnessTipsResult
import com.example.notifications.ReminderScheduler
import com.example.sensors.FallDetectionAlert
import com.example.sensors.FallDetectionMonitor
import com.example.sensors.FallTelemetryState
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CareDashboardData(
    val userProfile: UserProfile?,
    val elderProfiles: List<ElderProfile>,
    val caretakerLinks: List<CaretakerLink>,
    val pairingInvites: List<PairingInvite>,
    val medicines: List<MedicineSchedule>,
    val medicineLogs: List<MedicineLog>,
    val waterLogs: List<WaterLog>,
    val tasks: List<CareTask>,
    val emergencyContacts: List<EmergencyContact>,
    val appointments: List<CareAppointment>,
    val notifications: List<CareNotification>,
    val sosEvents: List<SosEvent>,
    val videoCalls: List<VideoCallSession>,
    val pendingSyncOps: List<PendingSyncOpEntity>
)

private data class CoreRecordsBundle(
    val userProfile: UserProfile?,
    val elderProfiles: List<ElderProfile>,
    val caretakerLinks: List<CaretakerLink>,
    val pairingInvites: List<PairingInvite>,
    val medicines: List<MedicineSchedule>
)

private data class DailyCareBundle(
    val medicineLogs: List<MedicineLog>,
    val waterLogs: List<WaterLog>,
    val tasks: List<CareTask>,
    val emergencyContacts: List<EmergencyContact>,
    val appointments: List<CareAppointment>
)

private data class AlertsBundle(
    val notifications: List<CareNotification>,
    val sosEvents: List<SosEvent>,
    val videoCalls: List<VideoCallSession>,
    val pendingSyncOps: List<PendingSyncOpEntity>
)

class CareViewModel(
    private val repository: CareRepository,
    private val prefsDataStore: UserPrefsDataStore,
    private val aiRepository: AiWellnessRepository,
    private val reminderScheduler: ReminderScheduler,
    private val locationHelper: LocationHelper,
    private val fallDetectionMonitor: FallDetectionMonitor,
    private val currentUserId: String
) : ViewModel() {

    val localPrefs: StateFlow<LocalAppPreferences> = prefsDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPreferences())

    val fallTelemetry: StateFlow<FallTelemetryState> = fallDetectionMonitor.telemetry

    private val _fallCountdownSeconds = MutableStateFlow<Int?>(null)
    val fallCountdownSeconds: StateFlow<Int?> = _fallCountdownSeconds.asStateFlow()

    private val _pendingFallAlert = MutableStateFlow<FallDetectionAlert?>(null)
    val pendingFallAlert: StateFlow<FallDetectionAlert?> = _pendingFallAlert.asStateFlow()

    private var fallCountdownJob: Job? = null
    private var lastHandledFallEventId: String? = null

    private val _selectedElderId = MutableStateFlow<String?>(null)
    val selectedElderId: StateFlow<String?> = _selectedElderId.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _aiTipsState = MutableStateFlow<UiState<WellnessTipsResult>>(UiState.Loading)
    val aiTipsState: StateFlow<UiState<WellnessTipsResult>> = _aiTipsState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val coreBundle: Flow<CoreRecordsBundle> = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeElderProfiles(currentUserId),
        repository.observeCaretakerLinks(currentUserId),
        repository.observePairingInvites(currentUserId),
        repository.observeMedicines(currentUserId)
    ) { user, elders, links, invites, meds ->
        CoreRecordsBundle(user, elders, links, invites, meds)
    }

    private val dailyBundle: Flow<DailyCareBundle> = combine(
        repository.observeMedicineLogs(currentUserId),
        repository.observeWaterLogs(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeEmergencyContacts(currentUserId),
        repository.observeAppointments(currentUserId)
    ) { medLogs, water, tasks, contacts, appts ->
        DailyCareBundle(medLogs, water, tasks, contacts, appts)
    }

    private val alertsBundle: Flow<AlertsBundle> = combine(
        repository.observeNotifications(currentUserId),
        repository.observeSosEvents(currentUserId),
        repository.observeVideoCalls(currentUserId),
        repository.observePendingOps(currentUserId)
    ) { notifs, sos, calls, pending ->
        AlertsBundle(notifs, sos, calls, pending)
    }

    val dashboardState: StateFlow<UiState<CareDashboardData>> = combine(
        coreBundle,
        dailyBundle,
        alertsBundle
    ) { core, daily, alerts ->
        CareDashboardData(
            userProfile = core.userProfile,
            elderProfiles = core.elderProfiles,
            caretakerLinks = core.caretakerLinks,
            pairingInvites = core.pairingInvites,
            medicines = core.medicines.sortedBy { it.timeMinutes },
            medicineLogs = daily.medicineLogs,
            waterLogs = daily.waterLogs,
            tasks = daily.tasks,
            emergencyContacts = daily.emergencyContacts.sortedBy { it.priorityOrder },
            appointments = daily.appointments,
            notifications = alerts.notifications.sortedByDescending { it.createdAt?.seconds ?: 0L },
            sosEvents = alerts.sosEvents.sortedByDescending { it.createdAt?.seconds ?: 0L },
            videoCalls = alerts.videoCalls.sortedByDescending { it.createdAt?.seconds ?: 0L },
            pendingSyncOps = alerts.pendingSyncOps
        )
    }
        .map<CareDashboardData, UiState<CareDashboardData>> { data ->
            // Keep elder's care documents aligned if new caretaker linked
            val ownElder = data.elderProfiles.firstOrNull { it.elderId == currentUserId }
            if (ownElder != null && ownElder.authorizedUids.size > 1) {
                viewModelScope.launch {
                    repository.propagateAuthorizedUidsForElder(ownElder.elderId, ownElder.authorizedUids)
                }
            }
            UiState.Success(data)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Care Connect dashboard", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load Care Connect data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            localPrefs.collect { prefs ->
                if (prefs.fallDetectionEnabled && prefs.selectedRole == UserRole.ELDER) {
                    fallDetectionMonitor.startMonitoring(prefs.fallSensitivity)
                } else if (!prefs.fallDetectionEnabled) {
                    fallDetectionMonitor.stopMonitoring()
                } else {
                    fallDetectionMonitor.updateSensitivity(prefs.fallSensitivity)
                }
            }
        }
        viewModelScope.launch {
            fallDetectionMonitor.telemetry.collect { state ->
                val alert = state.lastDetectedEvent ?: return@collect
                if (alert.eventId == lastHandledFallEventId) return@collect
                lastHandledFallEventId = alert.eventId
                startFallSosCountdown(alert)
            }
        }
    }

    override fun onCleared() {
        fallDetectionMonitor.stopMonitoring()
        super.onCleared()
    }

    fun setNetworkOnline(online: Boolean) {
        val wasOffline = !_isOnline.value
        _isOnline.value = online
        if (online && wasOffline) {
            syncPendingChanges()
        }
    }

    fun selectElder(elderId: String) {
        _selectedElderId.value = elderId
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun todayDateKey(): String = repository.todayDateKey()

    fun ensureProfileInitialized(
        role: UserRole,
        displayNameOverride: String? = null,
        avatarKey: String = if (role == UserRole.ELDER) "grandpa" else "caretaker"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val prefs = localPrefs.value
            val result = repository.ensureUserAndInitialProfile(
                role = role,
                displayNameOverride = displayNameOverride,
                avatarKey = avatarKey,
                themePreset = prefs.themePreset.id,
                darkMode = prefs.darkMode,
                textScale = prefs.textScale.id
            )
            _isBusy.value = false
            result.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to initialize user profile."
            }
        }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            prefsDataStore.setSelectedRole(role)
            repository.ensureUserAndInitialProfile(role = role).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to switch role."
            }
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _feedbackMessage.value = "Display name cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.updateUserSettings(displayName = trimmed)
                .onSuccess { _feedbackMessage.value = "Profile name updated." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to update profile." }
        }
    }

    fun updateThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            prefsDataStore.setThemePreset(preset)
            repository.updateUserSettings(themePreset = preset.id)
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setDarkMode(enabled)
            repository.updateUserSettings(darkMode = enabled)
        }
    }

    fun updateTextScale(scale: TextScaleOption) {
        viewModelScope.launch {
            prefsDataStore.setTextScale(scale)
            repository.updateUserSettings(textScale = scale.id)
        }
    }

    fun updateRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setRemindersEnabled(enabled)
        }
    }

    fun generatePairingCode(elderName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.generatePairingInvite(elderName)
                .onSuccess { code -> _feedbackMessage.value = "Pairing code created: $code" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not create pairing code." }
            _isBusy.value = false
        }
    }

    fun revokePairingCode(code: String) {
        viewModelScope.launch {
            repository.revokePairingInvite(code)
                .onSuccess { _feedbackMessage.value = "Pairing code revoked." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not revoke code." }
        }
    }

    fun claimPairingCode(code: String, caretakerName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.claimPairingInvite(code, caretakerName)
                .onSuccess { elderName ->
                    _feedbackMessage.value = "Connected to $elderName!"
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Invalid or expired pairing code."
                }
            _isBusy.value = false
        }
    }

    fun createManagedElder(displayName: String, avatarKey: String, dailyWaterGoal: Int, ageLabel: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.createManagedElderProfile(displayName, avatarKey, dailyWaterGoal, ageLabel)
                .onSuccess { out ->
                    _selectedElderId.value = out.documentId
                    _feedbackMessage.value = if (out.queuedOffline) {
                        "Elder profile saved offline; will sync when online."
                    } else {
                        "Elder profile added."
                    }
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not create elder profile."
                }
            _isBusy.value = false
        }
    }

    fun revokeCaretakerLink(link: CaretakerLink) {
        viewModelScope.launch {
            repository.revokeCaretakerLink(link)
                .onSuccess { _feedbackMessage.value = "Caretaker access revoked for ${link.caretakerName}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not revoke link." }
        }
    }

    fun saveMedicine(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        dosage: String,
        timeMinutes: Int,
        timeLabel: String,
        repeatSchedule: String,
        prescriptionNote: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveMedicine(
                existingId, elderId, authorizedUids, name, dosage, timeMinutes, timeLabel, repeatSchedule, prescriptionNote
            ).onSuccess { out ->
                if (localPrefs.value.remindersEnabled) {
                    reminderScheduler.scheduleDailyMedicineReminder(
                        medicineId = out.documentId,
                        medicineName = name.trim(),
                        dosage = dosage.trim(),
                        timeMinutes = timeMinutes
                    )
                }
                _feedbackMessage.value = if (out.queuedOffline) {
                    "Medicine saved offline; queued to sync."
                } else {
                    "Medicine schedule saved."
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not save medicine."
            }
            _isBusy.value = false
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            repository.deleteMedicine(medicineId)
                .onSuccess { _feedbackMessage.value = "Medicine removed." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not remove medicine." }
        }
    }

    fun logMedicineStatus(medicine: MedicineSchedule, status: String) {
        viewModelScope.launch {
            repository.logMedicineStatus(medicine, status)
                .onSuccess { out ->
                    _feedbackMessage.value = if (out.queuedOffline) {
                        "Marked $status offline (sync queued)."
                    } else {
                        "${medicine.name} marked $status."
                    }
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not update medicine status."
                }
        }
    }

    fun updateWaterCount(elder: ElderProfile, newGlasses: Int, newGoal: Int = elder.dailyWaterGoal) {
        viewModelScope.launch {
            val safeGoal = newGoal.coerceIn(1, 20)
            if (safeGoal != elder.dailyWaterGoal) {
                repository.updateElderProfile(elderId = elder.elderId, dailyWaterGoal = safeGoal)
            }
            repository.updateWaterCount(elder, newGlasses, safeGoal)
                .onSuccess { out ->
                    if (out.queuedOffline) {
                        _feedbackMessage.value = "Water intake queued offline."
                    }
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to save water intake."
                }
        }
    }

    fun saveTask(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        title: String,
        dueDateText: String,
        dueTimeText: String
    ) {
        viewModelScope.launch {
            repository.saveTask(existingId, elderId, authorizedUids, title, dueDateText, dueTimeText)
                .onSuccess { out ->
                    _feedbackMessage.value = if (out.queuedOffline) "Task queued offline." else "Task saved."
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to save task."
                }
        }
    }

    fun toggleTaskCompletion(task: CareTask) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update task." }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
                .onSuccess { _feedbackMessage.value = "Task deleted." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete task." }
        }
    }

    fun saveEmergencyContact(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        phoneNumber: String,
        relationship: String,
        priorityOrder: Int
    ) {
        viewModelScope.launch {
            repository.saveEmergencyContact(
                existingId, elderId, authorizedUids, name, phoneNumber, relationship, priorityOrder
            ).onSuccess { out ->
                _feedbackMessage.value = if (out.queuedOffline) "Contact queued offline." else "Emergency contact saved."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not save contact."
            }
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contactId)
                .onSuccess { _feedbackMessage.value = "Emergency contact removed." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not remove contact." }
        }
    }

    fun saveAppointment(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        title: String,
        location: String,
        dateTimeText: String,
        reminderEnabled: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.saveAppointment(
                existingId, elderId, authorizedUids, title, location, dateTimeText, reminderEnabled, notes
            ).onSuccess { out ->
                if (reminderEnabled && localPrefs.value.remindersEnabled) {
                    reminderScheduler.showImmediateNotification(
                        id = out.documentId.hashCode(),
                        title = "Appointment Scheduled: $title",
                        body = "$dateTimeText at $location"
                    )
                }
                _feedbackMessage.value = if (out.queuedOffline) "Appointment queued offline." else "Appointment saved."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not save appointment."
            }
        }
    }

    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            repository.deleteAppointment(appointmentId)
                .onSuccess { _feedbackMessage.value = "Appointment deleted." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Could not delete appointment." }
        }
    }

    fun toggleLocationSharing(elder: ElderProfile, enable: Boolean) {
        viewModelScope.launch {
            if (enable && !locationHelper.hasLocationPermission()) {
                _feedbackMessage.value = "Location permission is required to enable sharing."
                return@launch
            }
            val locationResult = if (enable) locationHelper.fetchCurrentAreaIfPermitted() else null
            val area = if (enable) {
                locationResult?.areaLabel ?: "Location sharing enabled"
            } else {
                "Location sharing paused"
            }
            repository.updateElderProfile(
                elderId = elder.elderId,
                locationSharingEnabled = enable,
                lastKnownArea = area,
                updateLocationTimestamp = enable
            ).onSuccess {
                _feedbackMessage.value = if (enable) "Location sharing active ($area)." else "Location sharing paused."
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update location sharing."
            }
        }
    }

    fun refreshLocationNow(elder: ElderProfile) {
        viewModelScope.launch {
            if (!elder.locationSharingEnabled || !locationHelper.hasLocationPermission()) {
                _feedbackMessage.value = "Enable location sharing and grant permission first."
                return@launch
            }
            val res = locationHelper.fetchCurrentAreaIfPermitted()
            if (res != null) {
                repository.updateElderProfile(
                    elderId = elder.elderId,
                    lastKnownArea = res.areaLabel,
                    updateLocationTimestamp = true
                ).onSuccess {
                    _feedbackMessage.value = "Updated location: ${res.areaLabel}"
                }
            } else {
                _feedbackMessage.value = "Could not fetch location fix right now."
            }
        }
    }

    fun triggerSos(
        elder: ElderProfile,
        source: String = "manual",
        peakGForce: Double? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val loc = if (elder.locationSharingEnabled && locationHelper.hasLocationPermission()) {
                locationHelper.fetchCurrentAreaIfPermitted()
            } else null
            val area = loc?.areaLabel ?: if (elder.locationSharingEnabled) {
                elder.lastKnownArea ?: "Location unavailable"
            } else {
                "Location not shared"
            }
            repository.triggerSos(
                elder = elder,
                areaLabel = area,
                latitude = loc?.latitude,
                longitude = loc?.longitude,
                source = source,
                peakGForce = peakGForce
            ).onSuccess { out ->
                _feedbackMessage.value = if (out.queuedOffline) {
                    "Offline: SOS queued to sync as soon as network returns."
                } else if (source == "fall_detection" || source == "fall_simulation") {
                    "Fall SOS alert delivered to linked caretakers."
                } else {
                    "SOS alert delivered to linked caretakers."
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to send SOS alert."
            }
            _isBusy.value = false
        }
    }

    fun toggleFallDetection(elder: ElderProfile?, enable: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setFallDetectionEnabled(enable)
            if (enable) {
                fallDetectionMonitor.startMonitoring(localPrefs.value.fallSensitivity)
            } else {
                fallDetectionMonitor.stopMonitoring()
                cancelFallCountdown(silent = true)
            }
            if (elder != null) {
                repository.updateElderProfile(elderId = elder.elderId, fallDetectionEnabled = enable)
            }
            _feedbackMessage.value = if (enable) "Fall detection monitoring enabled." else "Fall detection paused."
        }
    }

    fun updateFallSensitivity(sensitivity: FallSensitivity) {
        viewModelScope.launch {
            prefsDataStore.setFallSensitivity(sensitivity)
            fallDetectionMonitor.updateSensitivity(sensitivity)
            _feedbackMessage.value = "Fall sensitivity set to ${sensitivity.label}."
        }
    }

    fun triggerFallSimulation() {
        fallDetectionMonitor.triggerSimulatedFall()
    }

    private fun startFallSosCountdown(alert: FallDetectionAlert) {
        fallCountdownJob?.cancel()
        _pendingFallAlert.value = alert
        reminderScheduler.showImmediateNotification(
            id = alert.eventId.hashCode(),
            title = if (alert.isSimulation) "Simulated Fall Detected" else "Fall Detected!",
            body = "Sending SOS alert in 15 seconds unless cancelled."
        )
        fallCountdownJob = viewModelScope.launch {
            for (sec in 15 downTo 1) {
                _fallCountdownSeconds.value = sec
                delay(1000L)
            }
            _fallCountdownSeconds.value = 0
            val targetElder = resolveActiveElderForFall()
            if (targetElder != null) {
                confirmFallSosNow(targetElder)
            } else {
                _pendingFallAlert.value = null
                _fallCountdownSeconds.value = null
                fallDetectionMonitor.clearDetectedEvent()
                _feedbackMessage.value = "Fall detected, but no elder profile was found to dispatch SOS."
            }
        }
    }

    fun cancelFallCountdown(silent: Boolean = false) {
        fallCountdownJob?.cancel()
        fallCountdownJob = null
        _fallCountdownSeconds.value = null
        _pendingFallAlert.value = null
        fallDetectionMonitor.clearDetectedEvent()
        if (!silent) {
            _feedbackMessage.value = "Fall SOS cancelled — glad you are okay!"
        }
    }

    fun confirmFallSosNow(elder: ElderProfile) {
        val alert = _pendingFallAlert.value
        fallCountdownJob?.cancel()
        fallCountdownJob = null
        _fallCountdownSeconds.value = null
        _pendingFallAlert.value = null
        fallDetectionMonitor.clearDetectedEvent()
        val source = if (alert?.isSimulation == true) "fall_simulation" else "fall_detection"
        triggerSos(
            elder = elder,
            source = source,
            peakGForce = alert?.peakGForce?.toDouble()
        )
    }

    private fun resolveActiveElderForFall(): ElderProfile? {
        val data = (dashboardState.value as? UiState.Success<CareDashboardData>)?.data ?: return null
        val selected = _selectedElderId.value
        return data.elderProfiles.firstOrNull { it.elderId == currentUserId }
            ?: data.elderProfiles.firstOrNull { it.elderId == selected }
            ?: data.elderProfiles.firstOrNull()
    }

    fun startVideoCall(
        elder: ElderProfile,
        callerName: String,
        callerRole: UserRole,
        onStarted: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.startVideoCall(elder, callerName, callerRole)
                .onSuccess { session ->
                    _feedbackMessage.value = "Calling ${elder.displayName}..."
                    onStarted(session.id)
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Unable to start video call."
                }
            _isBusy.value = false
        }
    }

    fun answerVideoCall(call: VideoCallSession) {
        viewModelScope.launch {
            repository.updateVideoCallState(callId = call.id, callStatus = "active")
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to answer call." }
        }
    }

    fun declineVideoCall(call: VideoCallSession) {
        viewModelScope.launch {
            repository.updateVideoCallState(callId = call.id, callStatus = "declined")
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to decline call." }
        }
    }

    fun endVideoCall(call: VideoCallSession) {
        viewModelScope.launch {
            repository.updateVideoCallState(callId = call.id, callStatus = "ended")
                .onSuccess { _feedbackMessage.value = "Video call ended." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to end call." }
        }
    }

    fun toggleCallCamera(call: VideoCallSession, enabled: Boolean) {
        val isCaller = call.callerUid == currentUserId
        viewModelScope.launch {
            repository.updateVideoCallState(
                callId = call.id,
                callerCameraEnabled = if (isCaller) enabled else null,
                calleeCameraEnabled = if (!isCaller) enabled else null
            )
        }
    }

    fun toggleCallMic(call: VideoCallSession, muted: Boolean) {
        val isCaller = call.callerUid == currentUserId
        viewModelScope.launch {
            repository.updateVideoCallState(
                callId = call.id,
                callerMicMuted = if (isCaller) muted else null,
                calleeMicMuted = if (!isCaller) muted else null
            )
        }
    }

    fun acknowledgeSos(eventId: String) {
        viewModelScope.launch {
            repository.acknowledgeSos(eventId)
                .onSuccess { _feedbackMessage.value = "SOS acknowledged." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to acknowledge SOS." }
        }
    }

    fun cancelSos(eventId: String) {
        viewModelScope.launch {
            repository.cancelSos(eventId)
                .onSuccess { _feedbackMessage.value = "SOS alert cancelled." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to cancel SOS." }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead(list: List<CareNotification>) {
        viewModelScope.launch {
            repository.markAllNotificationsRead(list)
        }
    }

    fun loadAiWellnessTips(elderName: String, waterProgressLabel: String, pendingTasks: Int) {
        viewModelScope.launch {
            _aiTipsState.value = UiState.Loading
            aiRepository.generateDailyWellnessTips(elderName, waterProgressLabel, pendingTasks)
                .onSuccess { result ->
                    _aiTipsState.value = UiState.Success(result)
                }
                .onFailure { err ->
                    _aiTipsState.value = UiState.Error(
                        err.localizedMessage ?: "Unable to load AI wellness tips right now."
                    )
                }
        }
    }

    fun syncPendingChanges() {
        viewModelScope.launch {
            repository.flushPendingOps()
                .onSuccess { count ->
                    if (count > 0) {
                        _feedbackMessage.value = "Synced $count queued offline update(s)."
                    }
                }
        }
    }

    private companion object {
        const val TAG = "CareViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
