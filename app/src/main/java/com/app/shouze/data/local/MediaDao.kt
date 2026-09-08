package com.app.shouze.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY lastUpdated DESC")
    fun getAllItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE categoryId = :categoryId ORDER BY lastUpdated DESC")
    fun getItemsByCategory(categoryId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllItemsSnapshot(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE id = :itemId LIMIT 1")
    suspend fun getById(itemId: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE source = 'ANILIST' AND anilistId = :anilistId LIMIT 1")
    suspend fun findByAnilistId(anilistId: Int): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE source = 'ANILIST' AND mediaType = :mediaType")
    suspend fun getAniListItemsByType(mediaType: String): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE media_items SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun clearPendingSync(ids: List<String>)

    @Query("SELECT COUNT(*) FROM media_items WHERE pendingSync = 1")
    fun pendingSyncCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE source = 'ANILIST'")
    fun aniListItemCount(): Flow<Int>

    @Query("DELETE FROM media_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String): Int

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
