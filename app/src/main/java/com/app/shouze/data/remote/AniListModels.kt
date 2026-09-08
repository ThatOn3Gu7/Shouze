package com.app.shouze.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
) {
    /** Best user-facing title: english -> romaji -> native. */
    val display: String?
        get() = english?.ifBlank { null } ?: romaji?.ifBlank { null } ?: native?.ifBlank { null }
}

@Serializable
data class AniListCoverImage(
    val large: String? = null,
    val medium: String? = null,
    val extraLarge: String? = null
)

/** FuzzyDate as returned by AniList ({year, month, day}; all nullable). */
@Serializable
data class AniListFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

// ---------------------------------------------------------------------------
// Media (search / trending / airing / detail)
// ---------------------------------------------------------------------------

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
    val media: List<AniListMedia> = emptyList(),
    val pageInfo: AniListPageInfo? = null
)

@Serializable
data class AniListPageInfo(
    val total: Int? = null,
    val currentPage: Int? = null,
    val lastPage: Int? = null,
    val hasNextPage: Boolean? = null
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
    val format: String? = null,
    val type: String? = null,
    val bannerImage: String? = null,
    val siteUrl: String? = null,
    /** The authenticated user's list entry for this media (null when not tracked). */
    val mediaListEntry: AniListListEntry? = null
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

// ---------------------------------------------------------------------------
// Viewer (authenticated user)
// ---------------------------------------------------------------------------

@Serializable
data class AniListViewerResponse(
    val data: AniListViewerData? = null
)

@Serializable
data class AniListViewerData(
    val Viewer: AniListViewer? = null
)

@Serializable
data class AniListViewer(
    val id: Int,
    val name: String,
    val avatar: AniListAvatar? = null,
    val bannerImage: String? = null,
    val siteUrl: String? = null,
    val mediaListOptions: AniListMediaListOptions? = null
)

@Serializable
data class AniListAvatar(
    val large: String? = null,
    val medium: String? = null
)

@Serializable
data class AniListMediaListOptions(
    /** The user's preferred score format — needed to convert scores faithfully. */
    val scoreFormat: String? = null
)

// ---------------------------------------------------------------------------
// Media list collection (the user's library)
// ---------------------------------------------------------------------------

@Serializable
data class AniListMediaListCollectionResponse(
    val data: AniListMediaListCollectionData? = null
)

@Serializable
data class AniListMediaListCollectionData(
    val MediaListCollection: AniListMediaListCollection? = null
)

@Serializable
data class AniListMediaListCollection(
    val lists: List<AniListListGroup> = emptyList(),
    val user: AniListCollectionUser? = null
)

@Serializable
data class AniListCollectionUser(
    val id: Int,
    val name: String,
    val avatar: AniListAvatar? = null
)

@Serializable
data class AniListListGroup(
    val name: String,
    /** WATCHING, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING */
    val status: String? = null,
    val isCustomList: Boolean = false,
    val entries: List<AniListListEntry> = emptyList()
)

/**
 * A single tracking row. `media` is populated by list queries; when this object
 * appears as `mediaListEntry` on [AniListMedia] only the scalar fields are present.
 */
@Serializable
data class AniListListEntry(
    /** Defaults keep partial GraphQL selections (e.g. search's mediaListEntry)
     *  decodable — the mapper keys entities off [media].id anyway. */
    val id: Int = 0,
    val mediaId: Int = 0,
    val status: String? = null,
    val score: Double? = null,
    val progress: Int? = null,
    val progressVolumes: Int? = null,
    val repeat: Int? = null,
    val notes: String? = null,
    val startedAt: AniListFuzzyDate? = null,
    val completedAt: AniListFuzzyDate? = null,
    val updatedAt: Int? = null,
    val media: AniListMedia? = null
)

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

@Serializable
data class AniListSaveEntryResponse(
    val data: AniListSaveEntryData? = null
)

@Serializable
data class AniListSaveEntryData(
    val SaveMediaListEntry: AniListListEntry? = null
)

@Serializable
data class AniListDeleteEntryResponse(
    val data: AniListDeleteEntryData? = null
)

@Serializable
data class AniListDeleteEntryData(
    val DeleteMediaListEntry: AniListDeleted? = null
)

@Serializable
data class AniListDeleted(
    val deleted: Boolean? = null
)

// ---------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------

@Serializable
data class AniListErrorEnvelope(
    val errors: List<AniListError> = emptyList()
)

@Serializable
data class AniListError(
    val message: String? = null,
    val status: Int? = null
)
