package com.app.shouze.data

import android.content.Context
import com.app.shouze.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

/** Client credentials the user registers at https://anilist.co/settings/developer. */
data class AniListCredentials(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String
)

/** An AniList OAuth token (access token lasts ~1 year). */
data class AniListToken(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Long = 0L,
    val refreshToken: String? = null
) {
    val isExpired: Boolean get() = expiresAt > 0L && System.currentTimeMillis() >= expiresAt
}

@Serializable
private data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String? = null
)

/**
 * Handles AniList OAuth2: client credentials, the authorization URL, the token
 * exchange, and persistence of the access token.
 */
class AniListAuth(private val context: Context) {

    private val prefs = context.getSharedPreferences("anilist_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_REDIRECT_URI = "com.app.shouze://oauth"
        private const val AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize"
        private const val TOKEN_URL = "https://anilist.co/api/v2/oauth/token"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_REDIRECT_URI = "redirect_uri"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    fun credentials(): AniListCredentials? {
        val id = prefs.getString(KEY_CLIENT_ID, null).orEmpty()
        val secret = prefs.getString(KEY_CLIENT_SECRET, null).orEmpty()
        if (id.isBlank() || secret.isBlank()) return null
        return AniListCredentials(
            clientId = id,
            clientSecret = secret,
            redirectUri = prefs.getString(KEY_REDIRECT_URI, DEFAULT_REDIRECT_URI) ?: DEFAULT_REDIRECT_URI
        )
    }

    fun saveCredentials(clientId: String, clientSecret: String, redirectUri: String) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, clientId.trim())
            .putString(KEY_CLIENT_SECRET, clientSecret.trim())
            .putString(KEY_REDIRECT_URI, redirectUri.trim().ifBlank { DEFAULT_REDIRECT_URI })
            .apply()
    }

    fun token(): AniListToken? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return AniListToken(
            accessToken = access,
            tokenType = prefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer",
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L),
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        )
    }

    fun saveToken(token: AniListToken) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token.accessToken)
            .putString(KEY_TOKEN_TYPE, token.tokenType)
            .putLong(KEY_EXPIRES_AT, token.expiresAt)
            .putString(KEY_REFRESH_TOKEN, token.refreshToken)
            .apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }

    val isLoggedIn: Boolean
        get() = !token()?.accessToken.isNullOrBlank()

    fun authorizeUrl(): String? {
        val creds = credentials() ?: return null
        return "$AUTHORIZE_URL?client_id=${enc(creds.clientId)}&response_type=code&redirect_uri=${enc(creds.redirectUri)}"
    }

    suspend fun exchangeCode(code: String): Result<AniListToken> = withContext(Dispatchers.IO) {
        try {
            val creds = credentials()
                ?: return@withContext Result.failure(IOException("AniList API credentials aren't configured yet"))
            val form = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", creds.clientId)
                .add("client_secret", creds.clientSecret)
                .add("redirect_uri", creds.redirectUri)
                .add("code", code)
                .build()
            val request = Request.Builder().url(TOKEN_URL).post(form).build()
            NetworkModule.okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IOException("Token exchange failed (HTTP ${resp.code})"))
                }
                val body = resp.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty token response"))
                val dto = json.decodeFromString<OAuthTokenResponse>(body)
                val token = AniListToken(
                    accessToken = dto.accessToken,
                    tokenType = dto.tokenType,
                    expiresAt = if (dto.expiresIn > 0) System.currentTimeMillis() + dto.expiresIn * 1000L else 0L,
                    refreshToken = dto.refreshToken
                )
                saveToken(token)
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
