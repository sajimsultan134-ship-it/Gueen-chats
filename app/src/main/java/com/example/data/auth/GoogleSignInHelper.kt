package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleSignInAccount(
    val idToken: String?,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

class GoogleSignInHelper(private val context: Context) {
    private val TAG = "GoogleSignInHelper"
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(
        activityContext: Context,
        webClientId: String? = null
    ): Result<GoogleSignInAccount> {
        val clientId = webClientId?.takeIf { it.isNotBlank() }
            ?: runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: "123456789012-samplewebclientid.apps.googleusercontent.com"

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val account = GoogleSignInAccount(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
                Log.i(TAG, "Google Sign-In successful for email: ${account.email}")
                Result.success(account)
            } else {
                val errorMsg = "Unsupported credential type: ${credential::class.java.simpleName}"
                Log.w(TAG, errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In")
            Result.failure(Exception("Google Sign-In was cancelled.", e))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device: ${e.message}")
            Result.failure(Exception("No Google accounts found on this device or Google Play Services is unavailable.", e))
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error: ${e.message}")
            Result.failure(Exception(e.message ?: "Google Sign-In failed", e))
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in signInWithGoogle: ${e.message}")
            Result.failure(e)
        }
    }
}
