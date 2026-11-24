package com.example

import com.example.model.ElderProfile
import com.example.model.FallSensitivity
import com.example.model.MedicineSchedule
import com.example.model.SosEvent
import com.example.model.UserRole
import com.example.model.VideoCallSession
import com.example.ui.navigation.HashRouterState
import com.example.ui.navigation.PrimaryHashRoute
import com.example.ui.navigation.resolveTargetPrimaryHash
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.careTokensFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
