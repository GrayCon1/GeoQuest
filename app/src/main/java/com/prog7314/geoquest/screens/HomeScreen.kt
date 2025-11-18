package com.prog7314.geoquest.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.prog7314.geoquest.data.repo.UserRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.prog7314.geoquest.data.model.LocationViewModel
import com.prog7314.geoquest.data.model.UserViewModel

@Preview
@Composable
fun HomeScreenPreview() {
    // Preview removed - requires NavController and ViewModel
}

@Composable
fun HomeScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    lat: Double?,
    lng: Double?,
    name: String?,
    desc: String?
) {
    val locationViewModel: LocationViewModel = viewModel()
    MapScreen(navController, userViewModel, locationViewModel, lat, lng, name, desc)
}

@Composable
fun MapScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    locationViewModel: LocationViewModel,
    initialLat: Double?,
    initialLng: Double?,
    initialName: String?,
    initialDesc: String?
) {
    val context = LocalContext.current
    val currentDeviceLocation = remember { mutableStateOf<LatLng?>(null) }
    val locations by locationViewModel.locations.collectAsState()
    val isOnline by locationViewModel.isOnline.collectAsState()
    val unsyncedCount by locationViewModel.unsyncedCount.collectAsState()
    val syncStatus by locationViewModel.syncStatus.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()

    // Cache for user information (userId -> username)
    val userInfoCache = remember { mutableStateMapOf<String, String>() }
    val userRepo = remember { UserRepo(context) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }

    val isFromLogbook = initialLat != null && initialLng != null

    // Fetch user information for locations from other users
    LaunchedEffect(locations, currentUser?.id) {
        val userIdsToFetch = locations
            .map { it.userId }
            .distinct()
            .filter { it != currentUser?.id && it.isNotEmpty() && !userInfoCache.containsKey(it) }
        
        userIdsToFetch.forEach { userId ->
            launch(Dispatchers.IO) {
                try {
                    val result = userRepo.getUserProfile(userId)
                    result.onSuccess { userData ->
                        userInfoCache[userId] = userData.username.ifEmpty { userData.name.ifEmpty { userId } }
                    }.onFailure {
                        // If fetch fails, use userId as fallback
                        userInfoCache[userId] = userId
                    }
                } catch (e: Exception) {
                    userInfoCache[userId] = userId
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Reset when parameters change - if no parameters, load all locations
    LaunchedEffect(initialLat, initialLng, initialName, initialDesc) {
        if (isFromLogbook) {
            // Show single location from logbook
            locationViewModel.clearLocations()
        } else {
            // No parameters - show all locations
            locationViewModel.loadAllLocations()
        }
    }
    
    // Track route changes to reset when navigating back to home without parameters
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    LaunchedEffect(currentRoute, initialLat, initialLng) {
        // If we're on home route without parameters, ensure we're showing all locations
        if (currentRoute == "home" && !isFromLogbook) {
            locationViewModel.loadAllLocations()
        }
    }

    DisposableEffect(hasLocationPermission) {
        var locationCallback: LocationCallback? = null
        if (hasLocationPermission) {
            locationCallback = startLocationUpdates(context) { location ->
                currentDeviceLocation.value = location
            }
        }
        onDispose {
            locationCallback?.let {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.removeLocationUpdates(it)
            }
        }
    }

    val initialCameraPos = if (isFromLogbook) {
        LatLng(initialLat!!, initialLng!!)
    } else {
        currentDeviceLocation.value ?: LatLng(-33.974273681640625, 18.46971893310547)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCameraPos, 15f)
    }

    LaunchedEffect(currentDeviceLocation.value, initialLat, initialLng) {
        // Only move to current location if no initial position was provided from logbook
        if (!isFromLogbook) {
            currentDeviceLocation.value?.let {
                if (cameraPositionState.position.target != it) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(it, 15f)
                    )
                }
            }
        } else {
            // isFromLogbook is true, so initialLat and initialLng are guaranteed to be non-null
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(initialLat!!, initialLng!!), 15f)
            )
        }
    }

    // Map UI Settings - disable zoom controls, enable gestures
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false, // Remove +/- zoom buttons
            zoomGesturesEnabled = true, // Enable pinch-to-zoom on mobile
            scrollGesturesEnabled = true, // Enable scroll/pan gestures
            rotationGesturesEnabled = true, // Enable rotation gestures
            tiltGesturesEnabled = true, // Enable tilt gestures
            myLocationButtonEnabled = false // We'll use custom location button if needed
        )
    }

    // Animation for pulsing circle effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val circleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circleScale"
    )
    val circleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circleAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            currentDeviceLocation.value?.let { location ->
                // Outer pulsing circle
                Circle(
                    center = location,
                    radius = 80.0 * circleScale,
                    fillColor = Color(0xFF2196F3).copy(alpha = circleAlpha),
                    strokeColor = Color.Transparent,
                    strokeWidth = 0f
                )
                // Middle circle
                Circle(
                    center = location,
                    radius = 60.0,
                    fillColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                    strokeColor = Color.Transparent,
                    strokeWidth = 0f
                )
                // Inner solid circle (current location indicator)
                Circle(
                    center = location,
                    radius = 12.0,
                    fillColor = Color(0xFF2196F3),
                    strokeColor = Color.White,
                    strokeWidth = 3f
                )
            }
            if (isFromLogbook) {
                Marker(
                    state = MarkerState(position = LatLng(initialLat!!, initialLng!!)),
                    title = initialName,
                    snippet = initialDesc
                )
            } else {
                locations.forEach { locationData ->
                    // Check if location is from a different user
                    val isFromOtherUser = currentUser?.id != null && locationData.userId != currentUser?.id
                    val snippet = if (isFromOtherUser) {
                        // Show author username (or userId if not yet fetched)
                        val authorName = userInfoCache[locationData.userId] ?: locationData.userId
                        "Author: $authorName"
                    } else {
                        // Show description for own locations
                        locationData.description
                    }
                    
                    Marker(
                        state = MarkerState(position = LatLng(locationData.latitude, locationData.longitude)),
                        title = locationData.name,
                        snippet = snippet
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { showFilter = true },
                modifier = Modifier
                    .background(
                        Color.White,
                        CircleShape
                    )
                    .size(52.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF2C3E50)
                )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { showNotifications = true },
                modifier = Modifier
                    .background(
                        Color.White,
                        CircleShape
                    )
                    .size(52.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF2C3E50)
                )
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Sync Status Indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
                .clickable { if (unsyncedCount > 0) locationViewModel.syncNow() },
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Star else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isOnline) {
                        if (unsyncedCount > 0) "Tap to sync ($unsyncedCount items)" else "Online"
                    } else {
                        "Offline mode"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Sync Status Message
        syncStatus?.let { status ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable { locationViewModel.clearSyncStatus() },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2C3E50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showFilter) {
            FilterScreen(
                onDismiss = { showFilter = false },
                onApply = { type, fromDate, toDate ->
                    // Filter locations based on selected criteria
                    val currentUser = userViewModel.currentUser.value
                    currentUser?.id?.let { userId ->
                        locationViewModel.loadFilteredUserLocations(
                            userId,
                            fromDate,
                            toDate,
                            if (type.lowercase() == "all") null else type.lowercase()
                        )
                    }
                }
            )
        }

        if (showNotifications) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = { showNotifications = false }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable(enabled = false) { }
                ) {
                    NotificationScreen(
                        navController = navController,
                        userViewModel = userViewModel
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    context: Context,
    onLocationReceived: (LatLng) -> Unit
): LocationCallback {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000 // 5 seconds interval
    )
        .setMinUpdateIntervalMillis(2000) // 2 seconds fastest interval
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                onLocationReceived(LatLng(it.latitude, it.longitude))
            }
        }
    }

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    return locationCallback
}
