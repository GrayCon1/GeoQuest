package com.prog7314.geoquest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prog7314.geoquest.ui.theme.BackgroundLight
import com.prog7314.geoquest.ui.theme.PrimaryBlue
import com.prog7314.geoquest.ui.theme.TextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.prog7314.geoquest.data.model.UserViewModel
import com.prog7314.geoquest.screens.AddScreen
import com.prog7314.geoquest.screens.HomeScreen
import com.prog7314.geoquest.screens.LogbookScreen
import com.prog7314.geoquest.screens.LoginScreen
import com.prog7314.geoquest.screens.RegisterScreen
import com.prog7314.geoquest.screens.Screen
import com.prog7314.geoquest.screens.SettingsScreen
import com.prog7314.geoquest.data.preferences.LanguagePreferences
import com.prog7314.geoquest.ui.theme.PROG7314Theme
import com.prog7314.geoquest.utils.LocaleHelper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val base = newBase ?: return super.attachBaseContext(null)
        val languageCode = LanguagePreferences.getLanguage(base)
        val updatedContext = LocaleHelper.setLocale(base, languageCode)
        super.attachBaseContext(updatedContext)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            PROG7314Theme {
                Main()
            }
        }
    }
}

@Composable
fun Main() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Auto sign-in on app start
    LaunchedEffect(Unit) {
        userViewModel.autoSignIn()
    }

    val homeRoute = "home?lat={lat}&lng={lng}&name={name}&desc={desc}"
    val routesWithNavBar = listOf(homeRoute, "logbook", "add", "settings")
    val showNavBar = routesWithNavBar.any { currentRoute?.startsWith(it.split('?')[0]) ?: false }

    Scaffold(
        bottomBar = {
            if (showNavBar) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "login" // Start with login
            ) {
                // Auth screens
                composable(Screen.Login.route) {
                    LoginScreen(navController = navController, userViewModel = userViewModel)
                }
                composable(Screen.Register.route) {
                    RegisterScreen(navController = navController, userViewModel = userViewModel)
                }

                // Main app screens with navigation bar
                composable(
                    homeRoute,
                    arguments = listOf(
                        navArgument("lat") { type = NavType.StringType; nullable = true },
                        navArgument("lng") { type = NavType.StringType; nullable = true },
                        navArgument("name") { type = NavType.StringType; nullable = true },
                        navArgument("desc") { type = NavType.StringType; nullable = true }
                    )
                ) { backStackEntry ->
                    val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                    val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
                    val name = backStackEntry.arguments?.getString("name")
                        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                    val desc = backStackEntry.arguments?.getString("desc")
                        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                    HomeScreen(navController, userViewModel, lat, lng, name, desc)
                }
                composable(Screen.Logbook.route) {
                    LogbookScreen(navController = navController, userViewModel = userViewModel)
                }
                composable(Screen.Add.route) {
                    AddScreen(navController = navController, userViewModel = userViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController, userViewModel = userViewModel)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val homeRoute = "home?lat={lat}&lng={lng}&name={name}&desc={desc}"
    val isOnHome = currentRoute?.startsWith("home") == true
    val middleScreen = if (isOnHome) Screen.Add else Screen.Home

    val navigationItems = listOf(
        Screen.Logbook,
        middleScreen,
        Screen.Settings
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        navigationItems.forEachIndexed { index, item ->
            val isMiddleItem = index == 1
            val route = if (item.route.contains("?")) {
                item.route.substringBefore('?')
            } else {
                item.route
            }

            val isSelected = if (isMiddleItem) {
                currentRoute?.startsWith("home") == true || currentRoute?.startsWith("add") == true
            } else {
                currentRoute?.startsWith(route) == true
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { 
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium
                    ) 
                },
                selected = isSelected,
                onClick = {
                    // Determine the target route
                    val targetRoute = when {
                        item == Screen.Home -> "home"
                        item == Screen.Add -> "add"
                        item == Screen.Logbook -> "logbook"
                        item == Screen.Settings -> "settings"
                        else -> route
                    }

                    // Get current base route (without parameters)
                    val currentBaseRoute = currentRoute?.substringBefore('?') ?: ""

                    // Navigate if clicking a different screen OR if clicking home to clear parameters
                    if (currentBaseRoute != targetRoute || (targetRoute == "home" && currentRoute != "home")) {
                        // For home route, always navigate without parameters to clear any previous location
                        val finalRoute = if (targetRoute == "home") "home" else targetRoute
                        navController.navigate(finalRoute) {
                            // When navigating to logbook, pop any home routes with parameters first
                            if (targetRoute == "logbook" && currentBaseRoute == "home") {
                                popUpTo("home") {
                                    inclusive = false
                                    saveState = false
                                }
                            } else {
                                // Pop up to login to maintain clean back stack
                                popUpTo("login") {
                                    saveState = true
                                }
                            }
                            // Avoid multiple copies of same destination
                            launchSingleTop = true
                            // Don't restore state for Home to ensure fresh map without old pins
                            // Also don't restore state when navigating to logbook to prevent restoring home with params
                            restoreState = targetRoute != "home" && targetRoute != "logbook"
                        }
                    }
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    indicatorColor = PrimaryBlue.copy(alpha = 0.1f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
