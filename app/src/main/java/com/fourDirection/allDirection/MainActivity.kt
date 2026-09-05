package com.fourDirection.allDirection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.fourDirection.allDirection.data.Auth
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.page.login.LoginPage
import com.fourDirection.allDirection.page.login.RegisterPage
import com.fourDirection.allDirection.page.login.WelcomePage
import com.fourDirection.allDirection.ui.theme.AllDirectionTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

enum class Screen {
    Welcome, Register, Login, Home
}

class MainActivity : ComponentActivity() {
    private lateinit var authManager: Auth
    private lateinit var userRepository: UserRepository

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = Auth(this)
        userRepository = UserRepository()

        enableEdgeToEdge()
        setContent {
            AllDirectionTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (authManager.currentUser != null) Screen.Home else Screen.Welcome) 
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.Welcome -> {
                            WelcomePage(
                                onSignUpWithGoogle = { startGoogleSignIn() },
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
                            HomeScreen(
                                userName = authManager.currentUser?.displayName ?: "User",
                                onSignOut = {
                                    authManager.signOut()
                                    currentScreen = Screen.Welcome
                                },
                                modifier = Modifier.padding(innerPadding)
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

@Composable
fun HomeScreen(userName: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, $userName!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}
