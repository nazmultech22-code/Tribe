package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Destination
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DestinationSearchScreen(
  repository: TribeRepository,
  onDestinationSelected: (Destination) -> Unit,
  onCreateDestination: () -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var searchQuery by remember { mutableStateOf("") }
  var results by remember { mutableStateOf<List<Destination>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  // Initial load
  LaunchedEffect(Unit) {
    isLoading = true
    results = repository.getDestinations()
    isLoading = false
  }

  fun performSearch(query: String) {
    searchJob?.cancel()
    searchJob = scope.launch {
      delay(250) // debounce typing
      isLoading = true
      results = repository.searchDestinations(query)
      isLoading = false
    }
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.searchTitle(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onBackClick
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

      // Search input field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = {
          searchQuery = it
          performSearch(it)
        },
        placeholder = { Text(L10n.searchPlaceholder(lang)) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = {
                searchQuery = ""
                performSearch("")
              }
            ) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_destination_search")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Common aliases suggestion chips for quick testing (e.g., Cox's Bazar, C-O-X, সি ও এক, সাজেক)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val sampleChips = listOf(
          "কক্সবাজার",
          "Cox's Bazar",
          "C-O-X",
          "সি ও এক",
          "সাজেক"
        )
        sampleChips.take(4).forEach { tag ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable {
              searchQuery = tag
              performSearch(tag)
            }
          ) {
            Text(
              text = tag,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Add New Destination Action Row
      Surface(
        onClick = onCreateDestination,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_create_destination_banner")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AddLocationAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = L10n.createNewDestination(lang),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
              Text(
                text = if (lang == AppLanguage.BANGLA) "তালিকা না থাকলে নতুন স্পট যুক্ত করুন" else "Contribute a new travel destination",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              )
            }
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = TribeGreenPrimary)
        }
      } else if (results.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.LocationOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = L10n.noDestinationFound(lang),
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onCreateDestination,
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(L10n.createNewDestination(lang))
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(results) { dest ->
            ElevatedCard(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
              elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onDestinationSelected(dest) }
                .testTag("destination_item_${dest.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(48.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Place,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(26.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = dest.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (!dest.region.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${dest.region}${if (!dest.country.isNullOrBlank()) ", ${dest.country}" else ""}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    )
                  }
                  if (!dest.bestSeason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "${L10n.bestSeason(lang)}: ${dest.bestSeason}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                      ),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
