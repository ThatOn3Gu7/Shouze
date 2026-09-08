package com.app.shouze.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Who is signed in + the preferences the app needs to render their data faithfully. */
data class AniListSession(
    val token: String,
    val tokenExpiresAt: Long,
    val userId: Int,
    val userName: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val profileUrl: String? = null,
    /** AniList score format id (POINT_100, POINT_10, POINT_10_DECIMAL, POINT_5, POINT_3). */
    val scoreFormat: String = "POINT_100"
)

data class AniListAuthState(
    val session: AniListSession? = null,
    /** One-line reason the last login attempt failed, for the login UI. */
    val loginError: String? = null
) {
    val isSignedIn: Boolean get() = session != null
}

/**
 * Owns the AniList OAuth session.
 *
 * Token storage uses EncryptedSharedPreferences (Android Keystore-backed). AniList
 * uses the Implicit Grant for mobile clients (its official recommendation — it has
 * no PKCE support and the auth-code grant would require shipping a client secret in
 * the APK, which is strictly worse). Tokens live one year and cannot be refreshed,
 * so an expired/revoked token simply sends the user through login again.
 *
 * If the device keystore is broken (rare OEM failure mode), the session degrades to
 * in-memory only for the current process rather than crashing or storing plaintext.
 */
class AniListAuthRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences? = try {
        createEncryptedPrefs(context)
    } catch (t: Throwable) {
        null
    }

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AniListAuthState> = _state.asStateFlow()

    /** Synchronous token accessor for the API client's auth header provider. */
    fun accessToken(): String? {
        val session = _state.value.session ?: return null
        if (session.tokenExpiresAt <= System.currentTimeMillis()) return null
        return session.token
    }

    fun isTokenConfigured(): Boolean = !BuildConfigBridge.clientId.isNullOrBlank()

    fun startLoginUrl(): String {
        val clientId = BuildConfigBridge.clientId.orEmpty().trim()
        return buildString {
            append("https://anilist.co/api/v2/oauth/authorize")
            append("?client_id=").append(java.net.URLEncoder.encode(clientId, Charsets.UTF_8.name()))
            append("&redirect_uri=").append(java.net.URLEncoder.encode(REDIRECT_URI, Charsets.UTF_8.name()))
            append("&response_type=token")
        }
    }

    /**
     * Validates and installs a freshly received token, then attaches viewer profile
     * info once the caller has fetched it. Returns false if the token is unusable.
     */
    fun applyToken(token: String, expiresInSeconds: Long): Boolean {
        val session = _state.value.session
        _state.value = AniListAuthState(
            session = (session ?: AniListSession(token = token, tokenExpiresAt = 0L, userId = 0, userName = ""))
                .copy(
                    token = token,
                    tokenExpiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
                ),
            loginError = null
        )
        return true
    }

    fun applyViewer(
        userId: Int,
        userName: String,
        avatarUrl: String?,
        bannerUrl: String?,
        profileUrl: String?,
        scoreFormat: String
    ) {
        val session = _state.value.session ?: return
        _state.value = AniListAuthState(
            session = session.copy(
                userId = userId,
                userName = userName,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                profileUrl = profileUrl,
                scoreFormat = scoreFormat
            )
        )
        persist(_state.value.session)
    }

    fun setLoginError(message: String?) {
        _state.value = _state.value.copy(loginError = message)
    }

    fun logout() {
        _state.value = AniListAuthState()
        clearPersisted()
    }

    // ------------------------------------------------------------------

    private fun loadState(): AniListAuthState {
        val raw = try {
            prefs?.getString(KEY_SESSION, null)
        } catch (_: Throwable) {
            null
        } ?: return AniListAuthState()

        return try {
            val stored = json.decodeFromString(StoredSession.serializer(), raw)
            if (stored.tokenExpiresAt <= System.currentTimeMillis()) {
                clearPersisted()
                AniListAuthState()
            } else {
                AniListAuthState(
                    session = AniListSession(
                        token = stored.token,
                        tokenExpiresAt = stored.tokenExpiresAt,
                        userId = stored.userId,
                        userName = stored.userName,
                        avatarUrl = stored.avatarUrl,
                        bannerUrl = stored.bannerUrl,
                        profileUrl = stored.profileUrl,
                        scoreFormat = stored.scoreFormat
                    )
                )
            }
        } catch (_: Throwable) {
            AniListAuthState()
        }
    }

    private fun persist(session: AniListSession?) {
        val currentPrefs = prefs ?: return // keystore unavailable: memory-only session
        try {
            if (session == null) {
                clearPersisted()
                return
            }
            val stored = StoredSession(
                token = session.token,
                tokenExpiresAt = session.tokenExpiresAt,
                userId = session.userId,
                userName = session.userName,
                avatarUrl = session.avatarUrl,
                bannerUrl = session.bannerUrl,
                profileUrl = session.profileUrl,
                scoreFormat = session.scoreFormat
            )
            currentPrefs.edit().putString(KEY_SESSION, json.encodeToString(StoredSession.serializer(), stored)).apply()
        } catch (_: Throwable) {
            // Never let a storage failure crash the app on a background write.
        }
    }

    private fun clearPersisted() {
        try {
            prefs?.edit()?.remove(KEY_SESSION)?.apply()
        } catch (_: Throwable) {
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "anilist_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Serializable
    private data class StoredSession(
        val token: String,
        val tokenExpiresAt: Long,
        val userId: Int,
        val userName: String,
        val avatarUrl: String? = null,
        val bannerUrl: String? = null,
        val profileUrl: String? = null,
        val scoreFormat: String = "POINT_100"
    )

    companion object {
        private const val KEY_SESSION = "session_v1"
        const val REDIRECT_URI = "shouze://anilist-auth"
    }
}
