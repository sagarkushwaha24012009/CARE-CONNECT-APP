package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CachedRecordEntity
import com.example.data.local.CareDao
import com.example.data.local.CareDatabase
import com.example.data.local.PendingSyncOpEntity
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.CareAppointment
import com.example.model.CareNotification
import com.example.model.CareTask
import com.example.model.CaretakerLink
import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.MedicineLog
import com.example.model.MedicineSchedule
import com.example.model.PairingInvite
import com.example.model.SosEvent
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.VideoCallSession
import com.example.model.WaterLog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WriteOutcome(
    val documentId: String,
    val queuedOffline: Boolean = false
)

class CareRepository(
    private val db: FirebaseFirestore,
    private val careDao: CareDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        careDao = CareDatabase.getInstance(context.applicationContext).careDao()
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun newDocId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    fun observePendingOps(uid: String): Flow<List<PendingSyncOpEntity>> =
        careDao.observePendingOps(uid)

    private inline fun <reified T> observeAuthorizedCollection(
        collection: String,
        uid: String
    ): Flow<List<T>> = flow {
        val current = auth.currentUser?.uid
        if (uid.isBlank() || current != uid) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(collection)
                .whereArrayContains("authorizedUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        T::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, collection)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val current = auth.currentUser?.uid
        if (uid.isBlank() || current != uid) {
            emit(null)
            return@flow
        }
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.GET, "users/$uid")
                    }
                    throw error
                }
        )
    }

    fun observeElderProfiles(uid: String): Flow<List<ElderProfile>> =
        observeAuthorizedCollection("elder_profiles", uid)

    fun observeCaretakerLinks(uid: String): Flow<List<CaretakerLink>> =
        observeAuthorizedCollection("caretaker_links", uid)

    fun observeMedicines(uid: String): Flow<List<MedicineSchedule>> =
        observeAuthorizedCollection("medicines", uid)

    fun observeMedicineLogs(uid: String): Flow<List<MedicineLog>> =
        observeAuthorizedCollection("medicine_logs", uid)

    fun observeWaterLogs(uid: String): Flow<List<WaterLog>> =
        observeAuthorizedCollection("water_logs", uid)

    fun observeTasks(uid: String): Flow<List<CareTask>> =
        observeAuthorizedCollection("tasks", uid)

    fun observeEmergencyContacts(uid: String): Flow<List<EmergencyContact>> =
        observeAuthorizedCollection("emergency_contacts", uid)

    fun observeAppointments(uid: String): Flow<List<CareAppointment>> =
        observeAuthorizedCollection("appointments", uid)

    fun observeNotifications(uid: String): Flow<List<CareNotification>> =
        observeAuthorizedCollection("notifications", uid)

    fun observeSosEvents(uid: String): Flow<List<SosEvent>> =
        observeAuthorizedCollection("sos_events", uid)

    fun observeVideoCalls(uid: String): Flow<List<VideoCallSession>> =
        observeAuthorizedCollection("video_calls", uid)

    fun observePairingInvites(elderUid: String): Flow<List<PairingInvite>> = flow {
        val current = auth.currentUser?.uid
        if (elderUid.isBlank() || current != elderUid) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("pairing_invites")
                .whereEqualTo("elderId", elderUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PairingInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == elderUid) {
                        handleFirestoreError(ex, OperationType.LIST, "pairing_invites")
                    }
                    throw error
                }
        )
    }

    suspend fun cacheSnapshotSummary(uid: String, key: String, summaryJson: String) {
        withContext(Dispatchers.IO) {
            careDao.upsertCachedRecord(
                CachedRecordEntity(
                    cacheKey = "${uid}_$key",
                    ownerUid = uid,
                    collectionName = key,
                    payloadJson = summaryJson,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getCachedSummary(uid: String, key: String): String? = withContext(Dispatchers.IO) {
        careDao.getCachedRecord("${uid}_$key")?.payloadJson
    }

    suspend fun ensureUserAndInitialProfile(
        role: UserRole,
        displayNameOverride: String? = null,
        avatarKey: String = if (role == UserRole.ELDER) "grandpa" else "caretaker",
        themePreset: String = "classic_pastel",
        darkMode: Boolean = false,
        textScale: String = "normal"
    ): Result<UserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid"
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val firebaseUser = auth.currentUser
            val resolvedName = (displayNameOverride?.takeIf { it.isNotBlank() }
                ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                ?: if (role == UserRole.ELDER) "Family Elder" else "Family Caretaker")
                .take(60)
            val resolvedEmail = (firebaseUser?.email ?: "").take(120)

            if (!userDoc.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to resolvedName,
                    "role" to role.wire,
                    "email" to resolvedEmail,
                    "avatarKey" to avatarKey,
                    "themePreset" to themePreset,
                    "darkMode" to darkMode,
                    "textScale" to textScale,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                db.collection("users").document(uid).set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "role" to role.wire,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!displayNameOverride.isNullOrBlank()) {
                    updates["displayName"] = displayNameOverride.take(60)
                }
                db.collection("users").document(uid).update(updates).await()
            }

            if (role == UserRole.ELDER) {
                val elderDoc = db.collection("elder_profiles").document(uid).get().await()
                if (!elderDoc.exists()) {
                    val elderPayload = mapOf(
                        "elderId" to uid,
                        "displayName" to resolvedName,
                        "dailyWaterGoal" to 8,
                        "locationSharingEnabled" to false,
                        "onlineStatus" to "online",
                        "avatarKey" to if (avatarKey == "grandma") "grandma" else "grandpa",
                        "authorizedUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("elder_profiles").document(uid).set(elderPayload).await()
                }
            }

            val updatedSnap = db.collection("users").document(uid).get().await()
            val profile = updatedSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(
                userId = uid,
                displayName = resolvedName,
                role = role.wire,
                email = resolvedEmail,
                avatarKey = avatarKey,
                themePreset = themePreset,
                darkMode = darkMode,
                textScale = textScale
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(
        displayName: String? = null,
        role: UserRole? = null,
        avatarKey: String? = null,
        themePreset: String? = null,
        darkMode: Boolean? = null,
        textScale: String? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!displayName.isNullOrBlank()) updates["displayName"] = displayName.trim().take(60)
            if (role != null) updates["role"] = role.wire
            if (!avatarKey.isNullOrBlank()) updates["avatarKey"] = avatarKey
            if (!themePreset.isNullOrBlank()) updates["themePreset"] = themePreset
            if (darkMode != null) updates["darkMode"] = darkMode
            if (!textScale.isNullOrBlank()) updates["textScale"] = textScale

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createManagedElderProfile(
        displayName: String,
        avatarKey: String,
        dailyWaterGoal: Int = 8,
        ageLabel: String? = null
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 60) {
            return Result.failure(IllegalArgumentException("Name must be between 1 and 60 characters."))
        }
        val safeGoal = dailyWaterGoal.coerceIn(1, 20)
        val elderId = newDocId("managed_${uid.take(12)}")
        val payload = mapOf(
            "elderId" to elderId,
            "displayName" to trimmedName,
            "dailyWaterGoal" to safeGoal,
            "locationSharingEnabled" to false,
            "onlineStatus" to "online",
            "avatarKey" to if (avatarKey == "grandma") "grandma" else "grandpa",
            "ageLabel" to ageLabel?.trim()?.take(20)?.takeIf { it.isNotEmpty() },
            "authorizedUids" to listOf(uid, elderId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        return executeSetWithOfflineQueue(
            uid = uid,
            collection = "elder_profiles",
            documentId = elderId,
            payload = payload
        )
    }

    suspend fun updateElderProfile(
        elderId: String,
        displayName: String? = null,
        dailyWaterGoal: Int? = null,
        locationSharingEnabled: Boolean? = null,
        lastKnownArea: String? = null,
        updateLocationTimestamp: Boolean = false,
        fallDetectionEnabled: Boolean? = null
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (elderId.isBlank()) return Result.failure(IllegalArgumentException("Elder ID is required."))
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!displayName.isNullOrBlank()) updates["displayName"] = displayName.trim().take(60)
        if (dailyWaterGoal != null) updates["dailyWaterGoal"] = dailyWaterGoal.coerceIn(1, 20)
        if (locationSharingEnabled != null) updates["locationSharingEnabled"] = locationSharingEnabled
        if (!lastKnownArea.isNullOrBlank()) updates["lastKnownArea"] = lastKnownArea.trim().take(120)
        if (updateLocationTimestamp) updates["lastLocationAt"] = FieldValue.serverTimestamp()
        if (fallDetectionEnabled != null) updates["fallDetectionEnabled"] = fallDetectionEnabled

        return executeUpdateWithOfflineQueue(
            uid = uid,
            collection = "elder_profiles",
            documentId = elderId,
            updates = updates
        )
    }

    suspend fun generatePairingInvite(elderName: String): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val payload = mapOf(
            "code" to code,
            "elderId" to uid,
            "elderName" to elderName.trim().ifEmpty { "Family Elder" }.take(60),
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("pairing_invites").document(code).set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "pairing_invites/$code")
            Result.failure(e)
        }
    }

    suspend fun revokePairingInvite(code: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("pairing_invites").document(code).update(
                mapOf(
                    "status" to "revoked",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "pairing_invites/$code")
            Result.failure(e)
        }
    }

    suspend fun claimPairingInvite(rawCode: String, caretakerName: String): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = rawCode.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Z0-9]{6}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-character pairing code."))
        }
        return try {
            val inviteSnap = db.collection("pairing_invites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalStateException("Pairing code not found."))
            }
            val invite = inviteSnap.toObject(PairingInvite::class.java)
                ?: return Result.failure(IllegalStateException("Invalid pairing code record."))
            if (invite.status != "active") {
                return Result.failure(IllegalStateException("This pairing code has already been ${invite.status}."))
            }
            if (invite.elderId == uid) {
                return Result.failure(IllegalStateException("You cannot link to your own elder profile as a caretaker."))
            }

            val elderRef = db.collection("elder_profiles").document(invite.elderId)
            val elderSnap = elderRef.get().await()
            val existingAuthorized = (elderSnap.get("authorizedUids") as? List<*>)
                ?.filterIsInstance<String>()
                ?: listOf(invite.elderId)
            val updatedAuthorized = (existingAuthorized + uid).distinct().take(10)

            val linkId = "${invite.elderId}_$uid"
            val batch = db.batch()
            batch.set(
                db.collection("caretaker_links").document(linkId),
                mapOf(
                    "elderId" to invite.elderId,
                    "elderName" to invite.elderName,
                    "caretakerId" to uid,
                    "caretakerName" to caretakerName.trim().ifEmpty { "Caretaker" }.take(60),
                    "inviteCode" to code,
                    "status" to "active",
                    "authorizedUids" to listOf(invite.elderId, uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("pairing_invites").document(code),
                mapOf(
                    "status" to "claimed",
                    "claimedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                elderRef,
                mapOf(
                    "authorizedUids" to updatedAuthorized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(invite.elderName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "pairing_invites/$code")
            Result.failure(e)
        }
    }

    suspend fun propagateAuthorizedUidsForElder(elderId: String, authorizedUids: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        if (uid != elderId || authorizedUids.isEmpty()) return
        val collections = listOf(
            "medicines",
            "medicine_logs",
            "water_logs",
            "tasks",
            "emergency_contacts",
            "appointments",
            "notifications",
            "sos_events",
            "video_calls"
        )
        for (col in collections) {
            try {
                val snap = db.collection(col)
                    .whereArrayContains("authorizedUids", elderId)
                    .get()
                    .await()
                val batch = db.batch()
                var hasWrites = false
                for (doc in snap.documents) {
                    val currentUids = (doc.get("authorizedUids") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    if (currentUids.toSet() != authorizedUids.toSet()) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "authorizedUids" to authorizedUids.take(10),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        hasWrites = true
                    }
                }
                if (hasWrites) batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CareRepo", "Failed propagating authorizedUids to $col", e)
            }
        }
    }

    suspend fun revokeCaretakerLink(link: CaretakerLink): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("caretaker_links").document(link.id),
                mapOf(
                    "status" to "revoked",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val elderSnap = db.collection("elder_profiles").document(link.elderId).get().await()
            val currentUids = (elderSnap.get("authorizedUids") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            val remaining = currentUids.filter { it != link.caretakerId }
                .ifEmpty { listOf(link.elderId) }
            batch.update(
                db.collection("elder_profiles").document(link.elderId),
                mapOf(
                    "authorizedUids" to remaining,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            if (uid == link.elderId) {
                propagateAuthorizedUidsForElder(link.elderId, remaining)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "caretaker_links/${link.id}")
            Result.failure(e)
        }
    }

    suspend fun saveMedicine(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        dosage: String,
        timeMinutes: Int,
        timeLabel: String,
        repeatSchedule: String,
        prescriptionNote: String?
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = name.trim()
        val cleanDosage = dosage.trim()
        if (cleanName.isEmpty() || cleanName.length > 80) {
            return Result.failure(IllegalArgumentException("Medicine name must be 1–80 characters."))
        }
        if (cleanDosage.isEmpty() || cleanDosage.length > 60) {
            return Result.failure(IllegalArgumentException("Prescription dosage must be 1–60 characters."))
        }
        val safeAuth = (authorizedUids + elderId + uid).distinct().take(10)

        return if (existingId.isNullOrBlank()) {
            val docId = newDocId("med")
            val payload = mapOf(
                "elderId" to elderId,
                "name" to cleanName,
                "dosage" to cleanDosage,
                "timeMinutes" to timeMinutes.coerceIn(0, 1439),
                "timeLabel" to timeLabel.trim().take(20),
                "repeatSchedule" to repeatSchedule.trim().take(40),
                "activeStatus" to "active",
                "prescriptionNote" to prescriptionNote?.trim()?.take(160)?.takeIf { it.isNotEmpty() },
                "createdByUid" to uid,
                "authorizedUids" to safeAuth,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            executeSetWithOfflineQueue(uid, "medicines", docId, payload)
        } else {
            val updates = mapOf(
                "name" to cleanName,
                "dosage" to cleanDosage,
                "timeMinutes" to timeMinutes.coerceIn(0, 1439),
                "timeLabel" to timeLabel.trim().take(20),
                "repeatSchedule" to repeatSchedule.trim().take(40),
                "prescriptionNote" to prescriptionNote?.trim()?.take(160)?.takeIf { it.isNotEmpty() },
                "authorizedUids" to safeAuth,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            executeUpdateWithOfflineQueue(uid, "medicines", existingId, updates)
        }
    }

    suspend fun deleteMedicine(medicineId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("medicines").document(medicineId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "medicines/$medicineId")
            Result.failure(e)
        }
    }

    suspend fun logMedicineStatus(
        medicine: MedicineSchedule,
        status: String
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (status !in listOf("pending", "taken", "skipped")) {
            return Result.failure(IllegalArgumentException("Invalid status: $status"))
        }
        val dateKey = todayDateKey()
        val logId = "${medicine.id}_$dateKey".replace("-", "_")
        val safeAuth = (medicine.authorizedUids + medicine.elderId + uid).distinct().take(10)

        return try {
            val docRef = db.collection("medicine_logs").document(logId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mapOf(
                    "status" to status,
                    "loggedByUid" to uid,
                    "medicineName" to medicine.name.take(80),
                    "dosage" to medicine.dosage.take(60),
                    "authorizedUids" to safeAuth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "medicineId" to medicine.id,
                    "elderId" to medicine.elderId,
                    "medicineName" to medicine.name.take(80),
                    "dosage" to medicine.dosage.take(60),
                    "dateKey" to dateKey,
                    "status" to status,
                    "loggedByUid" to uid,
                    "authorizedUids" to safeAuth,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            createNotification(
                elderId = medicine.elderId,
                authorizedUids = safeAuth,
                category = "medicine",
                title = "Medicine $status: ${medicine.name}",
                body = "${medicine.dosage} marked $status for ${medicine.timeLabel}."
            )
            Result.success(WriteOutcome(logId, queuedOffline = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isOfflineException(e)) {
                val payload = mapOf(
                    "medicineId" to medicine.id,
                    "elderId" to medicine.elderId,
                    "medicineName" to medicine.name.take(80),
                    "dosage" to medicine.dosage.take(60),
                    "dateKey" to dateKey,
                    "status" to status,
                    "loggedByUid" to uid,
                    "authorizedUids" to safeAuth
                )
                queueOfflineOp(uid, "medicine_logs", logId, "SET", payload)
                Result.success(WriteOutcome(logId, queuedOffline = true))
            } else {
                handleFirestoreError(e, OperationType.WRITE, "medicine_logs/$logId")
                Result.failure(e)
            }
        }
    }

    suspend fun updateWaterCount(
        elder: ElderProfile,
        newGlasses: Int,
        newGoal: Int = elder.dailyWaterGoal
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val dateKey = todayDateKey()
        val logId = "${elder.elderId}_$dateKey".replace("-", "_")
        val clampedGlasses = newGlasses.coerceIn(0, 30)
        val clampedGoal = newGoal.coerceIn(1, 20)
        val safeAuth = (elder.authorizedUids + elder.elderId + uid).distinct().take(10)

        return try {
            val docRef = db.collection("water_logs").document(logId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "glasses" to clampedGlasses,
                        "goal" to clampedGoal,
                        "authorizedUids" to safeAuth,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "elderId" to elder.elderId,
                        "dateKey" to dateKey,
                        "glasses" to clampedGlasses,
                        "goal" to clampedGoal,
                        "authorizedUids" to safeAuth,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(WriteOutcome(logId, queuedOffline = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isOfflineException(e)) {
                queueOfflineOp(
                    uid = uid,
                    collection = "water_logs",
                    documentId = logId,
                    opType = "SET",
                    payload = mapOf(
                        "elderId" to elder.elderId,
                        "dateKey" to dateKey,
                        "glasses" to clampedGlasses,
                        "goal" to clampedGoal,
                        "authorizedUids" to safeAuth
                    )
                )
                Result.success(WriteOutcome(logId, queuedOffline = true))
            } else {
                handleFirestoreError(e, OperationType.WRITE, "water_logs/$logId")
                Result.failure(e)
            }
        }
    }

    suspend fun saveTask(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        title: String,
        dueDateText: String,
        dueTimeText: String
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 100) {
            return Result.failure(IllegalArgumentException("Task title must be 1–100 characters."))
        }
        val safeAuth = (authorizedUids + elderId + uid).distinct().take(10)
        return if (existingId.isNullOrBlank()) {
            val docId = newDocId("task")
            executeSetWithOfflineQueue(
                uid = uid,
                collection = "tasks",
                documentId = docId,
                payload = mapOf(
                    "elderId" to elderId,
                    "title" to cleanTitle,
                    "dueDateText" to dueDateText.trim().ifEmpty { "Today" }.take(30),
                    "dueTimeText" to dueTimeText.trim().ifEmpty { "5:00 PM" }.take(20),
                    "completionStatus" to "pending",
                    "assignedByUid" to uid,
                    "authorizedUids" to safeAuth,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            executeUpdateWithOfflineQueue(
                uid = uid,
                collection = "tasks",
                documentId = existingId,
                updates = mapOf(
                    "title" to cleanTitle,
                    "dueDateText" to dueDateText.trim().ifEmpty { "Today" }.take(30),
                    "dueTimeText" to dueTimeText.trim().ifEmpty { "5:00 PM" }.take(20),
                    "authorizedUids" to safeAuth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    suspend fun toggleTaskCompletion(task: CareTask): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val nextStatus = if (task.completionStatus == "completed") "pending" else "completed"
        val res = executeUpdateWithOfflineQueue(
            uid = uid,
            collection = "tasks",
            documentId = task.id,
            updates = mapOf(
                "completionStatus" to nextStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (res.isSuccess && nextStatus == "completed") {
            createNotification(
                elderId = task.elderId,
                authorizedUids = task.authorizedUids,
                category = "task",
                title = "Task Completed",
                body = "${task.title} was marked completed."
            )
        }
        return res
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    suspend fun saveEmergencyContact(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        name: String,
        phoneNumber: String,
        relationship: String,
        priorityOrder: Int
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanName = name.trim()
        val cleanPhone = phoneNumber.trim()
        val cleanRel = relationship.trim()
        if (cleanName.isEmpty() || cleanName.length > 60) {
            return Result.failure(IllegalArgumentException("Contact name must be 1–60 characters."))
        }
        if (cleanPhone.length < 3 || cleanPhone.length > 25) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number (3–25 characters)."))
        }
        if (cleanRel.isEmpty() || cleanRel.length > 40) {
            return Result.failure(IllegalArgumentException("Relationship label is required."))
        }
        val safeAuth = (authorizedUids + elderId + uid).distinct().take(10)
        val safeOrder = priorityOrder.coerceIn(1, 20)

        return if (existingId.isNullOrBlank()) {
            val docId = newDocId("ec")
            executeSetWithOfflineQueue(
                uid = uid,
                collection = "emergency_contacts",
                documentId = docId,
                payload = mapOf(
                    "elderId" to elderId,
                    "name" to cleanName,
                    "phoneNumber" to cleanPhone,
                    "relationship" to cleanRel,
                    "priorityOrder" to safeOrder,
                    "authorizedUids" to safeAuth,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            executeUpdateWithOfflineQueue(
                uid = uid,
                collection = "emergency_contacts",
                documentId = existingId,
                updates = mapOf(
                    "name" to cleanName,
                    "phoneNumber" to cleanPhone,
                    "relationship" to cleanRel,
                    "priorityOrder" to safeOrder,
                    "authorizedUids" to safeAuth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }

    suspend fun deleteEmergencyContact(contactId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("emergency_contacts").document(contactId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "emergency_contacts/$contactId")
            Result.failure(e)
        }
    }

    suspend fun saveAppointment(
        existingId: String?,
        elderId: String,
        authorizedUids: List<String>,
        title: String,
        location: String,
        dateTimeText: String,
        reminderEnabled: Boolean,
        notes: String?
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim()
        val cleanLocation = location.trim()
        val cleanDate = dateTimeText.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 100) {
            return Result.failure(IllegalArgumentException("Appointment title is required."))
        }
        if (cleanLocation.isEmpty() || cleanLocation.length > 120) {
            return Result.failure(IllegalArgumentException("Location is required."))
        }
        if (cleanDate.isEmpty() || cleanDate.length > 50) {
            return Result.failure(IllegalArgumentException("Date & time text is required."))
        }
        val safeAuth = (authorizedUids + elderId + uid).distinct().take(10)
        val reminderFlag = if (reminderEnabled) "enabled" else "disabled"

        return if (existingId.isNullOrBlank()) {
            val docId = newDocId("appt")
            val payload = mapOf(
                "elderId" to elderId,
                "title" to cleanTitle,
                "location" to cleanLocation,
                "dateTimeText" to cleanDate,
                "reminderFlag" to reminderFlag,
                "notes" to notes?.trim()?.take(200)?.takeIf { it.isNotEmpty() },
                "authorizedUids" to safeAuth,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            executeSetWithOfflineQueue(uid, "appointments", docId, payload)
        } else {
            val updates = mapOf(
                "title" to cleanTitle,
                "location" to cleanLocation,
                "dateTimeText" to cleanDate,
                "reminderFlag" to reminderFlag,
                "notes" to notes?.trim()?.take(200)?.takeIf { it.isNotEmpty() },
                "authorizedUids" to safeAuth,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            executeUpdateWithOfflineQueue(uid, "appointments", existingId, updates)
        }
    }

    suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("appointments").document(appointmentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "appointments/$appointmentId")
            Result.failure(e)
        }
    }

    suspend fun triggerSos(
        elder: ElderProfile,
        areaLabel: String,
        latitude: Double?,
        longitude: Double?,
        source: String = "manual",
        peakGForce: Double? = null
    ): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val eventId = newDocId("sos")
        val safeAuth = (elder.authorizedUids + elder.elderId + uid).distinct().take(10)
        val safeSource = when (source) {
            "fall_detection", "fall_simulation" -> source
            else -> "manual"
        }
        val payload = mapOf(
            "elderId" to elder.elderId,
            "elderName" to elder.displayName.take(60),
            "status" to "active",
            "areaLabel" to areaLabel.trim().ifEmpty { "Location not shared" }.take(120),
            "latitude" to latitude,
            "longitude" to longitude,
            "deliveryState" to "delivered",
            "source" to safeSource,
            "peakGForce" to peakGForce?.coerceIn(0.0, 50.0),
            "authorizedUids" to safeAuth,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        val res = executeSetWithOfflineQueue(uid, "sos_events", eventId, payload)
        if (res.isSuccess) {
            val title = when (safeSource) {
                "fall_detection" -> "Fall Detected SOS: ${elder.displayName}"
                "fall_simulation" -> "Simulated Fall SOS: ${elder.displayName}"
                else -> "SOS Alert: ${elder.displayName}"
            }
            val impactSuffix = peakGForce?.let { " • Impact ${String.format(Locale.US, "%.1f", it)}G" }.orEmpty()
            createNotification(
                elderId = elder.elderId,
                authorizedUids = safeAuth,
                category = "sos",
                title = title,
                body = "Emergency alert triggered (${areaLabel.take(60)})$impactSuffix."
            )
        }
        return res
    }

    suspend fun startVideoCall(
        elder: ElderProfile,
        callerName: String,
        callerRole: UserRole
    ): Result<VideoCallSession> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val callId = newDocId("call")
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val roomCode = (1..10).map { chars.random() }.joinToString("")
        val safeCallerName = callerName.trim().ifEmpty { callerRole.displayLabel }.take(60)
        val safeAuth = (elder.authorizedUids + elder.elderId + uid).distinct().take(10)

        val payload = mapOf<String, Any>(
            "elderId" to elder.elderId,
            "elderName" to elder.displayName.take(60),
            "callerUid" to uid,
            "callerName" to safeCallerName,
            "callerRole" to callerRole.wire,
            "roomCode" to roomCode,
            "callStatus" to "ringing",
            "callerCameraEnabled" to true,
            "calleeCameraEnabled" to true,
            "callerMicMuted" to false,
            "calleeMicMuted" to false,
            "authorizedUids" to safeAuth,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.collection("video_calls").document(callId).set(payload).await()
            createNotification(
                elderId = elder.elderId,
                authorizedUids = safeAuth,
                category = "call",
                title = "Video Call: $safeCallerName",
                body = "$safeCallerName started a secure Care Connect video call (${elder.displayName})."
            )
            Result.success(
                VideoCallSession(
                    id = callId,
                    elderId = elder.elderId,
                    elderName = elder.displayName.take(60),
                    callerUid = uid,
                    callerName = safeCallerName,
                    callerRole = callerRole.wire,
                    roomCode = roomCode,
                    callStatus = "ringing",
                    callerCameraEnabled = true,
                    calleeCameraEnabled = true,
                    callerMicMuted = false,
                    calleeMicMuted = false,
                    authorizedUids = safeAuth
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "video_calls/$callId")
            Result.failure(e)
        }
    }

    suspend fun updateVideoCallState(
        callId: String,
        callStatus: String? = null,
        callerCameraEnabled: Boolean? = null,
        calleeCameraEnabled: Boolean? = null,
        callerMicMuted: Boolean? = null,
        calleeMicMuted: Boolean? = null
    ): Result<Unit> {
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (callStatus != null) updates["callStatus"] = callStatus
            if (callerCameraEnabled != null) updates["callerCameraEnabled"] = callerCameraEnabled
            if (calleeCameraEnabled != null) updates["calleeCameraEnabled"] = calleeCameraEnabled
            if (callerMicMuted != null) updates["callerMicMuted"] = callerMicMuted
            if (calleeMicMuted != null) updates["calleeMicMuted"] = calleeMicMuted

            db.collection("video_calls").document(callId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "video_calls/$callId")
            Result.failure(e)
        }
    }

    suspend fun acknowledgeSos(eventId: String): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return executeUpdateWithOfflineQueue(
            uid = uid,
            collection = "sos_events",
            documentId = eventId,
            updates = mapOf(
                "status" to "acknowledged",
                "deliveryState" to "acknowledged",
                "acknowledgedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    suspend fun cancelSos(eventId: String): Result<WriteOutcome> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return executeUpdateWithOfflineQueue(
            uid = uid,
            collection = "sos_events",
            documentId = eventId,
            updates = mapOf(
                "status" to "cancelled",
                "deliveryState" to "cancelled",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return executeUpdateWithOfflineQueue(
            uid = uid,
            collection = "notifications",
            documentId = notificationId,
            updates = mapOf(
                "readStatus" to "read",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).map { }
    }

    suspend fun markAllNotificationsRead(notifications: List<CareNotification>): Result<Unit> {
        return try {
            requireUserId()
            val unread = notifications.filter { it.readStatus == "unread" && it.id.isNotBlank() }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.forEach { n ->
                batch.update(
                    db.collection("notifications").document(n.id),
                    mapOf(
                        "readStatus" to "read",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    private suspend fun createNotification(
        elderId: String,
        authorizedUids: List<String>,
        category: String,
        title: String,
        body: String
    ) {
        try {
            val uid = auth.currentUser?.uid ?: return
            val docId = newDocId("notif")
            val safeAuth = (authorizedUids + elderId + uid).distinct().take(10)
            db.collection("notifications").document(docId).set(
                mapOf(
                    "elderId" to elderId,
                    "category" to category,
                    "title" to title.take(80),
                    "body" to body.take(240),
                    "readStatus" to "unread",
                    "authorizedUids" to safeAuth,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CareRepo", "Notification insert skipped", e)
        }
    }

    suspend fun flushPendingOps(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.success(0)
        val pending = withContext(Dispatchers.IO) { careDao.getPendingOps(uid) }
        if (pending.isEmpty()) return Result.success(0)
        var syncedCount = 0
        for (op in pending) {
            try {
                val map = jsonToFirestoreMap(op.payloadJson, op.opType == "SET")
                val ref = db.collection(op.collectionName).document(op.documentId)
                if (op.opType == "SET") {
                    ref.set(map).await()
                } else {
                    ref.update(map).await()
                }
                withContext(Dispatchers.IO) { careDao.deletePendingOp(op.opId) }
                syncedCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "${op.collectionName}/${op.documentId}")
                if (isOfflineException(e)) {
                    return Result.success(syncedCount)
                }
            }
        }
        return Result.success(syncedCount)
    }

    private suspend fun executeSetWithOfflineQueue(
        uid: String,
        collection: String,
        documentId: String,
        payload: Map<String, Any>
    ): Result<WriteOutcome> {
        return try {
            db.collection(collection).document(documentId).set(payload).await()
            Result.success(WriteOutcome(documentId, queuedOffline = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isOfflineException(e)) {
                queueOfflineOp(uid, collection, documentId, "SET", payload)
                Result.success(WriteOutcome(documentId, queuedOffline = true))
            } else {
                handleFirestoreError(e, OperationType.CREATE, "$collection/$documentId")
                Result.failure(e)
            }
        }
    }

    private suspend fun executeUpdateWithOfflineQueue(
        uid: String,
        collection: String,
        documentId: String,
        updates: Map<String, Any>
    ): Result<WriteOutcome> {
        return try {
            db.collection(collection).document(documentId).update(updates).await()
            Result.success(WriteOutcome(documentId, queuedOffline = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isOfflineException(e)) {
                queueOfflineOp(uid, collection, documentId, "UPDATE", updates)
                Result.success(WriteOutcome(documentId, queuedOffline = true))
            } else {
                handleFirestoreError(e, OperationType.UPDATE, "$collection/$documentId")
                Result.failure(e)
            }
        }
    }

    private suspend fun queueOfflineOp(
        uid: String,
        collection: String,
        documentId: String,
        opType: String,
        payload: Map<String, Any>
    ) {
        val json = firestoreMapToJson(payload)
        withContext(Dispatchers.IO) {
            careDao.insertPendingOp(
                PendingSyncOpEntity(
                    ownerUid = uid,
                    collectionName = collection,
                    documentId = documentId,
                    opType = opType,
                    payloadJson = json,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private fun isOfflineException(e: Exception): Boolean {
        val fsEx = generateSequence(e as Throwable?) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return fsEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            fsEx?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
            e.message?.contains("offline", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true
    }

    private fun firestoreMapToJson(map: Map<String, Any>): String {
        val json = JSONObject()
        for ((k, v) in map) {
            if (v is FieldValue) continue
            when (v) {
                is List<*> -> json.put(k, JSONArray(v))
                else -> json.put(k, v)
            }
        }
        return json.toString()
    }

    private fun jsonToFirestoreMap(rawJson: String, isCreate: Boolean): Map<String, Any> {
        val obj = JSONObject(rawJson)
        val out = mutableMapOf<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.get(k)
            if (v is JSONArray) {
                val list = (0 until v.length()).map { v.get(it).toString() }
                out[k] = list
            } else if (v != JSONObject.NULL) {
                out[k] = v
            }
        }
        if (isCreate) {
            out["createdAt"] = FieldValue.serverTimestamp()
        }
        out["updatedAt"] = FieldValue.serverTimestamp()
        return out
    }
}
