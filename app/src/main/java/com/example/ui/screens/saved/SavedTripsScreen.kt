package com.example.ui.screens.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.room.SavedTripEntity
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.StatusBadge
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenContainer
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary

@Composable
fun SavedTripsScreen(
  viewModel: SavedTripsViewModel,
  repository: TribeRepository,
  onNavigateToTourDetail: (String) -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsStateWithLifecycle()
  val lang = sessionState.language

  val savedTrips by viewModel.tripsState.collectAsStateWithLifecycle()
  val currentFilter by viewModel.filterStatus.collectAsStateWithLifecycle()

  var tripToDelete by remember { mutableStateOf<SavedTripEntity?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.savedTripsTitle(lang),
        subtitle = L10n.roomDatabaseTag(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onBackClick
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = TribeGreenPrimary,
        contentColor = Color.White,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(L10n.addSavedTrip(lang), fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("fab_add_saved_trip")
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Room Database Info Banner
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            tint = TribeGreenDark,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (lang == AppLanguage.BANGLA) "Room SQLite অফলাইন স্টোরেজ" else "Room SQLite Offline Storage",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TribeGreenDark
              )
            )
            Text(
              text = if (lang == AppLanguage.BANGLA) "এই ট্রিপগুলো আপনার ডিভাইসের Room লোকাল ডাটাবেজে সংরক্ষিত রয়েছে।" else "These trips are stored in your device's local Room database.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TribeGreenDark.copy(alpha = 0.8f)
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Filter Chips (All, Planning, Ongoing, Completed)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = currentFilter == "all",
          onClick = { viewModel.setFilter("all") },
          label = { Text(if (lang == AppLanguage.BANGLA) "সব (${savedTrips.size})" else "All (${savedTrips.size})") },
          modifier = Modifier.testTag("filter_all")
        )
        FilterChip(
          selected = currentFilter == "planning",
          onClick = { viewModel.setFilter("planning") },
          label = { Text(L10n.statusPlanning(lang)) },
          modifier = Modifier.testTag("filter_planning")
        )
        FilterChip(
          selected = currentFilter == "ongoing",
          onClick = { viewModel.setFilter("ongoing") },
          label = { Text(L10n.statusOngoing(lang)) },
          modifier = Modifier.testTag("filter_ongoing")
        )
        FilterChip(
          selected = currentFilter == "completed",
          onClick = { viewModel.setFilter("completed") },
          label = { Text(L10n.statusCompleted(lang)) },
          modifier = Modifier.testTag("filter_completed")
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (savedTrips.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.BookmarkBorder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = L10n.noSavedTrips(lang),
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { showAddDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(L10n.addSavedTrip(lang))
            }
          }
        }
      } else {
        // MANDATORY: Displays list of saved trips using LazyColumn fetching from Room database
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("lazy_column_saved_trips"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(
            items = savedTrips,
            key = { it.id }
          ) { trip ->
            SavedTripCard(
              trip = trip,
              lang = lang,
              onDelete = { tripToDelete = trip },
              onViewDetails = { onNavigateToTourDetail(trip.id) }
            )
          }

          item {
            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (tripToDelete != null) {
    AlertDialog(
      onDismissRequest = { tripToDelete = null },
      title = { Text(L10n.deleteBtn(lang)) },
      text = {
        Text(
          text = if (lang == AppLanguage.BANGLA) "আপনি কি নিশ্চিতভাবে '${tripToDelete?.title}' ট্রিপটি Room ডাটাবেজ থেকে মুছে ফেলতে চান?" else "Are you sure you want to delete '${tripToDelete?.title}' from the Room database?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            tripToDelete?.let { viewModel.deleteTrip(it.id) }
            tripToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(L10n.deleteBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { tripToDelete = null }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Add Trip Dialog for quick local Room insertion
  if (showAddDialog) {
    var newTitle by remember { mutableStateOf("") }
    var newDest by remember { mutableStateOf("") }
    var newBudget by remember { mutableStateOf("12000") }
    var newStatus by remember { mutableStateOf("planning") }
    var newNotes by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(L10n.addSavedTrip(lang)) },
      text = {
        Column {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text(L10n.tourTitle(lang)) },
            placeholder = { Text("e.g. সাজেক ভ্যালি ট্যুর") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_saved_trip_title"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newDest,
            onValueChange = { newDest = it },
            label = { Text(L10n.destinationName(lang)) },
            placeholder = { Text("e.g. সাজেক ভ্যালি") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_saved_trip_dest"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newBudget,
            onValueChange = { newBudget = it },
            label = { Text(L10n.estimatedBudget(lang)) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_saved_trip_budget"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newNotes,
            onValueChange = { newNotes = it },
            label = { Text(L10n.description(lang)) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_saved_trip_notes"),
            minLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newTitle.isNotBlank()) {
              viewModel.saveTrip(
                title = newTitle.trim(),
                destinationName = newDest.trim().ifEmpty { "বাংলাদেশ" },
                startDate = "2026-12-14",
                endDate = "2026-12-16",
                status = newStatus,
                estimatedBudget = newBudget.toDoubleOrNull() ?: 10000.0,
                actualExpenses = 0.0,
                memberCount = 4,
                notes = newNotes.trim().ifBlank { null }
              )
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
          modifier = Modifier.testTag("btn_confirm_add_saved_trip")
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }
}

@Composable
fun SavedTripCard(
  trip: SavedTripEntity,
  lang: AppLanguage,
  onDelete: () -> Unit,
  onViewDetails: () -> Unit
) {
  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_saved_trip_${trip.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = trip.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadge(status = trip.status, lang = lang)
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Destination and dates
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Place,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = trip.destinationName,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        )

        if (!trip.startDate.isNullOrBlank()) {
          Spacer(modifier = Modifier.width(10.dp))
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${trip.startDate}${if (!trip.endDate.isNullOrBlank()) " - ${trip.endDate}" else ""}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }

      if (!trip.notes.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = trip.notes,
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Cost summary box
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if ((trip.actualExpenses ?: 0.0) > 0) L10n.actualExpense(lang) else L10n.estimatedBudget(lang),
              style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            val amount = if ((trip.actualExpenses ?: 0.0) > 0) trip.actualExpenses else trip.estimatedBudget
            Text(
              text = L10n.formatCurrency(amount, lang),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TribeGreenDark
              )
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier.testTag("btn_delete_saved_${trip.id}")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = L10n.deleteBtn(lang),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
            }

            OutlinedButton(
              onClick = onViewDetails,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_view_saved_${trip.id}")
            ) {
              Text(
                text = L10n.viewDetails(lang),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }
  }
}
