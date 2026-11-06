package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublicTourDiscoveryItem
import com.example.i18n.AppLanguage
import com.example.i18n.L10n
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TribeHeader(
  title: String,
  subtitle: String? = null,
  lang: AppLanguage,
  onLanguageToggle: (AppLanguage) -> Unit,
  onBackClick: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {}
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 12.sp
            ),
            maxLines = 1
          )
        }
      }
    },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.testTag("btn_back")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = L10n.back(lang)
          )
        }
      }
    },
    actions = {
      // Language Switcher Chip
      Surface(
        onClick = {
          val next = if (lang == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
          onLanguageToggle(next)
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
          .padding(end = 8.dp)
          .testTag("btn_language_switch")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = "Language",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (lang == AppLanguage.BANGLA) "বাংলা" else "EN",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }
      actions()
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface
    )
  )
}

@Composable
fun StatusBadge(status: String?, lang: AppLanguage) {
  val cleanStatus = status?.lowercase() ?: "planning"
  val (label, bgColor, textColor) = when (cleanStatus) {
    "completed" -> Triple(L10n.statusCompleted(lang), Color(0xFFDCFCE7), Color(0xFF15803D))
    "ongoing" -> Triple(L10n.statusOngoing(lang), Color(0xFFDBEAFE), Color(0xFF1D4ED8))
    "cancelled" -> Triple(L10n.statusCancelled(lang), Color(0xFFFEE2E2), Color(0xFFB91C1C))
    else -> Triple(L10n.statusPlanning(lang), Color(0xFFFEF3C7), Color(0xFFB45309))
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    modifier = Modifier.padding(2.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun PublicTourCard(
  tour: PublicTourDiscoveryItem,
  lang: AppLanguage,
  onViewDetails: () -> Unit
) {
  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
      .testTag("card_public_tour_${tour.tourId}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Title & Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = tour.tourTitle ?: "Tour",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          ),
          modifier = Modifier.weight(1f)
        )
        StatusBadge(status = tour.status, lang = lang)
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Destination & Dates
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = tour.destinationName ?: "Destination",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        )

        if (!tour.startDate.isNullOrBlank()) {
          Spacer(modifier = Modifier.width(12.dp))
          Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${tour.startDate}${if (!tour.endDate.isNullOrBlank()) " - ${tour.endDate}" else ""}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Financials Summary Box (Safe Aggregates)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Total recorded expenses or estimated budget
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = L10n.actualExpense(lang),
              style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (tour.actualExpenseTotal != null && tour.actualExpenseTotal > 0) {
              Text(
                text = L10n.formatCurrency(tour.actualExpenseTotal, lang),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TribeGreenDark
                )
              )
            } else {
              Text(
                text = L10n.noExpensesRecorded(lang),
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.outline,
                  fontSize = 11.sp
                )
              )
            }
          }

          // Per-Person cost or per-person estimate
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
          ) {
            val perPerson = tour.actualPerPerson ?: tour.estimatedPerPerson
            Text(
              text = if (tour.actualPerPerson != null) L10n.perPersonActual(lang) else L10n.perPersonEstimated(lang),
              style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = L10n.formatCurrency(perPerson, lang),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            )
          }
        }
      }

      // Category breakdown chips if any expenses exist
      val hasBreakdown = (tour.transportTotal ?: 0.0) > 0 ||
        (tour.accommodationTotal ?: 0.0) > 0 ||
        (tour.foodTotal ?: 0.0) > 0 ||
        (tour.otherTotal ?: 0.0) > 0

      if (hasBreakdown) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if ((tour.transportTotal ?: 0.0) > 0) {
            CostMiniChip(Icons.Default.DirectionsBus, L10n.formatCurrency(tour.transportTotal, lang))
          }
          if ((tour.accommodationTotal ?: 0.0) > 0) {
            CostMiniChip(Icons.Default.Hotel, L10n.formatCurrency(tour.accommodationTotal, lang))
          }
          if ((tour.foodTotal ?: 0.0) > 0) {
            CostMiniChip(Icons.Default.Restaurant, L10n.formatCurrency(tour.foodTotal, lang))
          }
          if ((tour.otherTotal ?: 0.0) > 0) {
            CostMiniChip(Icons.Default.Category, L10n.formatCurrency(tour.otherTotal, lang))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Button
      Button(
        onClick = onViewDetails,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_view_tour_${tour.tourId}")
      ) {
        Icon(
          imageVector = Icons.Default.Visibility,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = L10n.viewDetails(lang),
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun CostMiniChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.padding(vertical = 2.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(12.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
