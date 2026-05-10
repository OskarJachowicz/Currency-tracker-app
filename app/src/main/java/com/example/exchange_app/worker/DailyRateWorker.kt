package com.example.exchange_app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.exchange_app.CurrencyApplication
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.utils.NetworkHelperImpl
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyRateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("Worker", "WorkManager rozpoczął pracę w tle (dane historyczne)...")

        return try {
            val app = applicationContext as CurrencyApplication
            val networkHelper = NetworkHelperImpl(applicationContext)
            val securityManager = SecurityManager(applicationContext)
            val repository = CurrencyRepository(app.api, applicationContext, networkHelper, securityManager)

            repository.fetchAndSaveHistoricalRates("PLN")
            repository.fetchAndSaveHistoricalRates("USD")
            repository.fetchAndSaveHistoricalRates("EUR")

            // --- testowanie workera ---
            val testSdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val testTimestamp = testSdf.format(Date())
            val testFile = File(applicationContext.filesDir, "worker_test_$testTimestamp.txt")
            testFile.writeText("Worker wykonany pomyślnie o: $testTimestamp")
            Log.d("Worker", "Utworzono plik testowy: ${testFile.absolutePath}")
            // -----------------------------

            Log.d("Worker", "WorkManager zakończył sukcesem!")
            Result.success()
        } catch (e: Exception) {
            Log.e("Worker", "WorkManager napotkał błąd: ${e.message}")
            Result.retry()
        }
    }
}
