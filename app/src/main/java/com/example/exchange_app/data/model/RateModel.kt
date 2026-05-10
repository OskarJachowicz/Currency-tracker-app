package com.example.exchange_app.data.model

/**
 * Model reprezentujący pojedynczy wpis kursu walutowego.
 * [trend] przechowuje procentową zmianę w stosunku do poprzedniego dnia.
 */
data class RateModel(
    val date: String,
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val trend: Double? = null
)
