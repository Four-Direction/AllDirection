package com.fourDirection.allDirection.page.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fourDirection.allDirection.data.PlaceDetail
import com.fourDirection.allDirection.data.PlaceReview
import com.fourDirection.allDirection.data.PlacesRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailSheet(
    placeDetail: PlaceDetail,
    placesRepository: PlacesRepository,
    onDismiss: () -> Unit,
    onAddToTrip: (PlaceDetail) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Photo Carousel
            if (placeDetail.photoMetadatas.isNotEmpty()) {
                PhotoCarousel(placeDetail, placesRepository)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Name and Rating
                Text(
                    text = placeDetail.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    placeDetail.rating?.let { rating ->
                        RatingBar(rating = rating)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$rating (${placeDetail.userRatingsTotal ?: 0} reviews)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Address
                placeDetail.address?.let { addr ->
                    Text(
                        text = addr,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = { 
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { 
                            onAddToTrip(placeDetail)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Add to Trip Plan", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Opening Hours
                if (!placeDetail.openingHours.isNullOrEmpty()) {
                    Text(
                        text = "Opening Hours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    placeDetail.openingHours.forEach { dayText ->
                        Text(
                            text = dayText,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Reviews
                if (placeDetail.reviews.isNotEmpty()) {
                    Text(
                        text = "Recent Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    placeDetail.reviews.forEach { review ->
                        ReviewItem(review)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCarousel(placeDetail: PlaceDetail, repository: PlacesRepository) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(placeDetail.photoMetadatas) { metadata ->
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(metadata) {
                bitmap = repository.fetchPlacePhoto(metadata)
            }

            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GlowBlue)
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: PlaceReview) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = review.authorName, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = review.relativeTime, fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            RatingBar(rating = review.rating, size = 14.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.text,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RatingBar(rating: Double, size: Dp = 18.dp) {
    Row {
        repeat(5) { index ->
            val starIndex = index + 1
            val icon = when {
                rating >= starIndex -> Icons.Default.Star
                rating >= starIndex - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Default.StarOutline
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(size)
            )
        }
    }
}
