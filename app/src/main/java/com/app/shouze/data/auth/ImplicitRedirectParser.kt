package com.app.shouze.data.auth

/**
 * Parses the redirect AniList produces at the end of an OAuth2 Implicit Grant.
 *
 * AniList sends the token in the URL *fragment* of the registered redirect URI:
 *   shouze://anilist-auth#access_token=eyJ...&token_type=Bearer&expires=31536000
 *
 * Pure JVM (no android.net.Uri) so it is unit-testable. Tolerant of the token
 * arriving in the query string instead of the fragment, and of `error=` responses.
 */
object ImplicitRedirectParser {

    data class Parsed(
        val accessToken: String,
        val expiresInSeconds: Long
    )

    sealed interface Result {
        data class Success(val parsed: Parsed) : Result
    /**
     * The user denied the app or AniList returned an error.
     *
     * [error] is the raw OAuth error code (e.g. `access_denied`), [description]
     * the human-readable message (falls back to the code when absent).
     */
    data class Denied(val error: String, val description: String? = null) : Result
        /** Redirect isn't an AniList OAuth callback. */
        data object NotAnAuthRedirect : Result
    }

    fun parse(uriString: String): Result {
        val uri = uriString.trim()
        val hashIndex = uri.indexOf('#')
        val fragment = if (hashIndex >= 0) uri.substring(hashIndex + 1) else ""
        val query = if (hashIndex >= 0) uri.substring(0, hashIndex).substringAfter('?', "") else uri.substringAfter('?', "")

        val fragmentParams = parseParams(fragment)
        val queryParams = parseParams(query)

        val error = fragmentParams["error"] ?: queryParams["error"]
        if (error != null) {
            val description = fragmentParams["error_description"] ?: queryParams["error_description"]
            return Result.Denied(error, description)
        }

        val token = fragmentParams["access_token"] ?: queryParams["access_token"]
            ?: return if (fragmentParams.containsKey("token_type") || queryParams.containsKey("token_type")) {
                Result.Denied("invalid_request", "AniList did not return an access token.")
            } else {
                Result.NotAnAuthRedirect
            }

        if (token.isBlank()) return Result.Denied("invalid_request", "AniList returned an empty access token.")

        // `expires` is seconds until expiry (docs.anilist.co: tokens live 1 year).
        val expires = (fragmentParams["expires"] ?: queryParams["expires"])?.toLongOrNull() ?: DEFAULT_EXPIRY_SECONDS
        return Result.Success(Parsed(accessToken = token, expiresInSeconds = expires.coerceAtLeast(60L)))
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&')
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = pair.substring(0, idx).trim()
                val value = java.net.URLDecoder.decode(pair.substring(idx + 1).trim(), Charsets.UTF_8.name())
                key to value
            }
            .toMap()
    }

    private const val DEFAULT_EXPIRY_SECONDS = 365L * 24 * 60 * 60
}
