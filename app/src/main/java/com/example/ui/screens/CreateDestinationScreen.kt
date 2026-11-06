package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.Destination
import com.example.data.repository.TribeRepository
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.components.TribeHeader
import com.example.ui.theme.TribeGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun CreateDestinationScreen(
  repository: TribeRepository,
  onDestinationCreated: (Destination) -> Unit,
  onBackClick: () -> Unit
) {
  val sessionState by repository.sessionState.collectAsState()
  val lang = sessionState.language
  val scope = rememberCoroutineScope()

  var name by remember { mutableStateOf("") }
  var country by remember { mutableStateOf("Bangladesh") }
  var region by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var bestSeason by remember { mutableStateOf("") }
  var transportInfo by remember { mutableStateOf("") }
  var accommodationInfo by remember { mutableStateOf("") }
  var foodInfo by remember { mutableStateOf("") }
  var guideInfo by remember { mutableStateOf("") }
  var safetyTips by remember { mutableStateOf("") }
  var bnAliasesInput by remember { mutableStateOf("") }
  var enAliasesInput by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TribeHeader(
        title = L10n.createDestinationTitle(lang),
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

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(L10n.destinationName(lang)) },
        placeholder = { Text("e.g. সাজেক ভ্যালি / Sajek Valley") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_name"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = country,
          onValueChange = { country = it },
          label = { Text(L10n.country(lang)) },
          modifier = Modifier
            .weight(1f)
            .testTag("input_dest_country"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = region,
          onValueChange = { region = it },
          label = { Text(L10n.region(lang)) },
          placeholder = { Text("e.g. রাঙ্গামাটি / Chittagong") },
          modifier = Modifier
            .weight(1f)
            .testTag("input_dest_region"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(L10n.description(lang)) },
        placeholder = { Text("গন্তব্যের সুন্দর বিবরণ লিখুন...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_desc"),
        shape = RoundedCornerShape(12.dp),
        minLines = 3
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = bestSeason,
        onValueChange = { bestSeason = it },
        label = { Text(L10n.bestSeason(lang)) },
        placeholder = { Text("e.g. অক্টোবর - মার্চ (শীতকাল)") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_season"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = transportInfo,
        onValueChange = { transportInfo = it },
        label = { Text(L10n.transportInfo(lang)) },
        placeholder = { Text("কীভাবে যাবেন, বাস, ট্রেন, চাঁদের গাড়ি...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_transport"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = accommodationInfo,
        onValueChange = { accommodationInfo = it },
        label = { Text(L10n.hotelInfo(lang)) },
        placeholder = { Text("রিসোর্ট, হোটেল ও কটেজ তথ্য...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_hotel"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = foodInfo,
        onValueChange = { foodInfo = it },
        label = { Text(L10n.foodInfo(lang)) },
        placeholder = { Text("ঐতিহ্যবাহী খাবার, রেস্তোরাঁ...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_food"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = guideInfo,
        onValueChange = { guideInfo = it },
        label = { Text(L10n.guideInfo(lang)) },
        placeholder = { Text("রেজিস্টার্ড গাইড, যোগাযোগের তথ্য...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_guide"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = safetyTips,
        onValueChange = { safetyTips = it },
        label = { Text(L10n.safetyTips(lang)) },
        placeholder = { Text("জরুরি নম্বর, সতর্কতা ও নির্দেশিকা...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_safety"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Bengali & English Aliases Inputs
      Text(
        text = if (lang == AppLanguage.BANGLA) "বিকল্প নাম ও বানান (স্মার্ট সার্চের জন্য)" else "Aliases & Alternate Spellings (For Smart Search)",
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = bnAliasesInput,
        onValueChange = { bnAliasesInput = it },
        label = { Text(L10n.bnAlias(lang)) },
        placeholder = { Text("কক্স, সি ও এক, কক্সবাজার") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_bn_aliases"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = enAliasesInput,
        onValueChange = { enAliasesInput = it },
        label = { Text(L10n.enAlias(lang)) },
        placeholder = { Text("Cox, C-O-X, Coxs Bazar") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_dest_en_aliases"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          errorMessage = null
          if (name.isBlank()) {
            errorMessage = if (lang == AppLanguage.BANGLA) "দয়া করে গন্তব্যের নাম লিখুন" else "Please enter destination name"
            return@Button
          }

          val bnList = bnAliasesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          val enList = enAliasesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

          isLoading = true
          scope.launch {
            val res = repository.supabaseClient.createDestination(
              name = name.trim(),
              country = country.trim(),
              region = region.trim(),
              description = description.trim(),
              bestSeason = bestSeason.trim(),
              transportInfo = transportInfo.trim(),
              accommodationInfo = accommodationInfo.trim(),
              foodInfo = foodInfo.trim(),
              guideInfo = guideInfo.trim(),
              safetyTips = safetyTips.trim(),
              bnAliases = bnList,
              enAliases = enList
            )
            isLoading = false
            if (res.isSuccess) {
              onDestinationCreated(res.getOrThrow())
            } else {
              errorMessage = res.exceptionOrNull()?.message ?: L10n.errorOccurred(lang)
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TribeGreenPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_save_destination")
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        } else {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(L10n.saveBtn(lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
