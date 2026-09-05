package com.fourDirection.allDirection.page.login

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.R
import com.fourDirection.allDirection.ui.theme.AllDirectionTheme
import com.fourDirection.allDirection.ui.theme.GlowBlue
import kotlinx.coroutines.delay


data class BackgroundLocation(
    val resId: Int,
    val city: String,
    val country: String
)

// These resources are now located in res/drawable-nodpi/ with proper naming
val backgroundImages = listOf(
    BackgroundLocation(R.drawable.loc_usa_ny_1, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_usa_ny_2, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_usa_ny_3, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_1, "TOKYO", "JAPAN"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_2, "TOKYO", "JAPAN"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_3, "TOKYO", "JAPAN")
)

@Composable
fun WelcomePage(
    onSignUpWithGoogle: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentIndex = (currentIndex + 1) % backgroundImages.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image Carousel
        Crossfade(
            targetState = backgroundImages[currentIndex],
            animationSpec = tween(1000),
            label = "BackgroundFade"
        ) { location ->
            Image(
                painter = painterResource(id = location.resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark Overlay for readability and fade-away effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.3f),
                        0.4f to Color.Black.copy(alpha = 0.5f),
                        0.6f to Color.Black.copy(alpha = 0.7f),
                        0.8f to Color.Black
                    )
                )
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // App Logo / Title
            Text(
                text = "All Direction",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Navigate your world with ease and precision.",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Location Text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${backgroundImages[currentIndex].city}, ${backgroundImages[currentIndex].country}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons at the bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sign up with Google Button
                OutlinedButton(
                    onClick = onSignUpWithGoogle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign up with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                // Sign up Button
                Button(
                    onClick = onSignUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowBlue
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text("Sign up", fontSize = 16.sp, color = Color.White)
                }

                // Login Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Already have an account? ", color = Color.White.copy(alpha = 0.8f))
                    TextButton(onClick = onLogin) {
                        Text(
                            text = "Login here",
                            color = GlowBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePagePreview() {
    AllDirectionTheme {
        WelcomePage()
    }
}
