package com.workguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.workguard.core.datastore.AuthDataStore
import com.workguard.core.datastore.isAccessTokenValid
import com.workguard.core.util.Clock
import com.workguard.core.ui.theme.WorkGuardTheme
import com.workguard.navigation.AppNavGraph
import com.workguard.navigation.Routes
import com.workguard.notification.NotificationRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_OPEN_INTERNET_CONNECTIVITY =
            "com.workguard.action.OPEN_INTERNET_CONNECTIVITY"
        const val ACTION_OPEN_BLUETOOTH_SETTINGS =
            "com.workguard.action.OPEN_BLUETOOTH_SETTINGS"
    }

    @Inject
    lateinit var authDataStore: AuthDataStore
    @Inject
    lateinit var clock: Clock

    private var pendingHomeRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WorkGuard)
        super.onCreate(savedInstanceState)

        if (handleConnectivityShortcut(intent)) {
            finish()
            return
        }

        val notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        val hasValidToken = authDataStore.isAccessTokenValid(clock.nowMillis())
        if (!hasValidToken) {
            authDataStore.clear()
        }
        pendingHomeRoute = NotificationRoute.routeFromIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            WorkGuardTheme {
                val securityState by rememberSecurityState()

                when (securityState) {
                    SecurityState.Loading -> {
                        SecurityLoadingScreen()
                    }
                    is SecurityState.Blocked -> {
                        SecurityBlockScreen()
                    }
                    SecurityState.Allowed -> {
                        var showSplash by remember { mutableStateOf(true) }
                        val startDestination = if (hasValidToken) {
                            Routes.HomeRoot
                        } else {
                            Routes.Auth
                        }
                        Box {
                            AppNavGraph(
                                startDestination = startDestination,
                                initialHomeRoute = pendingHomeRoute,
                                onHomeRouteConsumed = { pendingHomeRoute = null }
                            )
                            if (showSplash) {
                                SplashScreen(onFinished = { showSplash = false })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (handleConnectivityShortcut(intent)) {
            finish()
            return
        }
        pendingHomeRoute = NotificationRoute.routeFromIntent(intent)
    }

    private fun handleConnectivityShortcut(intent: android.content.Intent?): Boolean {
        return when (intent?.action) {
            ACTION_OPEN_INTERNET_CONNECTIVITY -> {
                openInternetConnectivitySettings()
                true
            }
            ACTION_OPEN_BLUETOOTH_SETTINGS -> {
                openBluetoothSettings()
                true
            }
            else -> false
        }
    }

    private fun openInternetConnectivitySettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        runCatching { startActivity(intent) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }

    private fun openBluetoothSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }
}
