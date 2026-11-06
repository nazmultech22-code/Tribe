package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublicTourDiscoveryItem
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.PublicTourCard
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(
  repository: TribeRepository,
  onNavigateToTourDetail: (String) -> Unit,
  onNavigateToCreateTour: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var publicTours by remember { mutableStateOf<List<PublicTourDiscoveryItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }

  fun loadTours() {
    scope.launch {
      isLoading = true
      publicTours = repository.supabaseClient.getAllPublicTours()
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadTours()
  }

  val filteredTours = if (searchQuery.isBlank()) {
    publicTours
  } else {
    publicTours.filter {
      (it.tourTitle?.contains(searchQuery, ignoreCase = true) == true) ||
      (it.destinationName?.contains(searchQuery, ignoreCase = true) == true)
    }
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.navCommunity(lang),
        subtitle = L10n.communityBadge(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        actions = {
          IconButton(onClick = { loadTours() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
        }
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

      // Filter search
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(if (lang == AppLanguage.BANGLA) "কমিউনিটি ট্যুর খুঁজুন..." else "Filter community tours...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_community_search"),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = TribeGreenPrimary)
        }
      } else if (filteredTours.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Groups,
              contentDescription = null,
              modifier = Modifier.size(56.dp),
              tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "কোনো পাবলিক ট্যুর খুঁজে পাওয়া যায়নি" else "No shared public tours found",
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onNavigateToCreateTour,
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(L10n.planNewTour(lang))
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredTours) { tour ->
            PublicTourCard(
              tour = tour,
              lang = lang,
              onViewDetails = { onNavigateToTourDetail(tour.tourId) }
            )
          }

          item {
            Spacer(modifier = Modifier.height(30.dp))
          }
        }
      }
    }
  }
}
