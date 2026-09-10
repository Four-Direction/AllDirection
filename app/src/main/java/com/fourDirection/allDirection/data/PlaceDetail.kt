package com.fourDirection.allDirection.data

import android.graphics.Bitmap
import com.google.android.libraries.places.api.model.PhotoMetadata

data class PlaceDetail(
    val id: String,
    val name: String,
    val address: String? = null,
    val rating: Double? = null,
    val userRatingsTotal: Int? = null,
    val openingHours: List<String>? = null,
    val isOpenNow: Boolean? = null,
    val photoMetadatas: List<PhotoMetadata> = emptyList(),
    val reviews: List<PlaceReview> = emptyList()
)

data class PlaceReview(
    val authorName: String,
    val rating: Double,
    val text: String,
    val relativeTime: String
)

data class PlacePhoto(
    val bitmap: Bitmap
)
