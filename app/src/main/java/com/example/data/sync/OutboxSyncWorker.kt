package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.local.UserSessionManager
import com.example.data.local.room.TribeRoomDatabase
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OutboxSyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val sessionManager = UserSessionManager.getInstance(applicationContext)
    if (!sessionManager.sessionState.value.isLoggedIn) return@withContext Result.success()

    val dao = TribeRoomDatabase.getDatabase(applicationContext).syncOperationDao()
    val operations = dao.getPending(limit = 100)
    if (operations.isEmpty()) {
      dao.pruneCompleted(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
      return@withContext Result.success()
    }

    val client = OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .callTimeout(45, TimeUnit.SECONDS)
      .build()
    val allowedTables = setOf(
      "tours", "tour_members", "tour_itinerary_items", "tour_tasks",
      "tour_messages", "tour_expenses", "tour_join_requests",
      "saved_tours", "tour_guest_members"
    )
    var retryNeeded = false

    for (operation in operations) {
      if (operation.tableName !in allowedTables) {
        dao.markFailed(operation.id, "Unsupported sync table: " + operation.tableName)
        continue
      }

      val token = sessionManager.sessionState.value.accessToken
      if (token.isNullOrBlank()) {
        retryNeeded = true
        break
      }

      val baseUrl = BuildConfig.SUPABASE_URL.trimEnd("/")
      val filter = if (operation.operationType.equals("create", true)) "" else "?id=eq." + operation.recordId
      val url = baseUrl + "/rest/v1/" + operation.tableName + filter
      val builder = Request.Builder()
        .url(url)
        .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
        .header("Authorization", "Bearer " + token)
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")

      val body = operation.payloadJson.toRequestBody("application/json".toMediaType())
      val request = when (operation.operationType.lowercase()) {
        "create" -> builder.post(body).build()
        "update" -> builder.patch(body).build()
        "delete" -> builder.delete().build()
        else -> {
          dao.markFailed(operation.id, "Unsupported sync operation: " + operation.operationType)
          continue
        }
      }

      var stopForRefresh = false
      try {
        client.newCall(request).execute().use { response ->
          when {
            response.isSuccessful -> dao.markCompleted(operation.id)
            response.code == 401 -> {
              val refreshed = SupabaseClient(sessionManager).refreshSession()
              if (refreshed) {
                dao.recordFailure(operation.id, "Session refreshed; retrying operation")
                retryNeeded = true
                stopForRefresh = true
              } else {
                dao.markFailed(operation.id, "Session expired. Sign in again to sync.")
              }
            }
            response.code == 408 || response.code == 429 || response.code >= 500 -> {
              dao.recordFailure(operation.id, "Temporary server error: HTTP " + response.code)
              retryNeeded = true
            }
            else -> dao.markFailed(operation.id, "HTTP " + response.code + ": " + (response.body?.string() ?: "Request failed"))
          }
        }
      } catch (e: IOException) {
        dao.recordFailure(operation.id, e.localizedMessage ?: "Network error")
        retryNeeded = true
      }
      if (stopForRefresh) break
    }

    if (retryNeeded) Result.retry() else Result.success()
  }
}
