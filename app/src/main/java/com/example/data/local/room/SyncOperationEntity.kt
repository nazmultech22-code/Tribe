package com.example.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations", indices = [Index(value = ["state", "createdAt"])])
data class SyncOperationEntity(
  @PrimaryKey val id: String,
  val operationType: String,
  val tableName: String,
  val recordId: String,
  @ColumnInfo(name = "payload_json") val payloadJson: String,
  val createdAt: Long,
  @ColumnInfo(defaultValue = "0") val attemptCount: Int = 0,
  @ColumnInfo(name = "last_error") val lastError: String? = null,
  @ColumnInfo(defaultValue = "'pending'") val state: String = "pending"
)
