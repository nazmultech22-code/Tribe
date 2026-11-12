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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.transfer.BluetoothTransferManager
import com.example.data.transfer.NearbyTransferPeer
import com.example.data.transfer.ReceivedTransfer
import com.example.data.sync.DeviceIdentityKeyStore
import android.net.Uri
import android.provider.OpenableColumns
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
  val context = androidx.compose.ui.platform.LocalContext.current

  var tour by remember { mutableStateOf<Tour?>(null) }
  var destination by remember { mutableStateOf<Destination?>(null) }
  var expenses by remember { mutableStateOf<List<TourExpense>>(emptyList()) }
  var itinerary by remember { mutableStateOf<List<TourItineraryItem>>(emptyList()) }
  var checklists by remember { mutableStateOf<List<TourChecklistItem>>(emptyList()) }
  var guides by remember { mutableStateOf<List<TourGuide>>(emptyList()) }
  var comments by remember { mutableStateOf<List<TourComment>>(emptyList()) }
  var members by remember { mutableStateOf<List<TourMember>>(emptyList()) }
  var memberPermissions by remember { mutableStateOf<List<TourMemberPermission>>(emptyList()) }
  var editingMember by remember { mutableStateOf<TourMember?>(null) }
  var memberToRemove by remember { mutableStateOf<TourMember?>(null) }
  var permissionCanEdit by remember { mutableStateOf(false) }
  var permissionCanManageMembers by remember { mutableStateOf(false) }
  var permissionCanManageContent by remember { mutableStateOf(false) }
  var joinRequests by remember { mutableStateOf<List<TourJoinRequest>>(emptyList()) }
  var guestMembers by remember { mutableStateOf<List<TourGuestMember>>(emptyList()) }
  var attendance by remember { mutableStateOf<List<TourAttendance>>(emptyList()) }
  var guestAttendance by remember { mutableStateOf<List<TourGuestAttendance>>(emptyList()) }
  var devices by remember { mutableStateOf<List<TourDevice>>(emptyList()) }
  var syncConflicts by remember { mutableStateOf<List<TourSyncConflict>>(emptyList()) }
  var documents by remember { mutableStateOf<List<TourDocument>>(emptyList()) }
  var images by remember { mutableStateOf<List<TourImage>>(emptyList()) }
  var isUploadingFile by remember { mutableStateOf(false) }
  var nearbyPeers by remember { mutableStateOf<List<NearbyTransferPeer>>(emptyList()) }
  var selectedPeer by remember { mutableStateOf<BluetoothDevice?>(null) }
  var isReceivingTransfer by remember { mutableStateOf(false) }
  var transferStatus by remember { mutableStateOf<String?>(null) }
  var lastReceivedTransfer by remember { mutableStateOf<ReceivedTransfer?>(null) }
  var pendingBluetoothAction by remember { mutableStateOf("scan") }
  val bluetoothManager = remember(context) { BluetoothTransferManager(context.applicationContext) }
  var guestName by remember { mutableStateOf("") }
  var inviteCode by remember { mutableStateOf<String?>(null) }

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
        memberPermissions = repository.supabaseClient.getTourMemberPermissions(tourId)
        joinRequests = repository.supabaseClient.getTourJoinRequests(tourId)
        guestMembers = repository.supabaseClient.getTourGuestMembers(tourId)
        attendance = repository.supabaseClient.getTourAttendance(tourId)
        guestAttendance = repository.supabaseClient.getTourGuestAttendance(tourId)
        devices = repository.supabaseClient.getTourDevices(tourId)
        syncConflicts = repository.supabaseClient.getTourSyncConflicts(tourId)
        documents = repository.supabaseClient.getTourDocuments(tourId)
        images = repository.supabaseClient.getTourImages(tourId)
      }
      isLoading = false
    }
  }

  LaunchedEffect(tourId) {
    loadTourData()
  }


  val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        isUploadingFile = true
        val name = queryDisplayName(context, uri)
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes == null) {
          snackbarMessage = if (lang == AppLanguage.BANGLA) "ফাইল পড়া যায়নি" else "Could not read the selected file"
        } else {
          val result = repository.supabaseClient.uploadTourDocument(tourId, name, mime, bytes)
          if (result.isSuccess) {
            documents = repository.supabaseClient.getTourDocuments(tourId)
            snackbarMessage = if (lang == AppLanguage.BANGLA) "ডকুমেন্ট আপলোড হয়েছে" else "Document uploaded"
          } else snackbarMessage = result.exceptionOrNull()?.message
        }
        isUploadingFile = false
      }
    }
  }


  val transferFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    val peer = selectedPeer
    if (uri != null && peer != null) {
      scope.launch {
        transferStatus = if (lang == AppLanguage.BANGLA) "ফাইল প্রস্তুত হচ্ছে..." else "Preparing file..."
        val data = withContext(Dispatchers.IO) {
          runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
        if (data == null) {
          transferStatus = if (lang == AppLanguage.BANGLA) "ফাইল পড়া যায়নি" else "Could not read selected file"
        } else {
          val name = queryDisplayName(context, uri)
          val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
          val result = bluetoothManager.sendFile(peer, name, data, mime) { sent, total ->
            transferStatus = "Sending: " + (sent * 100 / total.coerceAtLeast(1)) + "%"
          }
          transferStatus = if (result.isSuccess) {
            if (lang == AppLanguage.BANGLA) "ফাইল নিরাপদে পাঠানো হয়েছে" else "Secure transfer completed"
          } else result.exceptionOrNull()?.localizedMessage
        }
        selectedPeer = null
      }
    }
  }

  fun performBluetoothAction(action: String) {
    if (!bluetoothManager.isAvailable()) {
      transferStatus = if (lang == AppLanguage.BANGLA) "এই ফোনে Bluetooth নেই" else "Bluetooth is not available on this phone"
      return
    }
    if (!bluetoothManager.isEnabled()) {
      transferStatus = if (lang == AppLanguage.BANGLA) "ফোনের Bluetooth চালু করুন" else "Turn on Bluetooth first"
      return
    }
    bluetoothManager.startAdvertising()
    if (action == "receive") {
      isReceivingTransfer = true
      transferStatus = if (lang == AppLanguage.BANGLA) "অন্য ফোনের সংযোগের অপেক্ষায়..." else "Waiting for another phone to connect..."
      scope.launch {
        val result = bluetoothManager.receiveFile { received, total ->
          transferStatus = "Receiving: " + (received * 100 / total.coerceAtLeast(1)) + "%"
        }
        isReceivingTransfer = false
        transferStatus = if (result.isSuccess) {
          val received = result.getOrThrow()
          lastReceivedTransfer = received
          (if (lang == AppLanguage.BANGLA) "ফাইল গ্রহণ সম্পন্ন: " else "Received securely: ") +
            received.fileName + " (" + (received.sizeBytes / 1024) + " KB, encrypted at rest)"
        } else result.exceptionOrNull()?.localizedMessage
      }
    } else {
      nearbyPeers = emptyList()
      val started = bluetoothManager.startScan { peer ->
        nearbyPeers = (nearbyPeers.filterNot { it.address == peer.address } + peer)
      }
      transferStatus = if (started) {
        if (lang == AppLanguage.BANGLA) "কাছাকাছি TRIBE ডিভাইস খোঁজা হচ্ছে..." else "Scanning for nearby TRIBE devices..."
      } else if (lang == AppLanguage.BANGLA) "Bluetooth Scan শুরু করা যায়নি" else "Could not start Bluetooth scan"
    }
  }

  val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { results ->
    if (results.values.all { it }) performBluetoothAction(pendingBluetoothAction)
    else transferStatus = if (lang == AppLanguage.BANGLA) "Bluetooth ব্যবহারের অনুমতি প্রয়োজন" else "Bluetooth permissions are required"
  }

  fun requestBluetoothAction(action: String) {
    pendingBluetoothAction = action
    val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
      )
    } else {
      listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val missing = required.filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isEmpty()) performBluetoothAction(action)
    else bluetoothPermissionLauncher.launch(missing.toTypedArray())
  }

  DisposableEffect(bluetoothManager) {
    onDispose {
      bluetoothManager.stopScan()
      bluetoothManager.stopAdvertising()
      bluetoothManager.stopReceiving()
    }
  }

  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        isUploadingFile = true
        val bytes = compressImageForUpload(context, uri)
        if (bytes == null) {
          snackbarMessage = if (lang == AppLanguage.BANGLA) "ছবিটি প্রসেস করা যায়নি" else "Could not process the image"
        } else {
          val name = queryDisplayName(context, uri).substringBeforeLast(".") + ".jpg"
          val result = repository.supabaseClient.uploadTourImage(tourId, name, "image/jpeg", bytes)
          if (result.isSuccess) {
            images = repository.supabaseClient.getTourImages(tourId)
            snackbarMessage = if (lang == AppLanguage.BANGLA) "ছবি আপলোড হয়েছে" else "Photo uploaded"
          } else snackbarMessage = result.exceptionOrNull()?.message
        }
        isUploadingFile = false
      }
    }
  }

  val isOwner = tour != null && sessionState.userId != null && tour?.ownerId == sessionState.userId
  val canManageMembers = isOwner || memberPermissions.any { it.userId == sessionState.userId && it.canManageMembers }
  val isPublic = tour?.visibility == "public"

  val clipboard = LocalClipboardManager.current
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
          Tab(
            selected = selectedTab == 5,
            onClick = { selectedTab = 5 },
            text = { Text(if (lang == AppLanguage.BANGLA) "সদস্য" else "Members", fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 6,
            onClick = { selectedTab = 6 },
            text = { Text(if (lang == AppLanguage.BANGLA) "ফাইল" else "Files", fontWeight = FontWeight.SemiBold) }
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
                        val queuedPayload = JSONObject().put("status", if (checked) "done" else "todo")
                        if (item.updatedAt != null) queuedPayload.put("_base_revision", item.updatedAt)
                        val queued = repository.enqueueSyncOperation(
                          "update", "tour_tasks", item.id,
                          queuedPayload.toString(),
                          tourId = tourId
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
                      val queued = repository.enqueueSyncOperation("create", "tour_messages", messageId, payload.toString(), tourId = tourId)
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


      // TAB 5: MEMBERS, INVITES, JOIN REQUESTS AND ATTENDANCE
      if (selectedTab == 5) {
        item {
          Text(
            text = if (lang == AppLanguage.BANGLA) "ট্যুর সদস্য" else "Tour members",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = (if (lang == AppLanguage.BANGLA) "মোট সদস্য: " else "Members: ") + members.size,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (sessionState.isLoggedIn && !isOwner && !members.any { it.userId == sessionState.userId } && isPublic) {
          item {
            Button(
              onClick = {
                scope.launch {
                  val result = repository.supabaseClient.requestToJoinTour(tourId, "")
                  snackbarMessage = if (result.isSuccess) {
                    if (lang == AppLanguage.BANGLA) "যোগদানের অনুরোধ পাঠানো হয়েছে" else "Join request sent"
                  } else result.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
                  joinRequests = repository.supabaseClient.getTourJoinRequests(tourId)
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(if (lang == AppLanguage.BANGLA) "এই ট্যুরে যোগদানের অনুরোধ" else "Request to join this tour")
            }
          }
        }

        if (members.isNotEmpty()) {
          items(members) { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TribeGreenPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("User " + member.userId.take(8), fontWeight = FontWeight.SemiBold)
                  Text(member.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (member.userId == sessionState.userId) {
                  Text(if (lang == AppLanguage.BANGLA) "আপনি" else "You", color = TribeGreenPrimary)
                }
                if (isOwner && member.userId != tour?.ownerId) {
                  IconButton(onClick = { memberToRemove = member }) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error)
                  }
                  TextButton(onClick = {
                    val permissions = memberPermissions.firstOrNull { it.userId == member.userId }
                    permissionCanEdit = permissions?.canEditTour ?: false
                    permissionCanManageMembers = permissions?.canManageMembers ?: false
                    permissionCanManageContent = permissions?.canManageContent ?: false
                    editingMember = member
                  }) {
                    Text(if (lang == AppLanguage.BANGLA) "অনুমতি" else "Access")
                  }
                }
              }
            }
          }
        }

        if (members.any { it.userId == sessionState.userId }) {
          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "আপনার উপস্থিতি" else "Your attendance",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            val currentResponse = attendance.firstOrNull { it.userId == sessionState.userId }?.response ?: "going"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("going", "maybe", "not_going").forEach { choice ->
                FilterChip(
                  selected = currentResponse == choice,
                  onClick = {
                    scope.launch {
                      val result = repository.supabaseClient.setTourAttendance(tourId, choice)
                      if (result.isSuccess) {
                        attendance = repository.supabaseClient.getTourAttendance(tourId)
                      } else if (result.exceptionOrNull() is IOException && sessionState.userId != null) {
                        val currentAttendance = attendance.firstOrNull { it.userId == sessionState.userId }
                        val payload = JSONObject().apply {
                          put("tour_id", tourId)
                          put("user_id", sessionState.userId)
                          put("response", choice)
                          if (currentAttendance?.updatedAt != null) put("_base_revision", currentAttendance.updatedAt)
                        }
                        val queued = repository.enqueueSyncOperation(
                          if (currentAttendance == null) "create" else "update",
                          "tour_attendance",
                          sessionState.userId,
                          payload.toString(),
                          tourId = tourId
                        )
                        if (queued) {
                          attendance = attendance.filterNot { it.userId == sessionState.userId } +
                            TourAttendance(tourId, sessionState.userId, choice)
                          snackbarMessage = if (lang == AppLanguage.BANGLA) "আপনার উপস্থিতি অফলাইনে সংরক্ষিত" else "Attendance queued offline"
                        }
                      } else snackbarMessage = result.exceptionOrNull()?.message
                    }
                  },
                  label = { Text(when (choice) {
                    "going" -> if (lang == AppLanguage.BANGLA) "যাব" else "Going"
                    "maybe" -> if (lang == AppLanguage.BANGLA) "সম্ভবত" else "Maybe"
                    else -> if (lang == AppLanguage.BANGLA) "যাব না" else "Not going"
                  }) }
                )
              }
            }
          }
        }

        if (canManageMembers) {
          item {
            Divider()
            Text(
              text = if (lang == AppLanguage.BANGLA) "সদস্য আমন্ত্রণ" else "Invite members",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = {
                scope.launch {
                  val result = repository.supabaseClient.createTourInvite(tourId, "app_member", 5)
                  inviteCode = result.getOrNull()
                  snackbarMessage = if (result.isSuccess) {
                    if (lang == AppLanguage.BANGLA) "আমন্ত্রণ কোড তৈরি হয়েছে" else "Invite code created"
                  } else result.exceptionOrNull()?.message
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Link, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (lang == AppLanguage.BANGLA) "অ্যাপ সদস্যের Invite Code তৈরি" else "Create app-member invite code")
            }
            if (inviteCode != null) {
              SelectionContainer { Text(inviteCode!!, style = MaterialTheme.typography.bodySmall) }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(inviteCode!!)) }) {
                  Text(if (lang == AppLanguage.BANGLA) "কপি" else "Copy")
                }
                OutlinedButton(onClick = {
                  val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "TRIBE tour invite code: " + inviteCode)
                  }
                  context.startActivity(Intent.createChooser(send, "Share TRIBE invite"))
                }) {
                  Text(if (lang == AppLanguage.BANGLA) "শেয়ার" else "Share")
                }
              }
            }
          }

          item {
            Text(
              text = if (lang == AppLanguage.BANGLA) "অতিথি সদস্য যোগ করুন" else "Add a guest member",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            OutlinedTextField(
              value = guestName,
              onValueChange = { guestName = it },
              label = { Text(if (lang == AppLanguage.BANGLA) "অতিথির নাম" else "Guest name") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
            Button(
              enabled = guestName.isNotBlank(),
              onClick = {
                scope.launch {
                  val invite = repository.supabaseClient.createTourInvite(tourId, "guest", 1)
                  val token = invite.getOrNull()
                  if (token == null) {
                    snackbarMessage = invite.exceptionOrNull()?.message
                  } else {
                    val redeemed = repository.supabaseClient.redeemTourInvite(token, guestName.trim())
                    if (redeemed.isSuccess) {
                      val result = JSONObject(redeemed.getOrThrow())
                      snackbarMessage = (if (lang == AppLanguage.BANGLA) "অতিথি যোগ হয়েছে। কোড: " else "Guest added. Code: ") + result.optString("guest_code")
                      guestName = ""
                      guestMembers = repository.supabaseClient.getTourGuestMembers(tourId)
                    } else snackbarMessage = redeemed.exceptionOrNull()?.message
                  }
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(if (lang == AppLanguage.BANGLA) "অতিথি যোগ করুন" else "Add guest")
            }
          }

          if (joinRequests.isNotEmpty()) {
            item {
              Divider()
              Text(
                text = if (lang == AppLanguage.BANGLA) "যোগদানের অপেক্ষমাণ অনুরোধ" else "Pending join requests",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
            items(joinRequests) { request ->
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("User " + request.requesterId.take(8), fontWeight = FontWeight.SemiBold)
                  if (request.message.isNotBlank()) Text(request.message)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                      scope.launch {
                        val result = repository.supabaseClient.reviewTourJoinRequest(request.id, "approved")
                        snackbarMessage = if (result.isSuccess) "Request approved" else result.exceptionOrNull()?.message
                        loadTourData()
                      }
                    }) { Text(if (lang == AppLanguage.BANGLA) "অনুমোদন" else "Approve") }
                    OutlinedButton(onClick = {
                      scope.launch {
                        val result = repository.supabaseClient.reviewTourJoinRequest(request.id, "rejected")
                        snackbarMessage = if (result.isSuccess) "Request rejected" else result.exceptionOrNull()?.message
                        loadTourData()
                      }
                    }) { Text(if (lang == AppLanguage.BANGLA) "বাতিল" else "Reject") }
                  }
                }
              }
            }
          }
        }


        if (isOwner || members.any { it.userId == sessionState.userId }) {
          item {
            Divider()
            Text(
              text = if (lang == AppLanguage.BANGLA) "এই ট্যুরের সংযুক্ত ডিভাইস" else "Devices linked to this tour",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            OutlinedButton(
              onClick = {
                scope.launch {
                  val result = repository.supabaseClient.registerTourDevice(
                    tourId,
                    DeviceIdentityKeyStore.publicKeyBase64(),
                    DeviceIdentityKeyStore.deviceLabel()
                  )
                  snackbarMessage = if (result.isSuccess) {
                    devices = repository.supabaseClient.getTourDevices(tourId)
                    if (lang == AppLanguage.BANGLA) "এই ফোনটি Sync-এর জন্য নিবন্ধিত হয়েছে" else "This phone is registered for sync"
                  } else result.exceptionOrNull()?.message
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Devices, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (lang == AppLanguage.BANGLA) "এই ফোন নিবন্ধন করুন" else "Register this phone")
            }
          }
          if (devices.isNotEmpty()) {
            items(devices) { device ->
              Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TribeGreenPrimary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(device.deviceLabel, fontWeight = FontWeight.SemiBold)
                    Text(device.platform + " • " + (device.lastSeenAt ?: "Registered"), style = MaterialTheme.typography.bodySmall)
                  }
                  if (device.userId == sessionState.userId) {
                    IconButton(onClick = {
                      scope.launch {
                        val result = repository.supabaseClient.revokeTourDevice(device.id)
                        snackbarMessage = if (result.isSuccess) "Device revoked" else result.exceptionOrNull()?.message
                        devices = repository.supabaseClient.getTourDevices(tourId)
                      }
                    }) { Icon(Icons.Default.LinkOff, contentDescription = "Revoke device") }
                  }
                }
              }
            }
          }
        }

        if (syncConflicts.isNotEmpty()) {
          item {
            Divider()
            Text(
              text = if (lang == AppLanguage.BANGLA) "Sync দ্বন্দ্ব" else "Sync conflicts",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          items(syncConflicts) { conflict ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(conflict.entityType.replace("_", " ").replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                Text(conflict.reason, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(onClick = {
                    scope.launch {
                      val result = repository.supabaseClient.resolveTourSyncConflict(conflict.id, "accepted_server")
                      if (result.isSuccess) {
                        repository.resolveLocalSyncConflict(conflict.operationId, keepLocal = false)
                        syncConflicts = repository.supabaseClient.getTourSyncConflicts(tourId)
                        snackbarMessage = if (lang == AppLanguage.BANGLA) "সার্ভারের সংস্করণ রাখা হয়েছে" else "Server version kept"
                      } else snackbarMessage = result.exceptionOrNull()?.message
                    }
                  }) {
                    Text(if (lang == AppLanguage.BANGLA) "সার্ভার রাখুন" else "Use server")
                  }
                  Button(onClick = {
                    scope.launch {
                      val result = repository.supabaseClient.resolveTourSyncConflict(conflict.id, "accepted_client")
                      if (result.isSuccess) {
                        repository.resolveLocalSyncConflict(conflict.operationId, keepLocal = true)
                        syncConflicts = repository.supabaseClient.getTourSyncConflicts(tourId)
                        snackbarMessage = if (lang == AppLanguage.BANGLA) "আপনার পরিবর্তন আবার Sync হবে" else "Your change will be retried"
                      } else snackbarMessage = result.exceptionOrNull()?.message
                    }
                  }) {
                    Text(if (lang == AppLanguage.BANGLA) "আমার পরিবর্তন রাখুন" else "Keep my change")
                  }
                }
              }
            }
          }
        }

        if (guestMembers.isNotEmpty()) {
          item {
            Divider()
            Text(
              text = if (lang == AppLanguage.BANGLA) "অতিথি সদস্য" else "Guest members",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          items(guestMembers) { guest ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(guest.displayName, fontWeight = FontWeight.SemiBold)
                  Text("Code: " + guest.guestCode, style = MaterialTheme.typography.bodySmall)
                  if (canManageMembers) {
                    val currentGuestResponse = guestAttendance.firstOrNull { it.guestMemberId == guest.id }
                    val selectedResponse = currentGuestResponse?.response ?: "going"
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      listOf("going", "maybe", "not_going").forEach { choice ->
                        FilterChip(
                          selected = selectedResponse == choice,
                          onClick = {
                            scope.launch {
                              val result = repository.supabaseClient.setTourGuestAttendance(tourId, guest.id, choice)
                              if (result.isSuccess) {
                                guestAttendance = repository.supabaseClient.getTourGuestAttendance(tourId)
                              } else if (result.exceptionOrNull() is IOException && sessionState.userId != null) {
                                val payload = JSONObject().apply {
                                  put("tour_id", tourId)
                                  put("guest_member_id", guest.id)
                                  put("response", choice)
                                  put("updated_by", sessionState.userId)
                                }
                                if (currentGuestResponse?.updatedAt != null) payload.put("_base_revision", currentGuestResponse.updatedAt)
                                val queued = repository.enqueueSyncOperation(
                                  if (currentGuestResponse == null) "create" else "update",
                                  "tour_guest_attendance",
                                  guest.id,
                                  payload.toString(),
                                  tourId = tourId
                                )
                                if (queued) {
                                  guestAttendance = guestAttendance.filterNot { it.guestMemberId == guest.id } +
                                    TourGuestAttendance(tourId, guest.id, choice, updatedBy = sessionState.userId)
                                  snackbarMessage = if (lang == AppLanguage.BANGLA) "অতিথির উপস্থিতি অফলাইনে সংরক্ষিত" else "Guest attendance queued offline"
                                }
                              }
                            }
                          },
                          label = { Text(when (choice) {
                            "going" -> if (lang == AppLanguage.BANGLA) "যাবে" else "Going"
                            "maybe" -> if (lang == AppLanguage.BANGLA) "সম্ভবত" else "Maybe"
                            else -> if (lang == AppLanguage.BANGLA) "যাবে না" else "Not going"
                          }) }
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }


      // TAB 6: PRIVATE TOUR DOCUMENTS AND GALLERY
      if (selectedTab == 6) {
        item {
          Text(
            text = if (lang == AppLanguage.BANGLA) "ভ্রমণের ডকুমেন্ট ও ছবি" else "Trip documents and gallery",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = if (lang == AppLanguage.BANGLA) "ফাইলগুলো শুধু অনুমোদিত ট্যুর সদস্যরা দেখতে পারবেন।" else "Files are stored privately and are visible only to authorized tour members.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { documentPicker.launch("*/*") },
              enabled = !isUploadingFile,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (lang == AppLanguage.BANGLA) "ডকুমেন্ট" else "Document")
            }
            OutlinedButton(
              onClick = { imagePicker.launch("image/*") },
              enabled = !isUploadingFile,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (lang == AppLanguage.BANGLA) "ছবি" else "Photo")
            }
          }
          if (isUploadingFile) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        item {
          Text(
            text = if (lang == AppLanguage.BANGLA) "ডকুমেন্ট" else "Documents",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
        if (documents.isEmpty()) {
          item { Text(if (lang == AppLanguage.BANGLA) "এখনো কোনো ডকুমেন্ট নেই" else "No documents uploaded yet") }
        } else {
          items(documents) { document ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = TribeGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(document.fileName, fontWeight = FontWeight.SemiBold)
                  Text((document.fileSize / 1024).toString() + " KB • " + document.category, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = {
                  scope.launch {
                    val link = repository.supabaseClient.getSignedStorageUrl("tour-documents", document.storagePath)
                    if (link.isSuccess) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.getOrThrow())))
                    else snackbarMessage = link.exceptionOrNull()?.message
                  }
                }) { Icon(Icons.Default.OpenInNew, contentDescription = "Open document") }
              }
            }
          }
        }

        item {
          Text(
            text = if (lang == AppLanguage.BANGLA) "ছবির গ্যালারি" else "Photo gallery",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
        if (images.isEmpty()) {
          item { Text(if (lang == AppLanguage.BANGLA) "এখনো কোনো ছবি নেই" else "No photos uploaded yet") }
        } else {
          items(images) { image ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = TribeGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(image.caption ?: image.storagePath.substringAfterLast("/"), modifier = Modifier.weight(1f))
                IconButton(onClick = {
                  scope.launch {
                    val link = repository.supabaseClient.getSignedStorageUrl("tour-gallery", image.storagePath)
                    if (link.isSuccess) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.getOrThrow())))
                    else snackbarMessage = link.exceptionOrNull()?.message
                  }
                }) { Icon(Icons.Default.OpenInNew, contentDescription = "Open photo") }
              }
            }
          }
        }

        item {
          Divider()
          Text(
            text = if (lang == AppLanguage.BANGLA) "কাছাকাছি ফোনে নিরাপদে ফাইল পাঠান" else "Secure nearby device transfer",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = if (lang == AppLanguage.BANGLA) "দুই ফোনে TRIBE খুলুন, Bluetooth চালু করুন এবং Android Bluetooth Settings থেকে ফোন দুটি Pair করুন।" else "Open TRIBE on both phones, turn on Bluetooth, and pair the phones in Android Bluetooth settings first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { requestBluetoothAction("scan") }, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.BluetoothSearching, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (lang == AppLanguage.BANGLA) "ফোন খুঁজুন" else "Find phones")
            }
            OutlinedButton(
              onClick = { requestBluetoothAction("receive") },
              enabled = !isReceivingTransfer,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.BluetoothConnected, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (lang == AppLanguage.BANGLA) "গ্রহণ করুন" else "Receive")
            }
          }
          if (isReceivingTransfer) {
            OutlinedButton(onClick = { bluetoothManager.stopReceiving(); isReceivingTransfer = false }) {
              Text(if (lang == AppLanguage.BANGLA) "বন্ধ করুন" else "Stop receiving")
            }
          }
          if (transferStatus != null) {
            Text(transferStatus!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
          if (lastReceivedTransfer != null) {
            OutlinedButton(
              onClick = {
                val received = lastReceivedTransfer!!
                val uri = bluetoothManager.exportReceivedFile(received)
                if (uri != null) {
                  context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, received.mimeType)
                      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  )
                } else {
                  transferStatus = if (lang == AppLanguage.BANGLA) "Downloads-এ ফাইল সংরক্ষণ করা যায়নি" else "Could not export the received file to Downloads"
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Download, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (lang == AppLanguage.BANGLA) "প্রাপ্ত ফাইল Downloads-এ সংরক্ষণ" else "Save received file to Downloads")
            }
          }
          nearbyPeers.forEach { peer ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = TribeGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(peer.displayName, fontWeight = FontWeight.SemiBold)
                  Text(peer.address, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = {
                  selectedPeer = peer.device
                  transferFilePicker.launch("*/*")
                }) {
                  Text(if (lang == AppLanguage.BANGLA) "পাঠান" else "Send")
                }
              }
            }
          }
          if (nearbyPeers.isNotEmpty()) {
            TextButton(onClick = { bluetoothManager.stopScan(); nearbyPeers = emptyList() }) {
              Text(if (lang == AppLanguage.BANGLA) "স্ক্যান বন্ধ" else "Stop scan")
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
                val expenseId = UUID.randomUUID().toString()
                val participantIds = members.map { it.userId }.distinct().ifEmpty { listOf(sessionState.userId!!) }
                val res = repository.supabaseClient.createTourExpense(
                  tourId = tourId,
                  title = expenseTitle.trim(),
                  amount = amt,
                  category = expenseCategory,
                  description = null,
                  expenseId = expenseId,
                  participantIds = participantIds
                )
                if (res.isSuccess) {
                  expenses = repository.supabaseClient.getTourExpenses(tourId)
                  showAddExpenseDialog = false
                } else if (res.exceptionOrNull() is IOException && sessionState.userId != null) {
                  val payload = repository.supabaseClient.buildCreateTourExpenseRpcPayload(
                    expenseId, tourId, expenseTitle.trim(), amt, expenseCategory, null, participantIds
                  )
                  val queued = repository.enqueueSyncOperation(
                    "create", "rpc_create_tour_expense", expenseId, payload.toString(), tourId = tourId
                  )
                  if (queued) {
                    expenses = expenses + TourExpense(
                      id = expenseId,
                      tourId = tourId,
                      paidBy = sessionState.userId,
                      title = expenseTitle.trim(),
                      amount = amt,
                      category = expenseCategory
                    )
                    showAddExpenseDialog = false
                    snackbarMessage = if (lang == AppLanguage.BANGLA) "খরচ ও সদস্যদের ভাগ অফলাইনে সংরক্ষিত; পরে Sync হবে" else "Expense and member shares queued offline"
                  }
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
                } else if ((res.exceptionOrNull() is IOException || res.exceptionOrNull()?.message == "Tour not found") && sessionState.userId != null && !tour?.startDate.isNullOrBlank()) {
                  val start = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tour!!.startDate!!) }.getOrNull()
                  if (start != null) {
                    val calendar = java.util.Calendar.getInstance().apply {
                      time = start
                      add(java.util.Calendar.DAY_OF_YEAR, (d - 1).coerceAtLeast(0))
                    }
                    val dayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                    val itemId = UUID.randomUUID().toString()
                    val payload = JSONObject().apply {
                      put("id", itemId)
                      put("tour_id", tourId)
                      put("day_date", dayDate)
                      put("sort_order", 0)
                      put("title", itinTitle.trim())
                      put("created_by", sessionState.userId)
                      if (itinDesc.isNotBlank()) put("details", itinDesc.trim())
                      if (itinLoc.isNotBlank()) put("location", itinLoc.trim())
                    }
                    val queued = repository.enqueueSyncOperation("create", "tour_itinerary_items", itemId, payload.toString(), tourId = tourId)
                    if (queued) {
                      itinerary = itinerary + TourItineraryItem(
                        id = itemId,
                        tourId = tourId,
                        dayNumber = 0,
                        title = itinTitle.trim(),
                        description = itinDesc.trim().ifBlank { null },
                        location = itinLoc.trim().ifBlank { null },
                        dayDate = dayDate
                      )
                      showAddItineraryDialog = false
                      snackbarMessage = if (lang == AppLanguage.BANGLA) "পরিকল্পনাটি অফলাইনে সংরক্ষিত; পরে Sync হবে" else "Itinerary item queued offline and will sync later"
                    }
                  }
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
                } else if (res.exceptionOrNull() is IOException && sessionState.userId != null) {
                  val taskId = UUID.randomUUID().toString()
                  val payload = JSONObject().apply {
                    put("id", taskId)
                    put("tour_id", tourId)
                    put("title", checkItemTitle.trim())
                    put("created_by", sessionState.userId)
                    put("status", "todo")
                  }
                  val queued = repository.enqueueSyncOperation("create", "tour_tasks", taskId, payload.toString(), tourId = tourId)
                  if (queued) {
                    checklists = checklists + TourChecklistItem(id = taskId, tourId = tourId, title = checkItemTitle.trim())
                    showAddChecklistDialog = false
                    snackbarMessage = if (lang == AppLanguage.BANGLA) "কাজটি অফলাইনে সংরক্ষিত; পরে Sync হবে" else "Checklist item queued offline and will sync later"
                  }
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

  if (editingMember != null) {
    AlertDialog(
      onDismissRequest = { editingMember = null },
      title = { Text(if (lang == AppLanguage.BANGLA) "সদস্যের অনুমতি" else "Member permissions") },
      text = {
        Column {
          Text("User " + editingMember!!.userId.take(8), fontWeight = FontWeight.SemiBold)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = permissionCanEdit, onCheckedChange = { permissionCanEdit = it })
            Text(if (lang == AppLanguage.BANGLA) "ট্যুর সম্পাদনা" else "Edit tour")
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = permissionCanManageMembers, onCheckedChange = { permissionCanManageMembers = it })
            Text(if (lang == AppLanguage.BANGLA) "সদস্য পরিচালনা" else "Manage members")
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = permissionCanManageContent, onCheckedChange = { permissionCanManageContent = it })
            Text(if (lang == AppLanguage.BANGLA) "ট্যুর কনটেন্ট পরিচালনা" else "Manage tour content")
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          val target = editingMember!!
          scope.launch {
            val result = repository.supabaseClient.setTourMemberPermissions(
              tourId, target.userId, permissionCanEdit, permissionCanManageMembers, permissionCanManageContent
            )
            if (result.isSuccess) {
              memberPermissions = repository.supabaseClient.getTourMemberPermissions(tourId)
              snackbarMessage = if (lang == AppLanguage.BANGLA) "অনুমতি আপডেট হয়েছে" else "Permissions updated"
            } else snackbarMessage = result.exceptionOrNull()?.message
            editingMember = null
          }
        }) { Text(L10n.saveBtn(lang)) }
      },
      dismissButton = {
        TextButton(onClick = { editingMember = null }) { Text(L10n.cancelBtn(lang)) }
      }
    )
  }

  if (memberToRemove != null) {
    AlertDialog(
      onDismissRequest = { memberToRemove = null },
      title = { Text(if (lang == AppLanguage.BANGLA) "সদস্য সরাবেন?" else "Remove member?") },
      text = {
        Text(
          if (lang == AppLanguage.BANGLA) "এই সদস্যকে ট্যুর থেকে সরানো হবে।" else "This member will lose access to this tour."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val target = memberToRemove!!
            scope.launch {
              val result = repository.supabaseClient.removeTourMember(tourId, target.userId)
              if (result.isSuccess) {
                members = repository.supabaseClient.getTourMembers(tourId)
                memberPermissions = repository.supabaseClient.getTourMemberPermissions(tourId)
                snackbarMessage = if (lang == AppLanguage.BANGLA) "সদস্য সরানো হয়েছে" else "Member removed"
              } else snackbarMessage = result.exceptionOrNull()?.message
              memberToRemove = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text(if (lang == AppLanguage.BANGLA) "সরান" else "Remove") }
      },
      dismissButton = {
        TextButton(onClick = { memberToRemove = null }) { Text(L10n.cancelBtn(lang)) }
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


private fun queryDisplayName(context: Context, uri: Uri): String {
  val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
  return cursor?.use {
    if (it.moveToFirst()) it.getString(0) else null
  } ?: "upload"
}

private fun compressImageForUpload(context: Context, uri: Uri): ByteArray? {
  return try {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
      ?: return null
    val maxDimension = 1920
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height))
    val outputBitmap = if (scale < 1f) {
      Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
      )
    } else bitmap
    ByteArrayOutputStream().use { output ->
      outputBitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
      if (outputBitmap !== bitmap) outputBitmap.recycle()
      bitmap.recycle()
      output.toByteArray()
    }
  } catch (_: Exception) {
    null
  }
}
