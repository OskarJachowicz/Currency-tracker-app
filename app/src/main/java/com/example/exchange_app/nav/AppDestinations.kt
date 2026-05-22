package com.example.exchange_app.nav

import com.example.exchange_app.R

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Dom", R.drawable.ic_home),
    FAVORITES("Obserwowane", R.drawable.ic_favourite),
    DETAILS("Szczegóły", R.drawable.ic_details),
    SETTINGS("Ustawienia", R.drawable.ic_settings),
}