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
    .readTimeout(20, TimeUnit.SECONDS)
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
        sessionManager.saveSession(accessToken, userId, email, fullName, username)
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

      sessionManager.saveSession(token, userId, email, fullName, username)
      Result.success("Signed in")
    } catch (e: Exception) {
      Log.e("SupabaseClient", "signIn error", e)
      Result.failure(e)
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
        adapter.fromJson(arr.getJSONObject(0).toString())
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
      val payload = JSONObject().apply {
        put("full_name", fullName)
        put("username", username)
        if (bio != null) put("bio", bio)
        val location = listOfNotNull(city?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() }).joinToString(", ")
        if (location.isNotBlank()) put("location", location)
        if (phone != null) put("phone", phone)
        if (avatarUrl != null) put("avatar_url", avatarUrl)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
        .header("Prefer", "return=representation")
        .patch(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      if (resp.isSuccessful) {
        sessionManager.updateProfile(fullName, username, avatarUrl)
        Result.success(true)
      } else {
        Result.failure(Exception("Update failed (${resp.code})"))
      }
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

  suspend fun createTourExpense(
    tourId: String,
    title: String,
    amount: Double,
    category: String,
    description: String?
  ): Result<TourExpense> = withContext(Dispatchers.IO) {
    try {
      val userId = sessionManager.sessionState.value.userId

      val payload = JSONObject().apply {
        put("tour_id", tourId)
        put("title", title)
        put("amount", amount)
        put("category", category)
        if (!userId.isNullOrEmpty()) put("paid_by", userId)
        if (!description.isNullOrBlank()) put("notes", description)
      }

      val req = Request.Builder()
        .url("$baseUrl/rest/v1/tour_expenses")
        .header("Prefer", "return=representation")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val resp = okHttpClient.newCall(req).execute()
      val body = resp.body?.string() ?: ""

      if (!resp.isSuccessful || !isValidJsonArray(body)) {
        return@withContext Result.failure(Exception("Failed to add expense: $body (${resp.code})"))
      }

      val arr = JSONArray(body)
      if (arr.length() == 0) return@withContext Result.failure(Exception("No expense returned"))
      val created = moshi.adapter(TourExpense::class.java).fromJson(arr.getJSONObject(0).toString())
        ?: return@withContext Result.failure(Exception("Failed to parse expense"))

      Result.success(created)
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
