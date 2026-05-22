package com.example.exchange_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.lifecycleScope
import com.example.exchange_app.ui.theme.Exchange_appTheme

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.exchange_app.data.SecurityManager
import com.example.exchange_app.data.SettingsManager
import com.example.exchange_app.data.repository.CurrencyRepository
import com.example.exchange_app.ui.screens.DetailsScreen
import com.example.exchange_app.ui.screens.FavouritesScreen
import com.example.exchange_app.ui.screens.MainScreen
import com.example.exchange_app.ui.screens.SettingScreen
import com.example.exchange_app.ui.theme.CustomYellow
import com.example.exchange_app.ui.theme.LightNavBarGreen
import com.example.exchange_app.utils.NetworkHelperImpl
import com.example.exchange_app.worker.DailyRateWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.res.Configuration
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.exchange_app.nav.ExchangeAppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securityManager = SecurityManager(this)
        val settingsManager = SettingsManager(this)
        setupWorkManager(settingsManager)

        val repository = CurrencyRepository(
            (applicationContext as CurrencyApplication).api,
            this,
            NetworkHelperImpl(this),
            securityManager
        )
        lifecycleScope.launch {
            if (repository.isOnline() && securityManager.getApiKey() != null) {
                repository.fetchAndSaveLatestRates(settingsManager.getDefaultCurrency())
            }
        }

        enableEdgeToEdge()
        setContent {
            Exchange_appTheme {
                ExchangeAppNavigation()
            }
        }
    }

    private fun setupWorkManager(settings: SettingsManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val intervalValue = settings.getSyncIntervalValue()
        val intervalUnitStr = settings.getSyncIntervalUnit()
        
        val timeUnit = when (intervalUnitStr) {
            "MINUTES" -> TimeUnit.MINUTES
            "HOURS" -> TimeUnit.HOURS
            "DAYS" -> TimeUnit.DAYS
            else -> TimeUnit.HOURS
        }

        val finalValue = if (timeUnit == TimeUnit.MINUTES && intervalValue < 15) 15L else intervalValue.toLong()

        val syncRequest = PeriodicWorkRequestBuilder<DailyRateWorker>(finalValue, timeUnit)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCurrencyFetch",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}
