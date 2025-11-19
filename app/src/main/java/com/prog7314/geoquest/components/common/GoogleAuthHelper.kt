package com.prog7314.geoquest.components.common

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.prog7314.geoquest.R
import com.prog7314.geoquest.data.model.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Utility object for handling Google Sign-In authentication
 */
object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    /**
     * Initiates Google Sign-In flow
     * @param context Android context
     * @param coroutineScope Coroutine scope for async operations
     * @param userViewModel ViewModel to handle sign-in
     * @param useNonce Whether to include a nonce in the request (for registration)
     */
    /**
     * Check if Google Play Services is available
     */
    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        val isAvailable = resultCode == ConnectionResult.SUCCESS
        if (!isAvailable) {
            Log.w(TAG, "Google Play Services not available. Result code: $resultCode")
        }
        return isAvailable
    }

    /**
     * Check if Google accounts are available on the device
     */
    private fun hasGoogleAccounts(context: Context): Boolean {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            Log.d(TAG, "Found ${accounts.size} Google account(s)")
            if (accounts.isNotEmpty()) {
                accounts.forEach { account ->
                    Log.d(TAG, "Google account: ${account.name}")
                }
            }
            accounts.isNotEmpty()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when checking for Google accounts", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for Google accounts", e)
            false
        }
    }

    fun signInWithGoogle(
        context: Context,
        coroutineScope: CoroutineScope,
        userViewModel: UserViewModel,
        useNonce: Boolean = false
    ) {
        coroutineScope.launch {
            try {
                // Check if Google Play Services is available
                if (!isGooglePlayServicesAvailable(context)) {
                    Log.w(TAG, "Google Play Services not available")
                    Toast.makeText(
                        context,
                        "Google Play Services is required for Google Sign-In. Please ensure it's installed and up to date.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Check if Google accounts are available first
                if (!hasGoogleAccounts(context)) {
                    Log.w(TAG, "No Google accounts found on device")
                    Toast.makeText(
                        context,
                        "No Google account found. Please add a Google account in Settings > Accounts.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val credentialManager = CredentialManager.create(context)
                val serverClientId = context.getString(R.string.default_web_client_id)
                Log.d(TAG, "Attempting Google Sign-In with server client ID: $serverClientId")
                
                val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)

                if (useNonce) {
                    googleIdOptionBuilder.setNonce(UUID.randomUUID().toString())
                }

                val googleIdOption = googleIdOptionBuilder.build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )

                val credential = result.credential
                when (credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                Log.d(TAG, "Google Sign-In successful, processing token...")
                                userViewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error creating GoogleIdTokenCredential", e)
                                Toast.makeText(
                                    context,
                                    "Failed to process Google credentials: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(TAG, "Unexpected credential type: ${credential.type}")
                            Toast.makeText(
                                context,
                                "Unexpected credential type received",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    else -> {
                        Log.e(TAG, "Credential is not CustomCredential: ${credential::class.java.simpleName}")
                        Toast.makeText(
                            context,
                            "Unexpected credential format: ${credential::class.java.simpleName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled Google Sign-In")
                Toast.makeText(
                    context,
                    "Sign-in cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: NoCredentialException) {
                Log.e(TAG, "No Google accounts found", e)
                // Show a more helpful message and prevent toast spam
                try {
                    Toast.makeText(
                        context,
                        "No Google account found. Please add a Google account in Settings, or use email/password login.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (toastException: Exception) {
                    // Ignore toast errors to prevent spam
                    Log.w(TAG, "Could not show toast", toastException)
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException occurred", e)
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                Toast.makeText(
                    context,
                    "An unexpected error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

