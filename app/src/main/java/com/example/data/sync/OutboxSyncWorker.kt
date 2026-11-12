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
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
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

    val supabase = SupabaseClient(sessionManager)
    val client = OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .callTimeout(45, TimeUnit.SECONDS)
      .build()
    val allowedTables = setOf(
      "tours", "tour_members", "tour_itinerary_items", "tour_tasks",
      "tour_messages", "tour_expenses", "tour_join_requests",
      "saved_tours", "tour_guest_members", "tour_attendance",
      "tour_guest_attendance", "tour_documents", "tour_images", "tour_member_permissions", "rpc_create_tour_expense"
    )
    val entityTypes = mapOf(
      "tours" to "tour",
      "tour_itinerary_items" to "itinerary",
      "tour_tasks" to "task",
      "tour_expenses" to "expense",
      "tour_messages" to "message",
      "tour_guest_members" to "guest",
      "tour_attendance" to "attendance",
      "tour_guest_attendance" to "attendance",
      "tour_documents" to "document",
      "tour_images" to "image",
      "tour_members" to "member",
      "tour_join_requests" to "join_request",
      "tour_member_permissions" to "member_permission",
      "rpc_create_tour_expense" to "expense"
    )
    var retryNeeded = false

    for (operation in operations) {
      if (operation.tableName !in allowedTables) {
        dao.markFailed(operation.id, "Unsupported sync table: " + operation.tableName)
        continue
      }

      if (sessionManager.sessionState.value.accessToken.isNullOrBlank()) {
        retryNeeded = true
        break
      }

      val payload = try {
        JSONObject(operation.payloadJson)
      } catch (_: Exception) {
        dao.markFailed(operation.id, "Invalid queued JSON payload")
        continue
      }
      val tourId = operation.tourId ?: payload.optString("tour_id").takeIf { it.isNotBlank() }
      val baseRevision = payload.optString("_base_revision").takeIf { it.isNotBlank() }
      payload.remove("_base_revision")
      val payloadJson = payload.toString()

      // The initial tour insert creates the parent row; device linking follows later.
      var registeredDeviceId: String? = null
      val entityType = entityTypes[operation.tableName]
      if (tourId != null && entityType != null &&
        !(operation.tableName == "tours" && operation.operationType.equals("create", true))
      ) {
        val publicKey = runCatching { DeviceIdentityKeyStore.publicKeyBase64() }.getOrNull()
        if (publicKey.isNullOrBlank()) {
          dao.markFailed(operation.id, "Could not access this device's Android Keystore identity")
          continue
        }
        val registration = supabase.registerTourDevice(
          tourId,
          publicKey,
          DeviceIdentityKeyStore.deviceLabel()
        )
        registeredDeviceId = registration.getOrNull()
        if (registeredDeviceId == null) {
          dao.recordFailure(operation.id, registration.exceptionOrNull()?.localizedMessage ?: "Could not register this device")
          retryNeeded = true
          continue
        }

        val payloadHash = MessageDigest.getInstance("SHA-256")
          .digest(operation.payloadJson.toByteArray(Charsets.UTF_8))
          .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val syncRecord = supabase.recordTourSyncOperation(
          operationId = operation.id,
          tourId = tourId,
          deviceId = registeredDeviceId,
          entityType = entityType,
          entityId = operation.recordId,
          operationType = operation.operationType,
          baseRevision = baseRevision,
          payloadHash = payloadHash
        )
        if (syncRecord.isFailure) {
          dao.recordFailure(operation.id, syncRecord.exceptionOrNull()?.localizedMessage ?: "Sync validation failed")
          retryNeeded = true
          continue
        }
        val syncStatus = runCatching { JSONObject(syncRecord.getOrThrow()).optString("status") }.getOrDefault("received")
        if (syncStatus == "conflict") {
          dao.markConflict(operation.id, "Server version changed after this device's last sync")
          continue
        }
        if (syncStatus !in setOf("received", "applied")) {
          dao.markFailed(operation.id, "Server rejected sync operation: " + syncStatus)
          continue
        }
      }

      val baseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
      val filter = when {
        operation.operationType.equals("create", true) -> ""
        operation.tableName == "tour_guest_attendance" && tourId != null ->
          "?tour_id=eq." + tourId + "&guest_member_id=eq." + operation.recordId
        operation.tableName in setOf("tour_attendance", "tour_members", "tour_member_permissions") && tourId != null ->
          "?tour_id=eq." + tourId + "&user_id=eq." + operation.recordId
        else -> "?id=eq." + operation.recordId
      }
      val url = if (operation.tableName == "rpc_create_tour_expense") {
        baseUrl + "/rest/v1/rpc/create_tour_expense"
      } else {
        baseUrl + "/rest/v1/" + operation.tableName + filter
      }
      val builder = Request.Builder()
      builder.url(url)
      builder.header("apikey", BuildConfig.SUPABASE_ANON_KEY)
      builder.header("Authorization", "Bearer " + sessionManager.sessionState.value.accessToken)
      builder.header("Content-Type", "application/json")
      builder.header("Prefer", "resolution=merge-duplicates,return=minimal")
      val body = payloadJson.toRequestBody("application/json".toMediaType())
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
            response.isSuccessful -> {
              if (registeredDeviceId != null) {
                val completed = supabase.completeTourSyncOperation(
                  operation.id,
                  "applied",
                  JSONObject().put("http_status", response.code)
                )
                if (completed.isFailure) {
                  dao.recordFailure(operation.id, completed.exceptionOrNull()?.localizedMessage ?: "Could not finalize sync record")
                  retryNeeded = true
                } else dao.markCompleted(operation.id)
              } else dao.markCompleted(operation.id)
            }
            response.code == 401 -> {
              if (supabase.refreshSession()) {
                dao.recordFailure(operation.id, "Session refreshed; retrying operation")
                retryNeeded = true
                stopForRefresh = true
              } else dao.markFailed(operation.id, "Session expired. Sign in again to sync.")
            }
            response.code == 408 || response.code == 429 || response.code >= 500 -> {
              dao.recordFailure(operation.id, "Temporary server error: HTTP " + response.code)
              retryNeeded = true
            }
            else -> {
              if (registeredDeviceId != null) {
                supabase.completeTourSyncOperation(operation.id, "rejected", JSONObject().put("http_status", response.code))
              }
              dao.markFailed(operation.id, "HTTP " + response.code + ": " + (response.body?.string() ?: "Request failed"))
            }
          }
        }
      } catch (e: IOException) {
        dao.recordFailure(operation.id, e.localizedMessage ?: "Network error")
        retryNeeded = true
      }
      if (stopForRefresh) break
    }

    if (retryNeeded || dao.getPending(limit = 1).isNotEmpty()) Result.retry() else Result.success()
  }
}
