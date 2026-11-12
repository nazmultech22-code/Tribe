package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun enqueue(operation: SyncOperationEntity): Long

  @Query("SELECT * FROM sync_operations WHERE state = 'pending' ORDER BY createdAt ASC LIMIT :limit")
  suspend fun getPending(limit: Int = 50): List<SyncOperationEntity>

  @Query("SELECT COUNT(*) FROM sync_operations WHERE state = 'pending'")
  fun observePendingCount(): Flow<Int>

  @Query("UPDATE sync_operations SET attemptCount = attemptCount + 1, last_error = :error WHERE id = :id")
  suspend fun recordFailure(id: String, error: String?)

  @Query("UPDATE sync_operations SET state = 'completed', last_error = NULL WHERE id = :id")
  suspend fun markCompleted(id: String)

  @Query("UPDATE sync_operations SET state = 'pending', last_error = :error WHERE id = :id")
  suspend fun retry(id: String, error: String?)

  @Query("DELETE FROM sync_operations WHERE state = 'completed' AND createdAt < :olderThan")
  suspend fun pruneCompleted(olderThan: Long): Int
}
