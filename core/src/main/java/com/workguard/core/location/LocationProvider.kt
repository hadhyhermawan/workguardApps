package com.workguard.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val isMocked: Boolean,
    val provider: String?
)

interface LocationProvider {
    suspend fun getLastKnownLocation(): LocationSnapshot?
}

class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {
    override suspend fun getLastKnownLocation(): LocationSnapshot? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val bestLastKnown = getBestLastKnown(manager)
        if (bestLastKnown != null) {
            return@withContext bestLastKnown.toSnapshot()
        }
        val provider = selectProvider(manager)
        if (provider == null) {
            return@withContext null
        }
        requestSingleUpdate(manager, provider)?.toSnapshot()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFinePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getBestLastKnown(manager: LocationManager): Location? {
        val providers = manager.getProviders(true)
        val locations = providers.mapNotNull { provider ->
            try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            }
        }
        return locations.minByOrNull { it.accuracy }
    }

    private fun selectProvider(manager: LocationManager): String? {
        val hasFine = hasFinePermission()
        val gpsEnabled = hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return when {
            gpsEnabled -> LocationManager.GPS_PROVIDER
            networkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> manager.getProviders(true).firstOrNull()
        }
    }

    private suspend fun requestSingleUpdate(
        manager: LocationManager,
        provider: String
    ): Location? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestCurrentLocation(manager, provider)
        } else {
            requestLegacySingleUpdate(manager, provider)
        }
    }

    private suspend fun requestCurrentLocation(
        manager: LocationManager,
        provider: String
    ): Location? {
        return withTimeoutOrNull(8000L) {
            suspendCancellableCoroutine { cont ->
                val cancellationSignal = CancellationSignal()
                cont.invokeOnCancellation { cancellationSignal.cancel() }
                try {
                    manager.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        ContextCompat.getMainExecutor(context)
                    ) { location ->
                        if (cont.isActive) {
                            cont.resume(location) {}
                        }
                    }
                } catch (e: SecurityException) {
                    if (cont.isActive) {
                        cont.resume(null) {}
                    }
                } catch (e: Exception) {
                    if (cont.isActive) {
                        cont.resume(null) {}
                    }
                }
            }
        }
    }

    private suspend fun requestLegacySingleUpdate(
        manager: LocationManager,
        provider: String
    ): Location? {
        return withTimeoutOrNull(8000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) {
                            cont.resume(location) {}
                        }
                    }
                }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    manager.removeUpdates(listener)
                    if (cont.isActive) {
                        cont.resume(null) {}
                    }
                } catch (e: Exception) {
                    manager.removeUpdates(listener)
                    if (cont.isActive) {
                        cont.resume(null) {}
                    }
                }
            }
        }
    }
}

private fun Location.toSnapshot(): LocationSnapshot {
    return LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        isMocked = isFromMockProvider,
        provider = provider
    )
}
