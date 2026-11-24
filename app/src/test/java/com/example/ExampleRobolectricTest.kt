package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedRecordEntity
import com.example.data.local.CareDatabase
import com.example.data.local.PendingSyncOpEntity
import com.example.model.UserRole
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.careTokensFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("Robolectric android-all SDK JARs cannot be downloaded in offline sandbox")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app metadata and safety resources are present`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Care Connect", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.medicine_safety_notice).isNotBlank())
        assertTrue(context.getString(R.string.ai_wellness_disclaimer).isNotBlank())
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `theme presets and text scales resolve cleanly in light and dark modes`() {
        assertEquals(ThemePreset.WARM_CREAM, ThemePreset.fromId("warm_cream"))
        assertEquals(ThemePreset.HIGH_CONTRAST, ThemePreset.fromId("high_contrast"))
        assertEquals(ThemePreset.CLASSIC_PASTEL, ThemePreset.fromId("unknown"))

        assertEquals(TextScaleOption.EXTRA_LARGE, TextScaleOption.fromId("extra_large"))
        assertEquals(1.30f, TextScaleOption.EXTRA_LARGE.multiplier, 0.001f)

        val lightHighContrast = careTokensFor(ThemePreset.HIGH_CONTRAST, darkMode = false)
        val darkWarmCream = careTokensFor(ThemePreset.WARM_CREAM, darkMode = true)
        assertFalse(lightHighContrast.isDark)
        assertTrue(darkWarmCream.isDark)
    }

    @Test
    fun `user roles parse wire values safely`() {
        assertEquals(UserRole.CARETAKER, UserRole.fromWire("caretaker"))
        assertEquals(UserRole.ELDER, UserRole.fromWire("elder"))
        assertEquals(UserRole.ELDER, UserRole.fromWire(null))
    }

    @Test
    fun `room cache and offline sync queue persist and flush records`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, CareDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.careDao()
            dao.upsertCachedRecord(
                CachedRecordEntity(
                    cacheKey = "uid1:water_logs",
                    ownerUid = "uid1",
                    collectionName = "water_logs",
                    payloadJson = "[]",
                    updatedAtMillis = 1000L
                )
            )
            val cached = dao.getCachedRecord("uid1:water_logs")
            assertNotNull(cached)
            assertEquals("water_logs", cached?.collectionName)

            dao.insertPendingOp(
                PendingSyncOpEntity(
                    ownerUid = "uid1",
                    collectionName = "water_logs",
                    documentId = "uid1_2026-11-24",
                    opType = "SET",
                    payloadJson = "{\"glasses\":3}",
                    createdAtMillis = 2000L
                )
            )
            val pending = dao.getPendingOps("uid1")
            assertEquals(1, pending.size)
            dao.deletePendingOp(pending.first().opId)
            assertTrue(dao.getPendingOps("uid1").isEmpty())
        } finally {
            db.close()
        }
    }
}
