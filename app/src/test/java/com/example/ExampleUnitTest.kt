package com.example

import com.example.model.ElderProfile
import com.example.model.MedicineSchedule
import com.example.model.SosEvent
import com.example.model.UserRole
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.careTokensFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val med = MedicineSchedule(id = "m1", elderId = "e1", name = "Vitamin D", dosage = "1 tab")
        assertEquals("active", med.activeStatus)

        val sos = SosEvent(id = "s1", elderId = "e1", elderName = "Grandpa")
        assertEquals("active", sos.status)
        assertEquals("delivered", sos.deliveryState)
    }
}
