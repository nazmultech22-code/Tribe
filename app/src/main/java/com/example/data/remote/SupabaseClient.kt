package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.UserSessionManager
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient(private val sessionManager: UserSessionManager) {

  private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
  private val apiKey = BuildConfig.SUPABASE_ANON_KEY

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .callTimeout(90, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    })
    .addInterceptor { chain ->
      val original = chain.request()
      val token = sessionManager.sessionState.value.accessToken
      val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else "Bearer $apiKey"

      val requestBuilder = original.newBuilder()
        .header("apikey", apiKey)
        .header("Authorization", authHeader)

      if (original.header("Content-Type") == null) {
        requestBuilder.header("Content-Type", "application/json")
      }
      chain.proceed(requestBuilder.build())
    }
    .build()

  private fun isValidJsonArray(body: String?): Boolean {
    if (body.isNullOrBlank()) return false
    val trimmed = body.trim()
    return trimmed.startsWith("[") && trimmed.endsWith("]")
  }

  // --------------------------------------------------------------------------
  // AUTHENTICATION
  // --------------------------------------------------------------------------

  suspend fun signUp(
    email: String,
    pass: String,
    fullName: String,
    username: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("email", email)
        put("password", pass)
        put("data", JSONObject().apply {
          put("full_name", fullName)
          put("username", username)
        })
      }

      val req = Request.Builder()
        .url("$baseUrl/auth/v1/signup")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""

      if (!resp.isSuccessful) {
        val errObj = runCatching { JSONObject(body) }.getOrNull()
        val errMsg = errObj?.optString("msg")
          ?: errObj?.optString("message")
          ?: errObj?.optString("error_description")
          ?: "Sign up failed (${resp.code})"
        return@withContext Result.failure(Exception(errMsg))
      }

      val resObj = JSONObject(body)
      val accessToken = resObj.optString("access_token")
      val userObj = resObj.optJSONObject("user")
      val userId = userObj?.optString("id") ?: ""

      if (accessToken.isNotEmpty() && userId.isNotEmpty()) {
        sessionManager.saveSession(accessToken, userId, email, fullName, username, resObj.optString("refresh_token").takeIf { it.isNotBlank() })
        // ensure profile record
        createOrUpdateProfile(userId, fullName, username, email)
        Result.success("Success")
      } else {
        // Confirmation email may be required
        Result.success("Confirmation email sent or account registered.")
      }
    } catch (e: Exception) {
      Log.e("SupabaseClient", "signUp error", e)
      Result.failure(e)
    }
  }

  suspend fun signIn(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("email", email)
        put("password", pass)
      }

      val req = Request.Builder()
        .url("$baseUrl/auth/v1/token?grant_type=password")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""

      if (!resp.isSuccessful) {
        val errObj = runCatching { JSONObject(body) }.getOrNull()
        val errMsg = errObj?.optString("error_description")
          ?: errObj?.optString("message")
          ?: errObj?.optString("msg")
          ?: "Sign in failed (${resp.code})"
        return@withContext Result.failure(Exception(errMsg))
      }

      val resObj = JSONObject(body)
      val token = resObj.getString("access_token")
      val userObj = resObj.getJSONObject("user")
      val userId = userObj.getString("id")
      val userMeta = userObj.optJSONObject("user_metadata")
      val fullName = userMeta?.optString("full_name", "") ?: ""
      val username = userMeta?.optString("username", "") ?: ""

      sessionManager.saveSession(token, userId, email, fullName, username, resObj.optString("refresh_token").takeIf { it.isNotBlank() })
      Result.success("Signed in")
    } catch (e: Exception) {
      Log.e("SupabaseClient", "signIn error", e)
      Result.failure(e)
    }
  }


  suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      if (idToken.isBlank()) return@withContext Result.failure(Exception("Google ID token is empty"))
      val payload = JSONObject().apply {
        put("provider", "google")
        put("id_token", idToken)
      }
      val req = Request.Builder()
        .url("$baseUrl/auth/v1/token?grant_type=id_token")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""
      if (!resp.isSuccessful) {
        val err = runCatching { JSONObject(body) }.getOrNull()
        val message = err?.optString("msg")?.takeIf { it.isNotBlank() }
          ?: err?.optString("message")?.takeIf { it.isNotBlank() }
          ?: err?.optString("error_description")?.takeIf { it.isNotBlank() }
          ?: "Google sign-in failed (${resp.code})"
        return@withContext Result.failure(Exception(message))
      }
      val session = JSONObject(body)
      val accessToken = session.optString("access_token")
      val user = session.optJSONObject("user")
        ?: return@withContext Result.failure(Exception("Supabase did not return a user"))
      val userId = user.optString("id")
      val email = user.optString("email")
      val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
      val fullName = metadata.optString("full_name").ifBlank {
        metadata.optString("name").ifBlank { email.substringBefore("@") }
      }
      val username = metadata.optString("username").ifBlank { email.substringBefore("@") }
      if (accessToken.isBlank() || userId.isBlank() || email.isBlank()) {
        return@withContext Result.failure(Exception("Incomplete Supabase session returned by Google sign-in"))
      }
      sessionManager.saveSession(accessToken, userId, email, fullName, username, session.optString("refresh_token").takeIf { it.isNotBlank() })
      createOrUpdateProfile(userId, fullName, username, email)
      Result.success("Signed in with Google")
    } catch (e: Exception) {
      Log.e("SupabaseClient", "signInWithGoogleIdToken error", e)
      Result.failure(e)
    }
  }


  suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
    val refreshToken = sessionManager.sessionState.value.refreshToken
      ?: return@withContext false
    try {
      val payload = JSONObject().put("refresh_token", refreshToken)
      val req = Request.Builder()
        .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful) return@withContext false
      val session = JSONObject(body)
      val user = session.optJSONObject("user") ?: JSONObject()
      val token = session.optString("access_token")
      val nextRefreshToken = session.optString("refresh_token").ifBlank { refreshToken }
      val userId = user.optString("id").ifBlank { sessionManager.sessionState.value.userId.orEmpty() }
      val email = user.optString("email").ifBlank { sessionManager.sessionState.value.email.orEmpty() }
      val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
      val fullName = metadata.optString("full_name").ifBlank { sessionManager.sessionState.value.fullName.orEmpty() }
      val username = metadata.optString("username").ifBlank { sessionManager.sessionState.value.username.orEmpty() }
      if (token.isBlank() || userId.isBlank() || email.isBlank()) return@withContext false
      sessionManager.saveSession(token, userId, email, fullName, username, nextRefreshToken)
      true
    } catch (e: Exception) {
      Log.w("SupabaseClient", "Session refresh failed", e)
      false
    }
  }

  suspend fun resetPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("email", email)
      }
      val req = Request.Builder()
        .url("$baseUrl/auth/v1/recover")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) {
        Result.success("Reset link sent")
      } else {
        Result.failure(Exception("Failed to send reset link (${resp.code})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    runCatching {
      val req = Request.Builder()
        .url("$baseUrl/auth/v1/logout")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()
      okHttpClient.newCall(req).execute()
    }
    sessionManager.clearSession()
  }

  // --------------------------------------------------------------------------
  // USER PROFILES
  // --------------------------------------------------------------------------

  private suspend fun createOrUpdateProfile(
    userId: String,
    fullName: String?,
    username: String?,
    email: String?
  ) {
    runCatching {
      val payload = JSONObject().apply {
        put("id", userId)
        if (!fullName.isNullOrEmpty()) put("full_name", fullName)
        if (!username.isNullOrEmpty()) put("username", username)
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/profiles")
        .header("Prefer", "resolution=merge-duplicates")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      okHttpClient.newCall(req).execute()
    }
  }

  suspend fun getProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext null
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/profiles?id=eq.$userId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext null
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext null
      val arr = JSONArray(body)
      if (arr.length() > 0) {
        val adapter = moshi.adapter(UserProfile::class.java)
        val storedProfile = adapter.fromJson(arr.getJSONObject(0).toString()) ?: return@withContext null
        val locationParts = storedProfile.city?.split(",", limit = 2)
        val privatePhone = runCatching {
          val privateRequest = Request.Builder()
            .url("$baseUrl/rest/v1/profile_private?user_id=eq.$userId&select=phone")
            .get()
            .build()
          okHttpClient.newCall(privateRequest).execute().use { privateResponse ->
            val privateBody = privateResponse.body?.string().orEmpty()
            if (!privateResponse.isSuccessful || !isValidJsonArray(privateBody)) null
            else JSONArray(privateBody).optJSONObject(0)?.optString("phone")?.takeIf { it.isNotBlank() }
          }
        }.getOrNull()
        storedProfile.copy(
          city = locationParts?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
          country = locationParts?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() },
          phone = privatePhone
        )
      } else null
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getProfile error", e)
      null
    }
  }

  suspend fun updateProfile(
    userId: String,
    fullName: String,
    username: String,
    bio: String?,
    city: String?,
    country: String?,
    phone: String?,
    avatarUrl: String?
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val location = listOfNotNull(city?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() }).joinToString(", ")
      val payload = JSONObject().apply {
        put("full_name", fullName)
        put("username", username)
        put("bio", bio ?: JSONObject.NULL)
        put("location", if (location.isBlank()) JSONObject.NULL else location)
        if (avatarUrl != null) put("avatar_url", avatarUrl)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
        .header("Prefer", "return=minimal")
        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val profileError = okHttpClient.newCall(req).execute().use { response ->
        if (response.isSuccessful) null
        else "Profile update failed (${response.code}): ${response.body?.string().orEmpty()}"
      }
      if (profileError != null) return@withContext Result.failure(Exception(profileError))

      // Contact details live in profile_private, not the public profiles table.
      val privatePayload = JSONObject()
        .put("user_id", userId)
        .put("phone", phone ?: JSONObject.NULL)
      val privateRequest = Request.Builder()
        .url("$baseUrl/rest/v1/profile_private?on_conflict=user_id")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(privatePayload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val privateError = okHttpClient.newCall(privateRequest).execute().use { response ->
        if (response.isSuccessful) null
        else "Private contact update failed (${response.code}): ${response.body?.string().orEmpty()}"
      }
      if (privateError != null) return@withContext Result.failure(Exception(privateError))

      sessionManager.updateProfile(fullName, username, avatarUrl)
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // DESTINATIONS & ALIASES
  // --------------------------------------------------------------------------

  suspend fun getDestinations(): List<Destination> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/destinations?select=*&order=name.asc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) {
        Log.w("SupabaseClient", "getDestinations response not array (${resp.code}): $body")
        return@withContext emptyList()
      }
      val type = Types.newParameterizedType(List::class.java, Destination::class.java)
      val adapter = moshi.adapter<List<Destination>>(type)
      adapter.fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getDestinations error", e)
      emptyList()
    }
  }

  suspend fun getDestinationById(id: String): Destination? = withContext(Dispatchers.IO) {
    if (id.isBlank()) return@withContext null
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/destinations?id=eq.$id&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext null
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext null
      val arr = JSONArray(body)
      if (arr.length() > 0) {
        val adapter = moshi.adapter(Destination::class.java)
        adapter.fromJson(arr.getJSONObject(0).toString())
      } else null
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getDestinationById error", e)
      null
    }
  }

  suspend fun createDestination(
    name: String,
    country: String?,
    region: String?,
    description: String?,
    bestSeason: String?,
    transportInfo: String?,
    accommodationInfo: String?,
    foodInfo: String?,
    guideInfo: String?,
    safetyTips: String?,
    bnAliases: List<String>,
    enAliases: List<String>
  ): Result<Destination> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("name", name)
        if (!country.isNullOrBlank()) put("country", country)
        if (!region.isNullOrBlank()) put("region", region)
        if (!description.isNullOrBlank()) put("description", description)
        if (!bestSeason.isNullOrBlank()) put("best_season", bestSeason)
        if (!transportInfo.isNullOrBlank()) put("transport_info", transportInfo)
        if (!accommodationInfo.isNullOrBlank()) put("accommodation_info", accommodationInfo)
        if (!foodInfo.isNullOrBlank()) put("food_info", foodInfo)
        if (!guideInfo.isNullOrBlank()) put("guide_info", guideInfo)
        if (!safetyTips.isNullOrBlank()) put("safety_tips", safetyTips)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/destinations")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""

      if (!resp.isSuccessful) {
        return@withContext Result.failure(Exception("Failed to create destination: $body (${resp.code})"))
      }

      if (!isValidJsonArray(body)) {
        return@withContext Result.failure(Exception("Unexpected response: $body"))
      }

      val arr = JSONArray(body)
      if (arr.length() == 0) return@withContext Result.failure(Exception("No destination returned"))
      val createdDest = moshi.adapter(Destination::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Failed to parse destination"))

      // Insert aliases
      val aliasesToInsert = mutableListOf<DestinationAlias>()
      bnAliases.forEach { if (it.isNotBlank()) aliasesToInsert.add(DestinationAlias(destinationId = createdDest.id, alias = it.trim(), language = "bn")) }
      enAliases.forEach { if (it.isNotBlank()) aliasesToInsert.add(DestinationAlias(destinationId = createdDest.id, alias = it.trim(), language = "en")) }

      if (aliasesToInsert.isNotEmpty()) {
        addAliases(aliasesToInsert)
      }

      Result.success(createdDest)
    } catch (e: Exception) {
      Log.e("SupabaseClient", "createDestination error", e)
      Result.failure(e)
    }
  }

  suspend fun addAliases(aliases: List<DestinationAlias>): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val arr = JSONArray()
      for (a in aliases) {
        arr.put(JSONObject().apply {
          put("destination_id", a.destinationId)
          put("alias", a.alias)
          if (!a.language.isNullOrBlank()) put("language", a.language)
        })
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/destination_aliases")
        .post(arr.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Failed to add aliases (${resp.code})"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getAliasesForDestination(destId: String): List<DestinationAlias> = withContext(Dispatchers.IO) {
    if (destId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/destination_aliases?destination_id=eq.$destId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, DestinationAlias::class.java)
      moshi.adapter<List<DestinationAlias>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun searchDestinations(query: String): List<Destination> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) return@withContext getDestinations()

    try {
      // 1. Fetch matching destinations directly by name or description
      val destReq = Request.Builder()
        .url("$baseUrl/rest/v1/destinations?or=(name.ilike.*$cleanQuery*,description.ilike.*$cleanQuery*,region.ilike.*$cleanQuery*)&select=*")
        .get()
        .build()
      val destResp = okHttpClient.newCall(destReq).execute()
      val destBody = destResp.body?.string() ?: "[]"
      val directMatches = if (destResp.isSuccessful && isValidJsonArray(destBody)) {
        val listType = Types.newParameterizedType(List::class.java, Destination::class.java)
        moshi.adapter<List<Destination>>(listType).fromJson(destBody) ?: emptyList()
      } else emptyList()

      // 2. Search aliases (matches Bengali, English, transliterations)
      val aliasReq = Request.Builder()
        .url("$baseUrl/rest/v1/destination_aliases?alias.ilike.*$cleanQuery*&select=*")
        .get()
        .build()
      val aliasResp = okHttpClient.newCall(aliasReq).execute()
      val aliasBody = aliasResp.body?.string() ?: "[]"
      val aliasMatches = if (aliasResp.isSuccessful && isValidJsonArray(aliasBody)) {
        val aliasType = Types.newParameterizedType(List::class.java, DestinationAlias::class.java)
        moshi.adapter<List<DestinationAlias>>(aliasType).fromJson(aliasBody) ?: emptyList()
      } else emptyList()

      val matchedDestIds = aliasMatches.map { it.destinationId }.toSet()
      val missingIds = matchedDestIds - directMatches.map { it.id }.toSet()

      val allResults = directMatches.toMutableList()
      for (id in missingIds) {
        val dest = getDestinationById(id)
        if (dest != null) {
          allResults.add(dest)
        }
      }

      // Also do normalized punctuation match if results empty (e.g., C-O-X -> COX)
      if (allResults.isEmpty()) {
        val normalized = cleanQuery.replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF]"), "")
        if (normalized.length >= 2 && normalized != cleanQuery) {
          return@withContext searchDestinations(normalized)
        }
      }

      allResults
    } catch (e: Exception) {
      Log.e("SupabaseClient", "searchDestinations error", e)
      emptyList()
    }
  }

  // --------------------------------------------------------------------------
  // PUBLIC TOUR DISCOVERY (VIEW: public_tour_discovery)
  // --------------------------------------------------------------------------

  suspend fun getPublicToursByDestination(destId: String): List<PublicTourDiscoveryItem> = withContext(Dispatchers.IO) {
    if (destId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours?destination=eq.$destId&is_public=eq.true&select=id,title,destination,start_date,end_date,status,budget_per_person,created_at&order=created_at.desc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) {
        Log.w("SupabaseClient", "getPublicToursByDestination response not array (${resp.code}): $body")
        return@withContext emptyList()
      }
      val type = Types.newParameterizedType(List::class.java, PublicTourDiscoveryItem::class.java)
      moshi.adapter<List<PublicTourDiscoveryItem>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getPublicToursByDestination error", e)
      emptyList()
    }
  }

  suspend fun getAllPublicTours(): List<PublicTourDiscoveryItem> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours?is_public=eq.true&select=id,title,destination,start_date,end_date,status,budget_per_person,created_at&order=created_at.desc&limit=30")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) {
        Log.w("SupabaseClient", "getAllPublicTours response not array (${resp.code}): $body")
        return@withContext emptyList()
      }
      val type = Types.newParameterizedType(List::class.java, PublicTourDiscoveryItem::class.java)
      moshi.adapter<List<PublicTourDiscoveryItem>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getAllPublicTours error", e)
      emptyList()
    }
  }

  // --------------------------------------------------------------------------
  // TOURS MANAGEMENT
  // --------------------------------------------------------------------------

  suspend fun getMyTours(userId: String): List<Tour> = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext emptyList()
    try {
      val type = Types.newParameterizedType(List::class.java, Tour::class.java)
      val adapter = moshi.adapter<List<Tour>>(type)

      // Fetch tours created by this user.
      val createdRequest = Request.Builder()
        .url("$baseUrl/rest/v1/tours?creator_id=eq.$userId&select=*&order=created_at.desc")
        .get()
        .build()
      val createdResponse = okHttpClient.newCall(createdRequest).execute()
      val createdBody = createdResponse.body?.string() ?: "[]"
      val createdTours = if (createdResponse.isSuccessful && isValidJsonArray(createdBody)) {
        adapter.fromJson(createdBody) ?: emptyList()
      } else {
        Log.w("SupabaseClient", "getMyTours created query failed (${createdResponse.code}): $createdBody")
        emptyList()
      }

      // Also include tours where the user is a member.
      val memberRequest = Request.Builder()
        .url("$baseUrl/rest/v1/tour_members?user_id=eq.$userId&select=tour_id")
        .get()
        .build()
      val memberResponse = okHttpClient.newCall(memberRequest).execute()
      val memberBody = memberResponse.body?.string() ?: "[]"
      val memberTourIds = if (memberResponse.isSuccessful && isValidJsonArray(memberBody)) {
        val rows = JSONArray(memberBody)
        (0 until rows.length()).mapNotNull { index ->
          rows.getJSONObject(index).optString("tour_id").takeIf { it.isNotBlank() }
        }.distinct()
      } else {
        Log.w("SupabaseClient", "getMyTours membership query failed (${memberResponse.code}): $memberBody")
        emptyList()
      }

      val joinedTours = if (memberTourIds.isNotEmpty()) {
        val joinedRequest = Request.Builder()
          .url("$baseUrl/rest/v1/tours?id=in.(${memberTourIds.joinToString(",")})&select=*&order=created_at.desc")
          .get()
          .build()
        val joinedResponse = okHttpClient.newCall(joinedRequest).execute()
        val joinedBody = joinedResponse.body?.string() ?: "[]"
        if (joinedResponse.isSuccessful && isValidJsonArray(joinedBody)) {
          adapter.fromJson(joinedBody) ?: emptyList()
        } else {
          Log.w("SupabaseClient", "getMyTours joined query failed (${joinedResponse.code}): $joinedBody")
          emptyList()
        }
      } else emptyList()

      (createdTours + joinedTours).distinctBy { it.id }
        .sortedByDescending { it.createdAt ?: "" }
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getMyTours error", e)
      emptyList()
    }
  }

  suspend fun getTourById(tourId: String): Tour? = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext null
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours?id=eq.$tourId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext null
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext null
      val arr = JSONArray(body)
      if (arr.length() > 0) {
        moshi.adapter(Tour::class.java).fromJson(arr.getJSONObject(0).toString())
      } else null
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getTourById error", e)
      null
    }
  }

  suspend fun createTour(
    title: String,
    destinationId: String?,
    startDate: String?,
    endDate: String?,
    maxMembers: Int?,
    meetingPoint: String?,
    description: String?
  ): Result<Tour> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("User not authenticated"))

      val payload = JSONObject().apply {
        put("title", title)
        put("creator_id", userId)
        put("is_public", false) // Tours are private by default.
        put("status", "upcoming")
        val destinationName = destinationId?.let { getDestinationById(it)?.name } ?: destinationId
        if (!destinationName.isNullOrBlank()) put("destination", destinationName)
        else put("destination", "Unknown destination")
        if (!startDate.isNullOrBlank()) put("start_date", startDate)
        if (!endDate.isNullOrBlank()) put("end_date", endDate)
        if (maxMembers != null) put("max_members", maxMembers)
        if (!description.isNullOrBlank()) put("description", description)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""

      if (!resp.isSuccessful || !isValidJsonArray(body)) {
        return@withContext Result.failure(Exception("Failed to create tour: $body (${resp.code})"))
      }

      val arr = JSONArray(body)
      if (arr.length() == 0) return@withContext Result.failure(Exception("No tour returned"))
      val created = moshi.adapter(Tour::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Failed to parse tour"))

      // Add owner as a tour member
      runCatching {
        addTourMember(created.id, userId, role = "creator")
      }

      Result.success(created)
    } catch (e: Exception) {
      Log.e("SupabaseClient", "createTour error", e)
      Result.failure(e)
    }
  }

  suspend fun updateTourVisibility(tourId: String, newVisibility: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("is_public", newVisibility == "public")
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours?id=eq.$tourId")
        .header("Prefer", "return=representation")
        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) {
        Result.success(true)
      } else {
        val body = resp.body?.string() ?: ""
        Result.failure(Exception("Update failed: $body (${resp.code})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateTourStatus(tourId: String, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("status", newStatus)
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tours?id=eq.$tourId")
        .header("Prefer", "return=representation")
        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Status update failed (${resp.code})"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // TOUR MEMBERS
  // --------------------------------------------------------------------------

  suspend fun getTourMembers(tourId: String): List<TourMember> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_members?tour_id=eq.$tourId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourMember::class.java)
      moshi.adapter<List<TourMember>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun addTourMember(tourId: String, userId: String, role: String = "member"): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("user_id", userId)
        put("role", if (role == "owner") "creator" else role)
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_members")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Failed to add member (${resp.code})"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // TOUR EXPENSES
  // --------------------------------------------------------------------------

  suspend fun getTourExpenses(tourId: String): List<TourExpense> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_expenses?tour_id=eq.$tourId&select=*&order=created_at.desc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourExpense::class.java)
      moshi.adapter<List<TourExpense>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }


  fun buildCreateTourExpenseRpcPayload(
    expenseId: String,
    tourId: String,
    title: String,
    amount: Double,
    category: String,
    notes: String?,
    participantIds: List<String>
  ): JSONObject {
    val ids = participantIds.distinct()
    require(ids.isNotEmpty()) { "At least one expense participant is required" }
    val total = java.math.BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP)
    val cents = total.movePointRight(2).longValueExact()
    require(cents > 0L) { "Expense amount must be greater than zero" }
    val baseShare = cents / ids.size
    val remainder = (cents % ids.size).toInt()
    val participants = JSONArray()
    ids.forEachIndexed { index, userId ->
      val shareCents = baseShare + if (index < remainder) 1 else 0
      participants.put(
        JSONObject()
          .put("user_id", userId)
          .put("share_amount", java.math.BigDecimal.valueOf(shareCents, 2))
      )
    }
    return JSONObject().apply {
      put("p_expense_id", expenseId)
      put("p_tour_id", tourId)
      put("p_title", title.trim())
      put("p_category", category.ifBlank { "other" })
      put("p_amount", total)
      put("p_expense_date", JSONObject.NULL)
      put("p_split_method", "equal")
      put("p_notes", notes ?: JSONObject.NULL)
      put("p_participants", participants)
    }
  }

  suspend fun createTourExpense(
    tourId: String,
    title: String,
    amount: Double,
    category: String,
    description: String?,
    expenseId: String = java.util.UUID.randomUUID().toString(),
    participantIds: List<String> = emptyList()
  ): Result<TourExpense> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("User not authenticated"))
      val ids = participantIds.distinct().ifEmpty {
        getTourMembers(tourId).map { it.userId }.distinct()
      }
      if (ids.isEmpty()) return@withContext Result.failure(Exception("Add at least one tour member before creating an expense"))
      val payload = buildCreateTourExpenseRpcPayload(
        expenseId, tourId, title, amount, category, description, ids
      )
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/rpc/create_tour_expense")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Failed to add expense: " + body))
      }
      Result.success(
        TourExpense(
          id = expenseId,
          tourId = tourId,
          paidBy = userId,
          title = title.trim(),
          description = description,
          amount = java.math.BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP).toDouble(),
          category = category
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // ITINERARY
  // --------------------------------------------------------------------------

  suspend fun getTourItinerary(tourId: String): List<TourItineraryItem> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_itinerary_items?tour_id=eq.$tourId&select=*&order=day_date.asc,sort_order.asc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourItineraryItem::class.java)
      moshi.adapter<List<TourItineraryItem>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun createItineraryItem(
    tourId: String,
    dayNumber: Int,
    title: String,
    description: String?,
    location: String?
  ): Result<TourItineraryItem> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("User not authenticated"))
      val tour = getTourById(tourId)
        ?: return@withContext Result.failure(Exception("Tour not found"))
      val startDate = tour.startDate ?: return@withContext Result.failure(Exception("Tour start date is missing"))
      val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(startDate)
        ?: return@withContext Result.failure(Exception("Tour start date is invalid"))
      val calendar = java.util.Calendar.getInstance().apply {
        time = start
        add(java.util.Calendar.DAY_OF_YEAR, (dayNumber - 1).coerceAtLeast(0))
      }
      val dayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("day_date", dayDate)
        put("sort_order", 0)
        put("title", title)
        put("created_by", userId)
        if (!description.isNullOrBlank()) put("details", description)
        if (!location.isNullOrBlank()) put("location", location)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_itinerary_items")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext Result.failure(Exception("Failed to add itinerary (${resp.code})"))

      val arr = JSONArray(body)
      val created = moshi.adapter(TourItineraryItem::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Failed to parse itinerary"))

      Result.success(created)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // CHECKLISTS
  // --------------------------------------------------------------------------

  suspend fun getTourChecklist(tourId: String): List<TourChecklistItem> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_tasks?tour_id=eq.$tourId&select=*&order=created_at.asc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourChecklistItem::class.java)
      moshi.adapter<List<TourChecklistItem>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun createChecklistItem(tourId: String, title: String): Result<TourChecklistItem> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("title", title)
        put("created_by", sessionManager.sessionState.value.userId ?: return@withContext Result.failure(Exception("User not authenticated")))
        put("status", "todo")
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_tasks")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext Result.failure(Exception("Failed to add checklist item (${resp.code})"))

      val arr = JSONArray(body)
      val created = moshi.adapter(TourChecklistItem::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Parse error"))
      Result.success(created)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleChecklistItem(itemId: String, isCompleted: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("status", if (isCompleted) "done" else "todo")
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_tasks?id=eq.$itemId")
        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Failed (${resp.code})"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // TOUR GUIDES
  // --------------------------------------------------------------------------

  suspend fun getGuidesForTour(tourId: String): List<TourGuide> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guides?tour_id=eq.$tourId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourGuide::class.java)
      moshi.adapter<List<TourGuide>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun getGuidesForDestination(destinationId: String): List<TourGuide> = withContext(Dispatchers.IO) {
    if (destinationId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guides?destination_id=eq.$destinationId&select=*")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourGuide::class.java)
      moshi.adapter<List<TourGuide>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun createGuide(tourId: String?, destinationId: String?, name: String, phone: String?, email: String?): Result<TourGuide> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("name", name)
        if (!tourId.isNullOrBlank()) put("tour_id", tourId)
        if (!destinationId.isNullOrBlank()) put("destination_id", destinationId)
        if (!phone.isNullOrBlank()) put("phone", phone)
        if (!email.isNullOrBlank()) put("email", email)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guides")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext Result.failure(Exception("Failed to add guide (${resp.code})"))

      val arr = JSONArray(body)
      val created = moshi.adapter(TourGuide::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Parse error"))
      Result.success(created)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // COMMENTS
  // --------------------------------------------------------------------------

  suspend fun getTourComments(tourId: String): List<TourComment> = withContext(Dispatchers.IO) {
    if (tourId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_messages?tour_id=eq.$tourId&select=*&order=created_at.asc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourComment::class.java)
      moshi.adapter<List<TourComment>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun postTourComment(tourId: String, bodyText: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId ?: return@withContext Result.failure(Exception("Not logged in"))
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("id", java.util.UUID.randomUUID().toString())
        put("sender_id", userId)
        put("content_type", "text")
        put("body", bodyText)
        put("client_created_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date()))
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_messages")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Failed (${resp.code})"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }


  suspend fun getTourJoinRequests(tourId: String): List<TourJoinRequest> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_join_requests?tour_id=eq.$tourId&status=eq.pending&select=*&order=created_at.asc")
        .get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourJoinRequest::class.java)
      moshi.adapter<List<TourJoinRequest>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getTourJoinRequests error", e)
      emptyList()
    }
  }

  suspend fun requestToJoinTour(tourId: String, message: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("Sign in to request access"))
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("requester_id", userId)
        put("message", message.take(500))
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_join_requests")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Join request failed (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun reviewTourJoinRequest(requestId: String, decision: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      require(decision == "approved" || decision == "rejected")
      val payload = JSONObject().put("p_request_id", requestId).put("p_decision", decision)
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/rpc/review_tour_join_request")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not review request (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourGuestMembers(tourId: String): List<TourGuestMember> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guest_members?tour_id=eq.$tourId&status=eq.active&select=*&order=created_at.asc")
        .get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourGuestMember::class.java)
      moshi.adapter<List<TourGuestMember>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      Log.e("SupabaseClient", "getTourGuestMembers error", e)
      emptyList()
    }
  }

  suspend fun createTourInvite(tourId: String, inviteType: String = "app_member", maxUses: Int = 1): Result<String> = withContext(Dispatchers.IO) {
    try {
      require(inviteType == "app_member" || inviteType == "guest")
      require(maxUses in 1..500)
      val random = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
      val token = random.joinToString("") { "%02x".format(it) }
      val tokenHash = java.security.MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
      val payload = JSONObject().apply {
        put("p_tour_id", tourId)
        put("p_token_hash", tokenHash)
        put("p_invite_type", inviteType)
        put("p_max_uses", maxUses)
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/rpc/create_tour_invite")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) Result.success(token)
      else Result.failure(Exception("Invite creation failed: " + body))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun redeemTourInvite(token: String, guestDisplayName: String? = null): Result<String> = withContext(Dispatchers.IO) {
    try {
      val cleanToken = token.trim().lowercase()
      if (!cleanToken.matches(Regex("^[0-9a-f]{64}$"))) {
        return@withContext Result.failure(Exception("Invalid invite code"))
      }
      val tokenHash = java.security.MessageDigest.getInstance("SHA-256")
        .digest(cleanToken.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
      val payload = JSONObject().put("p_token_hash", tokenHash)
      if (!guestDisplayName.isNullOrBlank()) payload.put("p_guest_display_name", guestDisplayName.trim())
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/rpc/redeem_tour_invite")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) Result.success(body)
      else Result.failure(Exception("Invite could not be used: " + body))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun setTourAttendance(tourId: String, responseValue: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      require(responseValue in setOf("going", "not_going", "maybe"))
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("Sign in required"))
      val payload = JSONObject().put("tour_id", tourId).put("user_id", userId).put("response", responseValue)
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_attendance?on_conflict=tour_id,user_id")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Attendance update failed (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourAttendance(tourId: String): List<TourAttendance> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_attendance?tour_id=eq.$tourId&select=*").get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourAttendance::class.java)
      moshi.adapter<List<TourAttendance>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }


  suspend fun getTourGuestAttendance(tourId: String): List<TourGuestAttendance> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guest_attendance?tour_id=eq.$tourId&select=*")
        .get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourGuestAttendance::class.java)
      moshi.adapter<List<TourGuestAttendance>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun setTourGuestAttendance(tourId: String, guestMemberId: String, responseValue: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      require(responseValue in setOf("going", "not_going", "maybe"))
      val userId = sessionManager.sessionState.value.userId
        ?: return@withContext Result.failure(Exception("Sign in required"))
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("guest_member_id", guestMemberId)
        put("response", responseValue)
        put("updated_by", userId)
      }
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_guest_attendance?on_conflict=tour_id,guest_member_id")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Guest attendance update failed (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourDocuments(tourId: String): List<TourDocument> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_documents?tour_id=eq.$tourId&select=*&order=created_at.desc").get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourDocument::class.java)
      moshi.adapter<List<TourDocument>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun getTourImages(tourId: String): List<TourImage> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_images?tour_id=eq.$tourId&select=*&order=created_at.desc").get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourImage::class.java)
      moshi.adapter<List<TourImage>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  private suspend fun uploadStorageObject(bucket: String, path: String, bytes: ByteArray, mimeType: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder()
        .url("$baseUrl/storage/v1/object/$bucket/$path")
        .header("Content-Type", mimeType)
        .header("x-upsert", "false")
        .post(bytes.toRequestBody(mimeType.toMediaType()))
        .build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Storage upload failed (" + response.code + "): " + (response.body?.string() ?: "")))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadTourDocument(
    tourId: String, fileName: String, mimeType: String, bytes: ByteArray, category: String = "other"
  ): Result<TourDocument> = withContext(Dispatchers.IO) {
    val userId = sessionManager.sessionState.value.userId
      ?: return@withContext Result.failure(Exception("Sign in required"))
    if (bytes.isEmpty() || bytes.size > 20 * 1024 * 1024) {
      return@withContext Result.failure(Exception("File must be between 1 byte and 20 MB"))
    }
    val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "document" }
    val path = tourId + "/" + userId + "/" + java.util.UUID.randomUUID() + "_" + safeName
    val upload = uploadStorageObject("tour-documents", path, bytes, mimeType)
    if (upload.isFailure) return@withContext Result.failure(upload.exceptionOrNull()!!)
    try {
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("uploaded_by", userId)
        put("storage_path", path)
        put("file_name", fileName.take(160))
        put("mime_type", mimeType)
        put("file_size", bytes.size)
        put("category", category)
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_documents")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext Result.failure(Exception("Document record failed: " + body))
      val item = moshi.adapter(TourDocument::class.java).fromJson(JSONArray(body).getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Could not read uploaded document"))
      Result.success(item)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadTourImage(
    tourId: String, fileName: String, mimeType: String, bytes: ByteArray, caption: String? = null
  ): Result<TourImage> = withContext(Dispatchers.IO) {
    val userId = sessionManager.sessionState.value.userId
      ?: return@withContext Result.failure(Exception("Sign in required"))
    if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024) return@withContext Result.failure(Exception("Image must be 1 byte to 10 MB"))
    if (mimeType !in setOf("image/jpeg", "image/png", "image/webp")) return@withContext Result.failure(Exception("Only JPEG, PNG and WebP images are supported"))
    val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "image.jpg" }
    val path = tourId + "/" + userId + "/" + java.util.UUID.randomUUID() + "_" + safeName
    val upload = uploadStorageObject("tour-gallery", path, bytes, mimeType)
    if (upload.isFailure) return@withContext Result.failure(upload.exceptionOrNull()!!)
    try {
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("uploaded_by", userId)
        put("storage_path", path)
        if (!caption.isNullOrBlank()) put("caption", caption.take(300))
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_images")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext Result.failure(Exception("Image record failed: " + body))
      val item = moshi.adapter(TourImage::class.java).fromJson(JSONArray(body).getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Could not read uploaded image"))
      Result.success(item)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getSignedStorageUrl(bucket: String, path: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      require(bucket == "tour-documents" || bucket == "tour-gallery")
      val safePath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
      val payload = JSONObject().put("expiresIn", 3600)
      val req = Request.Builder().url("$baseUrl/storage/v1/object/sign/$bucket/$safePath")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful) return@withContext Result.failure(Exception("Could not create file link (" + response.code + ")"))
      val signed = JSONObject(body).optString("signedURL").ifBlank { JSONObject(body).optString("signedUrl") }
      if (signed.isBlank()) return@withContext Result.failure(Exception("Signed URL missing"))
      Result.success(if (signed.startsWith("http")) signed else baseUrl + signed)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markNotificationRead(notificationId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())
      val payload = JSONObject().put("read_at", timestamp)
      val req = Request.Builder().url("$baseUrl/rest/v1/notifications?id=eq.$notificationId")
        .patch(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not update notification (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourMemberPermissions(tourId: String): List<TourMemberPermission> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_member_permissions?tour_id=eq.$tourId&select=*").get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourMemberPermission::class.java)
      moshi.adapter<List<TourMemberPermission>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun setTourMemberPermissions(
    tourId: String, memberUserId: String, canEditTour: Boolean, canManageMembers: Boolean, canManageContent: Boolean
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val grantedBy = sessionManager.sessionState.value.userId ?: return@withContext Result.failure(Exception("Sign in required"))
      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("user_id", memberUserId)
        put("can_edit_tour", canEditTour)
        put("can_manage_members", canManageMembers)
        put("can_manage_content", canManageContent)
        put("granted_by", grantedBy)
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_member_permissions?on_conflict=tour_id,user_id")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not update member permissions (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun removeTourMember(tourId: String, memberUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_members?tour_id=eq.$tourId&user_id=eq.$memberUserId").delete().build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not remove member (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registerTourDevice(tourId: String, publicKey: String, deviceLabel: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("p_tour_id", tourId)
        put("p_public_key", publicKey)
        put("p_device_label", deviceLabel.take(80))
        put("p_platform", "android")
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/rpc/register_tour_device")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string()?.trim().orEmpty()
      if (response.isSuccessful && body.isNotBlank()) Result.success(body.trim('"'))
      else Result.failure(Exception("Device registration failed: " + body))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourDevices(tourId: String): List<TourDevice> = withContext(Dispatchers.IO) {
    try {
      val url = "$baseUrl/rest/v1/tour_devices?select=id,user_id,device_label,platform,revoked_at,last_seen_at,created_at,tour_device_links!inner(tour_id)&revoked_at=is.null&tour_device_links.tour_id=eq.$tourId"
      val req = Request.Builder().url(url).get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourDevice::class.java)
      moshi.adapter<List<TourDevice>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun revokeTourDevice(deviceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/rpc/revoke_tour_device")
        .post(JSONObject().put("p_device_id", deviceId).toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not revoke device (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun recordTourSyncOperation(
    operationId: String, tourId: String, deviceId: String, entityType: String, entityId: String,
    operationType: String, baseRevision: String?, payloadHash: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("p_operation_id", operationId)
        put("p_tour_id", tourId)
        put("p_device_id", deviceId)
        put("p_entity_type", entityType)
        put("p_entity_id", entityId)
        put("p_operation_type", operationType)
        put("p_base_revision", baseRevision ?: JSONObject.NULL)
        put("p_payload_hash", payloadHash)
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/rpc/record_tour_sync_operation")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: ""
      if (response.isSuccessful) Result.success(body)
      else Result.failure(Exception("Sync operation rejected: " + body))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun completeTourSyncOperation(operationId: String, status: String, result: JSONObject = JSONObject()): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("p_operation_id", operationId)
        put("p_status", status)
        put("p_result", result)
      }
      val req = Request.Builder().url("$baseUrl/rest/v1/rpc/complete_tour_sync_operation")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not complete sync operation (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getTourSyncConflicts(tourId: String): List<TourSyncConflict> = withContext(Dispatchers.IO) {
    try {
      val req = Request.Builder().url("$baseUrl/rest/v1/tour_sync_conflicts?tour_id=eq.$tourId&resolved_at=is.null&select=*&order=created_at.desc").get().build()
      val response = okHttpClient.newCall(req).execute()
      val body = response.body?.string() ?: return@withContext emptyList()
      if (!response.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, TourSyncConflict::class.java)
      moshi.adapter<List<TourSyncConflict>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun resolveTourSyncConflict(conflictId: String, resolution: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      require(resolution in setOf("accepted_server", "accepted_client", "merged", "discarded"))
      val payload = JSONObject().put("p_conflict_id", conflictId).put("p_resolution", resolution)
      val req = Request.Builder().url("$baseUrl/rest/v1/rpc/resolve_tour_sync_conflict")
        .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
      val response = okHttpClient.newCall(req).execute()
      if (response.isSuccessful) Result.success(true)
      else Result.failure(Exception("Could not resolve sync conflict (" + response.code + ")"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // LIKES & SAVED TOURS
  // --------------------------------------------------------------------------

  suspend fun getSavedTourIds(userId: String): Set<String> = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext emptySet()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/saved_tours?user_id=eq.$userId&select=tour_id")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptySet()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptySet()
      val arr = JSONArray(body)
      val ids = mutableSetOf<String>()
      for (i in 0 until arr.length()) {
        ids.add(arr.getJSONObject(i).getString("tour_id"))
      }
      ids
    } catch (e: Exception) {
      emptySet()
    }
  }

  suspend fun toggleSaveTour(tourId: String, save: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId ?: return@withContext Result.failure(Exception("Not logged in"))
      if (save) {
        val payload = JSONObject().apply {
          put("tour_id", tourId)
          put("user_id", userId)
        }
        val req = Request.Builder()
          .url("$baseUrl/rest/v1/saved_tours")
          .post(payload.toString().toRequestBody("application/json".toMediaType()))
          .build()
        val resp = okHttpClient.newCall(req).execute()
        if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Save failed"))
      } else {
        val req = Request.Builder()
          .url("$baseUrl/rest/v1/saved_tours?tour_id=eq.$tourId&user_id=eq.$userId")
          .delete()
          .build()
        val resp = okHttpClient.newCall(req).execute()
        if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Unsave failed"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // --------------------------------------------------------------------------
  // NOTIFICATIONS
  // --------------------------------------------------------------------------

  suspend fun getNotifications(userId: String): List<AppNotification> = withContext(Dispatchers.IO) {
    if (userId.isBlank()) return@withContext emptyList()
    try {
      val req = Request.Builder()
        .url("$baseUrl/rest/v1/notifications?user_id=eq.$userId&select=*&order=created_at.desc")
        .get()
        .build()
      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: return@withContext emptyList()
      if (!resp.isSuccessful || !isValidJsonArray(body)) return@withContext emptyList()
      val type = Types.newParameterizedType(List::class.java, AppNotification::class.java)
      moshi.adapter<List<AppNotification>>(type).fromJson(body) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }
}
