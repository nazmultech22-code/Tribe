package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Destination
import com.example.data.model.DestinationAlias
import com.example.data.model.PublicTourDiscoveryItem
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.PublicTourCard
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun DestinationDetailScreen(
  destination: Destination,
  repository: TribeRepository,
  onNavigateToTourDetail: (String) -> Unit,
  onPlanTourHere: (Destination) -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var publicTours by remember { mutableStateOf<List<PublicTourDiscoveryItem>>(emptyList()) }
  var aliases by remember { mutableStateOf<List<DestinationAlias>>(emptyList()) }
  var isLoadingTours by remember { mutableStateOf(true) }
  var showAddAliasDialog by remember { mutableStateOf(false) }
  var newAliasText by remember { mutableStateOf("") }
  var newAliasLang by remember { mutableStateOf("bn") }
  var aliasSuccessMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(destination.id) {
    if (destination.id.isNotBlank()) {
      scope.launch {
        isLoadingTours = true
        // Exact destination ID matching on view "public_tour_discovery"
        publicTours = repository.supabaseClient.getPublicToursByDestination(destination.name)
        aliases = repository.supabaseClient.getAliasesForDestination(destination.id)
        isLoadingTours = false
      }
    } else {
      isLoadingTours = false
    }
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = destination.name,
        subtitle = destination.region ?: destination.country,
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onBackClick
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { onPlanTourHere(destination) },
        containerColor = TribeGreenPrimary,
        contentColor = Color.White,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(L10n.planTourHere(lang), fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("btn_plan_tour_here")
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Hero visual card
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = TribeGreenDark),
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF0F766E), Color(0xFF166534))
                )
              )
              .padding(16.dp)
          ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
              Text(
                text = destination.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              )
              if (!destination.region.isNullOrBlank()) {
                Text(
                  text = "${destination.region}, ${destination.country ?: "Bangladesh"}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFDCFCE7)
                  )
                )
              }
            }
          }
        }
      }

      // Aliases Section & Add Alias Action
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (lang == AppLanguage.BANGLA) "পরিচিত বিকল্প নাম / বানান" else "Known Aliases & Spellings",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
              TextButton(
                onClick = { showAddAliasDialog = true },
                modifier = Modifier.testTag("btn_open_add_alias")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = L10n.addAliasBtn(lang),
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }

            if (aliases.isEmpty()) {
              Text(
                text = if (lang == AppLanguage.BANGLA) "কোনো বিকল্প নাম নেই। নতুন নাম যোগ করতে পারেন।" else "No aliases registered yet. You can contribute one.",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                aliases.forEach { al ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                  ) {
                    Text(
                      text = "${al.alias}${if (!al.language.isNullOrBlank()) " (${al.language})" else ""}",
                      fontSize = 11.sp,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Description
      if (!destination.description.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.description(lang),
            icon = Icons.Default.Description,
            content = destination.description
          )
        }
      }

      // Best Visiting Season
      if (!destination.bestSeason.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.bestSeason(lang),
            icon = Icons.Default.CalendarMonth,
            content = destination.bestSeason
          )
        }
      }

      // Transport Information
      if (!destination.transportInfo.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.transportInfo(lang),
            icon = Icons.Default.DirectionsBus,
            content = destination.transportInfo
          )
        }
      }

      // Hotel & Accommodation Guidance
      if (!destination.accommodationInfo.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.hotelInfo(lang),
            icon = Icons.Default.Hotel,
            content = destination.accommodationInfo
          )
        }
      }

      // Food Information
      if (!destination.foodInfo.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.foodInfo(lang),
            icon = Icons.Default.Restaurant,
            content = destination.foodInfo
          )
        }
      }

      // Local Guide Information
      if (!destination.guideInfo.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.guideInfo(lang),
            icon = Icons.Default.SupportAgent,
            content = destination.guideInfo
          )
        }
      }

      // Safety Tips
      if (!destination.safetyTips.isNullOrBlank()) {
        item {
          InfoSectionCard(
            title = L10n.safetyTips(lang),
            icon = Icons.Default.Security,
            content = destination.safetyTips
          )
        }
      }

      // Section: Destination-Based Public Tour Discovery (Core Feature)
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = L10n.sharedToursForDestination(lang),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.primary
          )
        )
        Text(
          text = if (lang == AppLanguage.BANGLA) "শুধুমাত্র এই গন্তব্যের পাবলিক ট্যুর সমূহ দেখানো হচ্ছে" else "Showing exact matching public tours for this destination",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (isLoadingTours) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = TribeGreenPrimary)
          }
        }
      } else if (publicTours.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag("empty_state_destination_tours")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(42.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = L10n.noSharedToursFound(lang),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedButton(
                onClick = { onPlanTourHere(destination) },
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.planTourHere(lang))
              }
            }
          }
        }
      } else {
        items(publicTours) { tour ->
          PublicTourCard(
            tour = tour,
            lang = lang,
            onViewDetails = { onNavigateToTourDetail(tour.tourId) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
      }
    }
  }

  // Add Alias Dialog
  if (showAddAliasDialog) {
    AlertDialog(
      onDismissRequest = { showAddAliasDialog = false },
      title = { Text(L10n.addAliasBtn(lang)) },
      text = {
        Column {
          Text(
            text = if (lang == AppLanguage.BANGLA) "বাংলা বা ইংরেজি বিকল্প বানান অথবা ডাকনাম লিখুন:" else "Enter Bengali or English alias / spelling variation:",
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = newAliasText,
            onValueChange = { newAliasText = it },
            placeholder = { Text("e.g. Cox, সি ও এক, সেন্টমার্টিন") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_new_alias"),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = newAliasLang == "bn",
              onClick = { newAliasLang = "bn" },
              label = { Text("বাংলা (BN)") }
            )
            FilterChip(
              selected = newAliasLang == "en",
              onClick = { newAliasLang = "en" },
              label = { Text("English (EN)") }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAliasText.isNotBlank()) {
              scope.launch {
                val res = repository.supabaseClient.addAliases(
                  listOf(
                    DestinationAlias(
                      destinationId = destination.id,
                      alias = newAliasText.trim(),
                      language = newAliasLang
                    )
                  )
                )
                if (res.isSuccess) {
                  aliases = repository.supabaseClient.getAliasesForDestination(destination.id)
                  newAliasText = ""
                  showAddAliasDialog = false
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
          modifier = Modifier.testTag("btn_confirm_add_alias")
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddAliasDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }
}

@Composable
fun InfoSectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: String
) {
  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium.copy(
          lineHeight = 22.sp
        )
      )
    }
  }
}
