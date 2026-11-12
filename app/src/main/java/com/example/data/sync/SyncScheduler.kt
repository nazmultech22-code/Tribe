package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
  private const val PERIODIC_WORK_NAME = "tribe-periodic-outbox-sync"
  private const val IMMEDIATE_WORK_NAME = "tribe-immediate-outbox-sync"

  private fun networkConstraints() = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

  fun schedulePeriodic(context: Context) {
    val request = PeriodicWorkRequestBuilder<OutboxSyncWorker>(15, TimeUnit.MINUTES)
      .setConstraints(networkConstraints())
      .build()
    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request
    )
  }

  fun enqueueNow(context: Context) {
    val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
      .setConstraints(networkConstraints())
      .setBackoffCriteria(
        androidx.work.BackoffPolicy.EXPONENTIAL,
        30,
        TimeUnit.SECONDS
      )
      .build()
    WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
      IMMEDIATE_WORK_NAME,
      ExistingWorkPolicy.KEEP,
      request
    )
  }
}
