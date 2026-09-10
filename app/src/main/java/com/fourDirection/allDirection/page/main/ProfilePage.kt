package com.fourDirection.allDirection.page.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.util.Base64
import com.fourDirection.allDirection.ui.theme.GlowBlue

@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    userName: String = "User",
    userEmail: String = "",
    userPhotoUrl: String = "",
    onAccountClick: () -> Unit = {},
    onConnectionsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Profile Picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (userPhotoUrl.isNotEmpty()) {
                    val imageModel = remember(userPhotoUrl) {
                        if (userPhotoUrl.startsWith("http")) {
                            userPhotoUrl
                        } else {
                            try {
                                Base64.decode(userPhotoUrl, Base64.DEFAULT)
                            } catch (e: Exception) {
                                userPhotoUrl
                            }
                        }
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(60.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = userName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            if (userEmail.isNotEmpty()) {
                Text(
                    text = userEmail,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Account and Settings Buttons
            ProfileOption(
                icon = Icons.Default.AccountBox,
                title = "Account",
                onClick = onAccountClick
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            ProfileOption(
                icon = Icons.Default.Group,
                title = "Connections",
                onClick = onConnectionsClick
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ProfileOption(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlowBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
