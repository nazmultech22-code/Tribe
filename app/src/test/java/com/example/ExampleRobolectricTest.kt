package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserSessionManager
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context verifies TRIBE app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TRIBE", appName)
  }

  @Test
  fun `currency formatting handles Bangla and English digits`() {
    val amount = 22000.0
    val bnFormatted = L10n.formatCurrency(amount, AppLanguage.BANGLA)
    val enFormatted = L10n.formatCurrency(amount, AppLanguage.ENGLISH)

    assertEquals("৳ ২২০০০", bnFormatted)
    assertTrue(enFormatted.contains("22,000"))
  }

  @Test
  fun `session manager persists language switch`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = UserSessionManager(context)

    sessionManager.setLanguage(AppLanguage.ENGLISH)
    assertEquals(AppLanguage.ENGLISH, sessionManager.sessionState.value.language)

    sessionManager.setLanguage(AppLanguage.BANGLA)
    assertEquals(AppLanguage.BANGLA, sessionManager.sessionState.value.language)
  }

  @Test
  fun `bilingual strings provide proper translations for both modes`() {
    assertEquals("এক্সপ্লোর", L10n.navExplore(AppLanguage.BANGLA))
    assertEquals("Explore", L10n.navExplore(AppLanguage.ENGLISH))

    assertEquals("এই গন্তব্যে এখনো কোনো শেয়ার করা ট্যুর প্ল্যান পাওয়া যায়নি।", L10n.noSharedToursFound(AppLanguage.BANGLA))
    assertEquals("No shared tour plans found for this destination.", L10n.noSharedToursFound(AppLanguage.ENGLISH))
  }

  @Test
  fun `room database stores and retrieves saved trips via Flow`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.room.TribeRoomDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.savedTripDao()

    val testTrip = com.example.data.local.room.SavedTripEntity(
      id = "test-trip-001",
      title = "কক্সবাজার সমুদ্র ভ্রমণ",
      destinationName = "কক্সবাজার",
      startDate = "2026-12-01",
      endDate = "2026-12-05",
      status = "planning",
      estimatedBudget = 15000.0,
      actualExpenses = 5000.0,
      memberCount = 4,
      notes = "টেস্ট নোট"
    )

    dao.insertTrip(testTrip)

    val list = dao.getAllSavedTrips().first()

    assertEquals(1, list.size)
    assertEquals("test-trip-001", list[0].id)
    assertEquals("কক্সবাজার সমুদ্র ভ্রমণ", list[0].title)

    dao.deleteTripById("test-trip-001")
    val emptyList = dao.getAllSavedTrips().first()
    assertEquals(0, emptyList.size)

    db.close()
  }
}
