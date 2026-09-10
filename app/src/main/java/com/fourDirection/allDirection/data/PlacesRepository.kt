package com.fourDirection.allDirection.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await

class PlacesRepository(private val context: Context) {
    private var _placesClient: PlacesClient? = null

    private fun getClient(): PlacesClient? {
        if (_placesClient == null && Places.isInitialized()) {
            _placesClient = Places.createClient(context)
        }
        return _placesClient
    }

    suspend fun fetchPlaceDetails(placeId: String): PlaceDetail? {
        val client = getClient() ?: return null

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.RATING,
            Place.Field.USER_RATING_COUNT,
            Place.Field.OPENING_HOURS,
            Place.Field.PHOTO_METADATAS,
            Place.Field.REVIEWS
        )

        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        return try {
            val response = client.fetchPlace(request).await()
            val place = response.place
            Log.d("PlacesRepository", "Successfully fetched details for ${place.displayName}")
            
            PlaceDetail(
                id = place.id ?: placeId,
                name = place.displayName ?: "Unknown",
                address = place.formattedAddress,
                rating = place.rating,
                userRatingsTotal = place.userRatingCount,
                openingHours = place.openingHours?.weekdayText,
                isOpenNow = null, // Calculating isOpen requires a separate API call or local logic
                photoMetadatas = place.photoMetadatas ?: emptyList(),
                reviews = place.reviews?.map { review ->
                    PlaceReview(
                        authorName = review.authorAttribution.name ?: "Anonymous",
                        rating = review.rating ?: 0.0,
                        text = review.text ?: "",
                        relativeTime = review.relativePublishTimeDescription ?: ""
                    )
                } ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error fetching place details: ${e.message}", e)
            null
        }
    }

    suspend fun fetchPlacePhoto(photoMetadata: PhotoMetadata): Bitmap? {
        val client = getClient() ?: return null

        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(1000)
            .setMaxHeight(1000)
            .build()

        return try {
            val response = client.fetchPhoto(photoRequest).await()
            response.bitmap
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error fetching photo: ${e.message}", e)
            null
        }
    }

    suspend fun searchForPlaceId(name: String, latitude: Double, longitude: Double): String? {
        val client = getClient() ?: return null

        val center = LatLng(latitude, longitude)
        val circularBounds = CircularBounds.newInstance(center, 500.0) // 500 meter radius

        val request = SearchByTextRequest.builder(name, listOf(Place.Field.ID))
            .setLocationBias(circularBounds)
            .setMaxResultCount(1)
            .build()

        return try {
            val response = client.searchByText(request).await()
            response.places.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error searching for place ID: ${e.message}", e)
            null
        }
    }
}
