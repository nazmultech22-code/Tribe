package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SessionState(
  val isLoggedIn: Boolean = false,
  val accessToken: String? = null,
  val refreshToken: String? = null,
  val userId: String? = null,
  val email: String? = null,
  val fullName: String? = null,
  val username: String? = null,
  val avatarUrl: String? = null,
  val language: AppLanguage = AppLanguage.BANGLA
)

class UserSessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  private val _sessionState = MutableStateFlow(loadInitialState())
  val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

  private fun loadInitialState(): SessionState {
    val token = readSecureString(ACCESS_TOKEN_KEY)
    val refreshToken = readSecureString(REFRESH_TOKEN_KEY)
    if (token.isNullOrEmpty()) {
      // A refresh token without its access token cannot restore a valid local session.
      prefs.edit().remove(REFRESH_TOKEN_KEY).apply()
    }
    val userId = prefs.getString("user_id", null)
    val email = prefs.getString("email", null)
    val fullName = prefs.getString("full_name", null)
    val username = prefs.getString("username", null)
    val avatarUrl = prefs.getString("avatar_url", null)
    val langCode = prefs.getString("language", "bn")
    val language = if (langCode == "en") AppLanguage.ENGLISH else AppLanguage.BANGLA

    return SessionState(
      isLoggedIn = !token.isNullOrEmpty() && !userId.isNullOrEmpty(),
      accessToken = token,
      refreshToken = refreshToken,
      userId = userId,
      email = email,
      fullName = fullName,
      username = username,
      avatarUrl = avatarUrl,
      language = language
    )
  }

  /**
   * Tokens are encrypted with an Android Keystore AES-GCM key. Existing plaintext
   * values are migrated on first read so users do not have to sign in again.
   */
  private fun readSecureString(key: String): String? {
    val stored = prefs.getString(key, null) ?: return null
    return try {
      if (stored.startsWith(ENCRYPTED_PREFIX)) {
        decryptSecret(stored)
      } else {
        val encrypted = encryptSecret(stored)
        prefs.edit().putString(key, encrypted).apply()
        stored
      }
    } catch (e: Exception) {
      Log.e(TAG, "Could not read encrypted session data; clearing the affected token", e)
      prefs.edit().remove(key).apply()
      null
    }
  }

  private fun encryptSecret(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
    val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    val ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP)
    return ENCRYPTED_PREFIX + iv + ":" + ciphertext
  }

  private fun decryptSecret(value: String): String {
    val payload = value.removePrefix(ENCRYPTED_PREFIX)
    val parts = payload.split(":", limit = 2)
    if (parts.size != 2) throw IllegalArgumentException("Invalid encrypted session value")
    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
    val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
    if (iv.size != GCM_IV_LENGTH) throw IllegalArgumentException("Invalid session IV")
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
  }

  private fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null)
    if (existing is SecretKey) return existing

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }

  @Synchronized
  fun saveSession(
    token: String,
    userId: String,
    email: String,
    fullName: String? = null,
    username: String? = null,
    refreshToken: String? = null
  ) {
    val effectiveRefreshToken = refreshToken ?: _sessionState.value.refreshToken
    val editor = prefs.edit()
      .putString(ACCESS_TOKEN_KEY, encryptSecret(token))
      .putString("user_id", userId)
      .putString("email", email)
      .putString("full_name", fullName)
      .putString("username", username)
    if (effectiveRefreshToken.isNullOrBlank()) {
      editor.remove(REFRESH_TOKEN_KEY)
    } else {
      editor.putString(REFRESH_TOKEN_KEY, encryptSecret(effectiveRefreshToken))
    }
    editor.apply()

    _sessionState.value = _sessionState.value.copy(
      isLoggedIn = true,
      accessToken = token,
      refreshToken = effectiveRefreshToken,
      userId = userId,
      email = email,
      fullName = fullName ?: _sessionState.value.fullName,
      username = username ?: _sessionState.value.username
    )
  }

  fun updateProfile(fullName: String?, username: String?, avatarUrl: String?) {
    prefs.edit()
      .putString("full_name", fullName)
      .putString("username", username)
      .putString("avatar_url", avatarUrl)
      .apply()

    _sessionState.value = _sessionState.value.copy(
      fullName = fullName,
      username = username,
      avatarUrl = avatarUrl
    )
  }

  fun setLanguage(lang: AppLanguage) {
    prefs.edit().putString("language", lang.code).apply()
    _sessionState.value = _sessionState.value.copy(language = lang)
  }

  @Synchronized
  fun clearSession() {
    val currentLang = _sessionState.value.language
    prefs.edit()
      .remove(ACCESS_TOKEN_KEY)
      .remove(REFRESH_TOKEN_KEY)
      .remove("user_id")
      .remove("email")
      .remove("full_name")
      .remove("username")
      .remove("avatar_url")
      .apply()

    _sessionState.value = SessionState(
      isLoggedIn = false,
      accessToken = null,
      refreshToken = null,
      userId = null,
      email = null,
      fullName = null,
      username = null,
      avatarUrl = null,
      language = currentLang
    )
  }

  companion object {
    private const val TAG = "TribeSession"
    private const val PREFERENCES_NAME = "tribe_prefs"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "tribe_session_aes_gcm_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENCRYPTED_PREFIX = "enc:v1:"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var INSTANCE: UserSessionManager? = null

    fun getInstance(context: Context): UserSessionManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: UserSessionManager(context).also { INSTANCE = it }
      }
    }
  }
}
