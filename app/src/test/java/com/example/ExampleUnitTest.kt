package com.example

import com.example.model.ElderProfile
import com.example.model.EmergencyContact
import com.example.model.FallSensitivity
import com.example.model.MedicineSchedule
import com.example.model.PairingInvite
import com.example.model.SosEvent
import com.example.model.UserRole
import com.example.model.VideoCallSession
import com.example.notifications.EmergencySmsHelper
import com.example.ui.navigation.HashRouterState
import com.example.ui.navigation.PrimaryHashRoute
import com.example.ui.navigation.resolveTargetPrimaryHash
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.careTokensFor
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ExampleUnitTest {

    @Test
    fun theme_presets_and_text_scales_resolve_cleanly() {
        assertEquals(ThemePreset.WARM_CREAM, ThemePreset.fromId("warm_cream"))
        assertEquals(ThemePreset.HIGH_CONTRAST, ThemePreset.fromId("high_contrast"))
        assertEquals(ThemePreset.CLASSIC_PASTEL, ThemePreset.fromId("invalid"))

        assertEquals(TextScaleOption.LARGE, TextScaleOption.fromId("large"))
        assertEquals(TextScaleOption.EXTRA_LARGE, TextScaleOption.fromId("extra_large"))
        assertEquals(1.30f, TextScaleOption.EXTRA_LARGE.multiplier, 0.001f)

        assertFalse(careTokensFor(ThemePreset.HIGH_CONTRAST, darkMode = false).isDark)
        assertTrue(careTokensFor(ThemePreset.WARM_CREAM, darkMode = true).isDark)
    }

    @Test
    fun user_roles_and_care_model_defaults_are_valid() {
        assertEquals(UserRole.CARETAKER, UserRole.fromWire("caretaker"))
        assertEquals(UserRole.ELDER, UserRole.fromWire("elder"))
        assertEquals(UserRole.ELDER, UserRole.fromWire(null))

        val elder = ElderProfile(elderId = "e1", displayName = "Grandpa")
        assertEquals(8, elder.dailyWaterGoal)
        assertFalse(elder.locationSharingEnabled)
        assertTrue(elder.fallDetectionEnabled)

        val med = MedicineSchedule(id = "m1", elderId = "e1", name = "Vitamin D", dosage = "1 tab")
        assertEquals("active", med.activeStatus)

        val sos = SosEvent(id = "s1", elderId = "e1", elderName = "Grandpa")
        assertEquals("active", sos.status)
        assertEquals("delivered", sos.deliveryState)
        assertEquals("manual", sos.source)
        assertNull(sos.peakGForce)
    }

    @Test
    fun fall_sensitivity_and_video_call_defaults_resolve_cleanly() {
        assertEquals(FallSensitivity.LOW, FallSensitivity.fromId("low"))
        assertEquals(FallSensitivity.HIGH, FallSensitivity.fromId("high"))
        assertEquals(FallSensitivity.MEDIUM, FallSensitivity.fromId("unknown"))
        assertTrue(FallSensitivity.HIGH.impactThresholdG < FallSensitivity.LOW.impactThresholdG)

        val call = VideoCallSession(
            id = "call1",
            elderId = "e1",
            elderName = "Grandpa",
            callerUid = "u1",
            callerName = "Maya",
            roomCode = "AB12CD34"
        )
        assertEquals("ringing", call.callStatus)
        assertTrue(call.callerCameraEnabled)
        assertTrue(call.calleeCameraEnabled)
        assertFalse(call.callerMicMuted)
        assertFalse(call.calleeMicMuted)
    }

    @Test
    fun hash_router_manages_primary_screen_transitions() {
        assertEquals(PrimaryHashRoute.SPLASH, PrimaryHashRoute.fromHash("splash"))
        assertEquals(PrimaryHashRoute.AUTH, PrimaryHashRoute.fromHash("#auth"))
        assertEquals(PrimaryHashRoute.ELDER_DASHBOARD, PrimaryHashRoute.fromHash("#/elder/dashboard"))
        assertEquals(PrimaryHashRoute.CARETAKER_DASHBOARD, PrimaryHashRoute.fromHash("/caretaker/dashboard"))
        assertNull(PrimaryHashRoute.fromHash("#/nonexistent"))

        assertEquals(
            PrimaryHashRoute.SPLASH,
            resolveTargetPrimaryHash(isAuthResolved = false, isAuthenticated = false, onboardingCompleted = true, activeRole = UserRole.ELDER)
        )
        assertEquals(
            PrimaryHashRoute.AUTH,
            resolveTargetPrimaryHash(isAuthResolved = true, isAuthenticated = false, onboardingCompleted = true, activeRole = UserRole.ELDER)
        )
        assertEquals(
            PrimaryHashRoute.ELDER_DASHBOARD,
            resolveTargetPrimaryHash(isAuthResolved = true, isAuthenticated = true, onboardingCompleted = true, activeRole = UserRole.ELDER)
        )
        assertEquals(
            PrimaryHashRoute.CARETAKER_DASHBOARD,
            resolveTargetPrimaryHash(isAuthResolved = true, isAuthenticated = true, onboardingCompleted = true, activeRole = UserRole.CARETAKER)
        )

        val router = HashRouterState(PrimaryHashRoute.SPLASH)
        assertEquals("#/splash", router.currentHash.value)

        router.navigate(PrimaryHashRoute.AUTH, replace = true)
        assertEquals("#/auth", router.currentHash.value)
        assertFalse(router.canGoBack)

        router.navigateToHash("#/caretaker/dashboard")
        assertEquals(PrimaryHashRoute.CARETAKER_DASHBOARD, router.currentRoute.value)
        assertTrue(router.canGoBack)

        router.navigate(PrimaryHashRoute.ELDER_DASHBOARD, replace = true)
        assertEquals("#/elder/dashboard", router.currentHash.value)

        val popped = router.popBack()
        assertEquals(PrimaryHashRoute.AUTH, popped)
        assertEquals("#/auth", router.currentHash.value)
    }

    @Test
    fun pairing_invite_enforces_daily_rotation_and_expiration() {
        val now = 1_700_000_000_000L
        val validInvite = PairingInvite(
            code = "ABC234",
            elderId = "e1",
            elderName = "Grandpa",
            status = "active",
            dayKey = "2026-11-24",
            expiresAt = Timestamp(Date(now + 3_600_000L))
        )
        assertFalse(validInvite.hasExpiredAt(now))
        assertTrue(validInvite.usableToday("2026-11-24", now))
        assertFalse(validInvite.usableToday("2026-11-25", now))
        assertFalse(validInvite.copy(status = "claimed").usableToday("2026-11-24", now))

        val expiredInvite = validInvite.copy(expiresAt = Timestamp(Date(now - 1000L)))
        assertTrue(expiredInvite.hasExpiredAt(now))
        assertFalse(expiredInvite.usableToday("2026-11-24", now))

        val legacyInviteWithoutExpiry = validInvite.copy(expiresAt = null)
        assertTrue(legacyInviteWithoutExpiry.hasExpiredAt(now))
        assertFalse(legacyInviteWithoutExpiry.usableToday("2026-11-24", now))
    }

    @Test
    fun emergency_sms_helper_formats_recipients_and_sos_message() {
        val contacts = listOf(
            EmergencyContact(id = "c2", elderId = "e1", name = "Doctor", phoneNumber = "+1 (555) 222-3333", priorityOrder = 2),
            EmergencyContact(id = "c1", elderId = "e1", name = "Daughter", phoneNumber = "+1-555-111-0000", priorityOrder = 1),
            EmergencyContact(id = "c3", elderId = "e1", name = "Duplicate", phoneNumber = "+1 555 111 0000", priorityOrder = 3)
        )
        assertEquals("+15551110000;+15552223333", EmergencySmsHelper.buildRecipientsString(contacts))

        val body = EmergencySmsHelper.buildSosSmsBody(
            elderName = "Grandpa Arthur",
            areaLabel = "Near Oak Street",
            source = "fall_detection",
            peakGForce = 3.25,
            latitude = 37.422,
            longitude = -122.084
        )
        assertTrue(body.contains("Grandpa Arthur"))
        assertTrue(body.contains("Fall detected (3.2G impact)") || body.contains("Fall detected (3.3G impact)"))
        assertTrue(body.contains("Near Oak Street"))
        assertTrue(body.contains("https://maps.google.com/?q=37.42200,-122.08400"))
    }
}
