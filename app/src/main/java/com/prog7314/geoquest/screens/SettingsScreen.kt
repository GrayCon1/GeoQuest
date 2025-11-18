package com.prog7314.geoquest.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.prog7314.geoquest.components.overlays.CenteredLoadingIndicator
import com.prog7314.geoquest.components.textfields.StyledTextField
import com.prog7314.geoquest.data.model.UserViewModel
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.fragment.app.FragmentActivity
import com.prog7314.geoquest.components.common.BiometricAuthHelper
import com.prog7314.geoquest.data.preferences.BiometricPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.prog7314.geoquest.data.data.UserData

@Composable
fun SettingsScreen(navController: NavController, userViewModel: UserViewModel) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    val showGuestUpgradePrompt by userViewModel.showGuestUpgradePrompt.collectAsState()
    val isGuestMode by userViewModel.isGuestMode.collectAsState()
    val isConnected by userViewModel.isConnected.collectAsState()
    val context = LocalContext.current
    var isSigningOut by remember { mutableStateOf(false) }

    // Redirect only if truly logged out (null user and not guest mode)
    LaunchedEffect(currentUser, isSigningOut, isGuestMode) {
        if (currentUser == null && !isGuestMode) {
            val message = if (isSigningOut) {
                "Signed out successfully"
            } else {
                "Please log in to access settings"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            if (message == "Profile updated successfully") {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            userViewModel.clearError()
        }
    }

    val user = currentUser
    if (user == null && isGuestMode) {
        // Guest user placeholder data (optional)
        SettingsGuestContent(
            showUpgrade = showGuestUpgradePrompt,
            isConnected = isConnected,
            onLogin = { navController.navigate("login") },
            onRegister = { navController.navigate("register") },
            onDismiss = { userViewModel.dismissGuestUpgradePrompt() }
        )
        return
    } else if (user == null) {
        CenteredLoadingIndicator(message = "Loading user data...")
        return
    }

    SettingsContent(
        user = user,
        isLoading = isLoading,
        showGuestUpgradePrompt = showGuestUpgradePrompt && isGuestMode,
        isConnected = isConnected,
        onUpgradeLogin = { navController.navigate("login") },
        onUpgradeRegister = { navController.navigate("register") },
        onDismissUpgrade = { userViewModel.dismissGuestUpgradePrompt() },
        onUpdateUser = { userData, newPassword, currentPassword ->
            userViewModel.updateUser(userData, newPassword, currentPassword)
        },
        onSignOut = {
            isSigningOut = true
            userViewModel.logoutUser()
        }
    )
}

@Composable
private fun UpgradePromptCard(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF3E0)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, Color(0xFFF0D27A))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isConnected) "Online now – unlock syncing & achievements!" else "Offline – prompt will enable when online.",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7A5C15),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                    tint = Color(0xFF7A5C15)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Create an account to save progress, restore on any device, and receive location alerts.",
                color = Color(0xFF8B6F1C),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onLogin,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE9B00))
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Login", color = Color.White, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onRegister,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                ) {
                    Icon(Icons.Default.AppRegistration, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Register", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SettingsGuestContent(
    showUpgrade: Boolean,
    isConnected: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F4F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (showUpgrade) {
                UpgradePromptCard(
                    isConnected = isConnected,
                    onLogin = onLogin,
                    onRegister = onRegister,
                    onDismiss = onDismiss,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Guest Mode", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2C3E50))
                    Spacer(Modifier.height(8.dp))
                    Text("You're exploring as a guest. Some features are limited.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onLogin, enabled = isConnected) { Text("Login") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRegister, enabled = isConnected) { Text("Register") }
                }
            }
        }
    }
}

@Composable
fun SettingsContent(
    user: UserData,
    isLoading: Boolean,
    showGuestUpgradePrompt: Boolean,
    isConnected: Boolean,
    onUpgradeLogin: () -> Unit,
    onUpgradeRegister: () -> Unit,
    onDismissUpgrade: () -> Unit,
    onUpdateUser: (UserData, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }
    var isBiometricEnabled by remember { mutableStateOf(BiometricPreferences.isBiometricEnabled(context)) }

    val languages = listOf("English", "Spanish", "French", "German", "Portuguese")

    // Format date for display
    val formattedDate = user.dateJoined.let { timestamp ->
        val date = Date(timestamp)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    // Insert upgrade prompt at top if needed
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F4F8))
    ) {
        if (showGuestUpgradePrompt) {
            UpgradePromptCard(
                isConnected = isConnected,
                onLogin = onUpgradeLogin,
                onRegister = onUpgradeRegister,
                onDismiss = onDismissUpgrade,
                modifier = Modifier.padding(24.dp)
            )
        }
        // Existing content container moved inside Column
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB0B0B0))
                            .border(2.dp, Color(0xFF2C3E50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF2C3E50)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // User Info Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "User",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C3E50),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Date Created",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C3E50),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = user.name,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = formattedDate,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Validation Error Message
                if (validationError.isNotEmpty()) {
                    Text(
                        text = validationError,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }




                // Non-editable Email Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Email",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.email,
                            fontSize = 16.sp,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }

                // Editable Username Field
                StyledTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        validationError = ""
                    },
                    label = "Username",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Current Password Field
                StyledTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        validationError = ""
                    },
                    label = "Current Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // New Password Field
                StyledTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        validationError = ""
                    },
                    label = "New Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Language Dropdown
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    OutlinedTextField(
                        value = selectedLanguage,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Language", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLanguageDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { isLanguageDropdownExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "Language Options",
                                    tint = Color.Gray
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = isLanguageDropdownExpanded,
                        onDismissRequest = { isLanguageDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        languages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language) },
                                onClick = {
                                    selectedLanguage = language
                                    isLanguageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Biometric Authentication Toggle
                if (isBiometricAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric",
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Biometric Authentication",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2C3E50)
                                    )
                                    Text(
                                        text = if (isBiometricEnabled) "Enabled" else "Disabled",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricAuthHelper.authenticate(
                                                activity = activity,
                                                onSuccess = {
                                                    BiometricPreferences.enableBiometric(context, user.email)
                                                    isBiometricEnabled = true
                                                    Toast.makeText(
                                                        context,
                                                        "Biometric authentication enabled",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onError = { error ->
                                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        BiometricPreferences.disableBiometric(context)
                                        isBiometricEnabled = false
                                        Toast.makeText(
                                            context,
                                            "Biometric authentication disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF64B5F6),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Validation Error Message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sign Out Button
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFFE53935)),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Sign Out",
                            color = Color(0xFFE53935),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            when {
                                currentPassword.isBlank() -> {
                                    validationError = "Current password is required to save changes"
                                }

                                newPassword.isNotBlank() && newPassword.length < 6 -> {
                                    validationError = "New password must be at least 6 characters"
                                }

                                else -> {
                                    if (newPassword.isBlank() && username == user.username) {
                                        validationError = "No changes were made"
                                    } else {
                                        onUpdateUser(
                                            user.copy(username = username),
                                            newPassword,
                                            currentPassword
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF64B5F6)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val mockUser = UserData(
        id = "preview-id",
        name = "John Doe",
        username = "johndoe",
        email = "john.doe@example.com",
        dateJoined = System.currentTimeMillis()
    )

    SettingsContent(
        user = mockUser,
        isLoading = false,
        showGuestUpgradePrompt = false,
        isConnected = true,
        onUpgradeLogin = { },
        onUpgradeRegister = { },
        onDismissUpgrade = { },
        onUpdateUser = { _, _, _ ->
            // Mock function for preview
        },
        onSignOut = {}
    )
}
