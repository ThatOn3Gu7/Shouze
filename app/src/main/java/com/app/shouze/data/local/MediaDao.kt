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

    @Query("DELETE FROM media_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String): Int

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
