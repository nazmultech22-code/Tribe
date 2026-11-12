package com.example.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedTripEntity::class, SyncOperationEntity::class], version = 2, exportSchema = false)
abstract class TribeRoomDatabase : RoomDatabase() {

  abstract fun savedTripDao(): SavedTripDao
  abstract fun syncOperationDao(): SyncOperationDao

  companion object {
    /** Adds the outbox without deleting existing saved-trip rows. */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS sync_operations (" +
            "id TEXT NOT NULL, " +
            "operationType TEXT NOT NULL, " +
            "tableName TEXT NOT NULL, " +
            "recordId TEXT NOT NULL, " +
            "payload_json TEXT NOT NULL, " +
            "createdAt INTEGER NOT NULL, " +
            "attemptCount INTEGER NOT NULL DEFAULT 0, " +
            "last_error TEXT, " +
            "state TEXT NOT NULL DEFAULT 'pending', " +
            "PRIMARY KEY(id))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_state_createdAt ON sync_operations(state, createdAt)")
      }
    }
    @Volatile
    private var INSTANCE: TribeRoomDatabase? = null

    fun getDatabase(context: Context): TribeRoomDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          TribeRoomDatabase::class.java,
          "tribe_saved_trips.db"
        )
          .addMigrations(MIGRATION_1_2)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
