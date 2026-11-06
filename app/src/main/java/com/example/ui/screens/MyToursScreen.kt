package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tour
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.StatusBadge
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun MyToursScreen(
  repository: TribeRepository,
  onNavigateToTourDetail: (String) -> Unit,
  onNavigateToCreateTour: () -> Unit,
  onNavigateToAuth: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var tours by remember { mutableStateOf<List<Tour>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var selectedTab by remember { mutableIntStateOf(0) }

  fun fetchTours() {
    val uid = sessionState.userId
    if (uid != null) {
      scope.launch {
        isLoading = true
        tours = repository.supabaseClient.getMyTours(uid)
        isLoading = false
      }
    } else {
      isLoading = false
    }
  }

  LaunchedEffect(sessionState.userId) {
    fetchTours()
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.myToursTitle(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNavigateToCreateTour,
        containerColor = TribeGreenPrimary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier.testTag("fab_create_tour")
      ) {
        Icon(Icons.Default.Add, contentDescription = L10n.planNewTour(lang))
      }
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
            text = if (lang == AppLanguage.BANGLA) "আপনার ট্যুর দেখতে সাইন ইন করুন" else "Sign in to view your tours",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onNavigateToAuth,
            colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
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
        .padding(horizontal = 16.dp)
    ) {
      // Mockup-style tabs: "সম্পন্ন ট্যুর" / "প্ল্যানকৃত ট্যুর" / "সব"
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = androidx.compose.ui.graphics.Color.Transparent
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              if (lang == AppLanguage.BANGLA) "সব ট্যুর" else "All",
              fontWeight = FontWeight.SemiBold
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              if (lang == AppLanguage.BANGLA) "প্ল্যানকৃত ট্যুর" else "Planned",
              fontWeight = FontWeight.SemiBold
            )
          }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = {
            Text(
              if (lang == AppLanguage.BANGLA) "সম্পন্ন ট্যুর" else "Completed",
              fontWeight = FontWeight.SemiBold
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      val filteredTours = when (selectedTab) {
        1 -> tours.filter { it.status.equals("planning", true) }
        2 -> tours.filter { it.status.equals("completed", true) }
        else -> tours
      }

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
              imageVector = Icons.Default.Luggage,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "কোনো ট্যুর পাওয়া যায়নি" else "No tours found",
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
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredTours) { tour ->
            ElevatedCard(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToTourDetail(tour.id) }
                .testTag("my_tour_card_${tour.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Text(
                      text = tour.title,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                      ),
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (!tour.startDate.isNullOrBlank()) {
                      Text(
                        text = "${tour.startDate}${if (!tour.endDate.isNullOrBlank()) " - ${tour.endDate}" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontSize = 12.sp
                        )
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      StatusBadge(status = tour.status, lang = lang)
                      Spacer(modifier = Modifier.width(6.dp))
                      if (tour.visibility == "public") {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                          Text(
                            text = if (lang == AppLanguage.BANGLA) "পাবলিক" else "Public",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
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
