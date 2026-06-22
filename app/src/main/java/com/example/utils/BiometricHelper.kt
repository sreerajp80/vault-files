package com.example.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val status = biometricManager.canAuthenticate(authenticators)
        return status == BiometricManager.BIOMETRIC_SUCCESS || 
               status == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Triggered if biometric is invalid, system handles visual retry state
            }
        }

        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return failure callback structure so that we can invoke PIN-fallback input dialog seamlessly
            onError("Biometric security interface unavailable. Fallback configuration enabled.")
        }
    }
}
