package com.prog7314.geoquest.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.prog7314.geoquest.components.buttons.GoogleSignInButton
import com.prog7314.geoquest.components.buttons.PrimaryButton
import com.prog7314.geoquest.components.cards.AuthCard
import com.prog7314.geoquest.components.common.BiometricAuthHelper
import com.prog7314.geoquest.components.common.DividerWithText
import com.prog7314.geoquest.components.common.ErrorText
import com.prog7314.geoquest.components.common.GoogleAuthHelper
import com.prog7314.geoquest.components.textfields.StyledTextField
import com.prog7314.geoquest.data.model.UserViewModel
import com.prog7314.geoquest.data.preferences.BiometricPreferences
import com.prog7314.geoquest.utils.NetworkHelper
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // Preview removed - requires NavController and ViewModel
}

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    val currentUser by userViewModel.currentUser.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    val loginSuccess by userViewModel.loginSuccess.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Check network status
    var isOnline by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isOnline = NetworkHelper.isNetworkAvailable(context)
    }

    // Biometric authentication
    val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }
    val isBiometricEnabled = remember { BiometricPreferences.isBiometricEnabled(context) }
    val savedEmail = remember { BiometricPreferences.getSavedUserEmail(context) }

    // Automatically prompt for biometric if enabled
    LaunchedEffect(Unit) {
        if (isBiometricAvailable && isBiometricEnabled && !savedEmail.isNullOrBlank()) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        // Auto-fill email on successful biometric auth
                        email = savedEmail
                        Toast.makeText(
                            context,
                            "Biometric authentication successful! Please enter your password.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Handle successful login
    LaunchedEffect(loginSuccess) {
        if (loginSuccess && currentUser != null) {
            // Save biometric preference after successful login
            if (isBiometricAvailable && email.isNotBlank()) {
                BiometricPreferences.enableBiometric(context, email.trim().lowercase())
            }
            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Show error message from ViewModel
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            userViewModel.clearError()
        }
    }


    AuthCard(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(40.dp))

        // Offline indicator
        if (!isOnline) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFCC80))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "You're offline - Limited features available",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Welcome to",
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF757575)
        )
        Text(
            text = "GeoQuest",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(40.dp))

        ErrorText(message = validationError)

        StyledTextField(
            value = email,
            onValueChange = {
                email = it
                validationError = ""
            },
            label = "Enter your email",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        StyledTextField(
            value = password,
            onValueChange = {
                password = it
                validationError = ""
            },
            label = "Enter your password",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(Icons.Default.Visibility, contentDescription = "Toggle password")
                }
            }
        )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { /* Handle forgot password */ }) {
                    Text("Forgot Password?", color = Color(0xFF757575))
                }
            }
        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton(
            text = "Login",
            onClick = {
                when {
                    email.isBlank() -> {
                        validationError = "Please enter your email"
                    }
                    password.isBlank() -> {
                        validationError = "Please enter your password"
                    }
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        validationError = "Please enter a valid email address"
                    }
                    else -> {
                        userViewModel.loginUser(email.trim().lowercase(), password)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        DividerWithText(text = "Or Login with")

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(
            onClick = {
                GoogleAuthHelper.signInWithGoogle(context, coroutineScope, userViewModel)
            }
        )

        // Biometric Authentication Option
        if (isBiometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricAuthHelper.authenticate(
                                activity = activity,
                                onSuccess = {
                                    if (!savedEmail.isNullOrBlank()) {
                                        email = savedEmail
                                        Toast.makeText(
                                            context,
                                            "Authentication successful! Please enter your password.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Login",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF64B5F6)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Login with Biometric",
                    fontSize = 14.sp,
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Guest Mode Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    userViewModel.loginAsGuest()
                }
                .padding(vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = "Guest Mode",
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Continue as Guest",
                fontSize = 14.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Access app without signing in",
                fontSize = 11.sp,
                color = Color(0xFFBDBDBD)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? ", color = Color(0xFF212121))
            TextButton(
                onClick = { navController.navigate("register") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Register Now", color = Color(0xFF26C6DA))
            }
        }
    }
}
