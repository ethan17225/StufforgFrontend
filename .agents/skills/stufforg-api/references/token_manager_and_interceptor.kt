package com.stufforg.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stufforg.data.model.AccessTokenOut
import com.stufforg.data.model.RefreshTokenIn
import com.stufforg.data.model.UserCreateBox
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "stufforg_auth_prefs")

// ==========================================
// 1. Token Manager (DataStore)
// ==========================================

class TokenManager(private val context: Context) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val accessTokenFlow = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshTokenFlow = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            if (refreshToken != null) {
                prefs[KEY_REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun getAccessToken(): String? = accessTokenFlow.firstOrNull()

    suspend fun getRefreshToken(): String? = refreshTokenFlow.firstOrNull()

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }
}

// ==========================================
// 2. Auth Header Interceptor
// ==========================================

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Skip auth header for login, register, and refresh endpoints
        val path = original.url.encodedPath
        if (path.contains("/login") || path.contains("/users/") || path.contains("/refresh")) {
            return chain.proceed(original)
        }

        val token = runBlocking { tokenManager.getAccessToken() }
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}

// ==========================================
// 3. OkHttp Token Authenticator (Auto-Refresh on 401)
// ==========================================

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : Authenticator {

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if refresh itself returned 401
        if (response.request.url.encodedPath.contains("/refresh")) {
            return null
        }

        val currentRefreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return null

        // Make synchronous refresh call
        val refreshClient = OkHttpClient()
        val refreshPayload = json.encodeToString(RefreshTokenIn(refreshToken = currentRefreshToken))
        val refreshRequestBody = refreshPayload.toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/refresh")
            .post(refreshRequestBody)
            .build()

        return try {
            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string() ?: return null
                val tokenOut = json.decodeFromString<AccessTokenOut>(bodyString)

                runBlocking {
                    tokenManager.saveTokens(tokenOut.accessToken, tokenOut.refreshToken)
                }

                // Retry original request with the new access token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenOut.accessToken}")
                    .build()
            } else {
                runBlocking { tokenManager.clearTokens() }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// ==========================================
// 4. Multipart Helpers (Crucial for Boxes & YOLO)
// ==========================================

object MultipartHelpers {

    /**
     * Prepares the RequestBody for creating a box.
     * The backend requires the "box" field to be a JSON string of UserCreateBox.
     */
    fun createBoxRequestBody(box: UserCreateBox, json: Json = Json): RequestBody {
        val boxJsonString = json.encodeToString(box)
        return boxJsonString.toRequestBody("text/plain".toMediaType())
    }

    /**
     * Prepares an image file for uploading to the box endpoint.
     */
    fun createImagePart(imageFile: File, partName: String = "image"): MultipartBody.Part {
        val mediaType = when (imageFile.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "image/*"
        }.toMediaTypeOrNull()

        val requestBody = imageFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData(partName, imageFile.name, requestBody)
    }

    /**
     * Prepares the image file for YOLO detection.
     * Notice: The parameter name for YOLO is "file", NOT "image".
     */
    fun createYoloImagePart(imageFile: File): MultipartBody.Part {
        return createImagePart(imageFile, partName = "file")
    }
}

