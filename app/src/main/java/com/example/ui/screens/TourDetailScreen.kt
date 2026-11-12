package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.StatusBadge
import com.example.ui.components.TribeHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TourDetailScreen(
  tourId: String,
  repository: TribeRepository,
  onNavigateToDestination: (Destination) -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var tour by remember { mutableStateOf<Tour?>(null) }
  var destination by remember { mutableStateOf<Destination?>(null) }
  var expenses by remember { mutableStateOf<List<TourExpense>>(emptyList()) }
  var itinerary by remember { mutableStateOf<List<TourItineraryItem>>(emptyList()) }
  var checklists by remember { mutableStateOf<List<TourChecklistItem>>(emptyList()) }
  var guides by remember { mutableStateOf<List<TourGuide>>(emptyList()) }
  var comments by remember { mutableStateOf<List<TourComment>>(emptyList()) }
  var members by remember { mutableStateOf<List<TourMember>>(emptyList()) }

  var isLoading by remember { mutableStateOf(true) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var snackbarMessage by remember { mutableStateOf<String?>(null) }

  // Dialog states
  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var showAddItineraryDialog by remember { mutableStateOf(false) }
  var showAddChecklistDialog by remember { mutableStateOf(false) }
  var showAddGuideDialog by remember { mutableStateOf(false) }
  var newCommentText by remember { mutableStateOf("") }

  fun loadTourData() {
    scope.launch {
      isLoading = true
      val t = repository.supabaseClient.getTourById(tourId)
      tour = t
      if (t != null) {
        if (t.destinationName.isNotBlank()) {
          destination = com.example.data.model.Destination(name = t.destinationName)
        }
        expenses = repository.supabaseClient.getTourExpenses(tourId)
        itinerary = repository.supabaseClient.getTourItinerary(tourId)
        checklists = repository.supabaseClient.getTourChecklist(tourId)
        guides = repository.supabaseClient.getGuidesForTour(tourId)
        comments = repository.supabaseClient.getTourComments(tourId)
        members = repository.supabaseClient.getTourMembers(tourId)
      }
      isLoading = false
    }
  }

  LaunchedEffect(tourId) {
    loadTourData()
  }

  val isOwner = tour != null && (tour?.ownerId == sessionState.userId || sessionState.userId == null)
  val isPublic = tour?.visibility == "public"

  val context = androidx.compose.ui.platform.LocalContext.current
  val savedTripsRepo = remember {
    com.example.data.repository.SavedTripsRepository.getInstance(context.applicationContext)
  }
  val isSavedInRoom by savedTripsRepo.isTripSaved(tourId).collectAsState(initial = false)

  Scaffold(
    topBar = {
      TribeHeader(
        title = tour?.title ?: "ট্যুরের বিস্তারিত",
        subtitle = destination?.name,
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onBackClick,
        actions = {
          // Save to Room DB bookmark button
          if (tour != null) {
            IconButton(
              onClick = {
                val currentTour = tour ?: return@IconButton
                scope.launch {
                  if (isSavedInRoom) {
                    savedTripsRepo.deleteTripById(currentTour.id)
                    snackbarMessage = L10n.removedFromRoomSuccess(lang)
                  } else {
                    val expTotal = expenses.sumOf { it.amount }
                    val entity = com.example.data.local.room.SavedTripEntity(
                      id = currentTour.id,
                      title = currentTour.title,
                      destinationName = destination?.name ?: currentTour.destinationName.ifBlank { "Bangladesh" },
                      startDate = currentTour.startDate,
                      endDate = currentTour.endDate,
                      status = currentTour.status,
                      estimatedBudget = if (expTotal > 0) expTotal else 12000.0,
                      actualExpenses = expTotal,
                      memberCount = currentTour.maxMembers ?: 4,
                      notes = currentTour.description,
                      savedAt = System.currentTimeMillis()
                    )
                    savedTripsRepo.insertTrip(entity)
                    snackbarMessage = L10n.savedToRoomSuccess(lang)
                  }
                }
              },
              modifier = Modifier.testTag("btn_save_to_room")
            ) {
              Icon(
                imageVector = if (isSavedInRoom) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = L10n.saveToRoom(lang),
                tint = if (isSavedInRoom) TribeGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Share Tour Toggle (Requirement 8)
          if (isOwner && tour != null) {
            IconButton(
              onClick = {
                val nextVis = if (isPublic) "private" else "public"
                if (nextVis == "public" && tour?.destinationName.isNullOrBlank()) {
                  snackbarMessage = L10n.mustSelectDestinationBeforeSharing(lang)
                  return@IconButton
                }
                scope.launch {
                  val res = repository.supabaseClient.updateTourVisibility(tourId, nextVis)
                  if (res.isSuccess) {
                    tour = tour?.copy(isPublic = nextVis == "public")
                    snackbarMessage = if (nextVis == "public") L10n.sharedSuccess(lang) else L10n.madePrivateSuccess(lang)
                  } else {
                    snackbarMessage = res.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
                  }
                }
              },
              modifier = Modifier.testTag("btn_share_tour_toggle")
            ) {
              Icon(
                imageVector = if (isPublic) Icons.Default.Share else Icons.Default.Lock,
                contentDescription = if (isPublic) L10n.makePrivate(lang) else L10n.shareTour(lang),
                tint = if (isPublic) TribeGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      )
    }
  ) { padding ->
    if (isLoading && tour == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = TribeGreenPrimary)
      }
      return@Scaffold
    }

    if (tour == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center
      ) {
        Text(text = if (lang == AppLanguage.BANGLA) "ট্যুরটি খুঁজে পাওয়া যায়নি" else "Tour not found")
      }
      return@Scaffold
    }

    val currentTour = tour!!
    val totalExpense = expenses.sumOf { it.amount }
    val memberCount = (members.size.coerceAtLeast(1))
    val perPersonExpense = totalExpense / memberCount

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Snackbar notification banner
      if (snackbarMessage != null) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { snackbarMessage = null }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = snackbarMessage ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { snackbarMessage = null }) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // 1. Tour Hero Header Card
      item {
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = currentTour.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                modifier = Modifier.weight(1f)
              )
              StatusBadge(status = currentTour.status, lang = lang)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Destination, dates & member count
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              if (destination != null) {
                Surface(
                  onClick = { onNavigateToDestination(destination!!) },
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Place,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = destination!!.name,
                      style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                      )
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = L10n.activeMembers(memberCount, lang),
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            }

            if (!currentTour.startDate.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "${currentTour.startDate}${if (!currentTour.endDate.isNullOrBlank()) " - ${currentTour.endDate}" else ""}",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }

            // Owner Sharing Control Row (Requirement 8)
            if (isOwner) {
              Spacer(modifier = Modifier.height(14.dp))
              Divider()
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = if (isPublic) L10n.visibilityPublic(lang) else L10n.visibilityPrivate(lang),
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isPublic) TribeGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                  Text(
                    text = if (isPublic) (if (lang == AppLanguage.BANGLA) "গন্তব্য পেজে সবাই দেখতে পাচ্ছে" else "Visible in destination search")
                    else (if (lang == AppLanguage.BANGLA) "শুধুমাত্র আপনি ও আমন্ত্রিত সদস্যরা" else "Only you and members"),
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }

                Button(
                  onClick = {
                    val nextVis = if (isPublic) "private" else "public"
                    if (nextVis == "public" && currentTour.destinationName.isBlank()) {
                      snackbarMessage = L10n.mustSelectDestinationBeforeSharing(lang)
                      return@Button
                    }
                    scope.launch {
                      val res = repository.supabaseClient.updateTourVisibility(tourId, nextVis)
                      if (res.isSuccess) {
                        tour = tour?.copy(isPublic = nextVis == "public")
                        snackbarMessage = if (nextVis == "public") L10n.sharedSuccess(lang) else L10n.madePrivateSuccess(lang)
                      }
                    }
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPublic) MaterialTheme.colorScheme.surfaceVariant else TribeGreenPrimary,
                    contentColor = if (isPublic) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                  ),
                  modifier = Modifier.testTag("btn_share_tour_main")
                ) {
                  Icon(
                    imageVector = if (isPublic) Icons.Default.Lock else Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isPublic) L10n.makePrivate(lang) else L10n.shareTour(lang),
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }

      // 2. Financial Highlight Box (Matches mockup: মোট খরচ ৳ ২২,০০০ / জনপ্রতি ৳ ৪,৪০০)
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = L10n.actualExpense(lang),
                style = MaterialTheme.typography.labelMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = L10n.formatCurrency(totalExpense, lang),
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = TribeGreenDark
                )
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = L10n.perPersonActual(lang),
                style = MaterialTheme.typography.labelMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = L10n.formatCurrency(perPersonExpense, lang),
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
          }
        }
      }

      // 3. Tab Selector
      item {
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          edgePadding = 0.dp,
          containerColor = Color.Transparent,
          modifier = Modifier.fillMaxWidth()
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text(L10n.tabOverview(lang), fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text(L10n.tabItinerary(lang), fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text(L10n.tabChecklist(lang), fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            text = { Text(L10n.tabGuides(lang), fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 4,
            onClick = { selectedTab = 4 },
            text = { Text(L10n.tabComments(lang), fontWeight = FontWeight.SemiBold) }
          )
        }
      }

      // TAB 0: OVERVIEW & EXPENSES
      if (selectedTab == 0) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = L10n.expenseBreakdown(lang),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { showAddExpenseDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              modifier = Modifier.testTag("btn_add_expense_open")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(L10n.addExpense(lang))
            }
          }
        }

        if (expenses.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = L10n.noExpensesRecorded(lang),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          // Category Totals Summary
          item {
            val transTotal = expenses.filter { it.category.equals("Transport", true) }.sumOf { it.amount }
            val hotelTotal = expenses.filter { it.category.equals("Accommodation", true) }.sumOf { it.amount }
            val foodTotal = expenses.filter { it.category.equals("Food", true) }.sumOf { it.amount }
            val otherTotal = totalExpense - (transTotal + hotelTotal + foodTotal)

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                CategoryExpenseRow(Icons.Default.DirectionsBus, L10n.transportExpense(lang), transTotal, lang)
                CategoryExpenseRow(Icons.Default.Hotel, L10n.accommodationExpense(lang), hotelTotal, lang)
                CategoryExpenseRow(Icons.Default.Restaurant, L10n.foodExpense(lang), foodTotal, lang)
                CategoryExpenseRow(Icons.Default.Category, L10n.otherExpense(lang), otherTotal, lang)
              }
            }
          }

          // Individual expense items (for authorized members)
          items(expenses) { exp ->
            ElevatedCard(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = exp.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                  )
                  Text(
                    text = exp.category,
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  )
                }

                Text(
                  text = L10n.formatCurrency(exp.amount, lang),
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = TribeGreenDark
                )
              }
            }
          }
        }
      }

      // TAB 1: ITINERARY
      if (selectedTab == 1) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = L10n.tabItinerary(lang),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { showAddItineraryDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              modifier = Modifier.testTag("btn_add_itinerary_open")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(L10n.addItineraryDay(lang))
            }
          }
        }

        if (itinerary.isEmpty()) {
          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "কোনো সময়সূচি এখনো যোগ করা হয়নি।" else "No itinerary added yet.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp)
            )
          }
        } else {
          items(itinerary) { item ->
            ElevatedCard(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.Top
              ) {
                Surface(
                  shape = CircleShape,
                  color = TribeGreenContainer,
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "${item.dayNumber}",
                      fontWeight = FontWeight.Bold,
                      color = TribeGreenDark
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                  if (!item.location.isNullOrBlank()) {
                    Text(
                      text = "📍 ${item.location}",
                      style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                  }
                  if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = item.description,
                      style = MaterialTheme.typography.bodyMedium
                    )
                  }
                }
              }
            }
          }
        }
      }

      // TAB 2: CHECKLIST
      if (selectedTab == 2) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = L10n.tabChecklist(lang),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { showAddChecklistDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              modifier = Modifier.testTag("btn_add_checklist_open")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(L10n.addChecklistItem(lang))
            }
          }
        }

        if (checklists.isEmpty()) {
          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "চেকলিস্ট খালি। ভ্রমণের জন্য প্রয়োজনীয় জিনিস যোগ করুন।" else "Checklist is empty. Add travel essentials.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp)
            )
          }
        } else {
          items(checklists) { item ->
            ElevatedCard(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = item.isCompleted,
                  onCheckedChange = { checked ->
                    scope.launch {
                      val result = repository.supabaseClient.toggleChecklistItem(item.id, checked)
                      if (!result.isSuccess && result.exceptionOrNull() is IOException) {
                        val queued = repository.enqueueSyncOperation(
                          "update", "tour_tasks", item.id,
                          JSONObject().put("status", if (checked) "done" else "todo").toString()
                        )
                        if (queued) snackbarMessage = if (lang == AppLanguage.BANGLA) "পরিবর্তনটি অফলাইনে সংরক্ষিত; পরে Sync হবে" else "Change saved offline and queued to sync"
                      }
                      checklists = checklists.map { if (it.id == item.id) it.copy(status = if (checked) "done" else "todo") else it }
                    }
                  }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = item.title,
                  fontWeight = FontWeight.Medium,
                  style = if (item.isCompleted) MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  ) else MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }
      }

      // TAB 3: GUIDES (Mockup: মোঃ রকিকুল ইসলাম, 01712-345678)
      if (selectedTab == 3) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = L10n.tabGuides(lang),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { showAddGuideDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              modifier = Modifier.testTag("btn_add_guide_open")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(L10n.addGuide(lang))
            }
          }
        }

        if (guides.isEmpty()) {
          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "কোনো লোকাল গাইড এখনো যুক্ত করা হয়নি।" else "No local guide added yet.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp)
            )
          }
        } else {
          items(guides) { g ->
            ElevatedCard(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = CircleShape,
                    color = TribeGreenContainer,
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Person, contentDescription = null, tint = TribeGreenDark)
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(text = g.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!g.phone.isNullOrBlank()) {
                      Text(
                        text = "📞 ${g.phone}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                      )
                    }
                  }
                }

                if (!g.phone.isNullOrBlank()) {
                  IconButton(onClick = {}) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = TribeGreenPrimary)
                  }
                }
              }
            }
          }
        }
      }

      // TAB 4: COMMENTS
      if (selectedTab == 4) {
        item {
          Text(
            text = L10n.tabComments(lang),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = newCommentText,
              onValueChange = { newCommentText = it },
              placeholder = { Text(L10n.addComment(lang)) },
              modifier = Modifier
                .weight(1f)
                .testTag("input_tour_comment"),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (newCommentText.isNotBlank()) {
                  scope.launch {
                    val messageText = newCommentText.trim()
                    val res = repository.supabaseClient.postTourComment(tourId, messageText)
                    if (res.isSuccess) {
                      comments = repository.supabaseClient.getTourComments(tourId)
                      newCommentText = ""
                    } else if (res.exceptionOrNull() is IOException && sessionState.userId != null) {
                      val messageId = UUID.randomUUID().toString()
                      val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())
                      val payload = JSONObject().apply {
                        put("id", messageId)
                        put("tour_id", tourId)
                        put("sender_id", sessionState.userId)
                        put("content_type", "text")
                        put("body", messageText)
                        put("client_created_at", timestamp)
                      }
                      val queued = repository.enqueueSyncOperation("create", "tour_messages", messageId, payload.toString())
                      if (queued) {
                        comments = comments + TourComment(
                          id = messageId,
                          tourId = tourId,
                          userId = sessionState.userId!!,
                          body = messageText,
                          createdAt = timestamp,
                          clientCreatedAt = timestamp
                        )
                        newCommentText = ""
                        snackbarMessage = if (lang == AppLanguage.BANGLA) "মন্তব্যটি অফলাইনে রাখা হয়েছে; সংযোগ ফিরলে Sync হবে" else "Comment queued offline and will sync when online"
                      }
                    }
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("btn_post_comment")
            ) {
              Text(L10n.postComment(lang))
            }
          }
        }

        if (comments.isEmpty()) {
          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "কোনো মন্তব্য পাওয়া যায়নি।" else "No comments yet.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 12.dp)
            )
          }
        } else {
          items(comments) { c ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = c.body,
                  style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = c.createdAt?.take(10) ?: "",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outline
                  )
                )
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }

  // Add Expense Dialog
  if (showAddExpenseDialog) {
    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Transport") }

    AlertDialog(
      onDismissRequest = { showAddExpenseDialog = false },
      title = { Text(L10n.addExpense(lang)) },
      text = {
        Column {
          OutlinedTextField(
            value = expenseTitle,
            onValueChange = { expenseTitle = it },
            label = { Text(L10n.expenseTitle(lang)) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_expense_title"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = expenseAmount,
            onValueChange = { expenseAmount = it },
            label = { Text(L10n.expenseAmount(lang)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_expense_amount"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = L10n.expenseCategory(lang), style = MaterialTheme.typography.labelMedium)
          Spacer(modifier = Modifier.height(6.dp))
          val categories = listOf("Transport", "Accommodation", "Food", "Other")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            categories.forEach { cat ->
              FilterChip(
                selected = expenseCategory == cat,
                onClick = { expenseCategory = cat },
                label = { Text(cat) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amt = expenseAmount.toDoubleOrNull()
            if (expenseTitle.isNotBlank() && amt != null && amt > 0) {
              scope.launch {
                val res = repository.supabaseClient.createTourExpense(
                  tourId = tourId,
                  title = expenseTitle.trim(),
                  amount = amt,
                  category = expenseCategory,
                  description = null
                )
                if (res.isSuccess) {
                  expenses = repository.supabaseClient.getTourExpenses(tourId)
                  showAddExpenseDialog = false
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
          modifier = Modifier.testTag("btn_confirm_add_expense")
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddExpenseDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Add Itinerary Dialog
  if (showAddItineraryDialog) {
    var dayNumber by remember { mutableStateOf("1") }
    var itinTitle by remember { mutableStateOf("") }
    var itinDesc by remember { mutableStateOf("") }
    var itinLoc by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddItineraryDialog = false },
      title = { Text(L10n.addItineraryDay(lang)) },
      text = {
        Column {
          OutlinedTextField(
            value = dayNumber,
            onValueChange = { dayNumber = it },
            label = { Text(L10n.dayNumber(lang)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = itinTitle,
            onValueChange = { itinTitle = it },
            label = { Text(L10n.itineraryTitle(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = itinLoc,
            onValueChange = { itinLoc = it },
            label = { Text(L10n.location(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = itinDesc,
            onValueChange = { itinDesc = it },
            label = { Text(L10n.description(lang)) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val d = dayNumber.toIntOrNull() ?: 1
            if (itinTitle.isNotBlank()) {
              scope.launch {
                val res = repository.supabaseClient.createItineraryItem(
                  tourId = tourId,
                  dayNumber = d,
                  title = itinTitle.trim(),
                  description = itinDesc.trim().ifBlank { null },
                  location = itinLoc.trim().ifBlank { null }
                )
                if (res.isSuccess) {
                  itinerary = repository.supabaseClient.getTourItinerary(tourId)
                  showAddItineraryDialog = false
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddItineraryDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Add Checklist Dialog
  if (showAddChecklistDialog) {
    var checkItemTitle by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddChecklistDialog = false },
      title = { Text(L10n.addChecklistItem(lang)) },
      text = {
        OutlinedTextField(
          value = checkItemTitle,
          onValueChange = { checkItemTitle = it },
          placeholder = { Text(L10n.checklistItemPlaceholder(lang)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (checkItemTitle.isNotBlank()) {
              scope.launch {
                val res = repository.supabaseClient.createChecklistItem(tourId, checkItemTitle.trim())
                if (res.isSuccess) {
                  checklists = repository.supabaseClient.getTourChecklist(tourId)
                  showAddChecklistDialog = false
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddChecklistDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Add Guide Dialog
  if (showAddGuideDialog) {
    var guideName by remember { mutableStateOf("") }
    var guidePhone by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddGuideDialog = false },
      title = { Text(L10n.addGuide(lang)) },
      text = {
        Column {
          OutlinedTextField(
            value = guideName,
            onValueChange = { guideName = it },
            label = { Text(L10n.guideName(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = guidePhone,
            onValueChange = { guidePhone = it },
            label = { Text(L10n.phone(lang)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (guideName.isNotBlank()) {
              scope.launch {
                val res = repository.supabaseClient.createGuide(
                  tourId = tourId,
                  destinationId = tour?.destinationId,
                  name = guideName.trim(),
                  phone = guidePhone.trim().ifBlank { null },
                  email = null
                )
                if (res.isSuccess) {
                  guides = repository.supabaseClient.getGuidesForTour(tourId)
                  showAddGuideDialog = false
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddGuideDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }
}

@Composable
fun CategoryExpenseRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  amount: Double,
  lang: AppLanguage
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(32.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
      )
    }

    Text(
      text = L10n.formatCurrency(amount, lang),
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
  }
}
