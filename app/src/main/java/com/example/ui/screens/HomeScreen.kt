package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Destination
import com.example.data.model.PublicTourDiscoveryItem
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.PublicTourCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  repository: TribeRepository,
  onNavigateToSearch: () -> Unit,
  onNavigateToDestination: (Destination) -> Unit,
  onNavigateToCreateTour: () -> Unit,
  onNavigateToCommunity: () -> Unit,
  onNavigateToMyTours: () -> Unit,
  onNavigateToTourDetail: (String) -> Unit,
  onNavigateToAuth: () -> Unit,
  onNavigateToProfile: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }
  var publicTours by remember { mutableStateOf<List<PublicTourDiscoveryItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    scope.launch {
      isLoading = true
      val dList = repository.getDestinations()
      val tList = repository.supabaseClient.getAllPublicTours()
      destinations = dList
      publicTours = tList
      isLoading = false
    }
  }

  val userName = sessionState.fullName?.ifBlank { null }
    ?: sessionState.email?.substringBefore("@")
    ?: L10n.guest(lang)

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. Top Green Header Bar (Mockup style)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(TribeGreenDark, TribeGreenPrimary)
            )
          )
          .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp)
      ) {
        Column {
          // Row with Avatar, Greeting & Language Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clickable {
                  if (sessionState.isLoggedIn) onNavigateToProfile() else onNavigateToAuth()
                }
                .testTag("btn_home_profile")
            ) {
              Surface(
                shape = CircleShape,
                color = TribeGreenContainer,
                modifier = Modifier.size(44.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = TribeGreenDark,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Text(
                  text = L10n.greeting(userName, lang),
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (sessionState.isLoggedIn) (sessionState.email ?: "") else (if (lang == AppLanguage.BANGLA) "লগইন করতে ট্যাপ করুন" else "Tap to sign in"),
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                  )
                )
              }
            }

            // Language switcher pill
            Surface(
              onClick = {
                val next = if (lang == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                repository.setLanguage(next)
              },
              shape = RoundedCornerShape(16.dp),
              color = Color.White.copy(alpha = 0.2f),
              modifier = Modifier.testTag("btn_home_lang_toggle")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Translate,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (lang == AppLanguage.BANGLA) "বাংলা" else "EN",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Search Bar (Triggers DestinationSearchScreen)
          Surface(
            onClick = onNavigateToSearch,
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("search_bar_trigger")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 16.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = L10n.searchPlaceholder(lang),
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    // 2. Featured Destination Banner Card
    item {
      Spacer(modifier = Modifier.height(16.dp))
      val featured = destinations.firstOrNull() ?: TribeRepository.DEFAULT_POPULAR_DESTINATIONS.first()

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TribeGreenDark),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .clickable {
            onNavigateToDestination(featured)
          }
          .testTag("card_featured_banner")
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF134E4A), Color(0xFF166534), Color(0xFF15803D))
              )
            )
            .padding(18.dp)
        ) {
          Column(
            modifier = Modifier
              .align(Alignment.CenterStart)
              .fillMaxWidth(0.7f)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color.White.copy(alpha = 0.2f)
            ) {
              Text(
                text = L10n.featuredDestination(lang),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = featured.name,
              color = Color.White,
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "৩ দিন • ৳ ৮,৫০০+" else "3 Days • ৳ 8,500+",
              color = Color(0xFFDCFCE7),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "দেখুন বিস্তারিত →" else "View Details →",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Icon illustration on right
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
              .size(64.dp)
              .align(Alignment.CenterEnd)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Landscape,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
              )
            }
          }
        }
      }
    }

    // 3. Quick Access Grid (4 cards like the mockup)
    item {
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = L10n.quickAccess(lang),
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        ),
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickAccessCard(
          title = L10n.planNewTour(lang),
          icon = Icons.Default.FlightTakeoff,
          iconBg = Color(0xFFDCFCE7),
          iconColor = TribeGreenDark,
          modifier = Modifier.weight(1f),
          testTag = "quick_btn_new_tour",
          onClick = onNavigateToCreateTour
        )

        QuickAccessCard(
          title = L10n.viewCommunity(lang),
          icon = Icons.Default.Groups,
          iconBg = Color(0xFFDBEAFE),
          iconColor = Color(0xFF1D4ED8),
          modifier = Modifier.weight(1f),
          testTag = "quick_btn_community",
          onClick = onNavigateToCommunity
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickAccessCard(
          title = L10n.destinationGuide(lang),
          icon = Icons.Default.Map,
          iconBg = Color(0xFFFEF3C7),
          iconColor = Color(0xFFB45309),
          modifier = Modifier.weight(1f),
          testTag = "quick_btn_guide",
          onClick = onNavigateToSearch
        )

        QuickAccessCard(
          title = L10n.myTravelHistory(lang),
          icon = Icons.Default.EmojiEvents,
          iconBg = Color(0xFFF3E8FF),
          iconColor = Color(0xFF7E22CE),
          modifier = Modifier.weight(1f),
          testTag = "quick_btn_history",
          onClick = onNavigateToMyTours
        )
      }
    }

    // 4. Popular Destinations Section
    item {
      Spacer(modifier = Modifier.height(24.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = L10n.popularDestinations(lang),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        )
        TextButton(
          onClick = onNavigateToSearch,
          modifier = Modifier.testTag("btn_see_all_destinations")
        ) {
          Text(
            text = L10n.seeAll(lang),
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (destinations.isEmpty() && isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = TribeGreenPrimary)
        }
      } else {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(destinations) { dest ->
            ElevatedCard(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
              modifier = Modifier
                .width(170.dp)
                .clickable { onNavigateToDestination(dest) }
                .testTag("card_popular_dest_${dest.id}")
            ) {
              Column {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(
                      Brush.verticalGradient(
                        listOf(Color(0xFF0F766E), Color(0xFF15803D))
                      )
                    )
                    .padding(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Terrain,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                      .size(36.dp)
                      .align(Alignment.Center)
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.BottomStart)
                  ) {
                    Text(
                      text = dest.region ?: "Bangladesh",
                      color = Color.White,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = dest.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = if (dest.bestSeason != null) dest.bestSeason.take(20) else "All seasons",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }

    // 5. Community Shared Tours (from public_tour_discovery)
    item {
      Spacer(modifier = Modifier.height(24.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (lang == AppLanguage.BANGLA) "কমিউনিটি ভ্রমণ প্ল্যান সমূহ" else "Community Tour Plans",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        )
        TextButton(onClick = onNavigateToCommunity) {
          Text(
            text = L10n.seeAll(lang),
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }

    if (publicTours.isEmpty() && !isLoading) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.TravelExplore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "কমিউনিটিতে কোনো পাবলিক ট্যুর খুঁজে পাওয়া যায়নি।" else "No public tours available yet.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onNavigateToCreateTour,
              colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
            ) {
              Text(L10n.planNewTour(lang))
            }
          }
        }
      }
    } else {
      items(publicTours.take(4)) { tour ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
          PublicTourCard(
            tour = tour,
            lang = lang,
            onViewDetails = { onNavigateToTourDetail(tour.tourId) }
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
    }
  }
}

@Composable
fun QuickAccessCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBg: Color,
  iconColor: Color,
  modifier: Modifier = Modifier,
  testTag: String,
  onClick: () -> Unit
) {
  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = CircleShape,
        color = iconBg,
        modifier = Modifier.size(46.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
