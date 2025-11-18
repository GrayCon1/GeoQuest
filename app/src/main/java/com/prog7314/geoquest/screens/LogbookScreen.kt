package com.prog7314.geoquest.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.prog7314.geoquest.components.cards.LocationCard
import com.prog7314.geoquest.components.overlays.CenteredLoadingIndicator
import com.prog7314.geoquest.data.data.LocationData
import com.prog7314.geoquest.data.model.LocationViewModel
import com.prog7314.geoquest.data.model.UserViewModel
import com.prog7314.geoquest.ui.theme.PROG7314Theme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import com.prog7314.geoquest.components.common.StyledFilterChip

enum class VisibilityFilter {
    ALL, PUBLIC, PRIVATE
}

@Composable
fun LogbookScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    locationViewModel: LocationViewModel = run {
        val context = LocalContext.current
        viewModel(
            factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
                context.applicationContext as android.app.Application
            )
        )
    }
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val locations by locationViewModel.locations.collectAsState()
    val isLoading by locationViewModel.isLoading.collectAsState()
    var selectedVisibilityFilter by remember { mutableStateOf(VisibilityFilter.ALL) }
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(currentUser?.id, selectedVisibilityFilter, fromDate, toDate) {
        currentUser?.id?.let { userId ->
            if (userId.isNotBlank()) {
                val visibility = when (selectedVisibilityFilter) {
                    VisibilityFilter.PUBLIC -> "public"
                    VisibilityFilter.PRIVATE -> "private"
                    VisibilityFilter.ALL -> null
                }
                locationViewModel.loadFilteredUserLocations(userId, fromDate, toDate, visibility)
            }
        }
    }

    LogbookContent(
        navController = navController,
        locations = locations,
        isLoading = isLoading,
        onAddClick = { navController.navigate("add") },
        selectedVisibilityFilter = selectedVisibilityFilter,
        onVisibilityFilterSelected = { selectedVisibilityFilter = it },
        fromDate = fromDate,
        toDate = toDate,
        onDateRangeSelected = { start, end ->
            fromDate = start
            toDate = end
        },
        locationViewModel = locationViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookContent(
    navController: NavController,
    locations: List<LocationData>,
    isLoading: Boolean,
    onAddClick: () -> Unit,
    selectedVisibilityFilter: VisibilityFilter,
    onVisibilityFilterSelected: (VisibilityFilter) -> Unit,
    fromDate: Long?,
    toDate: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    locationViewModel: LocationViewModel? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var deletedLocationIds by remember { mutableStateOf(setOf<String>()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Location"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8F4F8))
                .padding(paddingValues)
        ) {
            Text(
                text = "My Logbook",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Visibility Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibilityFilter.entries.forEach { filter ->
                    StyledFilterChip(
                        label = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = selectedVisibilityFilter == filter,
                        onClick = { onVisibilityFilterSelected(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date Filter Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyledFilterChip(
                    label = "Custom Date",
                    selected = fromDate != null || toDate != null,
                    onClick = { showDatePicker = true }
                )
                StyledFilterChip(
                    label = "Clear Dates",
                    selected = false,
                    onClick = { onDateRangeSelected(null, null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CenteredLoadingIndicator(message = "Loading locations...")
            } else if (locations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No locations found for this filter.\nTap the '+' button to add one!",
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = locations.filter { it.id !in deletedLocationIds },
                        key = { it.id }
                    ) { location ->
                        AnimatedVisibility(
                            visible = location.id !in deletedLocationIds,
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeOut()
                        ) {
                            SwipeToDeleteLocationItem(
                                location = location,
                                onClick = {
                                    val encodedName = URLEncoder.encode(location.name, StandardCharsets.UTF_8.toString())
                                    val encodedDesc = URLEncoder.encode(location.description, StandardCharsets.UTF_8.toString())
                                    navController.navigate("home?lat=${location.latitude}&lng=${location.longitude}&name=$encodedName&desc=$encodedDesc")
                                },
                                onDelete = {
                                    deletedLocationIds = deletedLocationIds + location.id
                                    locationViewModel?.deleteLocation(location.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteLocationItem(
    location: LocationData,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                else -> Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        LocationCard(
            location = location,
            onClick = onClick
        )
    }
}


// Helper function to get the start of the day for a given timestamp
@Suppress("unused")
private fun getStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}


@Preview(showBackground = true)
@Composable
fun LogbookScreenPreview() {
    val mockNavController = rememberNavController()
    val mockLocations = listOf(
        LocationData(id = "1", name = "Eiffel Tower", description = "A visit to the famous landmark in Paris.", dateAdded = System.currentTimeMillis()),
        LocationData(id = "2", name = "Colosseum", description = "Exploring the ancient ruins in Rome.", dateAdded = System.currentTimeMillis() - 86400000)
    )

    PROG7314Theme {
        LogbookContent(
            navController = mockNavController,
            locations = mockLocations,
            isLoading = false,
            onAddClick = {},
            selectedVisibilityFilter = VisibilityFilter.ALL,
            onVisibilityFilterSelected = {},
            fromDate = null,
            toDate = null,
            onDateRangeSelected = { _, _ -> },
            locationViewModel = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogbookScreenEmptyPreview() {
    val mockNavController = rememberNavController()
    PROG7314Theme {
        LogbookContent(
            navController = mockNavController,
            locations = emptyList(),
            isLoading = false,
            onAddClick = {},
            selectedVisibilityFilter = VisibilityFilter.ALL,
            onVisibilityFilterSelected = {},
            fromDate = null,
            toDate = null,
            onDateRangeSelected = { _, _ -> },
            locationViewModel = null
        )
    }
}

