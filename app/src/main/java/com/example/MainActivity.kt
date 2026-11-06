package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
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
import com.example.data.model.Destination
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.screens.*
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import com.example.ui.theme.TribeTheme
import kotlinx.coroutines.launch

sealed class Screen {
  object Explore : Screen()
  object MyTours : Screen()
  object Community : Screen()
  object Profile : Screen()
  object SearchDestinations : Screen()
  data class DestinationDetail(val destination: Destination) : Screen()
  object CreateDestination : Screen()
  data class CreateTour(val preselectedDestination: Destination? = null) : Screen()
  data class TourDetail(val tourId: String) : Screen()
  object Auth : Screen()
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = TribeRepository.getInstance(applicationContext)

    setContent {
      TribeTheme {
        TribeApp(repository)
      }
    }
  }
}

@Composable
fun TribeApp(repository: TribeRepository) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  // Seed default popular destinations on first app launch
  LaunchedEffect(Unit) {
    scope.launch {
      repository.initializeAndSeedIfNeeded()
    }
  }

  val screenStack = remember { mutableStateListOf<Screen>(Screen.Explore) }
  val currentScreen = screenStack.lastOrNull() ?: Screen.Explore

  fun navigateTo(screen: Screen) {
    screenStack.add(screen)
  }

  fun navigateBack() {
    if (screenStack.size > 1) {
      screenStack.removeAt(screenStack.lastIndex)
    }
  }

  fun switchTab(screen: Screen) {
    screenStack.clear()
    screenStack.add(screen)
  }

  val isTopLevel = when (currentScreen) {
    is Screen.Explore, is Screen.MyTours, is Screen.Community, is Screen.Profile -> true
    else -> false
  }

  BackHandler(enabled = screenStack.size > 1) {
    navigateBack()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      if (isTopLevel) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp
        ) {
          NavigationBarItem(
            selected = currentScreen is Screen.Explore,
            onClick = { switchTab(Screen.Explore) },
            icon = { Icon(Icons.Default.Explore, contentDescription = L10n.navExplore(lang)) },
            label = { Text(L10n.navExplore(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TribeGreenDark,
              selectedTextColor = TribeGreenDark,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_explore")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.MyTours,
            onClick = { switchTab(Screen.MyTours) },
            icon = { Icon(Icons.Default.Luggage, contentDescription = L10n.navMyTours(lang)) },
            label = { Text(L10n.navMyTours(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TribeGreenDark,
              selectedTextColor = TribeGreenDark,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_my_tours")
          )

          // Center Create Button (+)
          NavigationBarItem(
            selected = false,
            onClick = { navigateTo(Screen.CreateTour()) },
            icon = {
              Surface(
                shape = CircleShape,
                color = TribeGreenPrimary,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = L10n.navCreate(lang),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            },
            label = { Text(L10n.navCreate(lang), fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_create_center")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.Community,
            onClick = { switchTab(Screen.Community) },
            icon = { Icon(Icons.Default.Groups, contentDescription = L10n.navCommunity(lang)) },
            label = { Text(L10n.navCommunity(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TribeGreenDark,
              selectedTextColor = TribeGreenDark,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_community")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.Profile,
            onClick = { switchTab(Screen.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = L10n.navProfile(lang)) },
            label = { Text(L10n.navProfile(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TribeGreenDark,
              selectedTextColor = TribeGreenDark,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_profile")
          )
        }
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(if (isTopLevel) padding else PaddingValues(0.dp))
    ) {
      when (val screen = currentScreen) {
        is Screen.Explore -> HomeScreen(
          repository = repository,
          onNavigateToSearch = { navigateTo(Screen.SearchDestinations) },
          onNavigateToDestination = { dest -> navigateTo(Screen.DestinationDetail(dest)) },
          onNavigateToCreateTour = { navigateTo(Screen.CreateTour()) },
          onNavigateToCommunity = { switchTab(Screen.Community) },
          onNavigateToMyTours = { switchTab(Screen.MyTours) },
          onNavigateToTourDetail = { tId -> navigateTo(Screen.TourDetail(tId)) },
          onNavigateToAuth = { navigateTo(Screen.Auth) },
          onNavigateToProfile = { switchTab(Screen.Profile) }
        )

        is Screen.MyTours -> MyToursScreen(
          repository = repository,
          onNavigateToTourDetail = { tId -> navigateTo(Screen.TourDetail(tId)) },
          onNavigateToCreateTour = { navigateTo(Screen.CreateTour()) },
          onNavigateToAuth = { navigateTo(Screen.Auth) }
        )

        is Screen.Community -> CommunityScreen(
          repository = repository,
          onNavigateToTourDetail = { tId -> navigateTo(Screen.TourDetail(tId)) },
          onNavigateToCreateTour = { navigateTo(Screen.CreateTour()) }
        )

        is Screen.Profile -> ProfileScreen(
          repository = repository,
          onNavigateToAuth = { navigateTo(Screen.Auth) }
        )

        is Screen.SearchDestinations -> DestinationSearchScreen(
          repository = repository,
          onDestinationSelected = { dest -> navigateTo(Screen.DestinationDetail(dest)) },
          onCreateDestination = { navigateTo(Screen.CreateDestination) },
          onBackClick = { navigateBack() }
        )

        is Screen.DestinationDetail -> DestinationDetailScreen(
          destination = screen.destination,
          repository = repository,
          onNavigateToTourDetail = { tId -> navigateTo(Screen.TourDetail(tId)) },
          onPlanTourHere = { dest -> navigateTo(Screen.CreateTour(dest)) },
          onBackClick = { navigateBack() }
        )

        is Screen.CreateDestination -> CreateDestinationScreen(
          repository = repository,
          onDestinationCreated = { dest ->
            navigateBack()
            navigateTo(Screen.DestinationDetail(dest))
          },
          onBackClick = { navigateBack() }
        )

        is Screen.CreateTour -> CreateTourScreen(
          repository = repository,
          preselectedDestination = screen.preselectedDestination,
          onTourCreated = { createdTour ->
            navigateBack()
            navigateTo(Screen.TourDetail(createdTour.id))
          },
          onNavigateToAuth = { navigateTo(Screen.Auth) },
          onBackClick = { navigateBack() }
        )

        is Screen.TourDetail -> TourDetailScreen(
          tourId = screen.tourId,
          repository = repository,
          onNavigateToDestination = { dest -> navigateTo(Screen.DestinationDetail(dest)) },
          onBackClick = { navigateBack() }
        )

        is Screen.Auth -> AuthScreen(
          repository = repository,
          onAuthSuccess = { navigateBack() },
          onDismiss = { navigateBack() }
        )
      }
    }
  }
}
