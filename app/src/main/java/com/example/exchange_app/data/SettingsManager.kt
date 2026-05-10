package com.example.exchange_app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_settings",
        Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_BASE_CURRENCY = "base_currency"
        const val KEY_SYNC_INTERVAL_VALUE = "sync_interval_value"
        const val KEY_SYNC_INTERVAL_UNIT = "sync_interval_unit"
        const val KEY_DECIMAL_PLACES = "decimal_places"
    }

    fun saveDefaultCurrency(currencyCode: String) {
        sharedPreferences.edit { putString(KEY_BASE_CURRENCY, currencyCode) }
    }
    fun getDefaultCurrency(): String {
        return sharedPreferences.getString(KEY_BASE_CURRENCY, "PLN") ?: "PLN"
    }

    fun saveSyncInterval(value: Int, unit: String) {
        sharedPreferences.edit {
            putInt(KEY_SYNC_INTERVAL_VALUE, value)
            putString(KEY_SYNC_INTERVAL_UNIT, unit)
        }
    }
    fun getSyncIntervalValue(): Int = sharedPreferences.getInt(KEY_SYNC_INTERVAL_VALUE, 24)
    fun getSyncIntervalUnit(): String = sharedPreferences.getString(KEY_SYNC_INTERVAL_UNIT, "HOURS") ?: "HOURS"

    fun saveDecimalPlaces(places: Int) {
        sharedPreferences.edit { putInt(KEY_DECIMAL_PLACES, places) }
    }
    fun getDecimalPlaces(): Int = sharedPreferences.getInt(KEY_DECIMAL_PLACES, 2)
}