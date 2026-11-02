package stufforg.capstone.frontend.core.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthHelper {

    suspend fun requestGoogleIdToken(
        context: Context,
        webClientId: String,
    ): Result<String> {
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured yet. Add GOOGLE_WEB_CLIENT_ID=<your-client-id> to local.properties.",
                ),
            )
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = CredentialManager.create(context).getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                Result.success(idToken)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received"))
            }
        } catch (cancelled: GetCredentialCancellationException) {
            Result.failure(cancelled)
        } catch (_: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "No Google account is signed in on this device. Add an account in Android Settings and try again.",
                ),
            )
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Google Sign-In failed"))
        }
    }
}

