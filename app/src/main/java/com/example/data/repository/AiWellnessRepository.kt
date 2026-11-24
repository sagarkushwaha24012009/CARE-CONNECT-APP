package com.example.data.repository

import android.util.Log
import com.example.model.WellnessTipItem
import com.example.model.WellnessTipsResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiWellnessRepo"

class AiWellnessRepository {

    private val tipSchema = Schema.obj(
        properties = mapOf(
            "tips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short friendly wellness headline"),
                        "tip" to Schema.string(description = "1-2 clear, safe, gentle lifestyle sentences"),
                        "category" to Schema.enumeration(
                            values = listOf("Movement", "Hydration", "Rest", "Social"),
                            description = "Wellness category"
                        )
                    )
                ),
                description = "List of 3 general wellness tips"
            ),
            "disclaimer" to Schema.string(
                description = "Brief reminder that these are general lifestyle wellness tips and not medical advice"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tipSchema
            }
        )
    }

    suspend fun generateDailyWellnessTips(
        elderName: String,
        waterProgressLabel: String,
        pendingTasksCount: Int
    ): Result<WellnessTipsResult> {
        val prompt = """
            Generate 3 gentle, encouraging daily wellness tips for an older adult named "$elderName".
            Context: Hydration progress today is $waterProgressLabel, and $pendingTasksCount daily tasks remain.
            Strict safety rules:
            - Offer ONLY simple, everyday lifestyle encouragement (gentle stretching, sipping water, resting eyes, calling family, enjoying fresh air).
            - NEVER diagnose conditions, recommend medicines, mention supplements, or suggest dosages.
            - Keep each tip warm, respectful, and easy to read in 1-2 short sentences.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("AI model returned an empty response.")
            val root = JSONObject(jsonString)
            val arr = root.getJSONArray("tips")
            val tips = (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                WellnessTipItem(
                    title = item.getString("title"),
                    tip = item.getString("tip"),
                    category = item.optString("category", "Movement")
                )
            }
            val disclaimer = root.optString(
                "disclaimer",
                "General lifestyle wellness tips only. Always follow your doctor's advice."
            )
            Result.success(WellnessTipsResult(tips = tips, disclaimer = disclaimer))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI tips request timed out", e)
            Result.failure(Exception("Wellness tips request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating AI wellness tips", e)
            Result.failure(e)
        }
    }
}
