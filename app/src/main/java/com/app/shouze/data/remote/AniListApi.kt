package com.app.shouze.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class AniListSearchResponse(
    val data: AniListData? = null
)

@Serializable
data class AniListData(
    val Page: AniListPage? = null
)

@Serializable
data class AniListPage(
    val media: List<AniListMedia> = emptyList()
)

@Serializable
data class AniListMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCoverImage? = null,
    val description: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val genres: List<String>? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val format: String? = null
)

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListCoverImage(
    val large: String? = null,
    val medium: String? = null
)

// --- Airing Schedule ---

@Serializable
data class AniListAiringScheduleResponse(
    val data: AiringScheduleData? = null
)

@Serializable
data class AiringScheduleData(
    val Page: AiringSchedulePage? = null
)

@Serializable
data class AiringSchedulePage(
    val airingSchedules: List<AiringSchedule> = emptyList()
)

@Serializable
data class AiringSchedule(
    val id: Int,
    val episode: Int,
    val airingAt: Long,
    val media: AiringScheduleMedia
)

@Serializable
data class AiringScheduleMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCoverImage? = null,
    val format: String? = null
)

// --- Streaming Episodes ---

@Serializable
data class AniListStreamingResponse(
    val data: StreamingData? = null
)

@Serializable
data class StreamingData(
    val Media: StreamingMedia? = null
)

@Serializable
data class StreamingMedia(
    val streamingEpisodes: List<StreamingEpisode>? = null,
    val externalLinks: List<ExternalLink>? = null
)

@Serializable
data class StreamingEpisode(
    val title: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val site: String? = null
)

@Serializable
data class ExternalLink(
    val url: String,
    val site: String
)

@Serializable
private data class AniListErrorEnvelope(
    val errors: List<AniListError> = emptyList()
)

@Serializable
private data class AniListError(
    val message: String? = null
)

class AniListApi {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun httpError(response: okhttp3.Response): IOException {
        val code = response.code
        val body = response.body?.string()
        val apiError = runCatching {
            body?.let { json.decodeFromString<AniListErrorEnvelope>(it) }
                ?.errors?.firstOrNull()?.message?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (code == 429) {
            val retryAfter = response.header("Retry-After")
            if (!retryAfter.isNullOrBlank()) {
                return IOException(
                    apiError ?: "AniList rate limit reached — try again in $retryAfter seconds"
                )
            }
        }
        return IOException(apiError ?: "HTTP $code")
    }

    suspend fun searchMedia(query: String, type: String = "ANIME"): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val graphqlQuery = """
                query SearchMedia(${"$"}search: String, ${"$"}type: MediaType) {
                    Page(page: 1, perPage: 20) {
                        media(search: ${"$"}search, type: ${"$"}type) {
                            id
                            title { romaji english native }
                            coverImage { large medium }
                            description
                            episodes
                            chapters
                            volumes
                            status
                            genres
                            averageScore
                            popularity
                            format
                        }
                    }
                }
            """.trimIndent()

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
                putJsonObject("variables") {
                    put("search", query)
                    put("type", type)
                }
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(httpError(response))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListSearchResponse>(body)
                val media = result.data?.Page?.media ?: emptyList()
                Result.success(media)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAiringSchedule(): Result<List<AiringSchedule>> = withContext(Dispatchers.IO) {
        try {
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

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(httpError(response))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListAiringScheduleResponse>(body)
                val schedules = result.data?.Page?.airingSchedules ?: emptyList()
                Result.success(schedules)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(type: String = "ANIME"): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            val graphqlQuery = """
                query Trending(${'$'}type: MediaType) {
                    Page(page: 1, perPage: 20) {
                        media(sort: POPULARITY_DESC, type: ${'$'}type) {
                            id
                            title { romaji english native }
                            coverImage { large medium }
                            description
                            episodes
                            chapters
                            volumes
                            status
                            genres
                            averageScore
                            popularity
                            format
                        }
                    }
                }
            """.trimIndent()

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(httpError(response))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListSearchResponse>(body)
                val media = result.data?.Page?.media ?: emptyList()
                Result.success(media)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStreamingEpisodes(mediaId: Int): Result<Pair<List<StreamingEpisode>, List<ExternalLink>>> = withContext(Dispatchers.IO) {
        try {
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

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
                putJsonObject("variables") {
                    put("id", mediaId)
                }
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(httpError(response))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListStreamingResponse>(body)
                val media = result.data?.Media
                val episodes = media?.streamingEpisodes ?: emptyList()
                val links = media?.externalLinks ?: emptyList()
                Result.success(episodes to links)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
