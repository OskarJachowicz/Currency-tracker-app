package com.example.exchange_app.data.model


data class RateModel(
    val date: String,
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val trend: Double? = null
)
