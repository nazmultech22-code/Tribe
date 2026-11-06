package com.example.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTripDao {

  @Query("SELECT * FROM saved_trips ORDER BY savedAt DESC")
  fun getAllSavedTrips(): Flow<List<SavedTripEntity>>

  @Query("SELECT * FROM saved_trips WHERE status = :status ORDER BY savedAt DESC")
  fun getSavedTripsByStatus(status: String): Flow<List<SavedTripEntity>>

  @Query("SELECT * FROM saved_trips WHERE id = :id LIMIT 1")
  suspend fun getTripById(id: String): SavedTripEntity?

  @Query("SELECT EXISTS(SELECT 1 FROM saved_trips WHERE id = :id)")
  fun isTripSaved(id: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrip(trip: SavedTripEntity)

  @Query("DELETE FROM saved_trips WHERE id = :id")
  suspend fun deleteTripById(id: String)

  @Query("SELECT COUNT(*) FROM saved_trips")
  suspend fun getCount(): Int

  @Query("DELETE FROM saved_trips")
  suspend fun deleteAll()
}
