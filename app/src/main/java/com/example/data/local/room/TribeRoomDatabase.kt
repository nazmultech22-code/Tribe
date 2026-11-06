package com.example.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedTripEntity::class], version = 1, exportSchema = false)
abstract class TribeRoomDatabase : RoomDatabase() {

  abstract fun savedTripDao(): SavedTripDao

  companion object {
    @Volatile
    private var INSTANCE: TribeRoomDatabase? = null

    fun getDatabase(context: Context): TribeRoomDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          TribeRoomDatabase::class.java,
          "tribe_saved_trips.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
