package com.prog7314.geoquest.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
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
    MapScreen(userViewModel, locationViewModel, lat, lng, name, desc)
}

@Composable
fun MapScreen(
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

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }

    val isFromLogbook = initialLat != null && initialLng != null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(isFromLogbook) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
        if (isFromLogbook) {
            locationViewModel.clearLocations()
        } else {
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

    LaunchedEffect(currentDeviceLocation.value, isFromLogbook) {
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
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(initialLat!!, initialLng!!), 15f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            currentDeviceLocation.value?.let { location ->
                Circle(
                    center = location,
                    radius = 50.0,
                    fillColor = Color.Blue.copy(alpha = 0.3f),
                    strokeColor = Color.Blue,
                    strokeWidth = 2f
                )
            }
            if (isFromLogbook) {
                Marker(
                    state = rememberMarkerState(position = LatLng(initialLat!!, initialLng!!)),
                    title = initialName,
                    snippet = initialDesc
                )
            } else {
                locations.forEach { locationData ->
                    Marker(
                        state = rememberMarkerState(position = LatLng(locationData.latitude, locationData.longitude)),
                        title = locationData.name,
                        snippet = locationData.description
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
                        Color.White.copy(alpha = 0.9f),
                        CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = Color.Black
                )
            }

            IconButton(
                onClick = { showNotifications = true },
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black
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
                contentAlignment = Alignment.TopCenter
            ) {
                NotificationScreen(navController = rememberNavController())
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


@Composable
fun NotificationOverlay(onDismiss: () -> Unit) {
    val notifications = remember {
        listOf(
            Notification(
                1,
                NotificationType.LOCATION,
                "Location:",
                "Location 1 has been added!",
                Icons.Default.LocationOn
            ),
            Notification(
                2,
                NotificationType.POINTS,
                "Points:",
                "You have earned +5 points",
                Icons.Default.Star
            ),
            Notification(
                3,
                NotificationType.LOCATION,
                "Location:",
                "Location 2 has been added!",
                Icons.Default.LocationOn
            ),
            Notification(
                4,
                NotificationType.POINTS,
                "Points:",
                "You have earned +5 points",
                Icons.Default.Star
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(onClick = {}), // Prevent dismiss when clicking inside
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = { /* Handle clear notifications */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, Color(0xFF4A90E2))
                ) {
                    Text(
                        text = "Clear Notification",
                        color = Color(0xFF4A90E2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(notification = notification)
                    }
                }
            }
        }
    }
}
