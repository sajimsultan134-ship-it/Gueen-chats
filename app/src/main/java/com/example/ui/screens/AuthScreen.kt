package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WhatsAppGreenLight

enum class AuthTab {
    LOGIN,
    REGISTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLogin: (email: String, pass: String, (Boolean, String) -> Unit) -> Unit,
    onRegister: (email: String, pass: String, name: String, (Boolean, String) -> Unit) -> Unit,
    onPasswordReset: (email: String, (Boolean, String) -> Unit) -> Unit,
    onGoogleSignIn: (context: Context, (Boolean, String) -> Unit) -> Unit,
    onQuickGoogleSignIn: (email: String, displayName: String, (Boolean, String) -> Unit) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    // Form validation states
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }
    var nameTouched by remember { mutableStateOf(false) }

    val isEmailValid = email.isBlank() || (email.contains("@") && email.contains("."))
    val isPasswordValid = password.isBlank() || password.length >= 6
    val isConfirmPasswordValid = confirmPassword.isBlank() || confirmPassword == password

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(chatColors.chatBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // WhatsApp Top Brand Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(chatColors.topBarBackground)
                .padding(top = 16.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = onContinueAsGuest,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .testTag("auth_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(WhatsAppGreenLight.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.drawable.ic_app_icon)
                            .build(),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sajim Chat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Firebase Authentication & Real-Time Sync",
                    color = WhatsAppGreenLight,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        // WhatsApp Auth Tabs (Log In vs Create Account)
        PrimaryTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = chatColors.textPrimary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(activeTab.ordinal),
                    color = WhatsAppGreenLight,
                    height = 3.5.dp
                )
            }
        ) {
            Tab(
                selected = activeTab == AuthTab.LOGIN,
                onClick = {
                    activeTab = AuthTab.LOGIN
                    errorMessage = null
                    infoMessage = null
                },
                modifier = Modifier.testTag("tab_login"),
                text = {
                    Text(
                        text = "LOG IN",
                        fontWeight = if (activeTab == AuthTab.LOGIN) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (activeTab == AuthTab.LOGIN) WhatsAppGreenLight else chatColors.textSecondary
                    )
                }
            )
            Tab(
                selected = activeTab == AuthTab.REGISTER,
                onClick = {
                    activeTab = AuthTab.REGISTER
                    errorMessage = null
                    infoMessage = null
                },
                modifier = Modifier.testTag("tab_register"),
                text = {
                    Text(
                        text = "CREATE ACCOUNT",
                        fontWeight = if (activeTab == AuthTab.REGISTER) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (activeTab == AuthTab.REGISTER) WhatsAppGreenLight else chatColors.textSecondary
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Auth Form Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Animated Error / Info Banners
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFDE8E8))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFC81E1E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC81E1E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(
                    visible = infoMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WhatsAppGreenLight.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = infoMessage ?: "",
                            color = WhatsAppGreenLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Register-specific: Display Name
                if (activeTab == AuthTab.REGISTER) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            nameTouched = true
                        },
                        label = { Text("Display Name (e.g. Sajim Sultan)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreenLight)
                        },
                        isError = nameTouched && displayName.isBlank(),
                        supportingText = if (nameTouched && displayName.isBlank()) {
                            { Text("Display name is required", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreenLight,
                            cursorColor = WhatsAppGreenLight
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailTouched = true
                    },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppGreenLight)
                    },
                    isError = emailTouched && !isEmailValid,
                    supportingText = if (emailTouched && !isEmailValid) {
                        { Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordTouched = true
                    },
                    label = { Text("Password (min 6 characters)") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreenLight)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = passwordTouched && !isPasswordValid,
                    supportingText = if (passwordTouched && !isPasswordValid) {
                        { Text("Password must be at least 6 characters", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreenLight,
                        cursorColor = WhatsAppGreenLight
                    )
                )

                // Register-specific: Confirm Password
                if (activeTab == AuthTab.REGISTER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordTouched = true
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreenLight)
                        },
                        isError = confirmPasswordTouched && !isConfirmPasswordValid,
                        supportingText = if (confirmPasswordTouched && !isConfirmPasswordValid) {
                            { Text("Passwords do not match", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreenLight,
                            cursorColor = WhatsAppGreenLight
                        )
                    )
                }

                // Forgot Password link for Login
                if (activeTab == AuthTab.LOGIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = WhatsAppGreenLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Submit Button
                Button(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        if (activeTab == AuthTab.LOGIN) {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both email and password"
                                return@Button
                            }
                            isLoading = true
                            onLogin(email.trim(), password) { success, msg ->
                                isLoading = false
                                if (!success) errorMessage = msg else infoMessage = msg
                            }
                        } else {
                            if (displayName.isBlank()) {
                                errorMessage = "Please enter your name"
                                return@Button
                            }
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Please enter a valid email address"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }
                            isLoading = true
                            onRegister(email.trim(), password, displayName.trim()) { success, msg ->
                                isLoading = false
                                if (!success) errorMessage = msg else infoMessage = msg
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (activeTab == AuthTab.LOGIN) "Log In" else "Create Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OR Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = chatColors.divider, thickness = 0.5.dp)
                    Text(
                        text = "OR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chatColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = chatColors.divider, thickness = 0.5.dp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Continue with Google Button
                Surface(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        isGoogleLoading = true
                        onGoogleSignIn(context) { success, msg ->
                            isGoogleLoading = false
                            if (!success) {
                                errorMessage = msg
                            } else {
                                infoMessage = msg
                            }
                        }
                    },
                    shape = RoundedCornerShape(25.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, chatColors.divider),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_with_google_button"),
                    enabled = !isLoading && !isGoogleLoading
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = WhatsAppGreenLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = chatColors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Google Account testing: Sajimsultan134@gmail.com
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        isGoogleLoading = true
                        onQuickGoogleSignIn("Sajimsultan134@gmail.com", "Sajim Sultan") { success, msg ->
                            isGoogleLoading = false
                            if (!success) {
                                errorMessage = msg
                            } else {
                                infoMessage = msg
                            }
                        }
                    },
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreenLight.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("google_test_account_button"),
                    enabled = !isLoading && !isGoogleLoading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Account: Sajimsultan134@gmail.com",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WhatsAppGreenLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security notice banner (Strict Compliance with Security Rules)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhatsAppGreenLight.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted & secure. Passwords and auth tokens are never stored in Firestore.",
                        fontSize = 11.sp,
                        color = chatColors.textSecondary,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("auth_guest_button"),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreenLight)
                ) {
                    Text(
                        text = "Continue with Current Session",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var resetStatus by remember { mutableStateOf<String?>(null) }
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address and we'll send you a password reset link:",
                        fontSize = 13.sp,
                        color = chatColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreenLight,
                            cursorColor = WhatsAppGreenLight
                        )
                    )
                    if (resetStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = resetStatus!!,
                            fontSize = 12.sp,
                            color = if (resetStatus!!.contains("sent", ignoreCase = true)) WhatsAppGreenLight else Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank() && resetEmail.contains("@")) {
                            isResetting = true
                            onPasswordReset(resetEmail.trim()) { success, msg ->
                                isResetting = false
                                resetStatus = if (success) "Password reset link sent to $resetEmail" else msg
                            }
                        } else {
                            resetStatus = "Please enter a valid email address"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreenLight)
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = chatColors.textSecondary)
                }
            }
        )
    }
}
