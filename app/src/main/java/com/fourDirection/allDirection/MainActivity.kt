package com.fourDirection.allDirection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.fourDirection.allDirection.data.Auth
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.page.login.LoginPage
import com.fourDirection.allDirection.page.login.RegisterPage
import com.fourDirection.allDirection.page.login.WelcomePage
import com.fourDirection.allDirection.page.main.MainContainer
import com.fourDirection.allDirection.ui.theme.AllDirectionTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.launch

enum class Screen {
    Welcome, Register, Login, Home
}

class MainActivity : ComponentActivity() {
    private val authManager: Auth by lazy { Auth(this) }
    private val userRepository: UserRepository by lazy { UserRepository() }

    private val googleSignInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Places SDK
        val apiKey = "AIzaSyDfx1WnO_cEL7FTITygLYBAO6xFk-iBIoY"
        if (apiKey.isNotEmpty() && !apiKey.contains("YOUR_API_KEY")) {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
        }
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        
        setContent {
            AllDirectionTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (authManager.currentUser != null) Screen.Home else Screen.Welcome) 
                }
                var userName by remember { mutableStateOf("User") }
                var totalDistance by remember { mutableDoubleStateOf(0.0) }
                var period by remember { mutableStateOf("day") }

                LaunchedEffect(currentScreen, authManager.currentUser) {
                    if (currentScreen == Screen.Home) {
                        authManager.currentUser?.uid?.let { uid ->
                            userRepository.updateLastLogin(uid)
                            val userData = userRepository.getUserTravelData(uid)
                            if (userData != null) {
                                userName = userData["name"] as? String ?: "User"
                                totalDistance = (userData["totalDistance"] as? Number)?.toDouble() ?: 0.0
                                
                                // Ensure name_lowercase exists for search
                                if (userData["name_lowercase"] == null && userName != "User") {
                                    lifecycleScope.launch {
                                        userRepository.saveUserToFirestore(authManager.currentUser!!, userName)
                                    }
                                }
                                
                                val createdAt = userData["createdAt"] as? com.google.firebase.Timestamp
                                if (createdAt != null) {
                                    val diffMillis = System.currentTimeMillis() - createdAt.toDate().time
                                    val diffDays = diffMillis / (1000 * 60 * 60 * 24)
                                    period = when {
                                        diffDays >= 365 -> "${diffDays / 365} year"
                                        diffDays >= 30 -> "${diffDays / 30} month"
                                        else -> "${diffDays.coerceAtLeast(1)} day"
                                    }
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        Screen.Welcome -> {
                            WelcomePage(
                                onSignUp = { currentScreen = Screen.Register },
                                onLogin = { currentScreen = Screen.Login }
                            )
                        }
                        Screen.Register -> {
                            RegisterPage(
                                onRegisterClick = { name, email, password, onError ->
                                    lifecycleScope.launch {
                                        val result = authManager.register(name, email, password)
                                        result.onSuccess {
                                            currentScreen = Screen.Home
                                        }.onFailure { e ->
                                            android.util.Log.e("MainActivity", "Registration Error: ${e.message}", e)
                                            onError(e.message ?: "Registration Failed")
                                        }
                                    }
                                },
                                onGoogleRegisterClick = { startGoogleSignIn() },
                                onLoginLinkClick = { currentScreen = Screen.Login }
                            )
                        }
                        Screen.Login -> {
                            LoginPage(
                                onLoginClick = { email, password, onError ->
                                    lifecycleScope.launch {
                                        val result = authManager.login(email, password)
                                        result.onSuccess {
                                            currentScreen = Screen.Home
                                        }.onFailure { e ->
                                            val message = when (e) {
                                                is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                                                is FirebaseAuthInvalidUserException -> "No account found with this email."
                                                else -> e.message ?: "Login Failed"
                                            }
                                            android.util.Log.e("MainActivity", "Login Error: ${e.message}", e)
                                            onError(message)
                                        }
                                    }
                                },
                                onGoogleLoginClick = { startGoogleSignIn() },
                                onRegisterLinkClick = { currentScreen = Screen.Register }
                            )
                        }
                        Screen.Home -> {
                            MainContainer(
                                userName = userName,
                                userEmail = authManager.currentUser?.email ?: "",
                                totalDistance = totalDistance,
                                period = period,
                                onSignOut = {
                                    authManager.signOut()
                                    currentScreen = Screen.Welcome
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = authManager.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(acct: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        lifecycleScope.launch {
            val result = authManager.firebaseAuthWithGoogle(acct)
            result.onSuccess {
                startActivity(Intent(this@MainActivity, MainActivity::class.java))
                finish()
            }.onFailure { e ->
                android.util.Log.e("MainActivity", "Auth Error: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Authentication Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

