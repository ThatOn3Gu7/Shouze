package com.app.shouze.data.remote

import com.app.shouze.data.local.SaveEntryPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * GraphQL client for https://graphql.anilist.co.
 *
 * All requests share one hardened execution path:
 *  - client-side rate limiting via [AniListRateLimiter] (the live limit is 30 req/min),
 *  - optional Bearer auth supplied by [accessTokenProvider],
 *  - typed [AniListException]s so the UI can distinguish offline / rate-limited /
 *    unauthorized / server failures and recover gracefully.
 *
 * Anonymous public-data calls (search, trending, airing, streaming) keep working
 * with no token; library queries and mutations require one.
 */
class AniListApi(
    private val accessTokenProvider: () -> String? = { null }
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client: OkHttpClient by lazy { NetworkModule.okHttpClient }

    // ------------------------------------------------------------------
    // Public data (no auth required)
    // ------------------------------------------------------------------

    @Serializable
    data class AniListMediaPage(
        val media: List<AniListMedia> = emptyList(),
        val hasNextPage: Boolean = false,
        val currentPage: Int = 1
    )

    suspend fun searchMedia(query: String, type: String = "ANIME"): Result<List<AniListMedia>> =
        searchMediaPaged(query, type, 1).map { it.media }

    suspend fun searchMediaPaged(query: String, type: String = "ANIME", page: Int = 1): Result<AniListMediaPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (query.isBlank()) return@runCatching AniListMediaPage()

                val entryField = if (hasToken()) "mediaListEntry { id mediaId status score progress }" else ""
                val graphqlQuery = """
                    query SearchMedia(${'$'}search: String, ${'$'}type: MediaType, ${'$'}page: Int) {
                        Page(page: ${'$'}page, perPage: 24) {
                            pageInfo { total currentPage lastPage hasNextPage }
                            media(search: ${'$'}search, type: ${'$'}type, sort: SEARCH_MATCH) {
                                id
                                title { romaji english native }
                                coverImage { large medium }
                                bannerImage
                                description
                                episodes
                                chapters
                                volumes
                                status
                                genres
                                averageScore
                                popularity
                                format
                                type
                                siteUrl
                                $entryField
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(
                    graphqlQuery,
                    buildJsonObject {
                        put("search", query.trim())
                        put("type", type)
                        put("page", page)
                    },
                    authenticated = false
                )
                val result = json.decodeFromString<AniListSearchResponse>(body)
                val pageData = result.data?.Page
                AniListMediaPage(
                    media = pageData?.media ?: emptyList(),
                    hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                    currentPage = pageData?.pageInfo?.currentPage ?: page
                )
            }.recoverFailure()
        }

    suspend fun getTrending(type: String = "ANIME"): Result<List<AniListMedia>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val entryField = if (hasToken()) "mediaListEntry { id mediaId status score progress }" else ""
                val graphqlQuery = """
                    query Trending(${'$'}type: MediaType) {
                        Page(page: 1, perPage: 20) {
                            media(sort: POPULARITY_DESC, type: ${'$'}type) {
                                id
                                title { romaji english native }
                                coverImage { large medium }
                                bannerImage
                                description
                                episodes
                                chapters
                                volumes
                                status
                                genres
                                averageScore
                                popularity
                                format
                                type
                                siteUrl
                                $entryField
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(
                    graphqlQuery,
                    buildJsonObject { put("type", type) },
                    authenticated = false
                )
                json.decodeFromString<AniListSearchResponse>(body).data?.Page?.media ?: emptyList()
            }.recoverFailure()
        }

    suspend fun getAiringSchedule(): Result<List<AiringSchedule>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    query {
                        Page(page: 1, perPage: 50) {
                            airingSchedules(notYetAired: true, sort: TIME) {
                                id
                                episode
                                airingAt
                                media {
                                    id
                                    title { romaji english }
                                    coverImage { large }
                                    format
                                }
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(graphqlQuery, buildJsonObject { }, authenticated = false)
                json.decodeFromString<AniListAiringScheduleResponse>(body)
                    .data?.Page?.airingSchedules ?: emptyList()
            }.recoverFailure()
        }

    suspend fun getStreamingEpisodes(mediaId: Int): Result<Pair<List<StreamingEpisode>, List<ExternalLink>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    query(${'$'}id: Int) {
                        Media(id: ${'$'}id) {
                            streamingEpisodes {
                                title
                                thumbnail
                                url
                                site
                            }
                            externalLinks {
                                url
                                site
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(
                    graphqlQuery,
                    buildJsonObject { put("id", mediaId) },
                    authenticated = false
                )
                val media = json.decodeFromString<AniListStreamingResponse>(body).data?.Media
                (media?.streamingEpisodes ?: emptyList()) to (media?.externalLinks ?: emptyList())
            }.recoverFailure()
        }

    // ------------------------------------------------------------------
    // Authenticated: viewer + library
    // ------------------------------------------------------------------

    /** Fetches the signed-in user. Pass [token] explicitly to validate a token before storing it. */
    suspend fun getViewer(token: String): Result<AniListViewer> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    query {
                        Viewer {
                            id
                            name
                            avatar { large medium }
                            bannerImage
                            siteUrl
                            mediaListOptions {
                                scoreFormat
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(graphqlQuery, buildJsonObject { }, authenticated = false, overrideToken = token)
                json.decodeFromString<AniListViewerResponse>(body).data?.Viewer
                    ?: throw AniListException("AniList returned no viewer for this token.", AniListException.Kind.UNAUTHORIZED)
            }.recoverFailure()
        }

    /** The user's full tracking library for one media type (ANIME or MANGA). */
    suspend fun getMediaListCollection(userId: Int, type: String): Result<AniListMediaListCollection> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    query(${'$'}userId: Int, ${'$'}type: MediaType) {
                        MediaListCollection(userId: ${'$'}userId, type: ${'$'}type) {
                            user { id name avatar { large medium } }
                            lists {
                                name
                                status
                                isCustomList
                                entries {
                                    id
                                    mediaId
                                    status
                                    score
                                    progress
                                    progressVolumes
                                    repeat
                                    notes
                                    updatedAt
                                    startedAt { year month day }
                                    completedAt { year month day }
                                    media {
                                        id
                                        title { romaji english native }
                                        coverImage { large medium }
                                        bannerImage
                                        episodes
                                        chapters
                                        volumes
                                        status
                                        genres
                                        averageScore
                                        format
                                        type
                                        siteUrl
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()

                val body = execute(
                    graphqlQuery,
                    buildJsonObject {
                        put("userId", userId)
                        put("type", type)
                    },
                    authenticated = true
                )
                json.decodeFromString<AniListMediaListCollectionResponse>(body).data?.MediaListCollection
                    ?: throw AniListException("AniList returned an empty library response.", AniListException.Kind.GRAPHQL)
            }.recoverFailure()
        }

    /** Create or update the tracking entry for [payload.mediaId]; returns the saved entry. */
    suspend fun saveMediaListEntry(payload: SaveEntryPayload): Result<AniListListEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    mutation(
                        ${'$'}mediaId: Int,
                        ${'$'}status: MediaListStatus,
                        ${'$'}score: Float,
                        ${'$'}progress: Int,
                        ${'$'}progressVolumes: Int,
                        ${'$'}notes: String,
                        ${'$'}repeat: Int,
                        ${'$'}startedAt: FuzzyDateInt,
                        ${'$'}completedAt: FuzzyDateInt
                    ) {
                        SaveMediaListEntry(
                            mediaId: ${'$'}mediaId
                            status: ${'$'}status
                            score: ${'$'}score
                            progress: ${'$'}progress
                            progressVolumes: ${'$'}progressVolumes
                            notes: ${'$'}notes
                            repeat: ${'$'}repeat
                            startedAt: ${'$'}startedAt
                            completedAt: ${'$'}completedAt
                        ) {
                            id
                            mediaId
                            status
                            score
                            progress
                            progressVolumes
                            updatedAt
                        }
                    }
                """.trimIndent()

                val startedAt = payload.startedAt?.let { FuzzyDate.fromEpochMillis(it) }
                val completedAt = payload.completedAt?.let { FuzzyDate.fromEpochMillis(it) }

                val body = execute(
                    graphqlQuery,
                    buildJsonObject {
                        put("mediaId", payload.mediaId)
                        put("status", payload.status)
                        payload.score?.let { put("score", it) }
                        payload.progress?.let { put("progress", it) }
                        payload.progressVolumes?.let { put("progressVolumes", it) }
                        payload.notes?.let { put("notes", it) }
                        payload.repeat?.let { put("repeat", it) }
                        startedAt?.let { put("startedAt", it) }
                        completedAt?.let { put("completedAt", it) }
                    },
                    authenticated = true
                )
                json.decodeFromString<AniListSaveEntryResponse>(body).data?.SaveMediaListEntry
                    ?: throw AniListException("AniList did not confirm the save.", AniListException.Kind.GRAPHQL)
            }.recoverFailure()
        }

    /** Removes a tracking entry from the user's library. Returns true when deleted. */
    suspend fun deleteMediaListEntry(entryId: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val graphqlQuery = """
                    mutation(${'$'}id: Int) {
                        DeleteMediaListEntry(id: ${'$'}id) {
                            deleted
                        }
                    }
                """.trimIndent()

                val body = execute(
                    graphqlQuery,
                    buildJsonObject { put("id", entryId) },
                    authenticated = true
                )
                json.decodeFromString<AniListDeleteEntryResponse>(body).data?.DeleteMediaListEntry?.deleted ?: false
            }.recoverFailure()
        }

    // ------------------------------------------------------------------
    // Execution core
    // ------------------------------------------------------------------

    private fun hasToken(): Boolean = !accessTokenProvider().isNullOrBlank()

    /**
     * Runs one GraphQL request and returns the raw response body.
     * Retries once on transient network failures (queries are idempotent; this
     * path is only used for the initial attempt of any operation).
     */
    private suspend fun execute(
        query: String,
        variables: JsonObject,
        authenticated: Boolean,
        overrideToken: String? = null,
        allowRetry: Boolean = true
    ): String {
        val token = overrideToken ?: accessTokenProvider()
        if (authenticated && token.isNullOrBlank()) {
            throw AniListException("Not signed in to AniList.", AniListException.Kind.UNAUTHORIZED)
        }

        AniListRateLimiter.acquire()

        val requestBody = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        val requestBuilder = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when {
                    response.code == 429 -> {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 60L
                        AniListRateLimiter.penalize(retryAfter)
                        throw AniListException(
                            "AniList rate limit reached — retrying in $retryAfter seconds.",
                            AniListException.Kind.RATE_LIMITED
                        )
                    }
                    response.code == 401 || response.code == 403 ->
                        throw AniListException(
                            firstErrorMessage(raw) ?: "AniList rejected this session.",
                            AniListException.Kind.UNAUTHORIZED
                        )
                    !response.isSuccessful ->
                        throw AniListException(
                            firstErrorMessage(raw) ?: "AniList returned HTTP ${response.code}.",
                            AniListException.Kind.HTTP
                        )
                }

                // 2xx: surface GraphQL-level errors that carry no data as failures,
                // but tolerate partial payloads (errors + data) which AniList sends sometimes.
                val envelope = runCatching { json.decodeFromString<AniListErrorEnvelope>(raw) }.getOrNull()
                if (envelope != null && envelope.errors.isNotEmpty()) {
                    val hasData = raw.contains("\"data\"") && !raw.contains("\"data\":null") && !raw.contains("\"data\": null")
                    if (!hasData) {
                        throw AniListException(
                            envelope.errors.firstOrNull()?.message ?: "AniList query failed.",
                            AniListException.Kind.GRAPHQL
                        )
                    }
                }
                if (raw.isBlank()) {
                    throw AniListException("AniList returned an empty response.", AniListException.Kind.HTTP)
                }
                return raw
            }
        } catch (e: AniListException) {
            throw e
        } catch (e: IOException) {
            if (allowRetry) {
                // One silent retry for transient network hiccups.
                return execute(query, variables, authenticated, overrideToken, allowRetry = false)
            }
            throw AniListException(e.message ?: "Network request to AniList failed.", AniListException.Kind.NETWORK, e)
        }
    }

    private fun firstErrorMessage(rawBody: String): String? =
        runCatching {
            json.decodeFromString<AniListErrorEnvelope>(rawBody).errors.firstOrNull()?.message
        }.getOrNull()

    private fun <T> Result<T>.recoverFailure(): Result<T> {
        val exception = exceptionOrNull() ?: return this
        if (exception is AniListException) return Result.failure(exception)
        return Result.failure(
            AniListException(exception.message ?: "Unexpected AniList error.", AniListException.Kind.GRAPHQL, exception)
        )
    }

    companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
        const val AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize"
        const val REDIRECT_URI = "shouze://anilist-auth"
    }
}

/** YYYYMMDD fuzzy date used by AniList's FuzzyDateInt fields. Pure JVM for testability. */
object FuzzyDate {
    fun fromEpochMillis(millis: Long): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return year * 10000 + month * 100 + day
    }

    fun toEpochMillis(fuzzy: Int): Long {
        val year = fuzzy / 10000
        val month = (fuzzy / 100) % 100
        val day = fuzzy % 100
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year.coerceIn(1970, 2200), (month.coerceIn(1, 12)) - 1, day.coerceIn(1, 31), 0, 0, 0)
        }
        return cal.timeInMillis
    }

    fun toEpochMillis(date: AniListFuzzyDate?): Long? {
        val y = date?.year ?: return null
        val m = date.month ?: 1
        val d = date.day ?: 1
        return toEpochMillis(y * 10000 + m * 100 + d)
    }
}
