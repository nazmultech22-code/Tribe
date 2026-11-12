package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenContainer
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  repository: TribeRepository,
  onNavigateToAuth: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var profile by remember { mutableStateOf<UserProfile?>(null) }
  var notifications by remember { mutableStateOf<List<com.example.data.model.AppNotification>>(emptyList()) }
  val pendingSyncCount by repository.observePendingSyncCount().collectAsState(initial = 0)
  val failedSyncCount by repository.observeFailedSyncCount().collectAsState(initial = 0)
  val conflictSyncCount by repository.observeSyncConflictCount().collectAsState(initial = 0)
  var isLoading by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showSignOutDialog by remember { mutableStateOf(false) }
  var showGoogleInfoDialog by remember { mutableStateOf(false) }

  // Edit fields
  var editFullName by remember { mutableStateOf("") }
  var editUsername by remember { mutableStateOf("") }
  var editBio by remember { mutableStateOf("") }
  var editCity by remember { mutableStateOf("") }
  var editCountry by remember { mutableStateOf("") }
  var editPhone by remember { mutableStateOf("") }

  LaunchedEffect(sessionState.userId) {
    val uid = sessionState.userId
    if (uid != null) {
      isLoading = true
      val p = repository.supabaseClient.getProfile(uid)
      profile = p
      notifications = repository.supabaseClient.getNotifications(uid)
      if (p != null) {
        editFullName = p.fullName ?: ""
        editUsername = p.username ?: ""
        editBio = p.bio ?: ""
        editCity = p.city ?: ""
        editCountry = p.country ?: ""
        editPhone = p.phone ?: ""
      }
      isLoading = false
    }
  }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.navProfile(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (!sessionState.isLoggedIn) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(72.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (lang == AppLanguage.BANGLA) "সাইন ইন করে পুরো ট্রাইব অভিজ্ঞতা উপভোগ করুন" else "Sign in for the complete TRIBE experience",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
      } else {
        // User Profile Header Card
        ElevatedCard(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              shape = CircleShape,
              color = TribeGreenContainer,
              modifier = Modifier.size(76.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  modifier = Modifier.size(40.dp),
                  tint = TribeGreenDark
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = profile?.fullName ?: sessionState.fullName ?: "Traveler",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
              text = "@${profile?.username ?: sessionState.username ?: "user"}",
              style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
            )

            Text(
              text = sessionState.email ?: "",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (!profile?.bio.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = profile?.bio ?: "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
              onClick = { showEditDialog = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("btn_edit_profile_open")
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(L10n.editProfile(lang))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Language Switch Card
      ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = L10n.languageSetting(lang),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            FilterChip(
              selected = lang == AppLanguage.BANGLA,
              onClick = { repository.setLanguage(AppLanguage.BANGLA) },
              label = { Text("বাংলা (Bangla)") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = lang == AppLanguage.ENGLISH,
              onClick = { repository.setLanguage(AppLanguage.ENGLISH) },
              label = { Text("English") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))


      if (sessionState.isLoggedIn) {
        Spacer(modifier = Modifier.height(12.dp))
        ElevatedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Notifications, contentDescription = null, tint = TribeGreenPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (lang == AppLanguage.BANGLA) "নোটিফিকেশন" else "Notifications",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.weight(1f))
              Text(notifications.count { !it.isRead }.toString(), color = TribeGreenPrimary, fontWeight = FontWeight.Bold)
            }
            if (notifications.isEmpty()) {
              Text(
                text = if (lang == AppLanguage.BANGLA) "নতুন কোনো নোটিফিকেশন নেই" else "No notifications yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              notifications.take(5).forEach { notification ->
                Row(
                  modifier = Modifier.fillMaxWidth().clickable {
                    if (!notification.isRead) {
                      scope.launch {
                        val result = repository.supabaseClient.markNotificationRead(notification.id)
                        if (result.isSuccess) notifications = notifications.map {
                          if (it.id == notification.id) it.copy(readAt = "read") else it
                        }
                      }
                    }
                  }.padding(vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    if (notification.isRead) Icons.Default.Drafts else Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = if (notification.isRead) MaterialTheme.colorScheme.outline else TribeGreenPrimary
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(notification.title, fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold)
                    if (!notification.body.isNullOrBlank()) Text(notification.body, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }
          }
        }

        if (pendingSyncCount > 0 || failedSyncCount > 0 || conflictSyncCount > 0) {
          Spacer(modifier = Modifier.height(12.dp))
          ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = TribeGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (lang == AppLanguage.BANGLA) "অফলাইন Sync" else "Offline sync",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
              }
              Text(
                text = (if (lang == AppLanguage.BANGLA) "অপেক্ষমাণ: " else "Pending: ") + pendingSyncCount +
                  (if (lang == AppLanguage.BANGLA) " • ব্যর্থ: " else " • Failed: ") + failedSyncCount +
                  (if (lang == AppLanguage.BANGLA) " • দ্বন্দ্ব: " else " • Conflicts: ") + conflictSyncCount,
                style = MaterialTheme.typography.bodySmall
              )
              if (failedSyncCount > 0) {
                OutlinedButton(onClick = { scope.launch { repository.retryFailedSyncOperations() } }) {
                  Text(if (lang == AppLanguage.BANGLA) "ব্যর্থ Sync আবার চেষ্টা করুন" else "Retry failed sync")
                }
              }
            }
          }
        }
      }

      // Google Sign-In Info Item
      ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showGoogleInfoDialog = true }
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (lang == AppLanguage.BANGLA) "গুগল সাইন-ইন কনফিগারেশন" else "Google Sign-In Status",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            Text(
              text = if (lang == AppLanguage.BANGLA) "বর্তমান স্ট্যাটাস ও নির্দেশিকা জানুন" else "Learn what is needed to activate Google Sign-In",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
      }

      if (sessionState.isLoggedIn) {
        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { showSignOutDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_sign_out")
        ) {
          Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(L10n.signOut(lang), fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditDialog && sessionState.userId != null) {
    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text(L10n.editProfile(lang)) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          OutlinedTextField(
            value = editFullName,
            onValueChange = { editFullName = it },
            label = { Text(L10n.fullName(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editUsername,
            onValueChange = { editUsername = it },
            label = { Text(L10n.username(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text(L10n.bio(lang)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editCity,
            onValueChange = { editCity = it },
            label = { Text(L10n.city(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editCountry,
            onValueChange = { editCountry = it },
            label = { Text(L10n.country(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editPhone,
            onValueChange = { editPhone = it },
            label = { Text(L10n.phone(lang)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = repository.supabaseClient.updateProfile(
                userId = sessionState.userId!!,
                fullName = editFullName.trim(),
                username = editUsername.trim(),
                bio = editBio.trim().ifBlank { null },
                city = editCity.trim().ifBlank { null },
                country = editCountry.trim().ifBlank { null },
                phone = editPhone.trim().ifBlank { null },
                avatarUrl = null
              )
              if (res.isSuccess) {
                profile = repository.supabaseClient.getProfile(sessionState.userId!!)
                showEditDialog = false
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
        ) {
          Text(L10n.saveBtn(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Sign out confirmation dialog
  if (showSignOutDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutDialog = false },
      title = { Text(L10n.signOut(lang)) },
      text = { Text(L10n.signOutConfirm(lang)) },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              repository.supabaseClient.signOut()
              showSignOutDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(L10n.signOut(lang))
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutDialog = false }) {
          Text(L10n.cancelBtn(lang))
        }
      }
    )
  }

  // Google Sign-In Status Dialog
  if (showGoogleInfoDialog) {
    AlertDialog(
      onDismissRequest = { showGoogleInfoDialog = false },
      title = { Text(if (lang == AppLanguage.BANGLA) "গুগল সাইন-ইন সক্রিয়করণ" else "Google Sign-In Activation") },
      text = {
        Text(
          text = L10n.googleLoginUnavailable(lang),
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = { showGoogleInfoDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary)
        ) {
          Text(L10n.closeBtn(lang))
        }
      }
    )
  }
}
