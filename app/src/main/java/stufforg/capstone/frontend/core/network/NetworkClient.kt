package stufforg.capstone.frontend.core.network

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import stufforg.capstone.frontend.core.model.AccessTokenOut
import stufforg.capstone.frontend.core.model.RefreshTokenIn
import stufforg.capstone.frontend.core.model.UserCreateBox
import stufforg.capstone.frontend.core.model.UserOut
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "stufforg_auth_prefs")

@Immutable
data class SessionState(
    val isLoaded: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserOut? = null,
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

open class SessionStore(
    private val dataStore: DataStore<Preferences>?,
    private val json: Json,
) {
    constructor(context: Context, json: Json) : this(context.authDataStore, json)

    private val inMemoryFallback = MutableStateFlow(SessionState(isLoaded = true))

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    val sessionFlow: Flow<SessionState> = dataStore?.data?.map { prefs ->
        val access = prefs[KEY_ACCESS_TOKEN]
        val refresh = prefs[KEY_REFRESH_TOKEN]
        cachedAccessToken = access
        cachedRefreshToken = refresh
        val user = prefs[KEY_USER_JSON]?.let { raw ->
            runCatching { json.decodeFromString<UserOut>(raw) }.getOrNull()
        }
        SessionState(
            isLoaded = true,
            onboardingCompleted = prefs[KEY_ONBOARDING_DONE] ?: false,
            accessToken = access,
            refreshToken = refresh,
            user = user,
        )
    } ?: inMemoryFallback

    suspend fun saveAuth(tokenOut: AccessTokenOut) {
        cachedAccessToken = tokenOut.accessToken
        if (tokenOut.refreshToken != null) {
            cachedRefreshToken = tokenOut.refreshToken
        }
        val store = dataStore
        if (store != null) {
            store.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = tokenOut.accessToken
                if (tokenOut.refreshToken != null) {
                    prefs[KEY_REFRESH_TOKEN] = tokenOut.refreshToken
                }
                prefs[KEY_USER_JSON] = json.encodeToString(tokenOut.user)
                prefs[KEY_ONBOARDING_DONE] = true
            }
        } else {
            inMemoryFallback.value = SessionState(
                isLoaded = true,
                onboardingCompleted = true,
                accessToken = tokenOut.accessToken,
                refreshToken = tokenOut.refreshToken ?: inMemoryFallback.value.refreshToken,
                user = tokenOut.user,
            )
        }
    }

    suspend fun completeOnboarding() {
        val store = dataStore
        if (store != null) {
            store.edit { prefs -> prefs[KEY_ONBOARDING_DONE] = true }
        } else {
            inMemoryFallback.value = inMemoryFallback.value.copy(onboardingCompleted = true)
        }
    }

    suspend fun clearAuth() {
        cachedAccessToken = null
        cachedRefreshToken = null
        val store = dataStore
        if (store != null) {
            store.edit { prefs ->
                prefs.remove(KEY_ACCESS_TOKEN)
                prefs.remove(KEY_REFRESH_TOKEN)
                prefs.remove(KEY_USER_JSON)
            }
        } else {
            inMemoryFallback.value = inMemoryFallback.value.copy(
                accessToken = null,
                refreshToken = null,
                user = null,
            )
        }
    }

    fun currentAccessTokenBlocking(): String? =
        cachedAccessToken ?: runBlocking { sessionFlow.firstOrNull()?.accessToken }

    fun currentRefreshTokenBlocking(): String? =
        cachedRefreshToken ?: runBlocking { sessionFlow.firstOrNull()?.refreshToken }

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_JSON = stringPreferencesKey("user_json")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}

class AuthInterceptor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val isPublic = PUBLIC_PATHS.any { path.endsWith(it) }
        if (isPublic) return chain.proceed(original)

        val token = sessionStore.currentAccessTokenBlocking()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }

    private companion object {
        val PUBLIC_PATHS = listOf(
            "/login",
            "/users/",
            "/refresh",
            "/google_oidc_login",
            "/apple_oidc_login",
            "/test-db",
        )
    }
}

class TokenAuthenticator(
    private val sessionStore: SessionStore,
    private val baseUrl: String,
    private val json: Json,
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("/refresh")) return null
        val attempts = generateSequence(response) { it.priorResponse }.count()
        if (attempts >= 2) return null

        val refreshToken = sessionStore.currentRefreshTokenBlocking() ?: return null
        val payload = json.encodeToString(RefreshTokenIn(refreshToken = refreshToken))
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/refresh")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            refreshClient.newCall(request).execute().use { refreshResp ->
                if (refreshResp.isSuccessful) {
                    val bodyString = refreshResp.body.string().takeIf { it.isNotBlank() } ?: return null
                    val tokenOut = json.decodeFromString<AccessTokenOut>(bodyString)
                    runBlocking { sessionStore.saveAuth(tokenOut) }
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokenOut.accessToken}")
                        .build()
                } else {
                    runBlocking { sessionStore.clearAuth() }
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

object MultipartHelpers {
    fun createBoxRequestBody(box: UserCreateBox, json: Json): RequestBody =
        json.encodeToString(box).toRequestBody("text/plain".toMediaType())

    fun createImagePart(imageFile: File, partName: String = "image"): MultipartBody.Part {
        val mediaType = when (imageFile.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }.toMediaTypeOrNull()
        return MultipartBody.Part.createFormData(
            partName,
            imageFile.name,
            imageFile.asRequestBody(mediaType),
        )
    }
}

object NetworkFactory {
    fun createJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createOkHttpClient(
        sessionStore: SessionStore,
        baseUrl: String,
        json: Json,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionStore))
        .authenticator(TokenAuthenticator(sessionStore, baseUrl, json))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createApiService(
        okHttpClient: OkHttpClient,
        baseUrl: String,
        json: Json,
    ): StuffOrgApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(StuffOrgApiService::class.java)
}
