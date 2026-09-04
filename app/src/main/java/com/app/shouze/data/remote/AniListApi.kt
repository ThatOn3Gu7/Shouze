package com.app.shouze.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// ---------------------------------------------------------------------------
// Shared / scalar DTOs
// ---------------------------------------------------------------------------

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class CoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
)

@Serializable
data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class Trailer(
    val id: String? = null,
    val site: String? = null,
    val thumbnail: String? = null
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
    val url: String? = null,
    val site: String? = null
)

@Serializable
data class NextAiringEpisode(
    val id: Int,
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long = 0L
)

@Serializable
data class Studio(
    val id: Int,
    val name: String
)

@Serializable
data class StudioConnection(val nodes: List<Studio>? = null)

@Serializable
data class StaffName(val full: String? = null)

@Serializable
data class StaffNode(
    val id: Int,
    val name: StaffName? = null
)

@Serializable
data class StaffEdge(
    val role: String? = null,
    val node: StaffNode? = null
)

@Serializable
data class StaffConnection(val edges: List<StaffEdge>? = null)

@Serializable
data class CharacterName(val full: String? = null)

@Serializable
data class CharacterImage(
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class CharacterNode(
    val id: Int,
    val name: CharacterName? = null,
    val image: CharacterImage? = null
)

@Serializable
data class CharacterEdge(
    val role: String? = null,
    val node: CharacterNode? = null
)

@Serializable
data class CharacterConnection(val edges: List<CharacterEdge>? = null)

@Serializable
data class RelationEdge(
    val relationType: String? = null,
    val node: MediaSummary? = null
)

@Serializable
data class RelationConnection(val edges: List<RelationEdge>? = null)

@Serializable
data class RecommendationConnection(val nodes: List<MediaSummary>? = null)

// ---------------------------------------------------------------------------
// Media DTOs
// ---------------------------------------------------------------------------

/** Lightweight media card shape used across lists, relations & recommendations. */
@Serializable
data class MediaSummary(
    val id: Int,
    val title: AniListTitle? = null,
    val coverImage: CoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val format: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val genres: List<String>? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val status: String? = null,
    val nextAiringEpisode: NextAiringEpisode? = null
)

/** Full detail shape for the media detail screen. */
@Serializable
data class MediaDetail(
    val id: Int,
    val title: AniListTitle? = null,
    val coverImage: CoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val format: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val duration: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val genres: List<String>? = null,
    val source: String? = null,
    val countryOfOrigin: String? = null,
    val isAdult: Boolean? = null,
    val siteUrl: String? = null,
    val startDate: FuzzyDate? = null,
    val endDate: FuzzyDate? = null,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val studios: StudioConnection? = null,
    val staff: StaffConnection? = null,
    val characters: CharacterConnection? = null,
    val relations: RelationConnection? = null,
    val recommendations: RecommendationConnection? = null,
    val streamingEpisodes: List<StreamingEpisode>? = null,
    val externalLinks: List<ExternalLink>? = null,
    val trailer: Trailer? = null
)

// ---------------------------------------------------------------------------
// Airing schedule DTOs
// ---------------------------------------------------------------------------

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
    val title: AniListTitle? = null,
    val coverImage: CoverImage? = null,
    val format: String? = null
)

// ---------------------------------------------------------------------------
// User / list DTOs
// ---------------------------------------------------------------------------

@Serializable
data class MediaListEntryDto(
    val id: Int,
    val mediaId: Int,
    val status: String? = null,
    val progress: Int? = null,
    val progressVolumes: Int? = null,
    val score: Double? = null,
    val repeat: Int? = null,
    val notes: String? = null,
    val startedAt: FuzzyDate? = null,
    val completedAt: FuzzyDate? = null,
    val media: MediaSummary? = null
)

@Serializable
data class MediaListGroup(
    val name: String? = null,
    val entries: List<MediaListEntryDto> = emptyList()
)

@Serializable
data class MediaListCollection(
    val lists: List<MediaListGroup> = emptyList()
)

@Serializable
data class ViewerAvatar(
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class StatusCount(
    val status: String? = null,
    val count: Int = 0
)

@Serializable
data class AnimeStats(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val minutesWatched: Int = 0,
    val episodesWatched: Int = 0,
    val statuses: List<StatusCount>? = null
)

@Serializable
data class MangaStats(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val chaptersRead: Int = 0,
    val volumesRead: Int = 0,
    val statuses: List<StatusCount>? = null
)

@Serializable
data class UserStatistics(
    val anime: AnimeStats? = null,
    val manga: MangaStats? = null
)

@Serializable
data class ViewerDto(
    val id: Int,
    val name: String,
    val about: String? = null,
    val bannerImage: String? = null,
    val avatar: ViewerAvatar? = null,
    val statistics: UserStatistics? = null
)

// ---------------------------------------------------------------------------
// Response envelopes
// ---------------------------------------------------------------------------

@Serializable
data class SearchPage(val media: List<MediaSummary> = emptyList())

@Serializable
data class SearchData(val Page: SearchPage? = null)

@Serializable
data class SearchResponse(val data: SearchData? = null)

@Serializable
data class AiringPage(val airingSchedules: List<AiringSchedule> = emptyList())

@Serializable
data class AiringData(val Page: AiringPage? = null)

@Serializable
data class AiringResponse(val data: AiringData? = null)

@Serializable
data class MediaByIdData(val Media: MediaDetail? = null)

@Serializable
data class MediaByIdResponse(val data: MediaByIdData? = null)

@Serializable
data class ViewerData(val Viewer: ViewerDto? = null)

@Serializable
data class ViewerResponse(val data: ViewerData? = null)

@Serializable
data class MediaListData(val MediaListCollection: MediaListCollection? = null)

@Serializable
data class MediaListResponse(val data: MediaListData? = null)

@Serializable
data class SaveEntryData(val SaveMediaListEntry: MediaListEntryDto? = null)

@Serializable
data class SaveEntryResponse(val data: SaveEntryData? = null)

@Serializable
data class DeletedEntry(val deleted: Boolean? = null)

@Serializable
data class DeleteEntryData(val DeleteMediaListEntry: DeletedEntry? = null)

@Serializable
data class DeleteEntryResponse(val data: DeleteEntryData? = null)

@Serializable
data class AniListErrorEnvelope(val errors: List<AniListError> = emptyList())

@Serializable
data class AniListError(val message: String? = null)

// ---------------------------------------------------------------------------
// API client
// ---------------------------------------------------------------------------

class AniListApi {
    private val client = NetworkModule.okHttpClient
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val GRAPHQL_URL = "https://graphql.anilist.co"
    }

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
                return IOException(apiError ?: "AniList rate limit reached — try again in $retryAfter seconds")
            }
        }
        return IOException(apiError ?: "HTTP $code")
    }

    private suspend fun post(
        query: String,
        variables: JsonObject = buildJsonObject { },
        token: String? = null
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        val builder = Request.Builder()
            .url(GRAPHQL_URL)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw httpError(response)
            }
            response.body?.string() ?: throw IOException("Empty response from AniList")
        }
    }

    // --- Discovery (unauthenticated) ---

    suspend fun searchMedia(query: String, type: String = "ANIME"): Result<List<MediaSummary>> = runCatching {
        if (query.isBlank()) return Result.success(emptyList())
        val gql = """
            query (${'$'}search: String, ${'$'}type: MediaType) {
                Page(page: 1, perPage: 24) {
                    media(search: ${'$'}search, type: ${'$'}type, sort: SEARCH_MATCH) {
                        id title { romaji english native }
                        coverImage { extraLarge large medium color } bannerImage
                        format type episodes chapters volumes averageScore meanScore popularity
                        genres season seasonYear status
                        nextAiringEpisode { id episode airingAt }
                    }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject {
            put("search", query)
            put("type", type)
        })
        json.decodeFromString<SearchResponse>(body).data?.Page?.media ?: emptyList()
    }

    suspend fun trending(type: String = "ANIME", sort: String = "POPULARITY_DESC"): Result<List<MediaSummary>> = runCatching {
        val gql = """
            query (${'$'}type: MediaType, ${'$'}sort: MediaSort) {
                Page(page: 1, perPage: 20) {
                    media(type: ${'$'}type, sort: [${'$'}sort]) {
                        id title { romaji english native }
                        coverImage { extraLarge large medium color } bannerImage
                        format type episodes chapters volumes averageScore meanScore popularity
                        genres season seasonYear status
                        nextAiringEpisode { id episode airingAt }
                    }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject {
            put("type", type)
            put("sort", sort)
        })
        json.decodeFromString<SearchResponse>(body).data?.Page?.media ?: emptyList()
    }

    suspend fun seasonal(type: String = "ANIME", season: String, seasonYear: Int): Result<List<MediaSummary>> = runCatching {
        val gql = """
            query (${'$'}type: MediaType, ${'$'}season: MediaSeason, ${'$'}seasonYear: Int) {
                Page(page: 1, perPage: 20) {
                    media(type: ${'$'}type, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: POPULARITY_DESC) {
                        id title { romaji english native }
                        coverImage { extraLarge large medium color } bannerImage
                        format type episodes chapters volumes averageScore meanScore popularity
                        genres season seasonYear status
                    }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject {
            put("type", type)
            put("season", season)
            put("seasonYear", seasonYear)
        })
        json.decodeFromString<SearchResponse>(body).data?.Page?.media ?: emptyList()
    }

    suspend fun airingSchedules(): Result<List<AiringSchedule>> = runCatching {
        val gql = """
            query {
                Page(page: 1, perPage: 50) {
                    airingSchedules(notYetAired: true, sort: TIME) {
                        id episode airingAt
                        media { id title { romaji english } coverImage { large } format }
                    }
                }
            }
        """.trimIndent()
        val body = post(gql)
        json.decodeFromString<AiringResponse>(body).data?.Page?.airingSchedules ?: emptyList()
    }

    suspend fun mediaById(id: Int): Result<MediaDetail> = runCatching {
        val gql = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id) {
                    id title { romaji english native }
                    coverImage { extraLarge large medium color } bannerImage
                    description format type status
                    episodes chapters volumes duration season seasonYear
                    averageScore meanScore popularity genres source countryOfOrigin isAdult siteUrl
                    startDate { year month day } endDate { year month day }
                    nextAiringEpisode { id episode airingAt timeUntilAiring }
                    studios(isMain: true) { nodes { id name } }
                    staff(sort: RELEVANCE, perPage: 8) { edges { role node { id name { full } } } }
                    characters(sort: ROLE, perPage: 12) { edges { role node { id name { full } image { large } } } }
                    relations { edges { relationType node { id title { romaji english } coverImage { large } format type episodes chapters averageScore } } }
                    recommendations(sort: RATING_DESC, perPage: 12) { nodes { id title { romaji english } coverImage { large } format type averageScore } }
                    streamingEpisodes { title thumbnail url site }
                    externalLinks { url site }
                    trailer { id site thumbnail }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject { put("id", id) })
        val media = json.decodeFromString<MediaByIdResponse>(body).data?.Media
            ?: throw IOException("Media $id not found")
        media
    }

    // --- Authenticated ---

    suspend fun viewer(token: String): Result<ViewerDto> = runCatching {
        val gql = """
            query {
                Viewer {
                    id name about bannerImage
                    avatar { large medium }
                    statistics {
                        anime {
                            count meanScore minutesWatched episodesWatched
                            statuses { status count }
                        }
                        manga {
                            count meanScore chaptersRead volumesRead
                            statuses { status count }
                        }
                    }
                }
            }
        """.trimIndent()
        val body = post(gql, token = token)
        json.decodeFromString<ViewerResponse>(body).data?.Viewer
            ?: throw IOException("Could not load AniList account")
    }

    suspend fun mediaListCollection(userId: Int, token: String): Result<List<MediaListEntryDto>> = runCatching {
        val gql = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: null) {
                    lists {
                        entries {
                            id mediaId status progress progressVolumes score repeat notes
                            startedAt { year month day } completedAt { year month day }
                            media {
                                id title { romaji english native }
                                coverImage { extraLarge large medium } bannerImage
                                format type episodes chapters volumes averageScore meanScore popularity
                                genres season seasonYear status description
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject { put("userId", userId) }, token)
        val collection = json.decodeFromString<MediaListResponse>(body).data?.MediaListCollection
        collection?.lists?.flatMap { it.entries } ?: emptyList()
    }

    suspend fun saveMediaListEntry(
        token: String,
        mediaId: Int,
        status: String? = null,
        progress: Int? = null,
        progressVolumes: Int? = null,
        score: Double? = null,
        notes: String? = null,
        repeat: Int? = null
    ): Result<MediaListEntryDto> = runCatching {
        val gql = """
            mutation (
                ${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int,
                ${'$'}progressVolumes: Int, ${'$'}score: Float, ${'$'}notes: String, ${'$'}repeat: Int
            ) {
                SaveMediaListEntry(
                    mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress,
                    progressVolumes: ${'$'}progressVolumes, score: ${'$'}score, notes: ${'$'}notes, repeat: ${'$'}repeat
                ) {
                    id mediaId status progress progressVolumes score repeat notes
                    startedAt { year month day } completedAt { year month day }
                }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject {
            put("mediaId", mediaId)
            put("status", status)
            put("progress", progress)
            put("progressVolumes", progressVolumes)
            put("score", score)
            put("notes", notes)
            put("repeat", repeat)
        }, token)
        json.decodeFromString<SaveEntryResponse>(body).data?.SaveMediaListEntry
            ?: throw IOException("Could not save entry to AniList")
    }

    suspend fun deleteMediaListEntry(token: String, listEntryId: Int): Result<Boolean> = runCatching {
        val gql = """
            mutation (${'$'}id: Int) {
                DeleteMediaListEntry(id: ${'$'}id) { deleted }
            }
        """.trimIndent()
        val body = post(gql, buildJsonObject { put("id", listEntryId) }, token)
        json.decodeFromString<DeleteEntryResponse>(body).data?.DeleteMediaListEntry?.deleted ?: false
    }
}
