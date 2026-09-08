package com.fourDirection.allDirection.page.login

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import com.fourDirection.allDirection.data.BackgroundLocation
import com.fourDirection.allDirection.data.backgroundImages
import com.fourDirection.allDirection.ui.theme.AllDirectionTheme
import com.fourDirection.allDirection.ui.theme.GlowBlue
import kotlinx.coroutines.delay


@Composable
fun WelcomePage(
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
                .safeDrawingPadding()
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
                // Login Button
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text("Login", fontSize = 16.sp, color = Color.White)
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
