package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.BuildConfig
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenDark
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  repository: TribeRepository,
  onAuthSuccess: () -> Unit,
  onDismiss: (() -> Unit)? = null
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var isSignUp by remember { mutableStateOf(false) }
  var isForgotPassword by remember { mutableStateOf(false) }

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var fullName by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var showGoogleInfoDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TribeHeader(
        title = if (isForgotPassword) L10n.resetPassword(lang)
        else if (isSignUp) L10n.signupTitle(lang)
        else L10n.loginTitle(lang),
        lang = lang,
        onLanguageToggle = { repository.setLanguage(it) },
        onBackClick = onDismiss
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // App Logo & Brand Header
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(80.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Image(
            painter = painterResource(id = R.drawable.ic_tribe_logo),
            contentDescription = "TRIBE Logo",
            modifier = Modifier.size(68.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = L10n.appName(lang),
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          color = TribeGreenDark
        )
      )

      Text(
        text = L10n.appTagline(lang),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Status messages
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
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }

      if (successMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFDCFCE7),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Text(
            text = successMessage ?: "",
            color = Color(0xFF166534),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }

      // Fields
      if (isSignUp && !isForgotPassword) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text(L10n.fullName(lang)) },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_fullname"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text(L10n.username(lang)) },
          leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_username"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
      }

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(L10n.email(lang)) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_email"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      if (!isForgotPassword) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(L10n.password(lang)) },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_password"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        if (!isSignUp) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = {
                isForgotPassword = true
                errorMessage = null
                successMessage = null
              },
              modifier = Modifier.testTag("btn_forgot_password")
            ) {
              Text(
                text = L10n.forgotPassword(lang),
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Main Action Button
      Button(
        onClick = {
          errorMessage = null
          successMessage = null
          if (email.isBlank()) {
            errorMessage = if (lang == AppLanguage.BANGLA) "দয়া করে ইমেইল লিখুন" else "Please enter your email"
            return@Button
          }

          if (isForgotPassword) {
            isLoading = true
            scope.launch {
              val res = repository.supabaseClient.resetPassword(email.trim())
              isLoading = false
              if (res.isSuccess) {
                successMessage = L10n.resetPasswordSent(lang)
              } else {
                errorMessage = res.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
              }
            }
            return@Button
          }

          if (password.length < 6) {
            errorMessage = if (lang == AppLanguage.BANGLA) "পাসওয়ার্ড অন্তত ৬ অক্ষরের হতে হবে" else "Password must be at least 6 characters"
            return@Button
          }

          isLoading = true
          scope.launch {
            if (isSignUp) {
              val res = repository.supabaseClient.signUp(
                email = email.trim(),
                pass = password,
                fullName = fullName.trim().ifEmpty { "Traveler" },
                username = username.trim().ifEmpty { email.substringBefore("@") }
              )
              isLoading = false
              if (res.isSuccess) {
                repository.initializeAndSeedIfNeeded()
                onAuthSuccess()
              } else {
                errorMessage = res.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
              }
            } else {
              val res = repository.supabaseClient.signIn(
                email = email.trim(),
                pass = password
              )
              isLoading = false
              if (res.isSuccess) {
                repository.initializeAndSeedIfNeeded()
                onAuthSuccess()
              } else {
                errorMessage = res.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
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
          .testTag("btn_auth_submit")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
        } else {
          Text(
            text = if (isForgotPassword) L10n.sendResetLink(lang)
            else if (isSignUp) L10n.signupTitle(lang)
            else L10n.loginTitle(lang),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Native Google Credential Manager sign-in, exchanged for a Supabase session.
      OutlinedButton(
        onClick = {
          val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
          if (webClientId.isBlank() || webClientId.startsWith("YOUR_")) {
            showGoogleInfoDialog = true
          } else {
            isLoading = true
            errorMessage = null
            scope.launch {
              try {
                val googleOption = GetGoogleIdOption.Builder()
                  .setServerClientId(webClientId)
                  .setFilterByAuthorizedAccounts(false)
                  .setAutoSelectEnabled(false)
                  .build()
                val request = GetCredentialRequest.Builder()
                  .addCredentialOption(googleOption)
                  .build()
                val credential = CredentialManager.create(context)
                  .getCredential(context, request).credential
                if (credential is CustomCredential &&
                  credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                  val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                  val result = repository.supabaseClient.signInWithGoogleIdToken(googleToken)
                  if (result.isSuccess) {
                    repository.initializeAndSeedIfNeeded()
                    onAuthSuccess()
                  } else {
                    errorMessage = result.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
                  }
                } else {
                  errorMessage = if (lang == AppLanguage.BANGLA) "Google অ্যাকাউন্ট পাওয়া যায়নি" else "Google account was not returned"
                }
              } catch (e: GetCredentialException) {
                errorMessage = e.localizedMessage ?: if (lang == AppLanguage.BANGLA) "Google সাইন-ইন বাতিল বা ব্যর্থ হয়েছে" else "Google sign-in was cancelled or failed"
              } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: L10n.errorOccurred(lang)
              } finally {
                isLoading = false
              }
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_google_info")
      ) {
        Icon(
          imageVector = Icons.Default.Login,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (lang == AppLanguage.BANGLA) "Google দিয়ে চালিয়ে যান" else "Continue with Google",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Toggle Sign Up / Sign In
      TextButton(
        onClick = {
          if (isForgotPassword) {
            isForgotPassword = false
          } else {
            isSignUp = !isSignUp
          }
          errorMessage = null
          successMessage = null
        },
        modifier = Modifier.testTag("btn_toggle_auth_mode")
      ) {
        Text(
          text = if (isForgotPassword) L10n.alreadyHaveAccount(lang)
          else if (isSignUp) L10n.alreadyHaveAccount(lang)
          else L10n.dontHaveAccount(lang),
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
          )
        )
      }
    }
  }

  if (showGoogleInfoDialog) {
    AlertDialog(
      onDismissRequest = { showGoogleInfoDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (lang == AppLanguage.BANGLA) "গুগল লগইন স্ট্যাটাস" else "Google Login Status")
        }
      },
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
