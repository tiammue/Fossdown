package com.fossdown.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ProgressEvent>>

    @Query("SELECT * FROM events WHERE archived = 0 ORDER BY createdAt DESC")
    suspend fun getActive(): List<ProgressEvent>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProgressEvent?

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProgressEvent?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: ProgressEvent): Long

    @Update
    suspend fun update(event: ProgressEvent)

    @Delete
    suspend fun delete(event: ProgressEvent)

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int
}
