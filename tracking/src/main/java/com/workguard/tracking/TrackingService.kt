package com.workguard.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.workguard.core.datastore.AuthDataStore
import com.workguard.core.datastore.isAccessTokenValid
import com.workguard.core.network.ApiService
import com.workguard.core.network.TrackingPingRequest
import com.workguard.core.util.BatteryStatusProvider
import com.workguard.core.util.Clock
import com.workguard.tracking.location.LocationTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.HttpException

@AndroidEntryPoint
class TrackingService : Service() {
    companion object {
        private const val TAG = "TrackingService"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 2001

        @Volatile
        private var isRunning = false
    }

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var authDataStore: AuthDataStore
    @Inject
    lateinit var clock: Clock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationTracker: LocationTracker

    override fun onCreate() {
        super.onCreate()
        if (isRunning) return
        isRunning = true

        startForeground(NOTIF_ID, buildNotification())

        locationTracker = LocationTracker(this) { location ->
            serviceScope.launch {
                sendTrackingPing(location)
            }
        }
        locationTracker.start()
        Log.d(TAG, "Tracking service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            locationTracker.stop()
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        Log.d(TAG, "Tracking service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun sendTrackingPing(location: Location) {
        if (!authDataStore.isAccessTokenValid(clock.nowMillis())) {
            Log.w(TAG, "Token invalid/expired, stop tracking")
            authDataStore.clear()
            stopSelf()
            return
        }
        val batteryStatus = BatteryStatusProvider.getStatus(this)
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val request = TrackingPingRequest(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = accuracy,
            isMockLocation = location.isFromMockProvider,
            batteryLevel = batteryStatus?.level,
            isCharging = batteryStatus?.isCharging
        )
        val validationError = validatePingRequest(request)
        if (validationError != null) {
            Log.w(TAG, "Tracking ping skipped: $validationError request=$request")
            return
        }
        try {
            val response = apiService.trackingPing(request)
            if (response.success == false) {
                Log.w(TAG, "Tracking ping failed: ${response.message} request=$request")
            } else {
                Log.d(TAG, "Tracking ping ok")
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            Log.w(
                TAG,
                "Tracking ping http error: code=${e.code()} body=$errorBody request=$request"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Tracking ping error: request=$request", e)
        }
    }

    private fun validatePingRequest(request: TrackingPingRequest): String? {
        if (request.latitude !in -90.0..90.0 || request.longitude !in -180.0..180.0) {
            return "Koordinat lokasi tidak valid"
        }
        val accuracy = request.accuracy
        if (accuracy == null || accuracy <= 0f) {
            return "Akurasi lokasi tidak valid"
        }
        if (request.isMockLocation == true) {
            return "Lokasi terdeteksi palsu"
        }
        val batteryLevel = request.batteryLevel
        if (batteryLevel == null || batteryLevel !in 0..100) {
            return "Level baterai tidak valid"
        }
        if (request.isCharging == null) {
            return "Status charging tidak valid"
        }
        return null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Tracking Lokasi",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WorkGuard aktif")
            .setContentText("Pelacakan lokasi berjalan")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
