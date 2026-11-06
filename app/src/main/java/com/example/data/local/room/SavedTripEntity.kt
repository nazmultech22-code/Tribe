package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_trips")
data class SavedTripEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val destinationName: String,
  val startDate: String? = null,
  val endDate: String? = null,
  val status: String = "planning",
  val estimatedBudget: Double? = null,
  val actualExpenses: Double? = null,
  val memberCount: Int = 1,
  val notes: String? = null,
  val savedAt: Long = System.currentTimeMillis()
)
