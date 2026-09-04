package com.app.shouze.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_entries ORDER BY lastUpdated DESC")
    fun observeAll(): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entries")
    suspend fun getAllSnapshot(): List<LibraryEntryEntity>

    @Query("SELECT * FROM library_entries WHERE mediaId = :mediaId LIMIT 1")
    suspend fun findByMediaId(mediaId: Int): LibraryEntryEntity?

    @Query("SELECT * FROM library_entries WHERE localId = :localId LIMIT 1")
    suspend fun findByLocalId(localId: String): LibraryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LibraryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<LibraryEntryEntity>)

    @Query("DELETE FROM library_entries WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    @Query("DELETE FROM library_entries WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Int)

    @Query("UPDATE library_entries SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: String)

    @Query("DELETE FROM library_entries")
    suspend fun clearAll()
}
