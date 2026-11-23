package com.quizango.gallery.ui.vault

import android.content.Context
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.quizango.gallery.R

/** Fingerprint unlock for the Private Vault (reference uses androidx.biometric too). */
object VaultBiometric {

    /** Null when fingerprint unlock can be used, otherwise the string to show (the androidx English the reference merges in). */
    @StringRes
    fun unavailableReason(context: Context): Int? =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> androidx.biometric.R.string.fingerprint_error_no_fingerprints
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> androidx.biometric.R.string.fingerprint_error_hw_not_present
            else -> androidx.biometric.R.string.fingerprint_error_hw_not_available
        }

    /** Shows the system fingerprint prompt. Cancel or errors simply leave the PIN/pattern screen in place. */
    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.private_vault)) // A4
            .setSubtitle(activity.getString(R.string.unlock_vault))
            .setNegativeButtonText(activity.getString(R.string.cancel)) // L11
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
