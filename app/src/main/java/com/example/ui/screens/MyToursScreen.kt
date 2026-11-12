package com.example.ui.screens

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
import org.json.JSONObject

@Composable
fun MyToursScreen(
  repository: TribeRepository,
  onNavigateToTourDetail: (String) -> Unit,
  onNavigateToCreateTour: () -> Unit,
  onNavigateToSavedTrips: () -> Unit,
  onNavigateToAuth: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var tours by remember { mutableStateOf<List<Tour>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var inviteCode by remember { mutableStateOf("") }
  var inviteMessage by remember { mutableStateOf<String?>(null) }
  var isRedeemingInvite by remember { mutableStateOf(false) }

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
        onLanguageToggle = { repository.setLanguage(it) },
        actions = {
          IconButton(
            onClick = onNavigateToSavedTrips,
            modifier = Modifier.testTag("btn_open_saved_trips_room")
          ) {
            Icon(
              imageVector = Icons.Default.Bookmark,
              contentDescription = L10n.savedTripsTitle(lang),
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
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
            colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
            modifier = Modifier.testTag("btn_auth_sign_in_prompt")
          ) {
            Text(L10n.loginTitle(lang))
          }
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(
            onClick = onNavigateToSavedTrips,
            modifier = Modifier.testTag("btn_offline_saved_trips_unauth")
          ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.savedTripsTitle(lang))
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

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = if (lang == AppLanguage.BANGLA) "Invite Code দিয়ে ট্যুরে যোগ দিন" else "Join a tour with an invite code",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          OutlinedTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it.trim() },
            label = { Text(if (lang == AppLanguage.BANGLA) "৬৪ অক্ষরের Invite Code" else "64-character invite code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Button(
            enabled = inviteCode.length == 64 && !isRedeemingInvite,
            onClick = {
              scope.launch {
                isRedeemingInvite = true
                inviteMessage = null
                val result = repository.supabaseClient.redeemTourInvite(inviteCode)
                isRedeemingInvite = false
                if (result.isSuccess) {
                  val response = JSONObject(result.getOrThrow())
                  inviteMessage = when (response.optString("status")) {
                    "request_submitted" -> if (lang == AppLanguage.BANGLA) "যোগদানের অনুরোধ পাঠানো হয়েছে" else "Join request submitted"
                    "already_member" -> if (lang == AppLanguage.BANGLA) "আপনি ইতিমধ্যে এই ট্যুরের সদস্য" else "You are already a member"
                    else -> if (lang == AppLanguage.BANGLA) "Invite সফলভাবে ব্যবহার হয়েছে" else "Invite redeemed successfully"
                  }
                  inviteCode = ""
                  fetchTours()
                } else {
                  inviteMessage = result.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
                }
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(if (isRedeemingInvite) "…" else if (lang == AppLanguage.BANGLA) "Invite ব্যবহার করুন" else "Redeem invite")
          }
          if (inviteMessage != null) {
            Text(inviteMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }

      // Room Saved Trips shortcut banner
      Card(
        onClick = onNavigateToSavedTrips,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("card_saved_trips_room_banner")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = TribeGreenPrimary,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = L10n.savedTripsTitle(lang),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TribeGreenDark)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = TribeGreenDark,
                modifier = Modifier.padding(2.dp)
              ) {
                Text(
                  text = L10n.roomDatabaseTag(lang),
                  color = Color.White,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
            Text(
              text = if (lang == AppLanguage.BANGLA) "লোকাল Room ডাটাবেসে সংরক্ষিত ট্রিপস দেখুন ও পরিচালনা করুন" else "View and manage trips in local Room DB",
              style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF2E7D32), fontSize = 11.sp)
            )
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TribeGreenDark
          )
        }
      }

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
