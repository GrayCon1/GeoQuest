package com.prog7314.geoquest.screens


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.navigation.NavController

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Logbook : Screen("logbook", "Logbook", Icons.Filled.MenuBook)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Login : Screen("login", "Login", Icons.Filled.MoreHoriz)
    object Register : Screen("register", "Register", Icons.Filled.MoreHoriz)

    object Add : Screen("add", "Add", Icons.Filled.AddCircle)

    object NavigationScreen : Screen("navigation_screen", "Navigation", Icons.Filled.MoreHoriz)
}




