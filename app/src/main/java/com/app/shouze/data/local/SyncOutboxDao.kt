package com.app.shouze.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox ORDER BY seq ASC")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun count(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncOutboxEntity): Long

    @Query("UPDATE sync_outbox SET attempts = attempts + 1, lastError = :error WHERE seq = :seq")
    suspend fun markFailed(seq: Long, error: String?)

    @Query("DELETE FROM sync_outbox WHERE seq = :seq")
    suspend fun delete(seq: Long)

    @Query("DELETE FROM sync_outbox WHERE localItemId IN (:localItemIds)")
    suspend fun deleteForItems(localItemIds: List<String>)

    @Query("DELETE FROM sync_outbox")
    suspend fun clear()
}
