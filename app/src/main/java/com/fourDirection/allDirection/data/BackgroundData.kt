package com.fourDirection.allDirection.data

import com.fourDirection.allDirection.R

data class BackgroundLocation(
    val resId: Int,
    val city: String,
    val country: String
)

val backgroundImages = listOf(
    BackgroundLocation(R.drawable.loc_usa_ny_1, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_usa_ny_2, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_usa_ny_3, "NEW YORK", "USA"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_1, "TOKYO", "JAPAN"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_2, "TOKYO", "JAPAN"),
    BackgroundLocation(R.drawable.loc_japan_tokyo_3, "TOKYO", "JAPAN"),
    BackgroundLocation(R.drawable.loc_italy_rome_1, "ROME", "ITALY"),
    BackgroundLocation(R.drawable.loc_italy_rome_2, "ROME", "ITALY"),
    BackgroundLocation(R.drawable.loc_italy_rome_3, "ROME", "ITALY"),
    BackgroundLocation(R.drawable.loc_france_paris_1, "PARIS", "FRANCE"),
    BackgroundLocation(R.drawable.loc_france_paris_2, "PARIS", "FRANCE"),
    BackgroundLocation(R.drawable.loc_france_paris_3, "PARIS", "FRANCE"),
    BackgroundLocation(R.drawable.loc_england_london_1, "LONDON", "ENGLAND"),
    BackgroundLocation(R.drawable.loc_england_london_2, "LONDON", "ENGLAND"),
    BackgroundLocation(R.drawable.loc_england_london_3, "LONDON", "ENGLAND"),
    BackgroundLocation(R.drawable.loc_thailand_bangkok_1, "BANGKOK", "THAILAND"),
    BackgroundLocation(R.drawable.loc_thailand_bangkok_2, "BANGKOK", "THAILAND"),
    BackgroundLocation(R.drawable.loc_thailand_bangkok_3, "BANGKOK", "THAILAND"),
    BackgroundLocation(R.drawable.loc_netherlands_amsterdam_1, "AMSTERDAM", "NETHERLANDS"),
    BackgroundLocation(R.drawable.loc_netherlands_amsterdam_2, "AMSTERDAM", "NETHERLANDS"),
    BackgroundLocation(R.drawable.loc_netherlands_amsterdam_3, "AMSTERDAM", "NETHERLANDS")
)
