package com.anish.veda.guardian

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.anish.veda.data.model.GpsCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class GuardianLocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentLocation = MutableStateFlow<GpsCoordinates?>(null)
    val currentLocation: StateFlow<GpsCoordinates?> = _currentLocation.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(): GpsCoordinates? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            return@withContext null
        }

        // Try getting last known location first as quick baseline
        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val bestLast = when {
            lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
            lastGps != null -> lastGps
            else -> lastNet
        }

        if (bestLast != null && (System.currentTimeMillis() - bestLast.time) < 30_000) {
            val coords = GpsCoordinates(
                latitude = bestLast.latitude,
                longitude = bestLast.longitude,
                accuracyMeters = bestLast.accuracy,
                timestamp = bestLast.time
            )
            _currentLocation.value = coords
            return@withContext coords
        }

        // Request a fresh single location fix asynchronously
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    locationManager.removeUpdates(this)
                    val coords = GpsCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        timestamp = loc.time
                    )
                    _currentLocation.value = coords
                    if (continuation.isActive) {
                        continuation.resume(coords)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                } else {
                    if (continuation.isActive) continuation.resume(null)
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
