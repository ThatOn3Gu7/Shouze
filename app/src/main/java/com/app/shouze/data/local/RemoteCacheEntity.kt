package com.app.shouze.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Tiny read-through cache for public AniList payloads (trending, airing schedule,
 * search pages). Lets discovery screens stay browsable offline and cuts request
 * volume against the strict AniList rate limit.
 */
@Entity(tableName = "remote_cache")
data class RemoteCacheEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Dao
interface RemoteCacheDao {

    @Query("SELECT * FROM remote_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): RemoteCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: RemoteCacheEntity)

    @Query("DELETE FROM remote_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM remote_cache WHERE fetchedAt < :olderThan")
    suspend fun pruneOlderThan(olderThan: Long)
}

/** Standard cache keys used across the app. */
object RemoteCacheKeys {
    const val TRENDING_PREFIX = "trending_"
    const val AIRING_SCHEDULE = "airing_schedule"
    const val SEARCH_PREFIX = "search_"

    fun search(query: String, type: String, page: Int): String =
        "${SEARCH_PREFIX}${type.lowercase()}_${page}_${query.trim().lowercase()}"
}
