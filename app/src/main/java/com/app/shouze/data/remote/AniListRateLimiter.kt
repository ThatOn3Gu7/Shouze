package com.app.shouze.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Client-side gate for the AniList GraphQL API.
 *
 * AniList's documented limit is 90 req/min but the API has been degraded to
 * 30 req/min (docs.anilist.co/guide/rate-limiting). Tripping the server-side
 * limiter penalizes the caller with a full 1-minute timeout, so this gate:
 *  1. paces requests through a sliding window (max [maxRequestsPerWindow] per 60s),
 *  2. once a 429 with Retry-After is observed, suspends ALL AniList calls until reset.
 *
 * Shared process-wide: every feature (search, sync, discovery) competes fairly
 * for the same quota instead of stampeding the API.
 */
object AniListRateLimiter {

    private const val WINDOW_MS = 60_000L
    private const val MAX_REQUESTS_PER_WINDOW = 28 // headroom below the live 30/min limit

    private val mutex = Mutex()
    private val requestTimestamps = ArrayDeque<Long>()
    @Volatile private var lockedUntil: Long = 0L

    /** Suspends until a request slot is available. */
    suspend fun acquire() {
        while (true) {
            val delayMs = mutex.withLock {
                val now = System.currentTimeMillis()
                val waitUntilLock = lockedUntil - now
                if (waitUntilLock > 0) return@withLock waitUntilLock

                while (requestTimestamps.isNotEmpty() && now - requestTimestamps.first() >= WINDOW_MS) {
                    requestTimestamps.removeFirst()
                }
                if (requestTimestamps.size < MAX_REQUESTS_PER_WINDOW) {
                    requestTimestamps.addLast(now)
                    return@withLock 0L
                }
                WINDOW_MS - (now - requestTimestamps.first()) + 50L
            }
            if (delayMs <= 0) return
            kotlinx.coroutines.delay(delayMs)
        }
    }

    /** Called when the server responds 429; [retryAfterSeconds] comes from the header. */
    suspend fun penalize(retryAfterSeconds: Long) {
        val until = System.currentTimeMillis() + (retryAfterSeconds.coerceIn(1, 120)) * 1000L
        mutex.withLock {
            if (until > lockedUntil) lockedUntil = until
        }
    }

    suspend fun reset() {
        mutex.withLock {
            requestTimestamps.clear()
            lockedUntil = 0L
        }
    }
}

/** Errors surfaced by [AniListApi]; [kind] lets callers react semantically. */
class AniListException(
    message: String,
    val kind: Kind,
    cause: Throwable? = null
) : IOException(message, cause) {

    enum class Kind {
        /** Device is offline / DNS / timeout / socket failure. */
        NETWORK,
        /** Non-2xx HTTP that wasn't rate limiting or auth. */
        HTTP,
        /** 429 — even after client-side pacing; Retry-After was honored. */
        RATE_LIMITED,
        /** 401/403 — token missing, expired or revoked. */
        UNAUTHORIZED,
        /** 2xx but the GraphQL payload carried errors. */
        GRAPHQL
    }
}
