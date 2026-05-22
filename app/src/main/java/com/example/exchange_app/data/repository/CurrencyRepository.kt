package com.example.exchange_app.data.repository

import android.content.Context
import com.example.exchange_app.data.remote.ExchangeRateApi
import com.example.exchange_app.utils.NetworkHelper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.model.RateModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrencyRepository(
    private val api: ExchangeRateApi,
    private val context: Context,
    private val networkHelper: NetworkHelper,
    private val securityManager: SecurityManager
) {
    private val historicalFileName = "historical_rates.txt"
    private val latestFileName = "latest_rates.txt"
    private val followedFileName = "followed.txt"
    private val syncInfoFileName = "sync_info.txt"

    companion object {
        private val _syncSignal = MutableStateFlow(0)
        val syncSignal: StateFlow<Int> = _syncSignal.asStateFlow()
        
        fun notifySync() {
            _syncSignal.value += 1
        }
    }

    suspend fun fetchAndSaveHistoricalRates(baseCode: String): Boolean {
        val response = fetchRatesFromApi(baseCode) ?: return false
        
        val now = Date()
        val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val historicalFile = File(context.filesDir, historicalFileName)
        
        response.conversionRates.forEach { (targetCurrency, rate) ->
            val line = "$dateOnly,$baseCode,$targetCurrency,$rate\n"
            historicalFile.appendText(line)
        }
        return true
    }

    suspend fun fetchAndSaveLatestRates(baseCode: String): Boolean {
        val response = fetchRatesFromApi(baseCode) ?: return false
        
        val now = Date()
        val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val fullDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(now)
        val latestFile = File(context.filesDir, latestFileName)
        
        val latestContent = StringBuilder()
        response.conversionRates.forEach { (targetCurrency, rate) ->
            val line = "$dateOnly,$baseCode,$targetCurrency,$rate\n"
            latestContent.append(line)
        }
        latestFile.writeText(latestContent.toString())
        saveLastSyncTime(fullDateTime)
        notifySync()
        return true
    }

    private suspend fun fetchRatesFromApi(baseCode: String): com.example.exchange_app.data.remote.HistoricalApiResponse? {
        val apiKey = securityManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            Log.e("Repo", "Błąd: Brak klucza API. Przejdź do ustawień i wpisz klucz.")
            return null
        }
        
        if (!networkHelper.isInternetAvailable()) {
            Log.e("Repo", "Błąd: Brak połączenia z internetem.")
            return null
        }

        return try {
            val response = api.getTodayRates(apiKey, baseCode)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("Repo", "Błąd API: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("Repo", "Wyjątek podczas pobierania danych: ${e.message}")
            null
        }
    }

    private fun saveLastSyncTime(time: String) {
        try {
            File(context.filesDir, syncInfoFileName).writeText(time)
        } catch (e: Exception) {
            Log.e("Repo", "Błąd zapisu czasu: ${e.message}")
        }
    }

    fun getLastSyncTime(): String {
        val file = File(context.filesDir, syncInfoFileName)
        return if (file.exists()) file.readText().trim() else "Nigdy"
    }

    fun isOnline(): Boolean = networkHelper.isInternetAvailable()

    fun getLatestRates(): List<RateModel> {
        val file = File(context.filesDir, latestFileName)
        if (!file.exists()) return emptyList()
        
        val latestRates = parseRatesFromFile(file)
        val historicalRates = getHistoricalRates()
        
        return latestRates.map { latest ->
            val previousRate = historicalRates
                .filter { 
                    it.baseCurrency == latest.baseCurrency && 
                    it.targetCurrency == latest.targetCurrency && 
                    it.date < latest.date 
                }
                .maxByOrNull { it.date }
            
            val trend = if (previousRate != null && previousRate.rate != 0.0) {
                ((latest.rate - previousRate.rate) / previousRate.rate) * 100
            } else null
            
            latest.copy(trend = trend)
        }
    }

    fun getHistoricalRates(): List<RateModel> {
        val file = File(context.filesDir, historicalFileName)
        if (!file.exists()) return emptyList()
        return parseRatesFromFile(file)
    }

    fun getHistoryForCurrency(baseCurrency: String, targetCurrency: String, days: Int): List<RateModel> {
        val allHistory = getHistoricalRates()
        val now = System.currentTimeMillis()

        val limit = now - (days.toLong() * 24 * 60 * 60 * 1000) - (24 * 60 * 60 * 1000)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        return allHistory.filter { 
            it.baseCurrency == baseCurrency && 
            it.targetCurrency == targetCurrency && 
            try { 
                val time = sdf.parse(it.date)?.time ?: 0L
                time >= limit 
            } catch (e: Exception) { false }
        }.sortedBy { it.date }
    }

    private fun parseRatesFromFile(file: File): List<RateModel> {
        return try {
            file.readLines().mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    RateModel(
                        date = parts[0],
                        baseCurrency = parts[1],
                        targetCurrency = parts[2],
                        rate = parts[3].toDoubleOrNull() ?: 0.0
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getFollowedCurrencies(): List<String> {
        val file = File(context.filesDir, followedFileName)
        if (!file.exists()) return emptyList()
        return try {
            file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllAvailableCurrencies(): List<String> {
        val rates = getLatestRates() + getHistoricalRates().takeLast(200)
        return rates.flatMap { listOf(it.baseCurrency, it.targetCurrency) }
            .distinct()
            .sorted()
    }

    fun toggleFollowedCurrency(currencyCode: String) {
        val currentFollowed = getFollowedCurrencies().toMutableList()
        if (currentFollowed.contains(currencyCode)) {
            currentFollowed.remove(currencyCode)
        } else {
            currentFollowed.add(currencyCode)
        }
        File(context.filesDir, followedFileName).writeText(currentFollowed.joinToString("\n"))
    }
}
