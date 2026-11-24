package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.FallSensitivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class FallTelemetryState(
    val sensorAvailable: Boolean = false,
    val monitoringActive: Boolean = false,
    val currentGForce: Float = 1.0f,
    val peakGForceObserved: Float = 1.0f,
    val simulationModeActive: Boolean = false,
    val lastDetectedEvent: FallDetectionAlert? = null
)

data class FallDetectionAlert(
    val eventId: String,
    val peakGForce: Float,
    val isSimulation: Boolean,
    val detectedAtMillis: Long
)

/**
 * Monitors real device accelerometer telemetry via [SensorManager] using a multi-phase
 * free-fall + impact + stillness algorithm. Also provides an explicit Safety Simulation
 * trigger for safe verification on emulators or without dropping a physical device.
 */
class FallDetectionMonitor(context: Context) {

    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _telemetry = MutableStateFlow(
        FallTelemetryState(sensorAvailable = accelerometer != null)
    )
    val telemetry: StateFlow<FallTelemetryState> = _telemetry.asStateFlow()

    private var sensitivity: FallSensitivity = FallSensitivity.MEDIUM
    private var freeFallSeenAtMillis: Long = 0L
    private var impactPeakG: Float = 0f
    private var impactSeenAtMillis: Long = 0L
    private var lastTriggerAtMillis: Long = 0L
    private var isRegistered = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            val gForce = magnitude / SensorManager.GRAVITY_EARTH
            processAccelerationSample(gForce, System.currentTimeMillis())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun startMonitoring(sensitivity: FallSensitivity) {
        this.sensitivity = sensitivity
        if (accelerometer == null || sensorManager == null) {
            _telemetry.value = _telemetry.value.copy(
                sensorAvailable = false,
                monitoringActive = false
            )
            return
        }
        if (!isRegistered) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            isRegistered = true
        }
        _telemetry.value = _telemetry.value.copy(
            sensorAvailable = true,
            monitoringActive = true
        )
    }

    fun updateSensitivity(sensitivity: FallSensitivity) {
        this.sensitivity = sensitivity
    }

    fun stopMonitoring() {
        if (isRegistered) {
            sensorManager?.unregisterListener(listener)
            isRegistered = false
        }
        resetDetectorWindow()
        _telemetry.value = _telemetry.value.copy(monitoringActive = false)
    }

    internal fun processAccelerationSample(gForce: Float, nowMillis: Long) {
        val currentPeak = maxOf(_telemetry.value.peakGForceObserved, gForce)
        _telemetry.value = _telemetry.value.copy(
            currentGForce = gForce,
            peakGForceObserved = currentPeak
        )

        // Cooldown of 10 seconds between automatic triggers
        if (nowMillis - lastTriggerAtMillis < COOLDOWN_MILLIS) return

        // Phase 1: Free-fall dip (< 0.45g)
        if (gForce < FREE_FALL_THRESHOLD_G) {
            freeFallSeenAtMillis = nowMillis
            return
        }

        // Phase 2: High-impact spike within 900ms after free-fall (or very strong impact >= threshold + 0.6g)
        val freeFallRecent = freeFallSeenAtMillis > 0L && (nowMillis - freeFallSeenAtMillis) <= FREE_FALL_TO_IMPACT_WINDOW_MS
        if (gForce >= sensitivity.impactThresholdG && (freeFallRecent || gForce >= sensitivity.impactThresholdG + 0.65f)) {
            if (gForce > impactPeakG) {
                impactPeakG = gForce
            }
            if (impactSeenAtMillis == 0L) {
                impactSeenAtMillis = nowMillis
            }
            return
        }

        // Phase 3: Post-impact stillness (~1.1s after impact, motion settles near 1g)
        if (impactSeenAtMillis > 0L) {
            val elapsedSinceImpact = nowMillis - impactSeenAtMillis
            if (elapsedSinceImpact in POST_IMPACT_SETTLE_MIN_MS..POST_IMPACT_SETTLE_MAX_MS && gForce in 0.65f..1.45f) {
                val peak = impactPeakG.coerceAtLeast(sensitivity.impactThresholdG)
                lastTriggerAtMillis = nowMillis
                resetDetectorWindow()
                _telemetry.value = _telemetry.value.copy(
                    simulationModeActive = false,
                    lastDetectedEvent = FallDetectionAlert(
                        eventId = "fall_${nowMillis}",
                        peakGForce = peak,
                        isSimulation = false,
                        detectedAtMillis = nowMillis
                    )
                )
            } else if (elapsedSinceImpact > POST_IMPACT_SETTLE_MAX_MS) {
                resetDetectorWindow()
            }
        }
    }

    /**
     * Explicit Safety Simulation trigger so users/caregivers can test the end-to-end
     * fall -> countdown -> auto-SOS workflow safely without dropping a real device.
     */
    fun triggerSimulatedFall(simulatedPeakG: Float = 3.15f) {
        val now = System.currentTimeMillis()
        _telemetry.value = _telemetry.value.copy(
            simulationModeActive = true,
            currentGForce = simulatedPeakG,
            peakGForceObserved = maxOf(_telemetry.value.peakGForceObserved, simulatedPeakG),
            lastDetectedEvent = FallDetectionAlert(
                eventId = "sim_fall_$now",
                peakGForce = simulatedPeakG,
                isSimulation = true,
                detectedAtMillis = now
            )
        )
    }

    fun clearDetectedEvent() {
        _telemetry.value = _telemetry.value.copy(
            simulationModeActive = false,
            lastDetectedEvent = null
        )
    }

    private fun resetDetectorWindow() {
        freeFallSeenAtMillis = 0L
        impactPeakG = 0f
        impactSeenAtMillis = 0L
    }

    private companion object {
        const val FREE_FALL_THRESHOLD_G = 0.45f
        const val FREE_FALL_TO_IMPACT_WINDOW_MS = 900L
        const val POST_IMPACT_SETTLE_MIN_MS = 900L
        const val POST_IMPACT_SETTLE_MAX_MS = 2200L
        const val COOLDOWN_MILLIS = 10_000L
    }
}
