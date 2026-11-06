package com.example.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.SavedTripEntity
import com.example.data.repository.SavedTripsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SavedTripsViewModel(private val repository: SavedTripsRepository) : ViewModel() {

  init {
    viewModelScope.launch {
      repository.seedInitialDataIfNeeded()
    }
  }

  private val _filterStatus = MutableStateFlow("all")
  val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

  val tripsState: StateFlow<List<SavedTripEntity>> = combine(
    repository.allSavedTrips,
    _filterStatus
  ) { trips, status ->
    if (status == "all") {
      trips
    } else {
      trips.filter { it.status.equals(status, ignoreCase = true) }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun setFilter(status: String) {
    _filterStatus.value = status
  }

  fun deleteTrip(id: String) {
    viewModelScope.launch {
      repository.deleteTripById(id)
    }
  }

  fun saveTrip(
    title: String,
    destinationName: String,
    startDate: String?,
    endDate: String?,
    status: String,
    estimatedBudget: Double?,
    actualExpenses: Double?,
    memberCount: Int,
    notes: String?
  ) {
    viewModelScope.launch {
      val entity = SavedTripEntity(
        id = "saved-" + UUID.randomUUID().toString().take(8),
        title = title,
        destinationName = destinationName,
        startDate = startDate,
        endDate = endDate,
        status = status,
        estimatedBudget = estimatedBudget,
        actualExpenses = actualExpenses,
        memberCount = memberCount,
        notes = notes
      )
      repository.insertTrip(entity)
    }
  }

  class Factory(private val repository: SavedTripsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return SavedTripsViewModel(repository) as T
    }
  }
}
