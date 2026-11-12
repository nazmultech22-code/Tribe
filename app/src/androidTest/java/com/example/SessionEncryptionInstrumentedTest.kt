package com.example

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.UserSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionEncryptionInstrumentedTest {

  private val context: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun sessionTokensAreEncryptedAndCanBeRestored() {
    val prefs = context.getSharedPreferences("tribe_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val accessToken = "test-access-token-for-tribe"
    val refreshToken = "test-refresh-token-for-tribe"
    val manager = UserSessionManager(context)
    manager.saveSession(
      token = accessToken,
      userId = "test-user-id",
      email = "test@example.com",
      refreshToken = refreshToken
    )

    val storedAccessToken = prefs.getString("access_token", null)
    val storedRefreshToken = prefs.getString("refresh_token", null)
    assertNotNull(storedAccessToken)
    assertNotNull(storedRefreshToken)
    assertTrue(storedAccessToken!!.startsWith("enc:v1:"))
    assertTrue(storedRefreshToken!!.startsWith("enc:v1:"))
    assertFalse(storedAccessToken.contains(accessToken))
    assertFalse(storedRefreshToken.contains(refreshToken))

    val restored = UserSessionManager(context)
    assertTrue(restored.sessionState.value.isLoggedIn)
    assertEquals(accessToken, restored.sessionState.value.accessToken)
    assertEquals(refreshToken, restored.sessionState.value.refreshToken)
    restored.clearSession()
  }

  @Test
  fun legacyPlaintextTokensAreMigratedOnFirstRead() {
    val prefs = context.getSharedPreferences("tribe_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear()
      .putString("access_token", "legacy-access-token")
      .putString("refresh_token", "legacy-refresh-token")
      .putString("user_id", "legacy-user-id")
      .putString("email", "legacy@example.com")
      .commit()

    val restored = UserSessionManager(context)
    assertTrue(restored.sessionState.value.isLoggedIn)
    assertEquals("legacy-access-token", restored.sessionState.value.accessToken)
    assertEquals("legacy-refresh-token", restored.sessionState.value.refreshToken)
    assertTrue(prefs.getString("access_token", "").orEmpty().startsWith("enc:v1:"))
    assertTrue(prefs.getString("refresh_token", "").orEmpty().startsWith("enc:v1:"))
    restored.clearSession()
  }
}
