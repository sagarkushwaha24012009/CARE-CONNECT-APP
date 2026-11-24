package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class CoarseLocationResult(
    val areaLabel: String,
    val latitude: Double?,
    val longitude: Double?
)

class LocationHelper(private val context: Context) {
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentAreaIfPermitted(): CoarseLocationResult? {
        if (!hasLocationPermission()) return null
        return try {
            val tokenSource = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).await() ?: fusedClient.lastLocation.await()

            if (location != null) {
                val area = resolveAreaName(location.latitude, location.longitude)
                CoarseLocationResult(
                    areaLabel = area,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } else {
                CoarseLocationResult(
                    areaLabel = "Permission granted (waiting for GPS/network fix)",
                    latitude = null,
                    longitude = null
                )
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "Unable to obtain location", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveAreaName(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                val first = addresses?.firstOrNull()
                formatAddress(first?.subLocality, first?.locality, first?.adminArea, lat, lng)
            } else {
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                val first = addresses?.firstOrNull()
                formatAddress(first?.subLocality, first?.locality, first?.adminArea, lat, lng)
            }
        } catch (_: Exception) {
            String.format(Locale.US, "Approx. %.3f, %.3f", lat, lng)
        }
    }

    private fun formatAddress(
        subLocality: String?,
        locality: String?,
        adminArea: String?,
        lat: Double,
        lng: Double
    ): String {
        val parts = listOfNotNull(subLocality, locality, adminArea).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) {
            "Near ${parts.take(2).joinToString(", ")}"
        } else {
            String.format(Locale.US, "Approx. %.3f, %.3f", lat, lng)
        }
    }
}
