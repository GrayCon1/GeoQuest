package com.prog7314.geoquest.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.prog7314.geoquest.data.data.LocationData
import com.prog7314.geoquest.data.repo.LocationRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel : ViewModel() {
    private val locationRepo = LocationRepo()

    private val _locations = MutableStateFlow<List<LocationData>>(emptyList())
    val locations: StateFlow<List<LocationData>> = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addLocation(locationData: LocationData) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = locationRepo.addLocation(locationData)
                result.onSuccess { addedLocation ->
                    // Send notification about the new location
                    notificationRepo.notifyLocationAdded(
                        userId = locationData.userId,
                        locationId = addedLocation.id,
                        locationName = addedLocation.name
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locations.value = locationRepo.getAllLocations().getOrThrow()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserLocations(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locations.value = locationRepo.getUserLocations(userId).getOrThrow()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserLocationsByDateRange(userId: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locations.value = locationRepo.getUserLocationsByDateRange(userId, startDate, endDate).getOrThrow()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFilteredUserLocations(userId: String, startDate: Long?, endDate: Long?, visibility: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locations.value = locationRepo.getFilteredUserLocations(userId, startDate, endDate, visibility).getOrThrow()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLocations() {
        _locations.value = emptyList()
    }
}
