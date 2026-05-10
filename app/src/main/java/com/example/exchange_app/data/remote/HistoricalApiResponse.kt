package com.example.exchange_app.data.remote

import com.google.gson.annotations.SerializedName

data class HistoricalApiResponse(
    @SerializedName("result") val result: String,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double>
)