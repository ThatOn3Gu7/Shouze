package com.app.shouze.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A queued write against the AniList API.
 *
 * Edits made while offline (or while AniList is unreachable) land here first so the
 * UI can stay optimistic; [com.app.shouze.data.sync.AniListLibraryRepository] drains
 * the queue oldest-first once connectivity returns, respecting the API rate limit.
 */
@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val op: OutboxOp,
    /** Local MediaItemEntity id this op was created from (for reconciliation). */
    val localItemId: String,
    /** Serialized [SaveEntryPayload] for SAVE ops, unused for DELETE. */
    val payloadJson: String,
    val attempts: Int = 0,
    val lastError: String? = null
)

enum class OutboxOp { SAVE, DELETE }

/**
 * Payload for a SaveMediaListEntry mutation. Kept explicit (rather than serializing the
 * entity) so an outbox row survives schema evolution of MediaItemEntity.
 */
@Serializable
data class SaveEntryPayload(
    val mediaId: Int,
    val status: String,
    val score: Double? = null,
    val progress: Int? = null,
    val progressVolumes: Int? = null,
    val notes: String? = null,
    val repeat: Int? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)

object OutboxPayloads {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(payload: SaveEntryPayload): String = json.encodeToString(SaveEntryPayload.serializer(), payload)

    fun decode(raw: String): SaveEntryPayload? = runCatching {
        json.decodeFromString(SaveEntryPayload.serializer(), raw)
    }.getOrNull()
}
