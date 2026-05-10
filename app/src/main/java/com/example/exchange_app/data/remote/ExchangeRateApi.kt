package com.example.exchange_app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

//GET https://v6.exchangerate-api.com/v6/YOUR-API-KEY/latest/USD
interface ExchangeRateApi {
    @GET("v6/{apiKey}/latest/{currencyCode}")
    suspend fun getTodayRates(
        @Path("apiKey") apiKey: String,
        @Path("currencyCode") currencyCode: String,
    ): Response<HistoricalApiResponse>
}