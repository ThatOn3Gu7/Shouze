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

class AniListApi {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

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
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
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
}
