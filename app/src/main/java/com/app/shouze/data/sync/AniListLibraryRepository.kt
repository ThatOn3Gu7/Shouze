package com.app.shouze.data.sync

import androidx.room.withTransaction
import com.app.shouze.data.auth.AniListAuthRepository
import com.app.shouze.data.local.AppDatabase
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.OutboxOp
import com.app.shouze.data.local.OutboxPayloads
import com.app.shouze.data.local.RemoteCacheDao
import com.app.shouze.data.local.RemoteCacheEntity
import com.app.shouze.data.local.SyncOutboxDao
import com.app.shouze.data.local.SyncOutboxEntity
import com.app.shouze.data.mapper.AniListMapper
import com.app.shouze.data.remote.AniListApi
import com.app.shouze.data.remote.AniListException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Keeps the on-device library and the user's AniList library in step.
 *
 * Model:
 *  - AniList is the source of truth for entries with source = ANILIST. A refresh
 *    pulls both collections (ANIME + MANGA), upserts them into Room and prunes
 *    entries the user removed on AniList.
 *  - Edits are applied locally first (optimistic) and queued in a durable outbox.
 *    A single drain loop replays them oldest-first while online, so the app stays
 *    fully usable offline and converges once connectivity returns.
 *  - Unsent saves for the same item coalesce, so rapid progress taps don't burn
 *    rate-limit quota.
 *  - Conflicts resolve last-writer-wins on the server: queued local changes are
 *    always pushed before a pull, so user intent is never silently overwritten.
 */
class AniListLibraryRepository(
    private val db: AppDatabase,
    private val api: AniListApi,
    private val auth: AniListAuthRepository
) {

    private val mediaDao = db.mediaDao()
    private val categoryDao = db.categoryDao()
    private val outboxDao: SyncOutboxDao = db.syncOutboxDao()
    private val cacheDao: RemoteCacheDao = db.remoteCacheDao()

    private val syncMutex = Mutex()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    data class SyncStatus(
        val isSyncing: Boolean = false,
        val pendingOps: Int = 0,
        val lastSyncAt: Long = 0L,
        val lastError: String? = null,
        val isStaleSession: Boolean = false,
        /** Last successful delivery of local changes to AniList (not a plain pull). */
        val lastDeliveredAt: Long = 0L,
        val lastDeliveredCount: Int = 0
    )

    // ------------------------------------------------------------------
    // Reads for the UI layer
    // ------------------------------------------------------------------

    suspend fun pendingOpsSnapshot(): Int = outboxDao.getAll().size

    // ------------------------------------------------------------------
    // Writes: optimistic save / delete with durable queueing
    // ------------------------------------------------------------------

    /**
     * Applies [item] locally immediately (so the UI updates instantly) and queues
     * the change for AniList. Returns the locally-applied entity.
     */
    suspend fun applyEntryEdit(item: MediaItemEntity): MediaItemEntity {
        val updated = item.copy(
            pendingSync = true,
            lastUpdated = System.currentTimeMillis()
        )
        mediaDao.insertOrUpdate(updated)
        enqueueSave(updated)
        return updated
    }

    /**
     * Deletes an AniList-backed entry locally and queues the server deletion.
     */
    suspend fun applyEntryDelete(item: MediaItemEntity) {
        mediaDao.deleteById(item.id)
        val entryId = item.listEntryId
        if (entryId != null) {
            outboxDao.insert(
                SyncOutboxEntity(
                    op = OutboxOp.DELETE,
                    localItemId = item.id,
                    payloadJson = entryId.toString()
                )
            )
        }
        refreshPendingCount()
    }

    private suspend fun enqueueSave(item: MediaItemEntity) {
        val mediaId = item.anilistId ?: return
        val scoreFormat = auth.state.value.session?.scoreFormat ?: "POINT_100"
        val payload = AniListMapper.entityToSavePayload(item.copy(anilistId = mediaId), scoreFormat)
        // Coalesce: replace any still-queued save for this item so bursts of
        // incremental progress taps become exactly one API call.
        outboxDao.deleteForItems(listOf(item.id))
        outboxDao.insert(
            SyncOutboxEntity(
                op = OutboxOp.SAVE,
                localItemId = item.id,
                payloadJson = OutboxPayloads.encode(payload)
            )
        )
        refreshPendingCount()
    }

    // ------------------------------------------------------------------
    // Outbox drain
    // ------------------------------------------------------------------

    /**
     * Replays queued ops oldest-first. Stops at the first op that cannot be
     * delivered right now (offline / rate limited / unauthorized) and resumes
     * from there on the next drain.
     */
    /** Drains the queue and returns how many changes actually reached AniList. */
    suspend fun flushOutbox(): Int = syncMutex.withLock {
        drainOutboxLocked()
    }

    private suspend fun drainOutboxLocked(): Int {
        val ops = outboxDao.getAll()
        if (ops.isEmpty()) {
            setPendingCount(0)
            return 0
        }
        setSyncing(true)
        var delivered = 0
        var hardError: String? = null

        for (op in ops) {
            try {
                when (op.op) {
                    OutboxOp.SAVE -> {
                        val payload = OutboxPayloads.decode(op.payloadJson)
                            ?: throw AniListException("Corrupted queued change", AniListException.Kind.GRAPHQL)
                        val saved = api.saveMediaListEntry(payload).getOrThrow()
                        db.withTransaction {
                            val local = mediaDao.getById(op.localItemId)
                            if (local != null && local.isAniListBacked) {
                                mediaDao.insertOrUpdate(
                                    local.copy(
                                        listEntryId = saved.id,
                                        pendingSync = false,
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                )
                            }
                            outboxDao.delete(op.seq)
                            delivered++
                        }
                    }
                    OutboxOp.DELETE -> {
                        val entryId = op.payloadJson.toIntOrNull()
                        if (entryId != null) {
                            api.deleteMediaListEntry(entryId).getOrThrow()
                        }
                        outboxDao.delete(op.seq)
                        delivered++
                    }
                }
            } catch (e: Throwable) {
                val kind = (e as? AniListException)?.kind ?: classify(e)
                when (kind) {
                    AniListException.Kind.UNAUTHORIZED -> {
                        // Session expired/revoked: keep the queue, wait for re-login.
                        hardError = "AniList session expired — please sign in again to sync your changes."
                        _syncStatus.update { it.copy(isStaleSession = true) }
                        break
                    }
                    AniListException.Kind.NETWORK, AniListException.Kind.RATE_LIMITED -> {
                        outboxDao.markFailed(op.seq, e.message)
                        hardError = "Offline — ${ops.size} change(s) will sync when you're back online."
                        break
                    }
                    else -> {
                        // Permanent-looking failure: retry a few times, then drop so the
                        // queue can't clog forever on an unrecoverable op.
                        outboxDao.markFailed(op.seq, e.message)
                        if (op.attempts + 1 >= MAX_ATTEMPTS) {
                            // Give up on delivering this op, but keep the row flagged
                            // pending: the user's edit must neither vanish nor be
                            // clobbered by the next server pull. Editing the item
                            // again re-queues a fresh attempt; signing out/in
                            // restores the server value.
                            outboxDao.delete(op.seq)
                            val failedItem = mediaDao.getById(op.localItemId)
                            hardError = "A change to \"${failedItem?.title ?: "an item"}\" couldn't be synced: ${e.message}"
                        }
                    }
                }
            }
        }

        setSyncing(false)
        if (delivered > 0) {
            // Remember the delivery so the profile card can say
            // "Synced N change(s) · just now" even after an app restart.
            val now = System.currentTimeMillis()
            cacheDao.put(RemoteCacheEntity(LAST_DELIVERED_AT_KEY, now.toString()))
            cacheDao.put(RemoteCacheEntity(LAST_DELIVERED_COUNT_KEY, delivered.toString()))
        }
        if (hardError != null) {
            _syncStatus.update { it.copy(lastError = hardError) }
        } else if (delivered > 0) {
            // Everything queued made it to AniList: clear any stale failure
            // banner and surface the delivery.
            _syncStatus.update {
                it.copy(
                    lastError = null,
                    isStaleSession = false,
                    lastDeliveredAt = System.currentTimeMillis(),
                    lastDeliveredCount = delivered
                )
            }
        }
        refreshPendingCount()
        return delivered
    }

    // ------------------------------------------------------------------
    // Library pull
    // ------------------------------------------------------------------

    /**
     * Flushes pending edits (so they win over server state), then pulls both
     * collections and reconciles Room. [force] bypasses the staleness window.
     */
    suspend fun refreshLibrary(force: Boolean = false): Result<Unit> = syncMutex.withLock {
        val session = auth.state.value.session
        if (session == null || session.userId == 0) {
            Result.failure(AniListException("Not signed in.", AniListException.Kind.UNAUTHORIZED))
        } else {
            // Local edits always win: push them before pulling server state.
            drainOutboxLocked()

            val lastSync = lastSyncAt()
            if (!force && System.currentTimeMillis() - lastSync < STALE_AFTER_MS) {
                Result.success(Unit)
            } else {
                setSyncing(true)
                val result = pullBothTypes(session.userId, session.scoreFormat)
                setSyncing(false)
                result.fold(
                    onSuccess = {
                        cacheDao.put(RemoteCacheEntity(LAST_SYNC_KEY, System.currentTimeMillis().toString()))
                        _syncStatus.update {
                            it.copy(lastSyncAt = System.currentTimeMillis(), lastError = null, isStaleSession = false)
                        }
                    },
                    onFailure = { e ->
                        val kind = (e as? AniListException)?.kind
                        val message = when (kind) {
                            AniListException.Kind.NETWORK -> "Offline — showing your cached library."
                            AniListException.Kind.RATE_LIMITED -> "AniList rate limit hit — retrying soon."
                            AniListException.Kind.UNAUTHORIZED -> "AniList session expired — please sign in again."
                            else -> "Sync failed: ${e.message}"
                        }
                        _syncStatus.update {
                            it.copy(lastError = message, isStaleSession = kind == AniListException.Kind.UNAUTHORIZED)
                        }
                    }
                )
                result
            }
        }
    }

    private suspend fun pullBothTypes(userId: Int, scoreFormat: String): Result<Unit> {
        return try {
            val categories = ensureDefaultCategories()
            var failures = 0
            for (type in listOf("ANIME", "MANGA")) {
                val collectionResult = api.getMediaListCollection(userId, type)
                collectionResult.fold(
                    onSuccess = { collection ->
                        val entities = AniListMapper.collectionToEntities(collection, scoreFormat, categories)
                        reconcileType(type, entities)
                    },
                    onFailure = { failures++ }
                )
            }
            if (failures == 2) {
                Result.failure(AniListException("Couldn't reach AniList.", AniListException.Kind.NETWORK))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upserts fresh entities and prunes cached rows the user removed on AniList.
     * Rows with a still-unsynced local edit (pendingSync) are never overwritten:
     * the user's change wins until it has been delivered to AniList.
     */
    private suspend fun reconcileType(mediaType: String, entities: List<MediaItemEntity>) {
        db.withTransaction {
            val pendingIds = mediaDao.getAniListItemsByType(mediaType)
                .filter { it.pendingSync }
                .map { it.id }
                .toSet()
            mediaDao.insertAll(if (pendingIds.isEmpty()) entities else entities.filter { it.id !in pendingIds })
            val freshIds = entities.map { it.id }.toSet()
            val stale = mediaDao.getAniListItemsByType(mediaType)
                .filter { it.id !in freshIds && !it.pendingSync }
            if (stale.isNotEmpty()) {
                mediaDao.deleteByIds(stale.map { it.id })
                outboxDao.deleteForItems(stale.map { it.id })
            }
        }
    }

    suspend fun ensureDefaultCategories(): List<CategoryEntity> {
        val existing = categoryDao.getAllSnapshot()
        val hasAnime = existing.any { it.name.equals("Anime", ignoreCase = true) }
        val hasManga = existing.any { it.name.equals("Manga", ignoreCase = true) }
        if (!hasAnime) categoryDao.insert(CategoryEntity(name = "Anime"))
        if (!hasManga) categoryDao.insert(CategoryEntity(name = "Manga"))
        return if (!hasAnime || !hasManga) categoryDao.getAllSnapshot() else existing
    }

    /** Queued ops can't survive a logout: they'd apply to whatever account signs in next. */
    suspend fun onSignedOut() {
        syncMutex.withLock {
            outboxDao.clear()
            val pending = mediaDao.getAllItemsSnapshot().filter { it.pendingSync }
            pending.forEach { mediaDao.insertOrUpdate(it.copy(pendingSync = false)) }
            _syncStatus.value = SyncStatus()
        }
    }

    // ------------------------------------------------------------------
    // Discovery cache (offline-friendly trending / airing / search)
    // ------------------------------------------------------------------

    suspend fun cachedJson(key: String): String? = cacheDao.get(key)?.json

    suspend fun cacheJson(key: String, json: String) {
        cacheDao.put(RemoteCacheEntity(key, json))
    }

    suspend fun lastSyncAt(): Long = cacheDao.get(LAST_SYNC_KEY)?.json?.toLongOrNull() ?: 0L

    suspend fun primeSyncStatus() {
        val pending = pendingOpsSnapshot()
        val last = lastSyncAt()
        val deliveredAt = cacheDao.get(LAST_DELIVERED_AT_KEY)?.json?.toLongOrNull() ?: 0L
        val deliveredCount = cacheDao.get(LAST_DELIVERED_COUNT_KEY)?.json?.toIntOrNull() ?: 0
        _syncStatus.update {
            it.copy(
                pendingOps = pending,
                lastSyncAt = last,
                lastDeliveredAt = deliveredAt,
                lastDeliveredCount = deliveredCount
            )
        }
    }

    // ------------------------------------------------------------------

    private suspend fun refreshPendingCount() {
        setPendingCount(outboxDao.getAll().size)
    }

    private suspend fun setPendingCount(count: Int) {
        _syncStatus.update { it.copy(pendingOps = count) }
    }

    private suspend fun setSyncing(active: Boolean) {
        _syncStatus.update { it.copy(isSyncing = active) }
    }

    private fun classify(e: Throwable): AniListException.Kind = when (e) {
        is AniListException -> e.kind
        is IOException -> AniListException.Kind.NETWORK
        else -> AniListException.Kind.GRAPHQL
    }

    companion object {
        private const val LAST_SYNC_KEY = "anilist_last_sync_at"
        private const val LAST_DELIVERED_AT_KEY = "last_delivered_at"
        private const val LAST_DELIVERED_COUNT_KEY = "last_delivered_count"
        private const val STALE_AFTER_MS = 30L * 60 * 1000 // 30 minutes
        private const val MAX_ATTEMPTS = 5
    }
}
