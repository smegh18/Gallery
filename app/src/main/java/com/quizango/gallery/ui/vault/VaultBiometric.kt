package com.quizango.gallery.ui.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Fingerprint unlock for the Private Vault (reference uses androidx.biometric too). */
object VaultBiometric {

    /** Null when fingerprint unlock can be used, otherwise the message to show (reference wording). */
    fun unavailableReason(context: Context): String? =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprints enrolled."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "This device does not have a fingerprint sensor"
            else -> "Fingerprint hardware not available."
        }

    /**
     * Shows the system fingerprint prompt. Cancel or errors simply leave the PIN/pattern screen in place.
     * shortcut: title and button text are not in the reference strings ("Unlock your private vault." is); adjust if found.
     */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Private Vault")
            .setSubtitle("Unlock your private vault.")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
