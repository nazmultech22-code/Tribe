package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    context.applicationContext.getSharedPreferences("tribe_prefs", Context.MODE_PRIVATE)

  private val _sessionState = MutableStateFlow(loadInitialState())
  val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

  private fun loadInitialState(): SessionState {
    val token = prefs.getString("access_token", null)
    val refreshToken = prefs.getString("refresh_token", null)
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

  fun saveSession(
    token: String,
    userId: String,
    email: String,
    fullName: String? = null,
    username: String? = null,
    refreshToken: String? = null
  ) {
    prefs.edit()
      .putString("access_token", token)
      .putString("refresh_token", refreshToken ?: prefs.getString("refresh_token", null))
      .putString("user_id", userId)
      .putString("email", email)
      .putString("full_name", fullName)
      .putString("username", username)
      .apply()

    _sessionState.value = _sessionState.value.copy(
      isLoggedIn = true,
      accessToken = token,
      refreshToken = refreshToken ?: _sessionState.value.refreshToken,
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

  fun clearSession() {
    val currentLang = _sessionState.value.language
    prefs.edit()
      .remove("access_token")
      .remove("refresh_token")
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
    @Volatile
    private var INSTANCE: UserSessionManager? = null

    fun getInstance(context: Context): UserSessionManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: UserSessionManager(context).also { INSTANCE = it }
      }
    }
  }
}
