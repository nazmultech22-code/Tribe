package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserSessionManager
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
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
}
