package com.prog7314.geoquest.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prog7314.geoquest.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prog7314.geoquest.components.buttons.GoogleSignInButton
import com.prog7314.geoquest.components.buttons.PrimaryButton
import com.prog7314.geoquest.components.cards.AuthCard
import com.prog7314.geoquest.components.common.DividerWithText
import com.prog7314.geoquest.components.common.ErrorText
import com.prog7314.geoquest.components.common.GoogleAuthHelper
import com.prog7314.geoquest.components.textfields.StyledTextField
import com.prog7314.geoquest.data.data.UserData
import com.prog7314.geoquest.data.model.UserViewModel

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    // Preview removed - requires NavController and ViewModel
}

@Composable
fun RegisterScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    val currentUser by userViewModel.currentUser.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle successful registration
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Toast.makeText(context, context.getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
            navController.navigate("home") {
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

    AuthCard {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.register_to_explore),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        )

        ErrorText(message = validationError)

        StyledTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                validationError = ""
            },
            label = stringResource(R.string.name_surname),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        StyledTextField(
            value = username,
            onValueChange = {
                username = it
                validationError = ""
            },
            label = stringResource(R.string.username),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        StyledTextField(
            value = email,
            onValueChange = {
                email = it
                validationError = ""
            },
            label = stringResource(R.string.email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        StyledTextField(
            value = password,
            onValueChange = {
                password = it
                validationError = ""
            },
            label = stringResource(R.string.password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        StyledTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                validationError = ""
            },
            label = stringResource(R.string.confirm_password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        PrimaryButton(
            text = stringResource(R.string.register),
            onClick = {
                when {
                    fullName.isBlank() -> {
                        validationError = context.getString(R.string.error_enter_full_name)
                    }
                    username.isBlank() -> {
                        validationError = context.getString(R.string.error_enter_username)
                    }
                    email.isBlank() -> {
                        validationError = context.getString(R.string.error_enter_email)
                    }
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        validationError = context.getString(R.string.error_valid_email)
                    }
                    password.isBlank() -> {
                        validationError = context.getString(R.string.error_enter_password)
                    }
                    password.length < 6 -> {
                        validationError = context.getString(R.string.error_password_length)
                    }
                    password != confirmPassword -> {
                        validationError = context.getString(R.string.error_passwords_match)
                    }
                    else -> {
                        val userData = UserData(
                            name = fullName.trim(),
                            username = username.trim(),
                            email = email.trim().lowercase()
                        )
                        userViewModel.registerUser(userData, password.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
            backgroundColor = Color(0xFF2C3E50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        DividerWithText(text = stringResource(R.string.or_register_with))

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(
            onClick = {
                GoogleAuthHelper.signInWithGoogle(context, coroutineScope, userViewModel, useNonce = true)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.already_have_account), color = Color(0xFF212121))
            TextButton(
                onClick = { navController.navigate("login") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(stringResource(R.string.login_now), color = Color(0xFF26C6DA))
            }
        }
    }
}
