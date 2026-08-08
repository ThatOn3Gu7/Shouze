package com.example.crossmediatracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO providing reactive queries and atomic CRUD operations.
 */
@Dao
interface MediaDao {

    /** Observe the full list, ordered by last update descending. */
    @Query("SELECT * FROM media_items ORDER BY lastUpdated DESC")
    fun getAllItems(): Flow<List<MediaItemEntity>>

    /** Single‑shot snapshot for JSON backups. Must be called inside a transaction. */
    @Query("SELECT * FROM media_items")
    suspend fun getAllItemsSnapshot(): List<MediaItemEntity>

    /** Insert a new item or replace an existing one (UPSERT). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemEntity)

    /** Delete by ID; returns number of deleted rows. */
    @Query("DELETE FROM media_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String): Int

    /** Clear the entire table (used during restore). */
    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}