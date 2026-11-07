/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/LockAndPrivacy.kt                 ║
║ • LockScreen: PIN pad (+ fingerprint if on). The PIN is never      ║
║   stored as-is: we save SHA-256(salt + PIN). "salt" = random text  ║
║   so two people with the same PIN get different stored values.    ║
║ • PrivacyScreen: what Health Connect shows when you tap "privacy   ║
║   policy" — explains exactly what Gym Cat reads and writes.        ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.gymcat.app.R
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.theme.GymColors
import java.security.MessageDigest
import java.security.SecureRandom

fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

fun pinHash(pin: String, salt: String): String =
    MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

@Composable
fun LockScreen(activity: FragmentActivity, vm: MainViewModel, onUnlocked: () -> Unit) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val title = stringResource(R.string.unlock_title)
    val cancel = stringResource(R.string.use_pin)

    fun biometric() {
        val can = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setNegativeButtonText(cancel)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK).build())
    }
    LaunchedEffect(Unit) { if (s.biometric) biometric() }

    // Opaque background + swallow taps so nothing underneath can be seen or pressed.
    Column(Modifier.fillMaxSize().background(c.bg)
        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
        .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CatArt(com.gymcat.app.ui.components.Cat.THINKING, "🐱", 96.dp)
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
        Spacer(Modifier.height(12.dp))
        Text("● ".repeat(pin.length).ifBlank { "—" }, fontSize = 24.sp, color = c.text)
        if (wrong) Muted(stringResource(R.string.wrong_pin))
        Spacer(Modifier.height(12.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(6.dp)) {
                row.forEach { k ->
                    FilledTonalButton(onClick = {
                        when (k) {
                            "⌫" -> pin = pin.dropLast(1)
                            "✓" -> if (pinHash(pin, s.pinSalt) == s.pinHash) onUnlocked() else { wrong = true; pin = "" }
                            else -> if (pin.length < 8) { pin += k; wrong = false }
                        }
                    }, modifier = Modifier.size(72.dp)) { Text(k, fontSize = 22.sp) }
                }
            }
        }
        if (s.biometric) TextButton(onClick = { biometric() }) { Text("👆 " + stringResource(R.string.fingerprint)) }
    }
}

@Composable
fun PrivacyScreen(onClose: () -> Unit) {
    val c = GymColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
        Text(stringResource(R.string.privacy_body), color = c.text)
        TextButton(onClick = onClose) { Text(stringResource(R.string.ok)) }
    }
}
