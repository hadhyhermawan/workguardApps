package com.workguard

import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.workguard.core.security.DeviceSecurityChecker
import com.workguard.core.security.FakeGpsDetector
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume

sealed interface SecurityState {
    data object Loading : SecurityState
    data object Allowed : SecurityState
    data class Blocked(val message: String) : SecurityState
}

@Composable
fun rememberSecurityState(): State<SecurityState> {
    val context = LocalContext.current
    return produceState<SecurityState>(initialValue = SecurityState.Loading, context) {
        if (BuildConfig.DEBUG) {
            value = SecurityState.Allowed
            return@produceState
        }
        val reasons = mutableListOf<String>()

        val deviceCheck = DeviceSecurityChecker.check(context)
        if (deviceCheck.isViolation) {
            reasons.addAll(deviceCheck.reasons)
        }

        val fakeGpsCheck = FakeGpsDetector.check(context)
        if (fakeGpsCheck.isViolation) {
            reasons.addAll(fakeGpsCheck.reasons)
            Toast.makeText(
                context,
                "Anda menggunakan aplikasi terlarang",
                Toast.LENGTH_LONG
            ).show()
        }

        val integrityOk = PlayIntegrityChecker(context).isIntegrityOk()
        if (!integrityOk) {
            reasons.add("Play Integrity gagal")
        }

        value = if (reasons.isEmpty()) {
            SecurityState.Allowed
        } else {
            SecurityState.Blocked("Perangkat tidak memenuhi standar keamanan perusahaan.")
        }
    }
}

@Composable
fun SecurityLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7F8)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Memeriksa keamanan perangkat...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6D7A7E)
            )
        }
    }
}

@Composable
fun SecurityBlockScreen() {
    val accent = Color(0xFF16B3A8)
    val accentDark = Color(0xFF0E8C84)
    val background = Brush.verticalGradient(
        colors = listOf(Color(0xFFF4F7F8), Color(0xFFEAF3F2))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(accent, accentDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WG",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Perangkat tidak memenuhi standar keamanan perusahaan.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2A30),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Silakan hubungi admin atau gunakan perangkat lain.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D7A7E),
                textAlign = TextAlign.Center
            )
        }
    }
}

private class PlayIntegrityChecker(private val context: Context) {
    suspend fun isIntegrityOk(): Boolean {
        return try {
            val manager = IntegrityManagerFactory.create(context)
            val nonce = createNonce()
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
            val token = requestIntegrityToken(manager, request)
            token.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun createNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private suspend fun requestIntegrityToken(
        manager: IntegrityManager,
        request: IntegrityTokenRequest
    ): String = suspendCancellableCoroutine { continuation ->
        manager.requestIntegrityToken(request)
            .addOnSuccessListener { response ->
                continuation.resume(response.token())
            }
            .addOnFailureListener {
                continuation.resume("")
            }
    }
}
