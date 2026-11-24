package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val wire: String, val displayLabel: String) {
    ELDER("elder", "Elder"),
    CARETAKER("caretaker", "Caretaker");

    companion object {
        fun fromWire(raw: String?): UserRole =
            entries.firstOrNull { it.wire == raw } ?: ELDER
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.ELDER.wire,
    val email: String = "",
    val avatarKey: String = "grandpa",
    val themePreset: String = "classic_pastel",
    val darkMode: Boolean = false,
    val textScale: String = "normal",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ElderProfile(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val displayName: String = "",
    val dailyWaterGoal: Int = 8,
    val locationSharingEnabled: Boolean = false,
    val onlineStatus: String = "online",
    val avatarKey: String = "grandpa",
    val lastKnownArea: String? = null,
    @ServerTimestamp val lastLocationAt: Timestamp? = null,
    val ageLabel: String? = null,
    val notes: String? = null,
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PairingInvite(
    @DocumentId val id: String = "",
    val code: String = "",
    val elderId: String = "",
    val elderName: String = "",
    val status: String = "active",
    val claimedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CaretakerLink(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val elderName: String = "",
    val caretakerId: String = "",
    val caretakerName: String = "",
    val inviteCode: String = "",
    val status: String = "active",
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicineSchedule(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val name: String = "",
    val dosage: String = "",
    val timeMinutes: Int = 480,
    val timeLabel: String = "8:00 AM",
    val repeatSchedule: String = "Daily",
    val activeStatus: String = "active",
    val prescriptionNote: String? = null,
    val createdByUid: String = "",
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicineLog(
    @DocumentId val id: String = "",
    val medicineId: String = "",
    val elderId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val dateKey: String = "",
    val status: String = "pending",
    val loggedByUid: String = "",
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WaterLog(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val dateKey: String = "",
    val glasses: Int = 0,
    val goal: Int = 8,
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CareTask(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val title: String = "",
    val dueDateText: String = "Today",
    val dueTimeText: String = "5:00 PM",
    val completionStatus: String = "pending",
    val assignedByUid: String = "",
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmergencyContact(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val priorityOrder: Int = 1,
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CareAppointment(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val title: String = "",
    val location: String = "",
    val dateTimeText: String = "",
    val reminderFlag: String = "enabled",
    val notes: String? = null,
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CareNotification(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val category: String = "system",
    val title: String = "",
    val body: String = "",
    val readStatus: String = "unread",
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SosEvent(
    @DocumentId val id: String = "",
    val elderId: String = "",
    val elderName: String = "",
    val status: String = "active",
    val areaLabel: String = "Location not shared",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deliveryState: String = "delivered",
    val acknowledgedByUid: String? = null,
    val authorizedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
data class WellnessTipItem(
    val title: String,
    val tip: String,
    val category: String
)

@Keep
data class WellnessTipsResult(
    val tips: List<WellnessTipItem>,
    val disclaimer: String
)
