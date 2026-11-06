package com.example.data.repository

import android.content.Context
import com.example.data.local.room.SavedTripDao
import com.example.data.local.room.SavedTripEntity
import com.example.data.local.room.TribeRoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SavedTripsRepository(private val dao: SavedTripDao) {

  val allSavedTrips: Flow<List<SavedTripEntity>> = dao.getAllSavedTrips()

  fun isTripSaved(tripId: String): Flow<Boolean> = dao.isTripSaved(tripId)

  suspend fun insertTrip(trip: SavedTripEntity) {
    dao.insertTrip(trip)
  }

  suspend fun deleteTripById(tripId: String) {
    dao.deleteTripById(tripId)
  }

  suspend fun seedInitialDataIfNeeded() {
    if (dao.getCount() == 0) {
      val defaultTrips = listOf(
        SavedTripEntity(
          id = "room-sajek-2025",
          title = "সাজেক ভ্যালি মেঘের মেলা",
          destinationName = "সাজেক ভ্যালি (Sajek Valley)",
          startDate = "2026-12-14",
          endDate = "2026-12-16",
          status = "completed",
          estimatedBudget = 8500.0,
          actualExpenses = 22000.0,
          memberCount = 5,
          notes = "রিসোর্ট রুন্ময় ও ব্যাম্বু চিকেন অভিজ্ঞতা দারুণ ছিল।"
        ),
        SavedTripEntity(
          id = "room-cox-2025",
          title = "কক্সবাজার সমুদ্র সৈকত উইকএন্ড",
          destinationName = "কক্সবাজার (Cox's Bazar)",
          startDate = "2026-11-20",
          endDate = "2026-11-23",
          status = "ongoing",
          estimatedBudget = 18000.0,
          actualExpenses = 14500.0,
          memberCount = 4,
          notes = "মেরিন ড্রাইভ রোড এবং হিমছড়ি সূর্যাস্ত দর্শন।"
        ),
        SavedTripEntity(
          id = "room-bandarban-2026",
          title = "বান্দরবান নীলগিরি ট্রেকিং",
          destinationName = "বান্দরবান (Bandarban)",
          startDate = "2027-01-10",
          endDate = "2027-01-14",
          status = "planning",
          estimatedBudget = 16000.0,
          actualExpenses = 0.0,
          memberCount = 6,
          notes = "বগালেক ও নাফাখুমের জন্য অনুমোদিত গাইড দরকার।"
        )
      )
      for (trip in defaultTrips) {
        dao.insertTrip(trip)
      }
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: SavedTripsRepository? = null

    fun getInstance(context: Context): SavedTripsRepository {
      return INSTANCE ?: synchronized(this) {
        val database = TribeRoomDatabase.getDatabase(context)
        INSTANCE ?: SavedTripsRepository(database.savedTripDao()).also { INSTANCE = it }
      }
    }
  }
}
