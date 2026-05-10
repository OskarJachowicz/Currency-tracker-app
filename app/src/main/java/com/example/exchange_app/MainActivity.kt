package com.example.exchange_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securityManager = SecurityManager(this)
        val settingsManager = SettingsManager(this)
        setupWorkManager(settingsManager)

        // Odświeżanie danych przy starcie aplikacji (jeśli jest internet i klucz)
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
                Exchange_appApp()
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

        // WorkManager wymaga min. 15 minut
        val finalValue = if (timeUnit == TimeUnit.MINUTES && intervalValue < 15) 15L else intervalValue.toLong()

        val syncRequest = PeriodicWorkRequestBuilder<DailyRateWorker>(finalValue, timeUnit)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCurrencyFetch",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

@PreviewScreenSizes
@Composable
fun Exchange_appApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var selectedCurrency by rememberSaveable { mutableStateOf<String?>(null) }
    var previousDestination by rememberSaveable { mutableStateOf<AppDestinations?>(null) }

    // Obsługa systemowego przycisku powrotu, gdy jesteśmy w szczegółach
    BackHandler(enabled = currentDestination == AppDestinations.DETAILS) {
        currentDestination = previousDestination ?: AppDestinations.HOME
        selectedCurrency = null
    }

    val navSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = CustomYellow.copy(alpha = 0.25f)
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = CustomYellow.copy(alpha = 0.25f)
        )
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                // Wyświetlamy w navbarze tylko te elementy, które NIE są szczegółami
                if (it != AppDestinations.DETAILS) {
                    item(
                        icon = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { 
                            currentDestination = it
                            selectedCurrency = null
                            previousDestination = null
                        },
                        colors = navSuiteItemColors
                    )
                }
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)){
                when (currentDestination) {
                    AppDestinations.HOME -> MainScreen(onCurrencyClick = {
                        selectedCurrency = it
                        previousDestination = AppDestinations.HOME
                        currentDestination = AppDestinations.DETAILS
                    })
                    AppDestinations.FAVORITES -> FavouritesScreen(onCurrencyClick = {
                        selectedCurrency = it
                        previousDestination = AppDestinations.FAVORITES
                        currentDestination = AppDestinations.DETAILS
                    })
                    AppDestinations.DETAILS -> DetailsScreen(currencyCode = selectedCurrency)
                    AppDestinations.SETTINGS -> SettingScreen()
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Dom", R.drawable.ic_home),
    FAVORITES("Obserwowane", R.drawable.ic_favourite),
    DETAILS("Szczegóły", R.drawable.ic_details),
    SETTINGS("Ustawienia", R.drawable.ic_settings),
}