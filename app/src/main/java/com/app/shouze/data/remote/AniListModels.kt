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
    val mediaListEntry: AniListListEntry? = null,
    // ---- Full-detail fields (only selected by getMediaDetail; default null keeps
    // ---- search/trending/library responses decodable with the light selection). ----
    val duration: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: AniListFuzzyDate? = null,
    val endDate: AniListFuzzyDate? = null,
    val synonyms: List<String>? = null,
    val countryOfOrigin: String? = null,
    /** ORIGINAL, MANGA, LIGHT_NOVEL, ... */
    val source: String? = null,
    val favourites: Int? = null,
    val studios: AniListStudioConnection? = null,
    val staff: AniListStaffConnection? = null,
    val characters: AniListCharacterConnection? = null,
    val tags: List<AniListMediaTag>? = null,
    val trailer: AniListTrailer? = null,
    val nextAiringEpisode: AniListNextAiringEpisode? = null,
    val relations: AniListRelationConnection? = null,
    val recommendations: AniListRecommendationConnection? = null,
    val rankings: List<AniListRanking>? = null,
    val statistics: AniListStatistics? = null,
    val streamingEpisodes: List<StreamingEpisode>? = null,
    val externalLinks: List<ExternalLink>? = null
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
    val site: String,
    /** STREAMING or INFO (only selected by the detail query). */
    val type: String? = null,
    /** ISO language code of the link, e.g. JP / EN. */
    val language: String? = null
)

// ---------------------------------------------------------------------------
// Full-detail media sub-objects
// ---------------------------------------------------------------------------

@Serializable
data class AniListStudioConnection(
    val edges: List<AniListStudioEdge> = emptyList()
)

@Serializable
data class AniListStudioEdge(
    val isMainStudio: Boolean? = null,
    val node: AniListStudio? = null
)

@Serializable
data class AniListStudio(
    val name: String? = null
)

// ---------------------------------------------------------------------------
// Detail-tab models (staff / characters / relations / stats / social)
// ---------------------------------------------------------------------------

@Serializable
data class AniListStaffResponse(
    val data: AniListStaffData? = null
)

@Serializable
data class AniListStaffData(
    val Media: AniListMedia? = null
)

@Serializable
data class AniListCharacterConnection(
    val edges: List<AniListCharacterEdge> = emptyList()
)

@Serializable
data class AniListCharacterEdge(
    /** MAIN, SUPPORTING, BACKGROUND */
    val role: String? = null,
    val node: AniListCharacter? = null,
    val voiceActors: List<AniListStaff> = emptyList()
)

@Serializable
data class AniListCharacter(
    val name: AniListStaffName? = null,
    val image: AniListCoverImage? = null
)

@Serializable
data class AniListRelationResponse(
    val data: AniListRelationData? = null
)

@Serializable
data class AniListRelationData(
    val Media: AniListMedia? = null
)

@Serializable
data class AniListRecommendationConnection(
    val edges: List<AniListRecommendationEdge> = emptyList()
)

@Serializable
data class AniListRecommendationEdge(
    val rating: Int? = null,
    val node: AniListRecommendation? = null
)

@Serializable
data class AniListRecommendation(
    val mediaRecommendation: AniListMedia? = null
)

@Serializable
data class AniListStatsResponse(
    val data: AniListStatsData? = null
)

@Serializable
data class AniListStatsData(
    val Media: AniListMedia? = null
)

@Serializable
data class AniListRanking(
    val rank: Int? = null,
    /** RATED or POPULAR */
    val type: String? = null,
    /** ALL_TIME, YEAR, ... */
    val context: String? = null,
    val year: Int? = null,
    val allTime: Boolean? = null
)

@Serializable
data class AniListStatistics(
    val statusDistribution: AniListStatusDistribution? = null,
    val scoreDistribution: List<AniListScoreCount> = emptyList()
)

@Serializable
data class AniListStatusDistribution(
    val statuses: List<AniListStatusCount> = emptyList()
)

@Serializable
data class AniListStatusCount(
    /** CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING */
    val status: String? = null,
    val amount: Int? = null
)

@Serializable
data class AniListScoreCount(
    /** 0-100 in steps of 10 */
    val score: Int? = null,
    val amount: Int? = null
)

@Serializable
data class AniListSocialResponse(
    val data: AniListSocialData? = null
)

@Serializable
data class AniListSocialData(
    val Media: AniListSocialMedia? = null,
    val Page: AniListSocialPage? = null
)

@Serializable
data class AniListSocialMedia(
    val reviews: AniListReviewConnection? = null
)

@Serializable
data class AniListReviewConnection(
    val nodes: List<AniListReview> = emptyList()
)

@Serializable
data class AniListReview(
    val summary: String? = null,
    /** 0-100 */
    val score: Int? = null,
    val rating: Int? = null,
    val user: AniListSocialUser? = null
)

@Serializable
data class AniListSocialPage(
    val threads: List<AniListThread> = emptyList(),
    val activities: List<AniListActivity> = emptyList()
)

@Serializable
data class AniListThread(
    val title: String? = null,
    val replyCount: Int? = null,
    val viewCount: Int? = null,
    val user: AniListSocialUser? = null
)

@Serializable
data class AniListActivity(
    val createdAt: Int? = null,
    /** e.g. "watched episode 5" / "read chapter 12" */
    val progress: String? = null,
    val user: AniListSocialUser? = null
)

@Serializable
data class AniListSocialUser(
    val name: String? = null,
    val avatar: AniListAvatar? = null
)

@Serializable
data class AniListStaffConnection(
    val edges: List<AniListStaffEdge> = emptyList()
)

@Serializable
data class AniListStaffEdge(
    val role: String? = null,
    val node: AniListStaff? = null
)

@Serializable
data class AniListStaff(
    val name: AniListStaffName? = null,
    val image: AniListCoverImage? = null
)

@Serializable
data class AniListStaffName(
    val full: String? = null
)

@Serializable
data class AniListMediaTag(
    val name: String? = null,
    val rank: Int? = null,
    val isMediaSpoiler: Boolean? = null
)

@Serializable
data class AniListTrailer(
    val id: String? = null,
    val site: String? = null
)

@Serializable
data class AniListNextAiringEpisode(
    val airingAt: Int? = null,
    val timeUntilAiring: Int? = null,
    val episode: Int? = null
)

@Serializable
data class AniListRelationConnection(
    val edges: List<AniListRelationEdge> = emptyList()
)

@Serializable
data class AniListRelationEdge(
    val relationType: String? = null,
    val node: AniListMedia? = null
)

@Serializable
data class AniListMediaSingleResponse(
    val data: AniListMediaSingleData? = null
)

@Serializable
data class AniListMediaSingleData(
    val Media: AniListMedia? = null
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
