package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Destination
import com.example.data.model.Tour
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenPrimary
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTourScreen(
  repository: TribeRepository,
  preselectedDestination: Destination? = null,
  onTourCreated: (Tour) -> Unit,
  onNavigateToAuth: () -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var title by remember { mutableStateOf(if (preselectedDestination != null) "${preselectedDestination.name} ভ্রমণ" else "") }
  var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }
  var selectedDestination by remember { mutableStateOf<Destination?>(preselectedDestination) }
  var isDestDropdownExpanded by remember { mutableStateOf(false) }

  var startDate by remember { mutableStateOf("2026-12-14") }
  var endDate by remember { mutableStateOf("2026-12-16") }
  var maxMembers by remember { mutableStateOf("5") }
  var meetingPoint by remember { mutableStateOf("ঢাকা (সায়দাবাদ / কলাবাগান)") }
  var description by remember { mutableStateOf("সবার সাথে আনন্দের একটি ভ্রমণ...") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    destinations = repository.getDestinations()
    if (selectedDestination == null && destinations.isNotEmpty()) {
      selectedDestination = destinations.first()
      if (title.isBlank()) {
        title = "${destinations.first().name} ভ্রমণ"
      }
    }
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.createTourTitle(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onBackClick
      )
    }
  ) { padding ->
    if (!sessionState.isLoggedIn) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = if (lang == AppLanguage.BANGLA) "ট্যুর প্ল্যান তৈরি করতে সাইন ইন করুন" else "Sign in to plan a tour",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (lang == AppLanguage.BANGLA) "আপনার ভ্রমণ সংরক্ষণ ও পরিচালনা করতে একটি অ্যাকাউন্ট প্রয়োজন।" else "An account is required to save and manage your tours.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onNavigateToAuth,
            colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(L10n.loginTitle(lang))
          }
        }
      }
      return@Scaffold
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // Tour Title
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(L10n.tourTitle(lang)) },
        placeholder = { Text("e.g. সাজেক ভ্যালি ভ্রমণ / Sajek Winter Trip") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_tour_title"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Destination Dropdown Selector
      ExposedDropdownMenuBox(
        expanded = isDestDropdownExpanded,
        onExpandedChange = { isDestDropdownExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedDestination?.name ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text(L10n.selectDestination(lang)) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDestDropdownExpanded) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .testTag("input_tour_destination")
        )

        ExposedDropdownMenu(
          expanded = isDestDropdownExpanded,
          onDismissRequest = { isDestDropdownExpanded = false }
        ) {
          destinations.forEach { dest ->
            DropdownMenuItem(
              text = { Text(dest.name) },
              onClick = {
                selectedDestination = dest
                if (title.isBlank() || title.endsWith("ভ্রমণ") || title.endsWith("Tour")) {
                  title = "${dest.name} ভ্রমণ"
                }
                isDestDropdownExpanded = false
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Dates: Start and End
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = startDate,
          onValueChange = { startDate = it },
          label = { Text(L10n.startDate(lang)) },
          leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
          modifier = Modifier
            .weight(1f)
            .testTag("input_tour_start_date"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = endDate,
          onValueChange = { endDate = it },
          label = { Text(L10n.endDate(lang)) },
          leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
          modifier = Modifier
            .weight(1f)
            .testTag("input_tour_end_date"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Max Members & Meeting Point
      OutlinedTextField(
        value = maxMembers,
        onValueChange = { maxMembers = it },
        label = { Text(L10n.maxMembers(lang)) },
        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_tour_max_members"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = meetingPoint,
        onValueChange = { meetingPoint = it },
        label = { Text(L10n.meetingPoint(lang)) },
        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_tour_meeting_point"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Description
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(L10n.description(lang)) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_tour_desc"),
        shape = RoundedCornerShape(12.dp),
        minLines = 3
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Default Visibility Note (Requirement 8)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (lang == AppLanguage.BANGLA) "নতুন তৈরি সকল ট্যুর ডিফল্টভাবে 'প্রাইভেট' থাকে। বিস্তারিত পেজ থেকে আপনি এটি শেয়ার করতে পারবেন।" else "All new tours are 'private' by default. You can share publicly from the tour details page.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          errorMessage = null
          if (title.isBlank()) {
            errorMessage = if (lang == AppLanguage.BANGLA) "দয়া করে ট্যুরের নাম লিখুন" else "Please enter tour title"
            return@Button
          }
          if (selectedDestination == null) {
            errorMessage = if (lang == AppLanguage.BANGLA) "দয়া করে গন্তব্য নির্বাচন করুন" else "Please select a destination"
            return@Button
          }

          isLoading = true
          scope.launch {
            val res = repository.supabaseClient.createTour(
              title = title.trim(),
              destinationId = selectedDestination?.name,
              startDate = startDate.trim().ifBlank { null },
              endDate = endDate.trim().ifBlank { null },
              maxMembers = maxMembers.toIntOrNull() ?: 5,
              meetingPoint = meetingPoint.trim().ifBlank { null },
              description = description.trim().ifBlank { null }
            )
            isLoading = false
            if (res.isSuccess) {
              onTourCreated(res.getOrThrow())
            } else {
              val cause = res.exceptionOrNull()
              if (cause is IOException && sessionState.userId != null) {
                val offlineId = UUID.randomUUID().toString()
                val offlinePayload = JSONObject().apply {
                  put("id", offlineId)
                  put("title", title.trim())
                  put("creator_id", sessionState.userId)
                  put("destination", selectedDestination?.name ?: "Unknown destination")
                  put("is_public", false)
                  put("status", "upcoming")
                  put("max_members", maxMembers.toIntOrNull() ?: 5)
                  if (startDate.isNotBlank()) put("start_date", startDate.trim())
                  if (endDate.isNotBlank()) put("end_date", endDate.trim())
                  if (description.isNotBlank()) put("description", description.trim())
                }
                val queued = repository.enqueueSyncOperation("create", "tours", offlineId, offlinePayload.toString(), tourId = offlineId)
                errorMessage = if (queued) {
                  if (lang == AppLanguage.BANGLA) "ইন্টারনেট নেই। ট্যুরটি Sync Queue-তে রাখা হয়েছে; সংযোগ ফিরলে স্বয়ংক্রিয়ভাবে Sync হবে।" else "You're offline. The tour is queued and will sync automatically when the connection returns."
                } else {
                  cause.localizedMessage ?: L10n.errorOccurred(lang)
                }
              } else {
                errorMessage = cause?.localizedMessage ?: L10n.errorOccurred(lang)
              }
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_save_tour")
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
        } else {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(L10n.saveTour(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
