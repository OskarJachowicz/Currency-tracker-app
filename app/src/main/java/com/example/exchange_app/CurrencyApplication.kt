package com.example.exchange_app

import android.app.Application
import com.example.exchange_app.data.remote.ExchangeRateApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CurrencyApplication : Application() {
    lateinit var api: ExchangeRateApi

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ExchangeRateApi::class.java)
    }
}